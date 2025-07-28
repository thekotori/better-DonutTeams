package eu.kotori.donutTeams.listeners;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class TeamEnderChestListener implements Listener {

    private final DonutTeams plugin;

    public TeamEnderChestListener(DonutTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Team team) {
            Player player = (Player) event.getPlayer();

            plugin.getTaskRunner().runAsync(() -> {
                plugin.getTeamManager().saveEnderChest(team);
            });
        }
    }
}