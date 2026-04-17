package ru.liko.pjmbasemod.common.match;

import java.util.UUID;

/**
 * Хранит статистику игрока за матч.
 */
public class PlayerMatchStats {
    private final UUID playerUUID;
    private int kills = 0;
    private int deaths = 0;
    private int assists = 0;
    private int capturePoints = 0;
    private int score = 0;

    public PlayerMatchStats(UUID uuid) {
        this.playerUUID = uuid;
    }

    public void addKill() {
        kills++;
        score += 100;
    }

    public void addDeath() {
        deaths++;
    }

    public void addAssist() {
        assists++;
        score += 25;
    }

    public void addCapturePoints(int points) {
        capturePoints += points;
        score += points;
    }

    public void reset() {
        kills = 0;
        deaths = 0;
        assists = 0;
        capturePoints = 0;
        score = 0;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getAssists() {
        return assists;
    }

    public int getCapturePoints() {
        return capturePoints;
    }

    public int getScore() {
        return score;
    }

    public float getKDRatio() {
        return deaths == 0 ? kills : (float) kills / deaths;
    }
}
