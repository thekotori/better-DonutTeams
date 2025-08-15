package eu.kotori.justTeams.listeners;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class TeamEnderChestListener implements Listener {

    private final JustTeams plugin;

    public TeamEnderChestListener(JustTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Team team) {
            plugin.getTaskRunner().runAsync(() -> {
                plugin.getTeamManager().saveEnderChest(team);
            });
        }
    }
}