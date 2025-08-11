package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NoTeamGUI implements IRefreshableGUI, InventoryHolder {

    private final JustTeams plugin;
    private final Player viewer;
    private final Inventory inventory;

    public NoTeamGUI(JustTeams plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        String title = guiManager.getString("no-team-gui.title", "ᴛᴇᴀᴍ ᴍᴇɴᴜ");

        this.inventory = Bukkit.createInventory(this, 27, Component.text(title));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();

        ItemStack border = new ItemBuilder(guiManager.getMaterial("no-team-gui.items.border.material", Material.GRAY_STAINED_GLASS_PANE))
                .withName(guiManager.getString("no-team-gui.items.border.name", " "))
                .build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        inventory.setItem(12, new ItemBuilder(guiManager.getMaterial("no-team-gui.items.create-team.material", Material.WRITABLE_BOOK))
                .withName(guiManager.getString("no-team-gui.items.create-team.name", "<gradient:#4C9DDE:#4C96D2><bold>ᴄʀᴇᴀᴛᴇ ᴀ ᴛᴇᴀᴍ</bold></gradient>"))
                .withLore(guiManager.getStringList("no-team-gui.items.create-team.lore"))
                .build());

        inventory.setItem(14, new ItemBuilder(guiManager.getMaterial("no-team-gui.items.leaderboards.material", Material.EMERALD))
                .withName(guiManager.getString("no-team-gui.items.leaderboards.name", "<gradient:#4C9DDE:#4C96D2><bold>ᴠɪᴇᴡ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅs</bold></gradient>"))
                .withLore(guiManager.getStringList("no-team-gui.items.leaderboards.lore"))
                .build());
    }

    @Override
    public void open() {
        viewer.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}