package eu.kotori.justTeams.gui.admin;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
public class AdminGUI implements InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Inventory inventory;
    public AdminGUI(JustTeams plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("admin-gui");
        String title = guiConfig.getString("title", "ᴛᴇᴀᴍ ᴀᴅᴍɪɴ ᴘᴀɴᴇʟ");
        int size = guiConfig.getInt("size", 27);
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        initializeItems(guiConfig);
    }
    private void initializeItems(ConfigurationSection guiConfig) {
        inventory.clear();
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) return;
        ConfigurationSection manageTeamsItem = itemsSection.getConfigurationSection("manage-teams");
        if (manageTeamsItem != null) {
            inventory.setItem(manageTeamsItem.getInt("slot"), new ItemBuilder(Material.matchMaterial(manageTeamsItem.getString("material")))
                    .withName(manageTeamsItem.getString("name"))
                    .withLore(manageTeamsItem.getStringList("lore"))
                    .withAction("manage-teams")
                    .build());
        }

        ConfigurationSection viewEnderchestItem = itemsSection.getConfigurationSection("view-enderchest");
        if (viewEnderchestItem != null) {
            inventory.setItem(viewEnderchestItem.getInt("slot"), new ItemBuilder(Material.matchMaterial(viewEnderchestItem.getString("material")))
                    .withName(viewEnderchestItem.getString("name"))
                    .withLore(viewEnderchestItem.getStringList("lore"))
                    .withAction("view-enderchest")
                    .build());
        }

        ConfigurationSection reloadPluginItem = itemsSection.getConfigurationSection("reload-plugin");
        if (reloadPluginItem != null) {
            inventory.setItem(reloadPluginItem.getInt("slot"), new ItemBuilder(Material.matchMaterial(reloadPluginItem.getString("material")))
                    .withName(reloadPluginItem.getString("name"))
                    .withLore(reloadPluginItem.getStringList("lore"))
                    .withAction("reload-plugin")
                    .build());
        }

        ConfigurationSection closeItem = itemsSection.getConfigurationSection("close");
        if (closeItem != null) {
            inventory.setItem(closeItem.getInt("slot"), new ItemBuilder(Material.matchMaterial(closeItem.getString("material")))
                    .withName(closeItem.getString("name"))
                    .withLore(closeItem.getStringList("lore"))
                    .withAction("close")
                    .build());
        }
        ConfigurationSection fillConfig = guiConfig.getConfigurationSection("fill-item");
        if (fillConfig != null) {
            ItemStack fillItem = new ItemBuilder(Material.matchMaterial(fillConfig.getString("material")))
                    .withName(fillConfig.getString("name", " "))
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        }
    }
    public void open() {
        viewer.openInventory(inventory);
    }
    public Inventory getInventory() {
        return inventory;
    }
}
