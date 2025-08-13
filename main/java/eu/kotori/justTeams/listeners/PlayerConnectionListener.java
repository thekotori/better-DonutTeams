package eu.kotori.justTeams.listeners;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
public class PlayerConnectionListener implements Listener {
    private final JustTeams plugin;
    private final TeamManager teamManager;
    public PlayerConnectionListener(JustTeams plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teamManager.handlePendingTeleport(player);
        teamManager.loadPlayerTeam(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        teamManager.unloadPlayer(event.getPlayer());
    }
}