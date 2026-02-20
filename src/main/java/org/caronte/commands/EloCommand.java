package org.caronte.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.caronte.Main;
import org.caronte.model.PlayerData;
import org.caronte.storage.SQLiteStorage;

import java.util.*;

public class EloCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final SQLiteStorage storage;
    private final Map<UUID, Long> restartConfirmations = new HashMap<>();

    public EloCommand(Main plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    // =================================================
    // COMMANDS
    // =================================================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        String name = cmd.getName().toLowerCase();

        // =========================
        // /elo
        // =========================
        if (name.equals("elo")) {

            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    send(sender, "Messages.only-players");
                    return true;
                }
                sendStats(sender, player.getName(),
                        storage.getPlayerData(player.getUniqueId()));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                send(sender, "Messages.player-not-found");
                return true;
            }

            sendStats(sender, target.getName(),
                    storage.getPlayerData(target.getUniqueId()));
            return true;
        }

        // =========================
        // /elotop
        // =========================
        if (name.equals("elotop")) {

            List<String> top = storage.getTop10();

            send(sender, "Messages.top.header");

            if (top.isEmpty()) {
                send(sender, "Messages.top.empty");
            } else {
                int pos = 1;
                for (String line : top) {
                    sender.sendMessage(plugin.color(
                            plugin.getConfig().getString("Messages.top.line")
                                    .replace("%pos%", String.valueOf(pos))
                                    .replace("%player%", line.split(" §8- ")[0])
                                    .replace("%elo%", line.split(" §8- ")[1].replace("§e", ""))
                    ));
                    pos++;
                }
            }
            return true;
        }

        // =========================
        // /royaleelo (ADMIN)
        // =========================
        if (!name.equals("royaleelo")) return false;

        if (!sender.hasPermission("royaleelo.admin")) {
            send(sender, "messages.no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        // reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.reloadCommandsConfig();
            send(sender, "messages.reload");
            return true;
        }

        // restart all
        if (args.length == 2 &&
                args[0].equalsIgnoreCase("restart") &&
                args[1].equalsIgnoreCase("all")) {

            long expire = System.currentTimeMillis() + parseTime(
                    plugin.getConfig().getString("Settings.Season.Restart-Confirm-Time"));

            UUID id = sender instanceof Player p ? p.getUniqueId() : UUID.randomUUID();
            restartConfirmations.put(id, expire);

            send(sender, "Messages.restart.request");
            return true;
        }

        // restart confirm
        if (args.length == 2 &&
                args[0].equalsIgnoreCase("restart") &&
                args[1].equalsIgnoreCase("confirm")) {

            UUID id = sender instanceof Player p ? p.getUniqueId() : null;

            if (id == null || !restartConfirmations.containsKey(id)) {
                send(sender, "Messages.restart.cancelled");
                return true;
            }

            if (System.currentTimeMillis() > restartConfirmations.get(id)) {
                restartConfirmations.remove(id);
                send(sender, "Messages.restart.timeout");
                return true;
            }

            restartConfirmations.remove(id);
            storage.resetAll(plugin.getConfig().getInt("Settings.Elo.Default"));

            send(sender, "Messages.restart.success");
            return true;
        }

        send(sender, "Messages.wrong-usage");
        return true;
    }

    // =================================================
    // TAB COMPLETER
    // =================================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!cmd.getName().equalsIgnoreCase("royaleelo")) return Collections.emptyList();

        if (args.length == 1)
            return Arrays.asList("help", "reload", "set", "restart");

        if (args.length == 2 && args[0].equalsIgnoreCase("restart"))
            return Arrays.asList("all", "confirm");

        return Collections.emptyList();
    }

    // =================================================
    // HELPERS
    // =================================================
    private void sendStats(CommandSender sender, String player, PlayerData data) {

        sender.sendMessage(plugin.color(
                plugin.getConfig().getString("Messages.stats.header")
                        .replace("%player%", player)));

        send(sender, "Messages.stats.elo", "%elo%", data.getElo());
        send(sender, "Messages.stats.kills", "%kills%", data.getKills());
        send(sender, "Messages.stats.deaths", "%deaths%", data.getDeaths());
        send(sender, "Messages.stats.kdr",
                "%kdr%", String.format("%.2f", data.getKDR()));
    }

    private void sendHelp(CommandSender sender) {

        send(sender, "Messages.help.header");

        for (String line : plugin.getConfig().getStringList("Messages.help.lines")) {
            sender.sendMessage(plugin.color(line));
        }

        send(sender, "Messages.help.footer");
    }

    private void send(CommandSender sender, String path, Object... vars) {
        String msg = plugin.getConfig().getString(path);
        if (msg == null) return;

        for (int i = 0; i < vars.length; i += 2) {
            msg = msg.replace(vars[i].toString(), vars[i + 1].toString());
        }
        sender.sendMessage(plugin.color(msg));
    }

    private long parseTime(String s) {
        s = s.toLowerCase();
        if (s.endsWith("s")) return Long.parseLong(s.replace("s", "")) * 1000;
        if (s.endsWith("m")) return Long.parseLong(s.replace("m", "")) * 60_000;
        return Long.parseLong(s);
    }
}