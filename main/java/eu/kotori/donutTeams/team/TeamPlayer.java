package eu.kotori.donutTeams.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.UUID;

public class TeamPlayer {

    private final UUID playerUuid;
    private TeamRole role;
    private final Instant joinDate;
    private boolean canWithdraw;
    private boolean canUseEnderChest;

    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate, boolean canWithdraw, boolean canUseEnderChest) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
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

    public boolean canWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public boolean canUseEnderChest() {
        return canUseEnderChest;
    }

    public void setCanUseEnderChest(boolean canUseEnderChest) {
        this.canUseEnderChest = canUseEnderChest;
    }

    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(playerUuid);
    }

    public boolean isOnline() {
        Player player = getBukkitPlayer();
        return player != null && player.isOnline();
    }
}