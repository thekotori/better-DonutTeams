package eu.kotori.donutTeams.gui.admin;

import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AdminDisbandConfirmGUI implements InventoryHolder {
    private final Player viewer;
    private final Team targetTeam;
    private final Inventory inventory;

    public AdminDisbandConfirmGUI(Player viewer, Team targetTeam) {
        this.viewer = viewer;
        this.targetTeam = targetTeam;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Confirm Disband: " + targetTeam.getName()));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        inventory.setItem(11, new ItemBuilder(Material.GREEN_WOOL)
                .withName("<green><bold>CONFIRM DISBAND</bold></green>")
                .withLore("<gray>The team <white>" + targetTeam.getName() + "</white> will be deleted forever.")
                .build());

        inventory.setItem(15, new ItemBuilder(Material.RED_WOOL)
                .withName("<red><bold>CANCEL</bold></red>")
                .withLore("<gray>Return to the previous menu.")
                .build());
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