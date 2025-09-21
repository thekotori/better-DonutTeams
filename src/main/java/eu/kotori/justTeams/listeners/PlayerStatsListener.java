package eu.kotori.justTeams.listeners;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
public class PlayerStatsListener implements Listener {
    private final TeamManager teamManager;
    public PlayerStatsListener(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
    }
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        Team victimTeam = teamManager.getPlayerTeam(victim.getUniqueId());
        if (victimTeam != null) {
            victimTeam.incrementDeaths();
            JustTeams.getInstance().getStorageManager().getStorage().updateTeamStats(victimTeam.getId(), victimTeam.getKills(), victimTeam.getDeaths());
        }
        if (killer != null) {
            Team killerTeam = teamManager.getPlayerTeam(killer.getUniqueId());
            if (killerTeam != null) {
                if (victimTeam == null || killerTeam.getId() != victimTeam.getId()) {
                    killerTeam.incrementKills();
                    JustTeams.getInstance().getStorageManager().getStorage().updateTeamStats(killerTeam.getId(), killerTeam.getKills(), killerTeam.getDeaths());
                }
            }
        }
    }
}
