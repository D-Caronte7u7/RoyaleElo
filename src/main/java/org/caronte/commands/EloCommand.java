package org.caronte.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.caronte.Main;
import org.caronte.model.PlayerData;
import org.caronte.storage.SQLiteStorage;

import java.util.List;

public class EloCommand implements CommandExecutor {

    private final Main plugin;
    private final SQLiteStorage storage;

    public EloCommand(Main plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        String commandName = cmd.getName().toLowerCase();
        String prefix = plugin.color(plugin.getConfig().getString("messages.prefix"));

        String permission = getPermission(commandName);

        if (permission != null && !permission.isEmpty()) {
            if (!sender.hasPermission(permission)) {
                sender.sendMessage(prefix + "§cNo tienes permiso.");
                return true;
            }
        }

        // =====================
        // /elo
        // =====================
        if (commandName.equals("elo")) {

            if (args.length == 0) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Solo jugadores.");
                    return true;
                }

                PlayerData data = storage.getPlayerData(player.getUniqueId());
                sendStats(sender, player.getName(), data);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(prefix + "§cJugador no encontrado.");
                return true;
            }

            PlayerData data = storage.getPlayerData(target.getUniqueId());
            sendStats(sender, target.getName(), data);
            return true;
        }

        // =====================
        // /elotop
        // =====================
        if (commandName.equals("elotop")) {

            List<String> top = storage.getTop10();

            sender.sendMessage(" ");
            sender.sendMessage("§6§lTop 10 Elo Global");
            sender.sendMessage(" ");

            int pos = 1;
            for (String line : top) {
                sender.sendMessage("§e#" + pos + " §7" + line);
                pos++;
            }

            sender.sendMessage(" ");
            return true;
        }

        // =====================
        // /royaleelo reload
        // =====================
        if (commandName.equals("royaleelo")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

                plugin.reloadConfig();
                plugin.reloadCommandsConfig();

                sender.sendMessage(prefix + plugin.color(
                        plugin.getConfig().getString("messages.reload")
                ));
                return true;
            }

            sender.sendMessage(prefix + "§cUso correcto: " + getUsage("royaleelo"));
            return true;
        }

        return false;
    }

    private void sendStats(CommandSender sender, String name, PlayerData data) {

        sender.sendMessage(" ");
        sender.sendMessage("§6§lEstadísticas de " + name);
        sender.sendMessage("§7Elo: §e" + data.getElo());
        sender.sendMessage("§7Kills: §a" + data.getKills());
        sender.sendMessage("§7Deaths: §c" + data.getDeaths());
        sender.sendMessage("§7KDR: §b" + String.format("%.2f", data.getKDR()));
        sender.sendMessage(" ");
    }

    private String getPermission(String command) {
        return plugin.getCommandsConfig()
                .getString("Commands." + command + ".Permission");
    }

    private String getUsage(String command) {
        return plugin.getCommandsConfig()
                .getString("Commands." + command + ".Usage");
    }
}