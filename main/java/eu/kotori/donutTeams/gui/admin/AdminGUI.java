package eu.kotori.donutTeams.gui.admin;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AdminGUI implements InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Inventory inventory;

    public AdminGUI(DonutTeams plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴛᴇᴀᴍ ᴀᴅᴍɪɴ ᴘᴀɴᴇʟ"));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        inventory.setItem(13, new ItemBuilder(Material.ANVIL)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴍᴀɴᴀɢᴇ ᴛᴇᴀᴍs</bold></gradient>")
                .withLore(
                        "<gray>View a list of all teams on the server",
                        "<gray>to manage or disband them.",
                        "",
                        "<yellow>Click to view all teams.</yellow>"
                ).build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}