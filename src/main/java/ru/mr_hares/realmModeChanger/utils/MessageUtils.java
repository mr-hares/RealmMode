package your.plugin.package; // TODO: поменяй на свой пакет

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

/**
 * Загрузчик сообщений.
 *
 * В jar (src/main/resources):
 *   messages/ru.yml
 *   messages/en.yml
 *
 * В config.yml плагина:
 *   language: ru
 *
 * Использование:
 *   onEnable():  MessageUtils.init(this);
 *   где угодно:  MessageUtils.get("no-permission");
 *                MessageUtils.get("welcome", "{player}", player.getName());
 */
public final class MessageUtils {

    private static final String FOLDER = "messages";
    /** Языки, которые лежат внутри jar и будут выгружены в папку плагина. */
    private static final String[] BUNDLED_LANGS = {"ru", "en"};

    private static JavaPlugin plugin;
    private static FileConfiguration userConfig;      // файл из папки плагина (его правит пользователь)
    private static FileConfiguration bundledConfig;   // файл из jar (null, если язык кастомный)
    private static String language;

    private MessageUtils() {}

    /** Вызывать в onEnable(). Можно вызывать повторно для перезагрузки. */
    public static void init(JavaPlugin pl) {
        plugin = pl;
        plugin.saveDefaultConfig();

        // 1. Выгружаем встроенные языки, только если их ещё нет (правки пользователя не затираются)
        for (String lang : BUNDLED_LANGS) {
            String path = FOLDER + "/" + lang + ".yml";
            if (!new File(plugin.getDataFolder(), path).exists()) {
                plugin.saveResource(path, false);
            }
        }

        // 2. Выбранный язык из config.yml
        language = plugin.getConfig().getString("language", "ru").toLowerCase();

        File file = new File(plugin.getDataFolder(), FOLDER + "/" + language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file '" + language + ".yml' not found in /" + FOLDER
                    + ", falling back to '" + BUNDLED_LANGS[0] + "'.");
            language = BUNDLED_LANGS[0];
            file = new File(plugin.getDataFolder(), FOLDER + "/" + language + ".yml");
        }

        // 3. Файл пользователя
        userConfig = YamlConfiguration.loadConfiguration(file);

        // 4. Дефолты из jar — только если такой язык есть внутри jar
        bundledConfig = null;
        try (InputStream in = plugin.getResource(FOLDER + "/" + language + ".yml")) {
            if (in != null) {
                bundledConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read bundled language file: " + e.getMessage());
        }
    }

    public static void reload() {
        plugin.reloadConfig();
        init(plugin);
    }

    public static String getLanguage() {
        return language;
    }

    /**
     * Берёт строку по пути.
     * Порядок: файл пользователя -> файл из jar (если есть) -> ошибка.
     * Пустые значения и null считаются «отсутствующими».
     */
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

    /** Строка с цветами (&a, &l и т.д.) и подстановками: get("key", "{player}", name, ...). */
    public static String get(String path, String... replacements) {
        String msg = getRaw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /** Список строк (например, для описания предметов). Та же логика фолбэка. */
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
            result.add(ChatColor.translateAlternateColorCodes('&', line));
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

    /** Ошибка: сообщения нет ни в файле пользователя, ни в jar. */
    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(String path, String lang) {
            super("Message '" + path + "' is not specified in language file '" + lang + ".yml'");
        }
    }
}
