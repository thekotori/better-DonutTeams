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
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().isBedrockSupportEnabled() && plugin.getBedrockSupport().isBedrockPlayer(player)) {
            plugin.getLogger().info("Bedrock player joined: " + player.getName() +
                " (UUID: " + player.getUniqueId() + ")");
            if (plugin.getConfigManager().isShowGamertags()) {
                String gamertag = plugin.getBedrockSupport().getBedrockGamertag(player);
                if (gamertag != null && !gamertag.equals(player.getName())) {
                    plugin.getLogger().info("Bedrock player gamertag: " + gamertag);
                }
            }
        }
        teamManager.handlePendingTeleport(player);
        teamManager.loadPlayerTeam(player);
    }
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().isBedrockSupportEnabled()) {
            plugin.getBedrockSupport().clearPlayerCache(player.getUniqueId());
        }
        teamManager.unloadPlayer(player);
    }
}
