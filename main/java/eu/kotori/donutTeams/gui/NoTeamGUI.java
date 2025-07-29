package eu.kotori.donutTeams.gui;

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

public class NoTeamGUI implements InventoryHolder {

    private final DonutTeams plugin;
    private final Player viewer;
    private final Inventory inventory;

    public NoTeamGUI(DonutTeams plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴛᴇᴀᴍ ᴍᴇɴᴜ"));
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

        inventory.setItem(12, new ItemBuilder(Material.WRITABLE_BOOK)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴄʀᴇᴀᴛᴇ ᴀ ᴛᴇᴀᴍ</bold></gradient>")
                .withLore(
                        "<gray>Start your own team and invite your friends!</gray>",
                        "",
                        "<yellow>Click to begin the creation process.</yellow>"
                ).build());

        inventory.setItem(14, new ItemBuilder(Material.EMERALD)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴠɪᴇᴡ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅs</bold></gradient>")
                .withLore(
                        "<gray>See the top teams on the server.</gray>",
                        "",
                        "<yellow>Click to view leaderboards.</yellow>"
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