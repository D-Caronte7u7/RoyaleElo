package org.caronte.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.caronte.Main;
import org.caronte.model.PlayerData;
import org.caronte.storage.SQLiteStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EloManager implements Listener {

    private final Main plugin;
    private final SQLiteStorage storage;

    private final Map<UUID, Map<UUID, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> repeats = new HashMap<>();

    public EloManager(Main plugin, SQLiteStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        UUID kId = killer.getUniqueId();
        UUID vId = victim.getUniqueId();

        long now = System.currentTimeMillis();
        long cooldownMs = parseTime(plugin.getConfig().getString("Settings.Elo.Cooldown"));

        // =====================
        // Cooldown
        // =====================
        cooldowns.putIfAbsent(kId, new HashMap<>());
        Long last = cooldowns.get(kId).get(vId);

        if (last != null && now - last < cooldownMs) {
            send(killer, "Messages.no-elo-cooldown");
            return;
        }

        cooldowns.get(kId).put(vId, now);

        // =====================
        // Repeat kills
        // =====================
        repeats.putIfAbsent(kId, new HashMap<>());
        int count = repeats.get(kId).getOrDefault(vId, 0) + 1;
        repeats.get(kId).put(vId, count);

        double repeatMultiplier =
                count == 1 ? 1.0 :
                        count == 2 ? 0.5 :
                                count == 3 ? 0.2 : 0.0;

        if (repeatMultiplier == 0.0) {
            send(killer, "Messages.no-elo-repeat");
            return;
        }

        // =====================
        // Load data
        // =====================
        PlayerData kd = storage.getPlayerData(kId);
        PlayerData vd = storage.getPlayerData(vId);

        int diff = kd.getElo() - vd.getElo();

        double diffMultiplier = getDiffMultiplier(diff);
        if (diffMultiplier == 0.0) {
            send(killer, "Messages.no-elo-diff");
            return;
        }

        int baseWin = plugin.getConfig().getInt("Settings.Elo.Player-Win");
        int baseLose = plugin.getConfig().getInt("Settings.Elo.Player-Lose");

        int eloGain = (int) Math.round(baseWin * repeatMultiplier * diffMultiplier);
        int eloLoss = (int) Math.round(baseLose * getPenaltyMultiplier(diff));

        // =====================
        // Apply
        // =====================
        kd.setElo(kd.getElo() + eloGain);
        kd.addKill();

        vd.setElo(vd.getElo() - eloLoss);
        vd.addDeath();

        storage.updatePlayer(kd);
        storage.updatePlayer(vd);

        send(killer, "Messages.elo-gain",
                "%elo_change%", eloGain,
                "%elo%", kd.getElo()
        );

        send(victim, "Messages.elo-loss",
                "%elo_change%", eloLoss,
                "%elo%", vd.getElo()
        );
    }

    // =====================
    // Helpers
    // =====================

    private double getDiffMultiplier(int diff) {
        if (diff < 100) return plugin.getConfig().getDouble("Abuse.Diff-Multiplier.100");
        if (diff < 300) return plugin.getConfig().getDouble("Abuse.Diff-Multiplier.300");
        if (diff < 500) return plugin.getConfig().getDouble("Abuse.Diff-Multiplier.500");
        return plugin.getConfig().getDouble("Abuse.Diff-Multiplier.default");
    }

    private double getPenaltyMultiplier(int diff) {
        if (diff > 300) return plugin.getConfig().getDouble("Abuse.Penalty-Multiplier.300");
        if (diff > 150) return plugin.getConfig().getDouble("Abuse.Penalty-Multiplier.150");
        return plugin.getConfig().getDouble("Abuse.Penalty-Multiplier.default");
    }

    private void send(Player p, String path, Object... vars) {
        String msg = plugin.getConfig().getString(path);
        if (msg == null) return;

        for (int i = 0; i < vars.length; i += 2) {
            msg = msg.replace(vars[i].toString(), vars[i + 1].toString());
        }

        p.sendMessage(plugin.color(msg));
    }

    private long parseTime(String s) {
        if (s == null) return 0;
        s = s.toLowerCase();
        if (s.endsWith("s")) return Long.parseLong(s.replace("s", "")) * 1000;
        if (s.endsWith("m")) return Long.parseLong(s.replace("m", "")) * 60_000;
        return Long.parseLong(s);
    }
}