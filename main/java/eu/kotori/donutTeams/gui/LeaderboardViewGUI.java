package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardViewGUI implements InventoryHolder {

    public enum LeaderboardType { KILLS, BALANCE, MEMBERS }

    private final Player viewer;
    private final Inventory inventory;
    private final DonutTeams plugin;

    public LeaderboardViewGUI(DonutTeams plugin, Player viewer, String title, Map<Integer, Team> topTeams, LeaderboardType type) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, Component.text(title));
        initializeItems(topTeams, type);
    }

    private void initializeItems(Map<Integer, Team> topTeams, LeaderboardType type) {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int slotIndex = 0;

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        for (Map.Entry<Integer, Team> entry : topTeams.entrySet()) {
            if (slotIndex >= slots.length) break;

            int rank = entry.getKey();
            Team team = entry.getValue();

            List<String> lore = new ArrayList<>();
            lore.add("<gray>Tag: <white>" + team.getTag());

            switch (type) {
                case KILLS -> lore.add("<gray>Kills: <white>" + team.getKills());
                case BALANCE -> lore.add("<gray>Balance: <white>" + String.format("%,.2f", team.getBalance()));
                case MEMBERS -> lore.add("<gray>Members: <white>" + team.getMembers().size());
            }

            inventory.setItem(slots[slotIndex++], new ItemBuilder(Material.PLAYER_HEAD)
                    .asPlayerHead(team.getOwnerUuid())
                    .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>#" + rank + " " + team.getName() + "</bold></gradient>")
                    .withLore(lore)
                    .build());
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW).withName("<gray><bold>ʙᴀᴄᴋ</bold></gray>").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}