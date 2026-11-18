package ru.mr_hares.realmMode.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.MessageUtils;

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
                    "/database.db");

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

    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void close() {
        if (connection == null) return;

        try {
            connection.close();
            plugin.getLogger().info("Close the database connection");
        } catch (SQLException e) {
            if (plugin != null) plugin.getLogger().severe("Failed to close the database connection: " + e.getMessage());
        } finally {
            connection = null;
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

    public static String getTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM info_list WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Instant then = Instant.parse(rs.getString("date"));
                Duration dr = Duration.between(Instant.now(), then.plus(getTime()));
                return getTimeFormat(dr);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        return null;
    }

    private static String getTimeFormat(Duration dr) {
        long seconds = dr.getSeconds();

        String days_format = MessageUtils.getRaw("time.days");
        String hours_format = MessageUtils.getRaw("time.hours");
        String minutes_format = MessageUtils.getRaw("time.minutes");
        String seconds_format = MessageUtils.getRaw("time.seconds");

        int days = (int) Math.floor((double) seconds / 86400);
        int hours = (int) Math.floor((double) (seconds - 86400L*days) / 3600);
        int minutes = (int) Math.floor((double) (seconds - 86400L*days - 3600L*hours) / 60);
        if (days > 0) {
            return String.format("%s %s %s %s",
                    days_format.replace("{days}", String.valueOf(days)),
                    hours_format.replace("{hours}", String.valueOf(hours)),
                    minutes_format.replace("{minutes}", String.valueOf(minutes)),
                    seconds_format.replace("{seconds}", String.valueOf(seconds))
            );
        } else if (hours > 0) {
            return String.format("%s %s %s",
                    hours_format.replace("{hours}", String.valueOf(hours)),
                    minutes_format.replace("{minutes}", String.valueOf(minutes)),
                    seconds_format.replace("{seconds}", String.valueOf(seconds))
            );
        } else if (minutes > 0) {
            return String.format("%s %s",
                    minutes_format.replace("{minutes}", String.valueOf(minutes)),
                    seconds_format.replace("{seconds}", String.valueOf(seconds))
            );
        } else {
            return seconds_format.replace("{seconds}", String.valueOf(seconds - 86400L*days - 3600L*hours - 60L*minutes));
        }
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

    public static void updateMode(UUID uuid, Mode mode) {
        try (PreparedStatement stmt =
                     connection.prepareStatement("UPDATE info_list SET mode = ? WHERE uuid = ?")) {
            stmt.setString(1, mode.getValue());
            stmt.setString(2, uuid.toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(e.toString());
        }

        try (PreparedStatement stmt =
                     connection.prepareStatement("UPDATE info_list SET date = ? WHERE uuid = ?")) {
            stmt.setString(1, Instant.now().toString());
            stmt.setString(2, uuid.toString());

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