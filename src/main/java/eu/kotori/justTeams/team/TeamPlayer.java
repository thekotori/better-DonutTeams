package eu.kotori.justTeams.team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.time.Instant;
import java.util.UUID;
public class TeamPlayer {
    private final UUID playerUuid;
    private volatile TeamRole role;
    private final Instant joinDate;
    private volatile boolean canWithdraw;
    private volatile boolean canUseEnderChest;
    private volatile boolean canSetHome;
    private volatile boolean canUseHome;
    private volatile boolean canEditMembers;
    private volatile boolean canEditCoOwners;
    private volatile boolean canKickMembers;
    private volatile boolean canPromoteMembers;
    private volatile boolean canDemoteMembers;
    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate, boolean canWithdraw, boolean canUseEnderChest, boolean canSetHome, boolean canUseHome) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
        this.canSetHome = canSetHome;
        this.canUseHome = canUseHome;
        setDefaultEditingPermissions();
    }
    public TeamPlayer(UUID playerUuid, TeamRole role, Instant joinDate, boolean canWithdraw, boolean canUseEnderChest, boolean canSetHome, boolean canUseHome,
                     boolean canEditMembers, boolean canEditCoOwners, boolean canKickMembers, boolean canPromoteMembers, boolean canDemoteMembers) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinDate = joinDate;
        this.canWithdraw = canWithdraw;
        this.canUseEnderChest = canUseEnderChest;
        this.canSetHome = canSetHome;
        this.canUseHome = canUseHome;
        this.canEditMembers = canEditMembers;
        this.canEditCoOwners = canEditCoOwners;
        this.canKickMembers = canKickMembers;
        this.canPromoteMembers = canPromoteMembers;
        this.canDemoteMembers = canDemoteMembers;
    }
    private void setDefaultEditingPermissions() {
        switch (role) {
            case OWNER:
                this.canEditMembers = true;
                this.canEditCoOwners = true;
                this.canKickMembers = true;
                this.canPromoteMembers = true;
                this.canDemoteMembers = true;
                break;
            case CO_OWNER:
                this.canEditMembers = true;
                this.canEditCoOwners = false;
                this.canKickMembers = true;
                this.canPromoteMembers = false;
                this.canDemoteMembers = false;
                break;
            case MEMBER:
                this.canEditMembers = false;
                this.canEditCoOwners = false;
                this.canKickMembers = false;
                this.canPromoteMembers = false;
                this.canDemoteMembers = false;
                break;
        }
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
        setDefaultEditingPermissions();
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
    public boolean canSetHome() {
        return canSetHome;
    }
    public void setCanSetHome(boolean canSetHome) {
        this.canSetHome = canSetHome;
    }
    public boolean canUseHome() {
        return canUseHome;
    }
    public void setCanUseHome(boolean canUseHome) {
        this.canUseHome = canUseHome;
    }
    public boolean canEditMembers() {
        return canEditMembers;
    }
    public void setCanEditMembers(boolean canEditMembers) {
        this.canEditMembers = canEditMembers;
    }
    public boolean canEditCoOwners() {
        return canEditCoOwners;
    }
    public void setCanEditCoOwners(boolean canEditCoOwners) {
        this.canEditCoOwners = canEditCoOwners;
    }
    public boolean canKickMembers() {
        return canKickMembers;
    }
    public void setCanKickMembers(boolean canKickMembers) {
        this.canKickMembers = canKickMembers;
    }
    public boolean canPromoteMembers() {
        return canPromoteMembers;
    }
    public void setCanPromoteMembers(boolean canPromoteMembers) {
        this.canPromoteMembers = canPromoteMembers;
    }
    public boolean canDemoteMembers() {
        return canDemoteMembers;
    }
    public void setCanDemoteMembers(boolean canDemoteMembers) {
        this.canDemoteMembers = canDemoteMembers;
    }
    public boolean canEditPlayer(TeamPlayer target) {
        if (target == null) return false;
        if (this.playerUuid.equals(target.getPlayerUuid())) {
            return false;
        }
        if (this.role == TeamRole.OWNER) {
            return true;
        }
        if (this.role == TeamRole.CO_OWNER) {
            if (target.getRole() == TeamRole.MEMBER) {
                return this.canEditMembers;
            } else if (target.getRole() == TeamRole.CO_OWNER) {
                return this.canEditCoOwners;
            }
        }
        return false;
    }
    public boolean canKickPlayer(TeamPlayer target) {
        if (target == null) return false;
        if (this.playerUuid.equals(target.getPlayerUuid())) {
            return false;
        }
        if (this.role == TeamRole.OWNER) {
            return true;
        }
        if (this.role == TeamRole.CO_OWNER) {
            if (target.getRole() == TeamRole.MEMBER) {
                return this.canKickMembers;
            }
        }
        return false;
    }
    public boolean canPromotePlayer(TeamPlayer target) {
        if (target == null) return false;
        if (this.playerUuid.equals(target.getPlayerUuid())) {
            return false;
        }
        if (this.role == TeamRole.OWNER) {
            return target.getRole() == TeamRole.MEMBER;
        }
        return false;
    }
    public boolean canDemotePlayer(TeamPlayer target) {
        if (target == null) return false;
        if (this.playerUuid.equals(target.getPlayerUuid())) {
            return false;
        }
        if (this.role == TeamRole.OWNER) {
            return target.getRole() == TeamRole.CO_OWNER;
        }
        return false;
    }
    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(playerUuid);
    }
    public boolean isOnline() {
        Player player = getBukkitPlayer();
        return player != null && player.isOnline();
    }
}
