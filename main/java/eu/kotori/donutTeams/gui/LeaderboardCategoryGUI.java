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

public class LeaderboardCategoryGUI implements InventoryHolder {

    private final DonutTeams plugin;
    private final Player viewer;
    private final Inventory inventory;

    public LeaderboardCategoryGUI(DonutTeams plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ"));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        inventory.setItem(11, new ItemBuilder(Material.DIAMOND_SWORD)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴛᴏᴘ ᴋɪʟʟs</bold></gradient>")
                .withLore("<gray>Shows the top 10 teams with the most kills.</gray>").build());

        inventory.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴛᴏᴘ ʙᴀʟᴀɴᴄᴇ</bold></gradient>")
                .withLore("<gray>Shows the top 10 richest teams.</gray>").build());

        inventory.setItem(15, new ItemBuilder(Material.PLAYER_HEAD)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴛᴏᴘ ᴍᴇᴍʙᴇʀs</bold></gradient>")
                .withLore("<gray>Shows the top 10 teams with the most members.</gray>").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public DonutTeams getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}