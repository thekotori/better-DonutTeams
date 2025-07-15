package eu.kotori.donutTeams.listeners;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final TeamManager teamManager;

    public PlayerConnectionListener(DonutTeams plugin) {
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