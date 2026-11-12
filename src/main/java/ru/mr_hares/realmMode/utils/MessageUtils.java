package ru.mr_hares.realmMode.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MessageUtils {

    private static final String FOLDER = "messages";
    private static final String[] BUNDLED_LANGS = {"ru", "en"};

    private static JavaPlugin plugin;
    private static FileConfiguration userConfig;
    private static FileConfiguration bundledConfig;
    private static String language;

    private MessageUtils() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        long start = System.currentTimeMillis();

        plugin.saveDefaultConfig();

        for (String lang : BUNDLED_LANGS) {
            String path = FOLDER + "/" + lang + ".yml";
            if (!new File(plugin.getDataFolder(), path).exists()) {
                plugin.saveResource(path, false);
            }
        }

        language = plugin.getConfig().getString("language", "en").toLowerCase();

        File file = new File(plugin.getDataFolder(), FOLDER + "/" + language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file '" + language + ".yml' not found in /" + FOLDER
                    + ", falling back to '" + BUNDLED_LANGS[0] + "'.");
            language = BUNDLED_LANGS[0];
            file = new File(plugin.getDataFolder(), FOLDER + "/" + language + ".yml");
        }

        userConfig = YamlConfiguration.loadConfiguration(file);

        bundledConfig = null;
        try (InputStream in = plugin.getResource(FOLDER + "/" + language + ".yml")) {
            if (in != null) {
                bundledConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read bundled language file: " + e.getMessage());
        }

        plugin.getLogger().info("Localization was loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    public static void reload() {
        plugin.reloadConfig();
        init(plugin);
    }

    public static String getLanguage() {
        return language;
    }

    public static String getRaw(String path) {
        ensureInit();

        String value = userConfig.getString(path);
        if (isBlank(value) && bundledConfig != null) {
            value = bundledConfig.getString(path);
        }
        if (isBlank(value)) {
            throw new MessageNotFoundException(path, language);
        }
        return value;
    }

    public static String get(String path, String... replacements) {
        String msg = getRaw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public static List<String> getList(String path) {
        ensureInit();

        List<String> list = userConfig.getStringList(path);
        if (list.isEmpty() && bundledConfig != null) {
            list = bundledConfig.getStringList(path);
        }
        if (list.isEmpty()) {
            throw new MessageNotFoundException(path, language);
        }
        List<String> result = new ArrayList<>(list.size());
        for (String line : list) {
            result.add(line);
        }
        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void ensureInit() {
        if (plugin == null) {
            throw new IllegalStateException("MessageUtils.init(plugin) was not called");
        }
    }

    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String path, String lang) {
            super("Message '" + path + "' is not specified in language file '" + lang + ".yml'");
        }
    }
}
