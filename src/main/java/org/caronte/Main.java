package org.caronte;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.caronte.commands.EloCommand;
import org.caronte.manager.EloManager;
import org.caronte.placeholder.RoyaleEloExpansion;
import org.caronte.storage.SQLiteStorage;

import java.io.File;

public class Main extends JavaPlugin {

    private static Main instance;

    private SQLiteStorage storage;
    private EloManager eloManager;

    private File commandsFile;
    public FileConfiguration commandsConfig;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        // =========================
        // Commands.yml
        // =========================
        commandsFile = new File(getDataFolder(), "commands.yml");

        if (!commandsFile.exists()) {
            saveResource("commands.yml", false);
        }

        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);

        // =========================
        // SQLite
        // =========================
        storage = new SQLiteStorage(this);
        storage.connect();
        storage.createTable();

        eloManager = new EloManager(this, storage);
        Bukkit.getPluginManager().registerEvents(eloManager, this);

        // =========================
        // Commands
        // =========================
        EloCommand eloCommand = new EloCommand(this);

        getCommand("elo").setExecutor(eloCommand);
        getCommand("elo").setTabCompleter(eloCommand);

        getCommand("elotop").setExecutor(eloCommand);
        getCommand("elotop").setTabCompleter(eloCommand);

        getCommand("royaleelo").setExecutor(eloCommand);
        getCommand("royaleelo").setTabCompleter(eloCommand);

        // =========================
        // PlaceholderAPI
        // =========================
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RoyaleEloExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registrada.");
        }

        getLogger().info("RoyaleElo iniciado correctamente.");
    }

    @Override
    public void onDisable() {
        storage.disconnect();
    }

    public static Main getInstance() {
        return instance;
    }

    public SQLiteStorage getStorage() {
        return storage;
    }

    public FileConfiguration getCommandsConfig() {
        return commandsConfig;
    }

    public void reloadCommandsConfig() {
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
    }

    public String color(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}