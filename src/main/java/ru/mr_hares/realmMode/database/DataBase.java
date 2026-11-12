package ru.mr_hares.realmMode.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.objects.Mode;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.*;

public class DataBase {
    private static Connection connection;
    private static Plugin plugin;

    private DataBase() {}

    public static void init(JavaPlugin pl) {
        plugin = pl;
        long start = System.currentTimeMillis();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() +
                    "/data.db");

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "uuid TEXT NOT NULL," +
                        "mode TEXT NOT NULL," +
                        "date TEXT NOT NULL)", "info_list"));
            }
            plugin.getLogger().info("Database connection was established in " + (System.currentTimeMillis() - start) +
                    "ms");
        } catch (Exception e) {
            plugin.getLogger().severe("Database connection error");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

    }

    private static TemporalAmount getTime() {
        String cooldown = plugin.getConfig().getString("cooldown-change", "3d").toLowerCase();
        final String ERROR = "Configuration parameter \"cooldown-change\" is not in the 3d/3h/3m format";

        if (cooldown.length() < 2) throw new NumberFormatException(ERROR);

        int time;
        try { time = Integer.parseInt(cooldown.substring(0, cooldown.length() - 1)); }
        catch (NumberFormatException e) { throw new NumberFormatException(ERROR); }

        return switch (cooldown.charAt(cooldown.length() - 1)) {
            case 'd' -> Duration.ofDays(time);
            case 'h' -> Duration.ofHours(time);
            case 'm' -> Duration.ofMinutes(time);
            default  -> throw new NumberFormatException(ERROR);
        };
    }

    public static Boolean upTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM info_list WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
               Instant then = Instant.parse(rs.getString("date"));
               return Instant.now().isAfter(then.plus(getTime()));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return true;
    }

    public static long getTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM info_list WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Instant then = Instant.parse(rs.getString("date"));
                Duration dr = Duration.between(Instant.now(), then.plus(getTime()));
                return dr.toSeconds();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return 0;
    }

    public static void setMode(UUID uuid, Mode mode) {
        try (PreparedStatement stmt =
                     connection.prepareStatement("INSERT INTO info_list (uuid, mode, date) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, mode.getValue());
            stmt.setString(3, Instant.now().toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }
    }

    public static Mode getMode(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM info_list WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Mode.fromValue(rs.getString("mode"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return null;
    }

    public static Map<UUID, Mode> getList() {
        Map<UUID, Mode> list = new HashMap<>();

        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM info_list")) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.put(UUID.fromString(rs.getString("uuid")), Mode.fromValue(rs.getString("mode")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return list;
    }
}