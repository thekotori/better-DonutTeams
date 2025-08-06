package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ConfirmGUI implements InventoryHolder {

    private final Player viewer;
    private final Inventory inventory;
    private final Consumer<Boolean> callback;

    public ConfirmGUI(Player viewer, String title, Consumer<Boolean> callback) {
        this.viewer = viewer;
        this.callback = callback;
        this.inventory = Bukkit.createInventory(this, 27, Component.text(title));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        inventory.setItem(11, new ItemBuilder(Material.GREEN_WOOL)
                .withName("<green><bold>CONFIRM</bold></green>")
                .withLore("<gray>This action cannot be undone.")
                .build());

        inventory.setItem(15, new ItemBuilder(Material.RED_WOOL)
                .withName("<red><bold>CANCEL</bold></red>")
                .withLore("<gray>Return to the previous menu.")
                .build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public void handleConfirm() {
        viewer.closeInventory();
        callback.accept(true);
    }

    public void handleCancel() {
        viewer.closeInventory();
        callback.accept(false);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}