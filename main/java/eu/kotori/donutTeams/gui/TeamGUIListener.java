package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.gui.sub.MemberEditGUI;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class TeamGUIListener implements Listener {

    private final DonutTeams plugin;
    private final TeamManager teamManager;

    public TeamGUIListener(DonutTeams plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onTeamGUIClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof TeamGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                if (team.isOwner(player.getUniqueId()) && !team.isOwner(skullMeta.getPlayerProfile().getId())) {
                    new MemberEditGUI(plugin, team, player, skullMeta.getPlayerProfile().getId()).open();
                }
            }
            return;
        }

        switch (clickedItem.getType()) {
            case TNT -> {
                player.closeInventory();
                teamManager.disbandTeam(player);
            }
            case BARRIER -> {
                player.closeInventory();
                teamManager.leaveTeam(player);
            }
            case ENDER_PEARL -> {
                player.closeInventory();
                teamManager.teleportToHome(player);
            }
            case IRON_SWORD -> {
                teamManager.togglePvp(player);
                gui.initializeItems();
            }
            case HOPPER -> gui.cycleSort();
            default -> {}
        }
    }

    @EventHandler
    public void onMemberEditGUIClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof MemberEditGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        switch (clickedItem.getType()) {
            case RED_WOOL -> {
                player.closeInventory();
                plugin.getTeamManager().kickPlayer(player, gui.getTargetUuid());
            }
            case BEACON -> {
                player.closeInventory();
                plugin.getTeamManager().transferOwnership(player, gui.getTargetUuid());
            }
            case ARROW -> new TeamGUI(plugin, team, player).open();
            default -> {}
        }
    }
}