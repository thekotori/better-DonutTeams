package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.gui.IRefreshableGUI;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.GuiConfigManager;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TeamSettingsGUI implements IRefreshableGUI, InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;

    public TeamSettingsGUI(DonutTeams plugin, Player viewer, Team team) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("team-settings-gui");
        String title = guiConfig.getString("title", "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs");
        int size = guiConfig.getInt("size", 27);

        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("team-settings-gui");
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
            if(itemConfig == null) continue;

            int slot = itemConfig.getInt("slot", -1);
            if (slot == -1) continue;

            Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
            String name = replacePlaceholders(itemConfig.getString("name", ""));
            List<String> lore = itemConfig.getStringList("lore").stream()
                    .map(this::replacePlaceholders)
                    .collect(Collectors.toList());

            inventory.setItem(slot, new ItemBuilder(material).withName(name).withLore(lore).build());
        }

        ConfigurationSection fillItemSection = guiConfig.getConfigurationSection("fill-item");
        if (fillItemSection != null) {
            ItemStack fillItem = new ItemBuilder(Material.matchMaterial(fillItemSection.getString("material", "GRAY_STAINED_GLASS_PANE")))
                    .withName(fillItemSection.getString("name", " "))
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        }
    }

    private String replacePlaceholders(String text) {
        if (team == null) return text;
        return text.replace("<team_tag>", team.getTag())
                .replace("<team_description>", team.getDescription());
    }

    @Override
    public void open() {
        viewer.openInventory(inventory);
    }

    public Team getTeam() {
        return team;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}