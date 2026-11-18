package ru.mr_hares.realmMode.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final String API = "https://api.modrinth.com/v2/project/";
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final Plugin plugin;
    private final String projectId;
    private final String currentVersion;
    private final String serverMcVersion;
    private final Set<String> serverLoaders;

    private boolean includeBeta;

    public UpdateChecker(Plugin plugin, String projectId) {
        this(plugin, projectId, plugin.getDescription().getVersion(), null, null);
    }

    public UpdateChecker(Plugin plugin, String projectId, String currentVersion,
                         String serverMcVersion, Set<String> serverLoaders) {
        this.plugin = plugin;
        this.projectId = projectId;
        this.currentVersion = currentVersion;
        this.serverMcVersion = serverMcVersion == null ? detectMcVersion() : serverMcVersion;
        this.serverLoaders = serverLoaders == null ? detectLoaders() : serverLoaders;
    }

    public UpdateChecker includeBeta(boolean include) {
        this.includeBeta = include;
        return this;
    }

    public void check(Consumer<Result> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Result result;
            try {
                result = findUpdate();
            } catch (Exception e) {
                result = Result.failed(currentVersion, e.getMessage());
            }

            Result answer = result;
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(answer));
        });
    }

    public void checkAndLog() {
        check(result -> {
            if (result.hasFailed()) {
                plugin.getLogger().warning("Не удалось проверить обновления: " + result.getError());
            } else if (result.isUpdateAvailable()) {
                plugin.getLogger().info("Доступна новая версия " + result.getLatestVersion()
                        + " (у вас " + result.getCurrentVersion() + ")."
                        + " Скачать: " + result.getDownloadUrl());
            } else {
                plugin.getLogger().info("Установлена последняя версия плагина.");
            }
        });
    }

    private Result findUpdate() throws IOException {
        List<RemoteVersion> versions = downloadVersions();

        RemoteVersion best = null;
        for (RemoteVersion version : versions) {
            if (!isAllowedType(version)) continue;
            if (!worksOnOurServer(version)) continue;

            if (best == null || compareVersions(version.number, best.number) > 0) {
                best = version;
            }
        }

        if (best == null || compareVersions(best.number, currentVersion) <= 0) {
            return Result.upToDate(currentVersion);
        }
        return Result.update(currentVersion, best, projectUrl(best));
    }

    private boolean isAllowedType(RemoteVersion version) {
        return includeBeta || "release".equalsIgnoreCase(version.type);
    }

    private boolean worksOnOurServer(RemoteVersion version) {
        if (!matchesLoader(version)) {
            return false;
        }
        for (String mc : version.gameVersions) {
            if (matchesMcVersion(mc)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesLoader(RemoteVersion version) {
        for (String loader : version.loaders) {
            if (serverLoaders.contains(loader.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesMcVersion(String supported) {
        if (supported.equalsIgnoreCase(serverMcVersion)) {
            return true;
        }

        return serverMcVersion.startsWith(supported + ".");
    }

    public static int compareVersions(String left, String right) {
        List<Integer> a = numbersOf(left);
        List<Integer> b = numbersOf(right);

        int size = Math.max(a.size(), b.size());
        for (int i = 0; i < size; i++) {
            int x = i < a.size() ? a.get(i) : 0;
            int y = i < b.size() ? b.get(i) : 0;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }

        boolean aPre = isPrerelease(left);
        boolean bPre = isPrerelease(right);
        if (aPre == bPre) {
            return 0;
        }
        return aPre ? -1 : 1;
    }

    private static List<Integer> numbersOf(String version) {
        String clean = version.toLowerCase(Locale.ROOT).replaceFirst("^v", "");

        int end = 0;
        while (end < clean.length()
                && (Character.isDigit(clean.charAt(end)) || clean.charAt(end) == '.')) {
            end++;
        }
        String core = end == 0 ? clean : clean.substring(0, end);

        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = DIGITS.matcher(core);
        while (matcher.find()) {
            try {
                numbers.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException e) {
                numbers.add(0);
            }
        }
        return numbers;
    }

    private static boolean isPrerelease(String version) {
        return version.toLowerCase(Locale.ROOT).replaceFirst("^v", "").matches(".*[a-z].*");
    }

    private List<RemoteVersion> downloadVersions() throws IOException {
        URL url = new URL(API + projectId + "/version");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", plugin.getName() + "/" + currentVersion);

        int code = connection.getResponseCode();
        if (code != 200) {
            throw new IOException("Modrinth ответил кодом " + code + " (проверь id проекта)");
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } finally {
            connection.disconnect();
        }
        return parseVersions(body.toString());
    }

    private static List<RemoteVersion> parseVersions(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<RemoteVersion> versions = new ArrayList<>(array.size());

        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            RemoteVersion version = new RemoteVersion();
            version.id = text(object, "id");
            version.number = text(object, "version_number");
            version.name = text(object, "name");
            version.type = text(object, "version_type");
            version.changelog = text(object, "changelog");
            version.published = text(object, "date_published");
            version.downloadUrl = primaryFileUrl(object);
            version.loaders = stringList(object, "loaders");
            version.gameVersions = stringList(object, "game_versions");
            versions.add(version);
        }
        return versions;
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static List<String> stringList(JsonObject object, String key) {
        List<String> result = new ArrayList<>();
        if (!object.has(key)) {
            return result;
        }
        for (JsonElement element : object.getAsJsonArray(key)) {
            result.add(element.getAsString());
        }
        return result;
    }

    private static String primaryFileUrl(JsonObject object) {
        if (!object.has("files")) {
            return "";
        }
        String fallback = "";
        for (JsonElement element : object.getAsJsonArray("files")) {
            JsonObject file = element.getAsJsonObject();
            String fileUrl = text(file, "url");
            if (fallback.isEmpty()) {
                fallback = fileUrl;
            }
            if (file.has("primary") && !file.get("primary").isJsonNull()
                    && file.get("primary").getAsBoolean()) {
                return fileUrl;
            }
        }
        return fallback;
    }

    private String projectUrl(RemoteVersion version) {
        return "https://modrinth.com/project/" + projectId + "/version/" + version.id;
    }

    private static String detectMcVersion() {
        String raw = Bukkit.getBukkitVersion();          // "1.20.4-R0.1-SNAPSHOT"
        int dash = raw.indexOf('-');
        return dash > 0 ? raw.substring(0, dash) : raw;
    }

    private static Set<String> detectLoaders() {
        String core = Bukkit.getName().toLowerCase(Locale.ROOT);
        switch (core) {
            case "purpur":
                return setOf("purpur", "paper", "spigot", "bukkit");
            case "paper":
                return setOf("paper", "spigot", "bukkit", "leaf");
            case "folia":
                return setOf("folia", "paper", "spigot", "bukkit");
            case "spigot":
                return setOf("spigot", "bukkit");
            case "bukkit":
                return setOf("bukkit");
            case "velocity":
                return setOf("velocity");
            case "waterfall":
                return setOf("waterfall", "bungeecord");
            case "bungeecord":
                return setOf("bungeecord");
            case "fabric":
                return setOf("fabric");
            case "forge":
                return setOf("forge");
            case "neoforge":
                return setOf("neoforge");
            default:
                return setOf("paper", "spigot", "bukkit");
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static final class RemoteVersion {
        String id = "";
        String number = "";
        String name = "";
        String type = "";
        String changelog = "";
        String published = "";
        String downloadUrl = "";
        List<String> loaders = new ArrayList<>();
        List<String> gameVersions = new ArrayList<>();
    }

    public static final class Result {

        private final boolean updateAvailable;
        private final String currentVersion;
        private final String latestVersion;
        private final String latestName;
        private final String versionType;
        private final String changelog;
        private final String published;
        private final String downloadUrl;
        private final String pageUrl;
        private final String error;

        private Result(boolean updateAvailable, String currentVersion, String latestVersion,
                       String latestName, String versionType, String changelog, String published,
                       String downloadUrl, String pageUrl, String error) {
            this.updateAvailable = updateAvailable;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.latestName = latestName;
            this.versionType = versionType;
            this.changelog = changelog;
            this.published = published;
            this.downloadUrl = downloadUrl;
            this.pageUrl = pageUrl;
            this.error = error;
        }

        static Result update(String currentVersion, RemoteVersion best, String pageUrl) {
            return new Result(true, currentVersion, best.number, best.name, best.type,
                    best.changelog, best.published, best.downloadUrl, pageUrl, null);
        }

        static Result upToDate(String currentVersion) {
            return new Result(false, currentVersion, currentVersion, "", "", "",
                    "", "", "", null);
        }

        static Result failed(String currentVersion, String error) {
            return new Result(false, currentVersion, currentVersion, "", "", "",
                    "", "", "", error == null ? "неизвестная ошибка" : error);
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        public boolean hasFailed() {
            return error != null;
        }

        public String getError() {
            return error;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public String getLatestName() {
            return latestName;
        }

        public String getVersionType() {
            return versionType;
        }

        public String getChangelog() {
            return changelog;
        }

        public String getPublished() {
            return published;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getPageUrl() {
            return pageUrl;
        }
    }
}
