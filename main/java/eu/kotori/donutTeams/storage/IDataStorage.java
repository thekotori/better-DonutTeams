package eu.kotori.donutTeams.storage;

import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import org.bukkit.Location;

import java.util.List;
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
    void transferOwnership(int teamId, UUID newOwnerUuid, UUID oldOwnerUuid);
    void setPvpStatus(int teamId, boolean status); // <-- NEU
}