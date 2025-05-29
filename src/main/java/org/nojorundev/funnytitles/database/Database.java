package org.nojorundev.funnytitles.database;

import org.bukkit.Bukkit;
import org.nojorundev.funnytitles.FunnyTitles;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.UUID;

public class Database {
    private final FunnyTitles plugin;
    private Connection connection;

    public Database(FunnyTitles plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = new File(plugin.getDataFolder(), "database.db");
            if (!dataFolder.exists()) {
                dataFolder.getParentFile().mkdirs();
                dataFolder.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);

            createTable();
        } catch (IOException | SQLException | ClassNotFoundException e) {
            Bukkit.getLogger().severe("Не удалось подключиться к базе данных: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_titles (" +
                    "uuid TEXT, " +
                    "title_id TEXT, " +
                    "PRIMARY KEY (uuid, title_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_selected_titles (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "title_id TEXT)");
        }
    }

    public void saveTitle(UUID uuid, String titleId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_titles (uuid, title_id) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось сохранить титул: " + e.getMessage());
        }
    }

    public void removeTitle(UUID uuid, String titleId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM player_titles WHERE uuid = ? AND title_id = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось удалить титул: " + e.getMessage());
        }
    }

    public void removeAllTitles(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM player_titles WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось удалить титулы: " + e.getMessage());
        }
    }

    public void saveSelectedTitle(UUID uuid, String titleId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO player_selected_titles (uuid, title_id) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось сохранить выбранный титул: " + e.getMessage());
        }
    }

    public String getSelectedTitle(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT title_id FROM player_selected_titles WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("title_id");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось загрузить выбранный титул: " + e.getMessage());
        }
        return null;
    }

    public void loadAllTitles(FunnyTitles plugin) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM player_titles")) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String titleId = rs.getString("title_id");

                if (!plugin.playerTitles.containsKey(uuid)) {
                    plugin.playerTitles.put(uuid, new HashSet<>());
                }
                plugin.playerTitles.get(uuid).add(titleId);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось загрузить титулы: " + e.getMessage());
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM player_selected_titles")) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String titleId = rs.getString("title_id");
                plugin.selectedTitles.put(uuid, titleId);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось загрузить выбранные титулы: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Не удалось закрыть соединение с базой данных: " + e.getMessage());
        }
    }
}