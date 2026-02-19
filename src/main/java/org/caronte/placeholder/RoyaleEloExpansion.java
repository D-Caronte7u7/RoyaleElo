package org.caronte.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.caronte.Main;
import org.caronte.model.PlayerData;

public class RoyaleEloExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public RoyaleEloExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "royaleelo"; }

    @Override
    public String getAuthor() { return "Caronte"; }

    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (player == null) return "";

        PlayerData data = plugin.getStorage().getPlayerData(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "elo" -> String.valueOf(data.getElo());
            case "kills" -> String.valueOf(data.getKills());
            case "deaths" -> String.valueOf(data.getDeaths());
            case "kdr" -> String.format("%.2f", data.getKDR());
            default -> null;
        };
    }
}