package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeamGUI implements IRefreshableGUI, InventoryHolder {

    private final JustTeams plugin;
    private final Team team;
    private final Inventory inventory;
    private final Player viewer;
    private Team.SortType currentSort;

    public TeamGUI(JustTeams plugin, Team team, Player viewer) {
        this.plugin = plugin;
        this.team = team;
        this.viewer = viewer;
        this.currentSort = Team.SortType.JOIN_DATE;

        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("team-gui");
        String title = guiConfig.getString("title", "Team")
                .replace("<members>", String.valueOf(team.getMembers().size()))
                .replace("<max_members>", String.valueOf(plugin.getConfigManager().getMaxTeamSize()));
        int size = guiConfig.getInt("size", 54);

        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }

    public void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("team-gui");
        if (guiConfig == null) return;
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig == null) return;

        ItemStack border = new ItemBuilder(guiManager.getMaterial("team-gui.fill-item.material", Material.GRAY_STAINED_GLASS_PANE))
                .withName(guiManager.getString("team-gui.fill-item.name", " "))
                .build();

        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        int memberSlot = 9;
        for (TeamPlayer member : team.getSortedMembers(currentSort)) {
            if (memberSlot >= 45) break;
            inventory.setItem(memberSlot++, createMemberHead(member, itemsConfig.getConfigurationSection("player-head")));
        }

        TeamPlayer viewerMember = team.getMember(viewer.getUniqueId());
        if (viewerMember == null) {
            viewer.closeInventory();
            return;
        }

        setItemFromConfig(itemsConfig, "sort");
        setItemFromConfig(itemsConfig, "pvp-toggle");

        if(team.hasElevatedPermissions(viewer.getUniqueId())) {
            setItemFromConfig(itemsConfig, "settings");
        } else {
            setItemFromConfig(itemsConfig, "settings-locked");
        }

        setItemFromConfig(itemsConfig, team.isOwner(viewer.getUniqueId()) ? "disband-button" : "leave-button");

        if (plugin.getConfigManager().isBankEnabled()) {
            setItemFromConfig(itemsConfig, "bank");
        } else {
            setItemFromConfig(itemsConfig, "bank-locked");
        }

        if (plugin.getConfigManager().isEnderChestEnabled()) {
            boolean hasAccess = viewer.hasPermission("justteams.bypass.enderchest.use") || viewerMember.canUseEnderChest();
            setItemFromConfig(itemsConfig, hasAccess ? "ender-chest" : "ender-chest-locked");
        } else {
            setItemFromConfig(itemsConfig, "ender-chest-locked");
        }

        setItemFromConfig(itemsConfig, "home");
    }

    private void setItemFromConfig(ConfigurationSection itemsConfig, String key) {
        ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(key);
        if (itemConfig == null) return;

        int slot = itemConfig.getInt("slot", -1);
        if (slot == -1) return;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        ItemBuilder builder = new ItemBuilder(material);

        String name = replacePlaceholders(itemConfig.getString("name", ""));
        builder.withName(name);

        List<String> lore = new ArrayList<>();
        if (key.equals("home")) {
            lore.addAll(itemConfig.getStringList(team.getHomeLocation() == null ? "lore-not-set" : "lore-set"));
        } else {
            lore.addAll(itemConfig.getStringList("lore"));
        }
        builder.withLore(lore.stream().map(this::replacePlaceholders).collect(Collectors.toList()));
        builder.withAction(key);

        inventory.setItem(slot, builder.build());
    }

    private String replacePlaceholders(String text) {
        if (text == null) return "";
        String pvpStatus = team.isPvpEnabled() ?
                plugin.getGuiConfigManager().getString("team-gui.items.pvp-toggle.status-on", "<green>ON") :
                plugin.getGuiConfigManager().getString("team-gui.items.pvp-toggle.status-off", "<red>OFF");

        String pvpPrompt = team.hasElevatedPermissions(viewer.getUniqueId()) ?
                plugin.getGuiConfigManager().getString("team-gui.items.pvp-toggle.can-toggle-prompt", "<yellow>Click to toggle") :
                plugin.getGuiConfigManager().getString("team-gui.items.pvp-toggle.cannot-toggle-prompt", "<red>Permission denied");

        return text
                .replace("<balance>", String.format("%,.2f", team.getBalance()))
                .replace("<status>", pvpStatus)
                .replace("<permission_prompt>", pvpPrompt)
                .replace("<sort_status_join_date>", getSortLore(Team.SortType.JOIN_DATE))
                .replace("<sort_status_alphabetical>", getSortLore(Team.SortType.ALPHABETICAL))
                .replace("<sort_status_online_status>", getSortLore(Team.SortType.ONLINE_STATUS));
    }

    private ItemStack createMemberHead(TeamPlayer member, ConfigurationSection headConfig) {
        String playerName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
        String nameFormat = member.isOnline() ?
                headConfig.getString("online-name-format", "<green><player>") :
                headConfig.getString("offline-name-format", "<gray><player>");

        String name = nameFormat
                .replace("<role_icon>", getRoleIcon(member.getRole()))
                .replace("<player>", playerName != null ? playerName : "Unknown");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

        TeamPlayer viewerMember = team.getMember(viewer.getUniqueId());
        boolean canEdit = false;
        if (viewerMember != null) {
            canEdit = (viewerMember.getRole() == TeamRole.OWNER && member.getRole() != TeamRole.OWNER) ||
                    (viewerMember.getRole() == TeamRole.CO_OWNER && member.getRole() == TeamRole.MEMBER);
        }

        String permissionPrompt = canEdit ?
                headConfig.getString("can-edit-prompt", "<yellow>Click to edit.") :
                headConfig.getString("cannot-edit-prompt", "");

        List<String> lore = headConfig.getStringList("lore").stream()
                .map(line -> line
                        .replace("<role>", getRoleName(member.getRole()))
                        .replace("<joindate>", formatter.format(member.getJoinDate()))
                        .replace("<permission_prompt>", permissionPrompt))
                .collect(Collectors.toList());

        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .asPlayerHead(member.getPlayerUuid())
                .withName(name)
                .withLore(lore)
                .withAction("player-head");

        if (member.getRole() == TeamRole.OWNER) {
            builder.withGlow();
        }

        return builder.build();
    }

    private String getSortLore(Team.SortType type) {
        String name = type.name().replace("_", " ").toLowerCase();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String prefix = (currentSort == type ? "<green>▪ <white>" : "<gray>▪ <white>");
        return prefix + name;
    }

    private String getRoleIcon(TeamRole role) {
        return switch (role) {
            case OWNER -> "⭐ ";
            case CO_OWNER -> "✦ ";
            case MEMBER -> "";
        };
    }

    private String getRoleName(TeamRole role) {
        String name = role.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public void open() {
        viewer.openInventory(inventory);
    }

    public void cycleSort() {
        currentSort = switch (currentSort) {
            case JOIN_DATE -> Team.SortType.ALPHABETICAL;
            case ALPHABETICAL -> Team.SortType.ONLINE_STATUS;
            case ONLINE_STATUS -> Team.SortType.JOIN_DATE;
        };
        initializeItems();
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