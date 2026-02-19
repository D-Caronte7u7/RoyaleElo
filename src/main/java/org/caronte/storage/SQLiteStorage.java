package org.caronte.storage;

import org.bukkit.Bukkit;
import org.caronte.Main;
import org.caronte.model.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.UUID;

public class SQLiteStorage {

    private final Main plugin;
    private Connection connection;

    public SQLiteStorage(Main plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "database.db");

            connection = DriverManager.getConnection("jdbc:sqlite:" + file);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTable() {
        try (PreparedStatement ps = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "uuid TEXT PRIMARY KEY," +
                        "elo INTEGER DEFAULT 1000," +
                        "kills INTEGER DEFAULT 0," +
                        "deaths INTEGER DEFAULT 0)"
        )) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM player_stats WHERE uuid=?"
        )) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new PlayerData(
                        uuid,
                        rs.getInt("elo"),
                        rs.getInt("kills"),
                        rs.getInt("deaths")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        createPlayer(uuid);
        return new PlayerData(uuid, 1000, 0, 0);
    }

    private void createPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_stats(uuid) VALUES(?)"
        )) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayer(PlayerData data) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET elo=?, kills=?, deaths=? WHERE uuid=?"
        )) {
            ps.setInt(1, data.getElo());
            ps.setInt(2, data.getKills());
            ps.setInt(3, data.getDeaths());
            ps.setString(4, data.getUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTop10() {

        List<String> list = new java.util.ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, elo FROM player_stats ORDER BY elo DESC LIMIT 10"
        )) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                int elo = rs.getInt("elo");

                String name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();

                list.add(name + " §8- §e" + elo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}