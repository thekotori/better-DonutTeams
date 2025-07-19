package eu.kotori.donutTeams.storage;

import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IDataStorage {
    boolean init();
    void shutdown();
    boolean isConnected();
    Optional<Team> createTeam(String name, String tag, UUID ownerUuid, boolean defaultPvp);
    void deleteTeam(int teamId);
    void addMemberToTeam(int teamId, UUID playerUuid);
    void removeMemberFromTeam(UUID playerUuid);
    Optional<Team> findTeamByPlayer(UUID playerUuid);
    Optional<Team> findTeamByName(String name);
    Optional<Team> findTeamById(int id);
    List<TeamPlayer> getTeamMembers(int teamId);
    void setTeamHome(int teamId, Location location);
    void setTeamTag(int teamId, String tag);
    void setTeamDescription(int teamId, String description);
    void transferOwnership(int teamId, UUID newOwnerUuid, UUID oldOwnerUuid);
    void setPvpStatus(int teamId, boolean status);
    void updateTeamBalance(int teamId, double balance);
    void updateTeamStats(int teamId, int kills, int deaths);
    void saveEnderChest(int teamId, String serializedInventory);
    String getEnderChest(int teamId);
    void updateMemberPermissions(int teamId, UUID memberUuid, boolean canWithdraw, boolean canUseEnderChest);
    Map<Integer, Team> getTopTeamsByKills(int limit);
    Map<Integer, Team> getTopTeamsByBalance(int limit);
    Map<Integer, Team> getTopTeamsByMembers(int limit);
}