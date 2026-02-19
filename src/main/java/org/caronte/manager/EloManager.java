package org.caronte.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.caronte.Main;
import org.caronte.model.PlayerData;
import org.caronte.storage.SQLiteStorage;

public class EloManager implements Listener {

    private final Main plugin;
    private final SQLiteStorage storage;
    private final int ELO_CHANGE = 10;

    public EloManager(Main plugin, SQLiteStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;

        PlayerData killerData = storage.getPlayerData(killer.getUniqueId());
        PlayerData victimData = storage.getPlayerData(victim.getUniqueId());

        killerData.setElo(killerData.getElo() + ELO_CHANGE);
        killerData.addKill();

        victimData.setElo(victimData.getElo() - ELO_CHANGE);
        victimData.addDeath();

        storage.updatePlayer(killerData);
        storage.updatePlayer(victimData);

        sendMessages(killer, victim, killerData, victimData);
    }

    private void sendMessages(Player killer, Player victim, PlayerData kd, PlayerData vd) {

        String prefix = plugin.color(plugin.getConfig().getString("messages.prefix"));

        killer.sendMessage(prefix + plugin.color(
                plugin.getConfig().getString("messages.player-kill.killer-message")
                        .replace("%victim%", victim.getName())
        ));

        killer.sendMessage(prefix + plugin.color(
                plugin.getConfig().getString("messages.player-kill.killer-elo")
                        .replace("%elo_change%", String.valueOf(ELO_CHANGE))
                        .replace("%elo%", String.valueOf(kd.getElo()))
        ));

        victim.sendMessage(prefix + plugin.color(
                plugin.getConfig().getString("messages.player-kill.victim-message")
                        .replace("%killer%", killer.getName())
        ));

        victim.sendMessage(prefix + plugin.color(
                plugin.getConfig().getString("messages.player-kill.victim-elo")
                        .replace("%elo_change%", String.valueOf(ELO_CHANGE))
                        .replace("%elo%", String.valueOf(vd.getElo()))
        ));
    }
}