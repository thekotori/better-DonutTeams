package eu.kotori.justTeams.gui;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
public class BlacklistGUI implements InventoryHolder, IRefreshableGUI {
    private final JustTeams plugin;
    private final Team team;
    private final Player viewer;
    private Inventory inventory;
    public BlacklistGUI(JustTeams plugin, Team team, Player viewer) {
        this.plugin = plugin;
        this.team = team;
        this.viewer = viewer;
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("blacklist-gui");
        String title = guiConfig != null ? guiConfig.getString("title", "ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ") : "ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ";
        int size = guiConfig != null ? guiConfig.getInt("size", 54) : 54;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        initializeItems();
    }
    public void initializeItems() {
        inventory.clear();
        ItemStack fillItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .withName(" ")
            .build();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, fillItem);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, fillItem);
        }
        inventory.setItem(4, new ItemBuilder(Material.BARRIER)
            .withName("<white><bold>ᴛᴇᴀᴍ ʙʟᴀᴄᴋʟɪsᴛ</bold></white>")
            .withLore(
                "<gray>Players who cannot join this team</gray>",
                "<gray>Click on a player head to remove them</gray>"
            )
            .withAction("title")
            .build());
        loadBlacklistedPlayers();
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
            .withName("<gray><bold>ʙᴀᴄᴋ</bold></gray>")
            .withLore("<yellow>Click to return to the main menu.</yellow>")
            .withAction("back-button")
            .build());
    }
    private void loadBlacklistedPlayers() {
        plugin.getTaskRunner().runAsync(() -> {
            try {
                List<BlacklistedPlayer> blacklist = plugin.getStorageManager().getStorage().getTeamBlacklist(team.getId());
                if (blacklist.isEmpty()) {
                    plugin.getTaskRunner().runOnEntity(viewer, () -> {
                        inventory.setItem(22, new ItemBuilder(Material.BOOK)
                            .withName("<gray><bold>No Blacklisted Players</bold></gray>")
                            .withLore(
                                "<gray>No players are currently blacklisted.</gray>",
                                "<gray>Use /team blacklist <player> to add someone.</gray>"
                            )
                            .withAction("no-blacklisted")
                            .build());
                    });
                    return;
                }
                int slot = 9;
                for (BlacklistedPlayer blacklistedPlayer : blacklist) {
                    if (slot >= 45) break;
                    final int currentSlot = slot;
                    plugin.getTaskRunner().runOnEntity(viewer, () -> {
                        inventory.setItem(currentSlot, createBlacklistedPlayerItem(blacklistedPlayer));
                    });
                    slot++;
                    if ((slot - 9) % 9 == 0) {
                        slot += 0;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading team blacklist: " + e.getMessage());
                plugin.getTaskRunner().runOnEntity(viewer, () -> {
                    inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                        .withName("<red><bold>Error Loading Blacklist</bold></red>")
                        .withLore("<red>Could not load blacklisted players.</red>")
                        .withAction("error")
                        .build());
                });
            }
        });
    }
    private ItemStack createBlacklistedPlayerItem(BlacklistedPlayer blacklistedPlayer) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(blacklistedPlayer.getPlayerUuid());
        OfflinePlayer blacklistedBy = Bukkit.getOfflinePlayer(blacklistedPlayer.getBlacklistedByUuid());
        String timeAgo = formatTimeAgo(blacklistedPlayer.getBlacklistedAt());
        String actionKey = "remove-blacklist:" + blacklistedPlayer.getPlayerUuid().toString();
        plugin.getLogger().info("Creating blacklist item for " + blacklistedPlayer.getPlayerName() + " with action: " + actionKey);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(offlinePlayer);
            skull.setItemMeta(skullMeta);
        }
        skull = new ItemBuilder(skull)
            .withName("<gradient:#4C9D9D:#4C96D2><bold>" + blacklistedPlayer.getPlayerName() + "</bold></gradient>")
            .withLore(
                "<gray>Reason: <white>" + blacklistedPlayer.getReason() + "</white>",
                "<gray>Blacklisted by: <white>" + blacklistedBy.getName() + "</white>",
                "<gray>Date: <white>" + timeAgo + "</white>",
                "",
                "<yellow>Click to remove from blacklist</yellow>"
            )
            .withAction(actionKey)
            .build();
        if (skull.getItemMeta() != null) {
            String actualAction = skull.getItemMeta().getPersistentDataContainer().get(JustTeams.getActionKey(), PersistentDataType.STRING);
            plugin.getLogger().info("Action key verification for " + blacklistedPlayer.getPlayerName() + " - Expected: " + actionKey + ", Actual: " + actualAction);
            if (!actionKey.equals(actualAction)) {
                plugin.getLogger().warning("Action key mismatch! Expected: " + actionKey + ", Actual: " + actualAction);
                ItemMeta meta = skull.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(JustTeams.getActionKey(), PersistentDataType.STRING, actionKey);
                    skull.setItemMeta(meta);
                    plugin.getLogger().info("Manually set action key for " + blacklistedPlayer.getPlayerName());
                }
            }
        }
        return skull;
    }
    private String formatTimeAgo(Instant blacklistedAt) {
        Duration duration = Duration.between(blacklistedAt, Instant.now());
        if (duration.toDays() > 0) {
            return duration.toDays() + " day" + (duration.toDays() == 1 ? "" : "s") + " ago";
        } else if (duration.toHours() > 0) {
            return duration.toHours() + " hour" + (duration.toHours() == 1 ? "" : "s") + " ago";
        } else if (duration.toMinutes() > 0) {
            return duration.toMinutes() + " minute" + (duration.toMinutes() == 1 ? "" : "s") + " ago";
        } else {
            return "Just now";
        }
    }
    public void open() {
        viewer.openInventory(inventory);
    }
    public Inventory getInventory() {
        return inventory;
    }
    public Team getTeam() {
        return team;
    }
    public void refresh() {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Refreshing blacklist GUI for team " + team.getName());
        }

        if (viewer != null && viewer.isOnline()) {
            plugin.getGuiManager().getUpdateThrottle().scheduleUpdate(viewer.getUniqueId(), () -> {
                plugin.getTaskRunner().runOnEntity(viewer, () -> {
                    try {
                        initializeItems();
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Blacklist GUI refresh completed for team " + team.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error refreshing blacklist GUI for team " + team.getName() + ": " + e.getMessage());
                    }
                });
            });
        }
    }
}
