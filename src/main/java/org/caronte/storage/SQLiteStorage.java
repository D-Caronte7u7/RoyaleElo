package org.caronte.storage;

import org.bukkit.Bukkit;
import org.caronte.Main;
import org.caronte.model.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
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
                        "elo INTEGER," +
                        "kills INTEGER," +
                        "deaths INTEGER)"
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
        int defaultElo = plugin.getConfig().getInt("Settings.Elo.Default");
        return new PlayerData(uuid, defaultElo, 0, 0);
    }

    private void createPlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_stats(uuid, elo, kills, deaths) VALUES(?, ?, 0, 0)"
        )) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, plugin.getConfig().getInt("Settings.Elo.Default"));
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

    public void setAllElo(int elo) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET elo=?"
        )) {
            ps.setInt(1, elo);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetAll(int defaultElo) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET elo=?, kills=0, deaths=0"
        )) {
            ps.setInt(1, defaultElo);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTop10() {
        List<String> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, elo FROM player_stats ORDER BY elo DESC LIMIT 10"
        )) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                int elo = rs.getInt("elo");
                String name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                if (name == null) name = uuid.substring(0, 8);
                list.add(name + " §8- §e" + elo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}