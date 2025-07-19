package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
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

        inventory.setItem(46, new ItemBuilder(Material.GOLD_NUGGET)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ʙᴀɴᴋ</bold></gradient>")
                .withLore(
                        "<gray>Balance: <white>" + String.format("%,.2f", team.getBalance()) + "</white>",
                        "",
                        "<yellow>Click to manage the bank.</yellow>"
                ).build());

        inventory.setItem(47, new ItemBuilder(Material.ENDER_PEARL)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ʜᴏᴍᴇ</bold></gradient>")
                .withLore(
                        "<gray>Click to teleport to your team's home.",
                        "",
                        team.getHomeLocation() == null ? "<red>Home not set." : "<yellow>Click to teleport!</yellow>"
                ).build());

        inventory.setItem(48, new ItemBuilder(Material.ENDER_CHEST)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ</bold></gradient>")
                .withLore(
                        "<gray>A shared inventory for your team.",
                        "",
                        "<yellow>Click to open.</yellow>"
                ).build());

        inventory.setItem(49, createSortItem());
        inventory.setItem(50, createPvpItem());

        if (team.isOwner(viewer.getUniqueId())) {
            inventory.setItem(51, new ItemBuilder(Material.COMPARATOR)
                    .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs</bold></gradient>")
                    .withLore("<yellow>Click to manage team settings.</yellow>")
                    .build());
            inventory.setItem(52, new ItemBuilder(Material.TNT)
                    .withName("<red><bold>ᴅɪsʙᴀɴᴅ ᴛᴇᴀᴍ</bold></red>")
                    .withLore(
                            "<gray>Permanently deletes the team.",
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
                        "<gray>Determines if members can damage each other.",
                        "",
                        "Currently: " + (pvp ? "<green>ON" : "<red>OFF"),
                        "",
                        viewer.getUniqueId().equals(team.getOwnerUuid()) ? "<yellow>Click to toggle" : "<red>Only the owner can change this."
                ).build();
    }

    private ItemStack createSortItem() {
        return new ItemBuilder(Material.HOPPER)
                .withName("<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + "><bold>sᴏʀᴛ ᴍᴇᴍʙᴇʀs</bold></gradient>")
                .withLore(
                        "<gray>Click to change the sorting.",
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
        boolean isOwner = team.isOwner(member.getPlayerUuid());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Role: <white>" + (isOwner ? "Owner" : "Member"));
        lore.add("<gray>Joined: <white>" + formatter.format(member.getJoinDate()));
        lore.add("");
        if (team.isOwner(viewer.getUniqueId()) && !isOwner) {
            lore.add("<yellow>Click to edit this member.</yellow>");
        }

        ItemBuilder itemBuilder = new ItemBuilder(Material.PLAYER_HEAD)
                .asPlayerHead(member.getPlayerUuid())
                .withName((member.isOnline() ? "<gradient:" + plugin.getConfigManager().getMainColor() + ":" + plugin.getConfigManager().getAccentColor() + ">" : "<gray>") + (isOwner ? "⭐ " : "") + playerName)
                .withLore(lore);

        if (isOwner) {
            itemBuilder.withGlow();
        }

        return itemBuilder.build();
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