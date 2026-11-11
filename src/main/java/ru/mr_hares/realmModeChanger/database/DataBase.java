package ru.mr_hares.realmModeChanger.database;

import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;

public class DataBase {
    private Connection connection;
    private Plugin plugin;

    public DataBase(Plugin plugin) {
        this.plugin = plugin;

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() +
                    "/data.db");

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format("CREATE TABLE IF NOT EXISTS %s (" +
                        "name TEXT NOT NULL)", "table_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к базе данных");
        }
    }

    public void Example(String args) {
        String sql = "";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}