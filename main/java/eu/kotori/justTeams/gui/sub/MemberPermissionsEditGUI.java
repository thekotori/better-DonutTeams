package eu.kotori.justTeams.gui.sub;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MemberPermissionsEditGUI implements InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Team team;
    private final TeamPlayer targetMember;
    private final Inventory inventory;
    private final ConfigurationSection guiConfig;

    public MemberPermissionsEditGUI(JustTeams plugin, Player viewer, Team team, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.targetMember = team.getMember(targetUuid);
        this.guiConfig = plugin.getGuiConfigManager().getGUI("member-permissions-edit-menu");

        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        String title = guiConfig.getString("title", "ᴘᴇʀᴍs: <target_name>").replace("<target_name>", targetName != null ? targetName : "Unknown");
        int size = guiConfig.getInt("size", 27);

        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        initializeItems();
    }

    public void initializeItems() {
        inventory.clear();
        if (targetMember == null) return;

        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                    if (itemConfig == null) continue;

                    Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
                    String name = itemConfig.getString("name", "");
                    List<String> lore = itemConfig.getStringList("lore");

                    name = replacePlaceholders(name);
                    lore = lore.stream().map(this::replacePlaceholders).collect(Collectors.toList());

                    inventory.setItem(slot, new ItemBuilder(material).withName(name).withLore(lore).build());
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot '" + key + "' in gui.yml for member-permissions-edit-menu.");
                }
            }
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
        if (targetMember == null) return text;
        return text.replace("<withdraw_status>", targetMember.canWithdraw() ? "<green>ENABLED" : "<red>DISABLED")
                .replace("<enderchest_status>", targetMember.canUseEnderChest() ? "<green>ENABLED" : "<red>DISABLED")
                .replace("<set_home_status>", targetMember.canSetHome() ? "<green>ENABLED" : "<red>DISABLED")
                .replace("<use_home_status>", targetMember.canUseHome() ? "<green>ENABLED" : "<red>DISABLED");
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public Team getTeam() {
        return team;
    }

    public TeamPlayer getTargetMember() {
        return targetMember;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}