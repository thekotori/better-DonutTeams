package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
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

public class BankGUI implements IRefreshableGUI, InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;

    public BankGUI(DonutTeams plugin, Player viewer, Team team) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("bank-gui");
        String title = guiConfig.getString("title", "ᴛᴇᴀᴍ ʙᴀɴᴋ");
        int size = guiConfig.getInt("size", 27);

        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("bank-gui");
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) return;

        setItemFromConfig(itemsSection, "deposit");
        setItemFromConfig(itemsSection, "balance");
        setItemFromConfig(itemsSection, "back-button");

        TeamPlayer member = team.getMember(viewer.getUniqueId());
        boolean canWithdraw = (member != null && member.canWithdraw()) || viewer.hasPermission("donutteams.bypass.bank.withdraw");

        if(canWithdraw) {
            setItemFromConfig(itemsSection, "withdraw");
        } else {
            setItemFromConfig(itemsSection, "withdraw-locked");
        }

        ConfigurationSection fillConfig = guiConfig.getConfigurationSection("fill-item");
        if(fillConfig != null) {
            ItemStack fillItem = new ItemBuilder(Material.matchMaterial(fillConfig.getString("material", "GRAY_STAINED_GLASS_PANE")))
                    .withName(fillConfig.getString("name", " "))
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                if(inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        }
    }

    private void setItemFromConfig(ConfigurationSection itemsSection, String key) {
        ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
        if (itemConfig == null) return;

        int slot = itemConfig.getInt("slot", -1);
        if (slot == -1) return;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        String name = replacePlaceholders(itemConfig.getString("name", ""));
        List<String> lore = itemConfig.getStringList("lore").stream()
                .map(this::replacePlaceholders)
                .collect(Collectors.toList());

        ItemBuilder builder = new ItemBuilder(material).withName(name).withLore(lore);
        if (key.equals("balance")) {
            builder.withGlow();
        }

        inventory.setItem(slot, builder.build());
    }

    private String replacePlaceholders(String text) {
        if (text == null) return "";
        return text.replace("<balance>", String.format("%,.2f", team.getBalance()));
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