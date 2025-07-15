package eu.kotori.donutTeams.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

public class TeamPlayer {

    private final UUID playerUuid;
    private TeamRole role;
    private final Instant joinDate;

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinDate = joinDate;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public TeamRole getRole() {
        return role;
    }

    public Instant getJoinDate() {
        return joinDate;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(playerUuid);
    }

    public boolean isOnline() {
        Player player = getBukkitPlayer();
        return player != null && player.isOnline();
    }
}