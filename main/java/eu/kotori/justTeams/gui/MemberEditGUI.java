package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MemberEditGUI implements IRefreshableGUI, InventoryHolder {

    private final JustTeams plugin;
    private final Team team;
    private final Player viewer;
    private final UUID targetUuid;
    private final Inventory inventory;

    public MemberEditGUI(JustTeams plugin, Team team, Player viewer, UUID targetUuid) {
        this.plugin = plugin;
        this.team = team;
        this.viewer = viewer;
        this.targetUuid = targetUuid;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("member-edit-gui");
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String title = guiConfig.getString("title", "Edit: <player_name>")
                .replace("<player_name>", target.getName() != null ? target.getName() : "Unknown");
        int size = guiConfig.getInt("size", 54);

        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }

    public void initializeItems() {
        inventory.clear();
        TeamPlayer targetMember = getTargetMember();
        if (targetMember == null) return;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("member-edit-gui");
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) return;

        setItemFromConfig("player-info-head", itemsSection);

        if (targetMember.getRole() == TeamRole.MEMBER) {
            setItemFromConfig("promote-button", itemsSection);
        } else if (targetMember.getRole() == TeamRole.CO_OWNER) {
            setItemFromConfig("demote-button", itemsSection);
        }

        setItemFromConfig("kick-button", itemsSection);
        if (team.isOwner(viewer.getUniqueId())) {
            setItemFromConfig("transfer-button", itemsSection);
        }

        setItemFromConfig("withdraw-permission", itemsSection);
        setItemFromConfig("enderchest-permission", itemsSection);
        setItemFromConfig("sethome-permission", itemsSection);
        setItemFromConfig("usehome-permission", itemsSection);
        setItemFromConfig("back-button", itemsSection);

        ConfigurationSection fillConfig = guiConfig.getConfigurationSection("fill-item");
        if (fillConfig != null) {
            ItemStack fillItem = new ItemBuilder(Material.matchMaterial(fillConfig.getString("material", "GRAY_STAINED_GLASS_PANE")))
                    .withName(fillConfig.getString("name", " "))
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, fillItem);
                }
            }
        }
    }

    private void setItemFromConfig(String key, ConfigurationSection parentSection) {
        ConfigurationSection itemConfig = parentSection.getConfigurationSection(key);
        if (itemConfig == null) return;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        String name = replacePlaceholders(itemConfig.getString("name", ""));
        List<String> lore = itemConfig.getStringList("lore").stream()
                .map(this::replacePlaceholders)
                .collect(Collectors.toList());
        int slot = itemConfig.getInt("slot");

        ItemBuilder builder = new ItemBuilder(material).withName(name).withLore(lore);

        if (key.equals("player-info-head")) {
            builder.asPlayerHead(targetUuid);
        }

        inventory.setItem(slot, builder.build());
    }

    private String replacePlaceholders(String text) {
        TeamPlayer targetMember = getTargetMember();
        if (targetMember == null) return text;
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetUuid);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

        return text
                .replace("<player_name>", targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown")
                .replace("<role>", getRoleName(targetMember.getRole()))
                .replace("<joindate>", formatter.format(targetMember.getJoinDate()))
                .replace("<withdraw_status>", getStatus(targetMember.canWithdraw()))
                .replace("<enderchest_status>", getStatus(targetMember.canUseEnderChest()))
                .replace("<set_home_status>", getStatus(targetMember.canSetHome()))
                .replace("<use_home_status>", getStatus(targetMember.canUseHome()));
    }

    private String getStatus(boolean hasPerm) {
        return hasPerm ? "<green>ENABLED" : "<red>DISABLED";
    }

    private String getRoleName(TeamRole role) {
        String name = role.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public void open() {
        viewer.openInventory(inventory);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public TeamPlayer getTargetMember() {
        return team.getMember(targetUuid);
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