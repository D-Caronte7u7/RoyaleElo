package org.caronte.model;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int elo;
    private int kills;
    private int deaths;

    public PlayerData(UUID uuid, int elo, int kills, int deaths) {
        this.uuid = uuid;
        this.elo = elo;
        this.kills = kills;
        this.deaths = deaths;
    }

    public UUID getUuid() { return uuid; }
    public int getElo() { return elo; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }

    public void setElo(int elo) { this.elo = elo; }
    public void addKill() { this.kills++; }
    public void addDeath() { this.deaths++; }

    public double getKDR() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}