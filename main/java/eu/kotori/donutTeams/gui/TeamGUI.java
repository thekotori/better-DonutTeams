package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.team.TeamRole;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TeamGUI implements InventoryHolder {

    private final DonutTeams plugin;
    private final Team team;
    private final Inventory inventory;
    private final Player viewer;
    private Team.SortType currentSort;

    public TeamGUI(DonutTeams plugin, Team team, Player viewer) {
        this.plugin = plugin;
        this.team = team;
        this.viewer = viewer;
        this.currentSort = Team.SortType.JOIN_DATE;
        String title = "ᴛᴇᴀᴍ - " + team.getMembers().size() + "/" + plugin.getConfigManager().getMaxTeamSize();
        this.inventory = Bukkit.createInventory(this, 54, Component.text(title));
        initializeItems();
    }

    public void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        int memberSlot = 9;
        for (TeamPlayer member : team.getSortedMembers(currentSort)) {
            if (memberSlot >= 45) break;
            inventory.setItem(memberSlot++, createMemberHead(member));
        }

        TeamPlayer viewerMember = team.getMember(viewer.getUniqueId());
        if (viewerMember == null) {
            viewer.closeInventory();
            return;
        }

        boolean hasBankAccess = plugin.getConfigManager().isBankEnabled() &&
                (viewer.hasPermission("donutteams.bank.withdraw.bypass") || viewerMember.canWithdraw());

        if (hasBankAccess) {
            inventory.setItem(46, new ItemBuilder(Material.GOLD_NUGGET)
                    .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ʙᴀɴᴋ</bold></gradient>")
                    .withLore(
                            "<gray>Balance: <white>" + String.format("%,.2f", team.getBalance()) + "</white>",
                            "",
                            "<yellow>Click to manage the bank.</yellow>"
                    ).build());
        } else {
            inventory.setItem(46, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .withName("<red><bold>BANK LOCKED</bold></red>")
                    .withLore(
                            "<gray>You do not have permission to access the team bank.",
                            "<gray>Ask the team owner to grant you access."
                    ).build());
        }


        inventory.setItem(47, new ItemBuilder(Material.ENDER_PEARL)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ʜᴏᴍᴇ</bold></gradient>")
                .withLore(
                        "<gray>Click to teleport to your team's home.</gray>",
                        "",
                        team.getHomeLocation() == null ? "<red>Home not set." : "<yellow>Click to teleport!</yellow>"
                ).build());

        boolean hasECAccess = plugin.getConfigManager().isEnderChestEnabled() &&
                (viewer.hasPermission("donutteams.enderchest.bypass") || viewerMember.canUseEnderChest());

        if (hasECAccess) {
            inventory.setItem(48, new ItemBuilder(Material.ENDER_CHEST)
                    .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ</bold></gradient>")
                    .withLore(
                            "<gray>A shared inventory for your team.</gray>",
                            "",
                            "<yellow>Click to open.</yellow>"
                    ).build());
        } else {
            inventory.setItem(48, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .withName("<red><bold>ENDER CHEST LOCKED</bold></red>")
                    .withLore(
                            "<gray>You do not have permission to access the team ender chest.",
                            "<gray>Ask the team owner to grant you access."
                    ).build());
        }

        inventory.setItem(49, createSortItem());
        inventory.setItem(50, createPvpItem());

        if (team.hasElevatedPermissions(viewer.getUniqueId())) {
            inventory.setItem(51, new ItemBuilder(Material.COMPARATOR)
                    .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs</bold></gradient>")
                    .withLore("<yellow>Click to manage team settings.</yellow>")
                    .build());
        }

        if (team.isOwner(viewer.getUniqueId())) {
            inventory.setItem(52, new ItemBuilder(Material.TNT)
                    .withName("<red><bold>ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ</bold></red>")
                    .withLore(
                            "<gray>Permanently deletes the team.</gray>",
                            "<dark_red>This action cannot be undone!</dark_red>"
                    ).build());
        } else {
            inventory.setItem(52, new ItemBuilder(Material.BARRIER)
                    .withName("<red><bold>ʟᴇᴀᴠᴇ ᴛᴇᴀᴍ</bold></red>")
                    .withLore("<yellow>Click to leave the team.</yellow>")
                    .build());
        }
    }

    private ItemStack createPvpItem() {
        boolean pvp = team.isPvpEnabled();
        return new ItemBuilder(Material.IRON_SWORD)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ᴘᴠᴘ</bold></gradient>")
                .withLore(
                        "<gray>Determines if members can damage each other.</gray>",
                        "",
                        "<gray>Currently: " + (pvp ? "<green>ON" : "<red>OFF"),
                        "",
                        team.hasElevatedPermissions(viewer.getUniqueId()) ? "<yellow>Click to toggle" : "<red>Only the owner or co-owners can change this.</red>"
                ).build();
    }

    private ItemStack createSortItem() {
        return new ItemBuilder(Material.HOPPER)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>sᴏʀᴛ ᴍᴇᴍʙᴇʀs</bold></gradient>")
                .withLore(
                        "<gray>Click to change the sorting.</gray>",
                        "",
                        getSortLore(Team.SortType.JOIN_DATE),
                        getSortLore(Team.SortType.ALPHABETICAL),
                        getSortLore(Team.SortType.ONLINE_STATUS)
                ).build();
    }

    private String getSortLore(Team.SortType type) {
        String name = type.name().replace("_", " ").toLowerCase();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return (currentSort == type ? "<green>▪ <white>" : "<gray>▪ <white>") + name;
    }

    private ItemStack createMemberHead(TeamPlayer member) {
        String playerName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
        TeamRole role = member.getRole();
        String roleName = role.name().charAt(0) + role.name().substring(1).toLowerCase();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Role: <white>" + roleName + "</white>");
        lore.add("<gray>Joined: <white>" + formatter.format(member.getJoinDate()) + "</white>");
        lore.add("");
        if (team.hasElevatedPermissions(viewer.getUniqueId()) && role != TeamRole.OWNER) {
            if (!(role == TeamRole.CO_OWNER && !team.isOwner(viewer.getUniqueId()))) {
                lore.add("<yellow>Click to edit this member.</yellow>");
            }
        }

        ItemBuilder itemBuilder = new ItemBuilder(Material.PLAYER_HEAD)
                .asPlayerHead(member.getPlayerUuid())
                .withName((member.isOnline() ? "<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + ">" : "<gray>") + (getRoleIcon(role)) + playerName)
                .withLore(lore);

        if (role == TeamRole.OWNER) {
            itemBuilder.withGlow();
        }

        return itemBuilder.build();
    }

    private String getRoleIcon(TeamRole role) {
        return switch(role) {
            case OWNER -> "⭐ ";
            case CO_OWNER -> "✦ ";
            case MEMBER -> "";
        };
    }

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