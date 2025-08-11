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

public class AdminTeamManageGUI implements InventoryHolder {

    private final JustTeams plugin;
    private final Player viewer;
    private final Team targetTeam;
    private final Inventory inventory;

    public AdminTeamManageGUI(JustTeams plugin, Player viewer, Team targetTeam) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetTeam = targetTeam;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Manage: " + targetTeam.getName()));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        inventory.setItem(13, new ItemBuilder(Material.TNT)
                .withName("<red><bold>DISBAND TEAM</bold></red>")
                .withLore(
                        "<gray>Permanently deletes this team.",
                        "<dark_red>This action cannot be undone!",
                        "",
                        "<yellow>Click to disband.</yellow>"
                ).build());

        inventory.setItem(22, new ItemBuilder(Material.ARROW).withName("<gray>Back to Team List").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public Team getTargetTeam() {
        return targetTeam;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}