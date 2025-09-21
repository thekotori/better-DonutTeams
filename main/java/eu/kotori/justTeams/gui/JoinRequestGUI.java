package eu.kotori.justTeams.gui;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
public class JoinRequestGUI implements IRefreshableGUI, InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;
    public JoinRequestGUI(JustTeams plugin, Player viewer, Team team) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("join-requests-gui");
        String title = guiConfig != null ? guiConfig.getString("title", "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs") : "ᴊᴏɪɴ ʀᴇǫᴜᴇsᴛs";
        int size = guiConfig != null ? guiConfig.getInt("size", 54) : 54;
        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }
    public void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("join-requests-gui");
        if (guiConfig == null) {
            plugin.getLogger().warning("join-requests-gui section not found in gui.yml!");
            return;
        }
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig == null) return;
        ItemStack border = new ItemBuilder(guiManager.getMaterial("join-requests-gui.fill-item.material", Material.GRAY_STAINED_GLASS_PANE))
                .withName(guiManager.getString("join-requests-gui.fill-item.name", " "))
                .build();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }
        plugin.getTaskRunner().runAsync(() -> {
            List<UUID> requests = plugin.getStorageManager().getStorage().getJoinRequests(team.getId());
            plugin.getTaskRunner().runOnEntity(viewer, () -> {
                if (requests.isEmpty()) {
                    ConfigurationSection noRequestsConfig = itemsConfig.getConfigurationSection("no-requests");
                    if (noRequestsConfig != null) {
                        ItemStack noRequestsItem = new ItemBuilder(Material.matchMaterial(noRequestsConfig.getString("material", "BARRIER")))
                                .withName(noRequestsConfig.getString("name", "<red><bold>No Join Requests</bold></red>"))
                                .withLore(noRequestsConfig.getStringList("lore"))
                                .build();
                        inventory.setItem(noRequestsConfig.getInt("slot", 22), noRequestsItem);
                    }
                } else {
                    int slot = 9;
                    for (UUID requestUuid : requests) {
                        if (slot >= 45) break;
                        OfflinePlayer requester = Bukkit.getOfflinePlayer(requestUuid);
                        if (requester.getName() == null) continue;
                        ConfigurationSection headConfig = itemsConfig.getConfigurationSection("player-head");
                        if (headConfig == null) continue;
                        String name = headConfig.getString("name", "<gradient:#4C9D9D:#4C96D2><status_indicator><bold><player_name></bold></gradient>")
                                .replace("<player_name>", requester.getName())
                                .replace("<status_indicator>", requester.isOnline() ? "<green>● </green>" : "<red>● </red>");
                        List<String> lore = headConfig.getStringList("lore");
                        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                                .asPlayerHead(requestUuid)
                                .withName(name)
                                .withLore(lore)
                                .withAction("player-head")
                                .withData("player_uuid", requestUuid.toString())
                                .build();
                        inventory.setItem(slot++, head);
                    }
                }
                ConfigurationSection backConfig = itemsConfig.getConfigurationSection("back-button");
                if (backConfig != null) {
                    ItemStack backButton = new ItemBuilder(Material.matchMaterial(backConfig.getString("material", "ARROW")))
                            .withName(backConfig.getString("name", "<gray><bold>ʙᴀᴄᴋ</bold></gray>"))
                            .withLore(backConfig.getStringList("lore"))
                            .withAction("back-button")
                            .build();
                    inventory.setItem(backConfig.getInt("slot", 49), backButton);
                }
            });
        });
    }
    public void open() {
        viewer.openInventory(inventory);
    }
    public void refresh() {
        open();
    }
    public Team getTeam() {
        return team;
    }
    public Inventory getInventory() {
        return inventory;
    }
}
