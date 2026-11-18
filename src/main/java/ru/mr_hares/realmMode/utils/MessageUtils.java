package ru.mr_hares.realmMode.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MessageUtils {

    private static final String FOLDER = "messages";
    private static final String[] BUNDLED_LANGS = {"ru", "en"};

    private static JavaPlugin plugin;
    private static FileConfiguration userConfig;
    private static String language;

    private MessageUtils() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        long start = System.currentTimeMillis();

        plugin.saveDefaultConfig();

        for (String lang : BUNDLED_LANGS) {
            syncLanguageFile(lang);
        }

        language = plugin.getConfig().getString("language", "en").toLowerCase();
        File file = languageFile(language);
        if (!file.exists()) {
            plugin.getLogger().warning("Language file '" + language + ".yml' not found in /" + FOLDER
                    + ", falling back to '" + BUNDLED_LANGS[0] + "'.");
            language = BUNDLED_LANGS[0];
            file = languageFile(language);
        }

        userConfig = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = readBundled(language);
        if (defaults != null) {
            userConfig.setDefaults(defaults);
            userConfig.options().copyDefaults(true);
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
        if (isBlank(value) && userConfig.getDefaults() != null) {
            value = userConfig.getDefaults().getString(path);
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
        if (list.isEmpty() && userConfig.getDefaults() != null) {
            list = userConfig.getDefaults().getStringList(path);
        }
        if (list.isEmpty()) {
            throw new MessageNotFoundException(path, language);
        }
        return list;
    }

    private static void syncLanguageFile(String lang) {
        String resource = FOLDER + "/" + lang + ".yml";
        File file = languageFile(lang);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
            return;
        }

        List<String> jarLines = readResourceLines(resource);
        if (jarLines == null) {
            return;
        }

        String text;
        try {
            text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read " + resource + ": " + e.getMessage());
            return;
        }

        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> userLines = new ArrayList<>(Arrays.asList(text.split("\r?\n", -1)));

        YamlConfiguration user = YamlConfiguration.loadConfiguration(file);
        List<String> merged = mergeLines(userLines, jarLines, user);
        if (merged == null) {
            return;
        }

        try {
            Files.write(file.toPath(), String.join(newline, merged).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to update " + resource + ": " + e.getMessage());
        }
    }

    private static List<String> mergeLines(List<String> userLines, List<String> jarLines, YamlConfiguration user) {
        Node jarRoot = parse(jarLines);
        List<Node> jarNodes = new ArrayList<>();
        collect(jarRoot, jarNodes);

        List<String> result = new ArrayList<>(userLines);
        List<String> insertedSections = new ArrayList<>();
        boolean changed = false;

        for (Node node : jarNodes) {
            if (user.isSet(node.path) || underAny(node.path, insertedSections)) {
                continue;
            }
            if (!node.parentPath.isEmpty() && !user.isConfigurationSection(node.parentPath)) {
                continue;
            }
            Node userRoot = parse(result);
            insert(result, jarLines, node, jarRoot, userRoot);
            if (!node.children.isEmpty()) {
                insertedSections.add(node.path);
            }
            changed = true;
        }

        return changed ? result : null;
    }

    private static void insert(List<String> target, List<String> jarLines, Node jarNode,
                               Node jarRoot, Node userRoot) {
        String parentPath = jarNode.parentPath;
        Node jarParent = parentPath.isEmpty() ? jarRoot : find(jarRoot, parentPath);
        Node userParent = parentPath.isEmpty() ? userRoot : find(userRoot, parentPath);
        if (jarParent == null) {
            jarParent = jarRoot;
        }
        if (userParent == null) {
            userParent = userRoot;
        }

        int indent = targetIndent(userParent, jarNode, jarParent);
        int at = anchor(userRoot, jarParent, jarNode, userParent);
        int delta = indent - jarNode.indent;

        List<String> block = new ArrayList<>();
        for (int i = jarNode.firstLine; i < jarNode.blockEnd; i++) {
            block.add(reindent(jarLines.get(i), delta));
        }
        target.addAll(at, block);
    }

    private static int targetIndent(Node userParent, Node jarNode, Node jarParent) {
        if (!userParent.children.isEmpty()) {
            return userParent.children.values().iterator().next().indent;
        }
        if (userParent.indent < 0) {
            return Math.max(0, jarNode.indent);
        }
        int relative = jarNode.indent - jarParent.indent;
        return userParent.indent + Math.max(relative, 2);
    }

    private static int anchor(Node userRoot, Node jarParent, Node jarNode, Node userParent) {
        List<Node> siblings = new ArrayList<>(jarParent.children.values());
        int index = siblings.indexOf(jarNode);

        for (int i = index - 1; i >= 0; i--) {
            Node previous = find(userRoot, siblings.get(i).path);
            if (previous != null) {
                return previous.blockEnd;
            }
        }
        for (int i = index + 1; i < siblings.size(); i++) {
            Node next = find(userRoot, siblings.get(i).path);
            if (next != null) {
                return next.firstLine;
            }
        }
        return userParent.blockEnd;
    }

    private static Node parse(List<String> lines) {
        Node root = new Node("", -1, 0, 0);
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        List<Node> ordered = new ArrayList<>();
        int blockScalarIndent = -1;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.trim();
            int indent = indentOf(raw);

            if (trimmed.isEmpty()) {
                continue;
            }
            if (blockScalarIndent >= 0) {
                if (indent > blockScalarIndent) {
                    continue;
                }
                blockScalarIndent = -1;
            }
            if (trimmed.startsWith("#") || trimmed.equals("-") || trimmed.startsWith("- ")) {
                continue;
            }

            int colon = colonIndex(trimmed);
            if (colon <= 0) {
                continue;
            }
            String key = unquote(trimmed.substring(0, colon).trim());
            String value = trimmed.substring(colon + 1).trim();

            while (!stack.peek().path.isEmpty() && stack.peek().indent >= indent) {
                stack.pop();
            }
            Node parent = stack.peek();
            String path = parent.path.isEmpty() ? key : parent.path + "." + key;

            int first = i;
            while (first > 0 && lines.get(first - 1).trim().startsWith("#")) {
                first--;
            }

            Node node = new Node(path, indent, first, i);
            parent.children.put(key, node);
            stack.push(node);
            ordered.add(node);

            if (value.startsWith("|") || value.startsWith(">")) {
                blockScalarIndent = indent;
            }
        }

        for (int i = 0; i < ordered.size(); i++) {
            Node node = ordered.get(i);
            int end = lines.size();
            for (int j = i + 1; j < ordered.size(); j++) {
                if (ordered.get(j).indent <= node.indent) {
                    end = ordered.get(j).firstLine;
                    break;
                }
            }
            node.blockEnd = trimTrailingBlank(lines, node.keyLine + 1, end);
        }
        root.blockEnd = trimTrailingBlank(lines, 0, lines.size());

        return root;
    }

    private static void collect(Node node, List<Node> out) {
        for (Node child : node.children.values()) {
            out.add(child);
            collect(child, out);
        }
    }

    private static Node find(Node root, String path) {
        if (path.isEmpty()) {
            return root;
        }
        Node node = root;
        for (String part : path.split("\\.")) {
            node = node.children.get(part);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    private static boolean underAny(String path, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static int colonIndex(String trimmed) {
        char quote = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == ':' && (i + 1 == trimmed.length() || trimmed.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static String unquote(String key) {
        if (key.length() >= 2) {
            char first = key.charAt(0);
            char last = key.charAt(key.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return key.substring(1, key.length() - 1);
            }
        }
        return key;
    }

    private static int trimTrailingBlank(List<String> lines, int from, int end) {
        int i = end;
        while (i > from && lines.get(i - 1).trim().isEmpty()) {
            i--;
        }
        return i;
    }

    private static String reindent(String line, int delta) {
        if (delta == 0 || line.trim().isEmpty()) {
            return line;
        }
        if (delta > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < delta; i++) {
                sb.append(' ');
            }
            return sb.append(line).toString();
        }
        int remove = 0;
        while (remove < -delta && remove < line.length() && line.charAt(remove) == ' ') {
            remove++;
        }
        return line.substring(remove);
    }

    private static List<String> readResourceLines(String resource) {
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read bundled " + resource + ": " + e.getMessage());
            return null;
        }
    }

    private static YamlConfiguration readBundled(String lang) {
        String resource = FOLDER + "/" + lang + ".yml";
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read bundled " + resource + ": " + e.getMessage());
            return null;
        }
    }

    private static File languageFile(String lang) {
        return new File(plugin.getDataFolder(), FOLDER + "/" + lang + ".yml");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void ensureInit() {
        if (plugin == null) {
            throw new IllegalStateException("MessageUtils.init(plugin) was not called");
        }
    }

    private static final class Node {
        final String path;
        final String parentPath;
        final int indent;
        final int firstLine;
        final int keyLine;
        final Map<String, Node> children = new LinkedHashMap<>();
        int blockEnd;

        Node(String path, int indent, int firstLine, int keyLine) {
            this.path = path;
            int dot = path.lastIndexOf('.');
            this.parentPath = dot < 0 ? "" : path.substring(0, dot);
            this.indent = indent;
            this.firstLine = firstLine;
            this.keyLine = keyLine;
        }
    }

    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String path, String lang) {
            super("Message '" + path + "' is not specified in language file '" + lang + ".yml'");
        }
    }
}
