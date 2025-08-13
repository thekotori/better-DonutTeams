package eu.kotori.justTeams.gui.admin;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AdminTeamListGUI implements InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Inventory inventory;
    private final List<Team> allTeams;
    private int page;

    public AdminTeamListGUI(JustTeams plugin, Player viewer, List<Team> allTeams, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.allTeams = allTeams;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("All Teams - Page " + (page + 1)));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        int maxItemsPerPage = 36;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allTeams.size());

        for (int i = startIndex; i < endIndex; i++) {
            Team team = allTeams.get(i);
            String ownerName = Bukkit.getOfflinePlayer(team.getOwnerUuid()).getName();
            inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD)
                    .asPlayerHead(team.getOwnerUuid())
                    .withName("<gold><bold>" + team.getName() + "</bold></gold>")
                    .withLore(
                            "<gray>Owner: <white>" + (ownerName != null ? ownerName : "Unknown"),
                            "<gray>Members: <white>" + team.getMembers().size(),
                            "",
                            "<yellow>Click to manage this team.</yellow>"
                    )
                    .withAction("team-head")
                    .build());
        }

        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).withName("<gray>Previous Page").withAction("previous-page").build());
        }
        if (endIndex < allTeams.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).withName("<gray>Next Page").withAction("next-page").build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).withName("<red>Back to Admin Menu").withAction("back-button").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public List<Team> getAllTeams() {
        return allTeams;
    }

    public int getPage() {
        return page;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}