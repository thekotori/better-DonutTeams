package eu.kotori.justTeams.listeners;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final TeamManager teamManager;

    public PlayerConnectionListener(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teamManager.loadPlayerTeam(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        teamManager.unloadPlayer(event.getPlayer());
    }
}