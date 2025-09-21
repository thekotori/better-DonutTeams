package eu.kotori.justTeams.gui;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.storage.IDataStorage;
import eu.kotori.justTeams.team.Team;
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
import java.util.List;
public class WarpsGUI implements IRefreshableGUI, InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;
    public WarpsGUI(JustTeams plugin, Team team, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("warps-gui");
        String title = guiConfig != null ? guiConfig.getString("title", "ᴛᴇᴀᴍ ᴡᴀʀᴘs") : "ᴛᴇᴀᴍ ᴡᴀʀᴘs";
        int size = guiConfig != null ? guiConfig.getInt("size", 54) : 54;
        this.inventory = Bukkit.createInventory(this, size, plugin.getMiniMessage().deserialize(title));
        initializeItems();
    }
    public void initializeItems() {
        inventory.clear();
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("warps-gui");
        if (guiConfig == null) {
            plugin.getLogger().warning("warps-gui section not found in gui.yml!");
            return;
        }
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig == null) return;
        ItemStack border = new ItemBuilder(guiManager.getMaterial("warps-gui.fill-item.material", Material.GRAY_STAINED_GLASS_PANE))
                .withName(guiManager.getString("warps-gui.fill-item.name", " "))
                .build();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }
        plugin.getTaskRunner().runAsync(() -> {
            List<IDataStorage.TeamWarp> warps = plugin.getStorageManager().getStorage().getWarps(team.getId());
            plugin.getTaskRunner().runOnEntity(viewer, () -> {
                if (warps.isEmpty()) {
                    ConfigurationSection noWarpsConfig = itemsConfig.getConfigurationSection("no-warps");
                    if (noWarpsConfig != null) {
                        ItemStack noWarps = new ItemBuilder(Material.matchMaterial(noWarpsConfig.getString("material", "BARRIER")))
                                .withName(noWarpsConfig.getString("name", "<red><bold>No Warps Set</bold></red>"))
                                .withLore(noWarpsConfig.getStringList("lore"))
                                .build();
                        inventory.setItem(noWarpsConfig.getInt("slot", 22), noWarps);
                    }
                } else {
                    int slot = 9;
                    for (IDataStorage.TeamWarp warp : warps) {
                        if (slot >= 45) break;
                        boolean canDelete = team.hasElevatedPermissions(viewer.getUniqueId()) ||
                                          warp.name().equals(viewer.getName());
                        ConfigurationSection warpConfig = itemsConfig.getConfigurationSection("warp-item");
                        if (warpConfig == null) continue;
                        String name = warpConfig.getString("name", "<gradient:#4C9D9D:#4C96D2><bold><warp_name></bold></gradient>")
                                .replace("<warp_name>", warp.name());
                        List<String> lore = warpConfig.getStringList("lore");
                        for (int i = 0; i < lore.size(); i++) {
                            String line = lore.get(i);
                            line = line.replace("<server_name>", warp.serverName())
                                     .replace("<warp_protection_status>", warp.password() != null ? "<red>Password Protected" : "<green>Public")
                                     .replace("<delete_prompt>", canDelete ? "<red>Right-Click to delete." : "");
                            lore.set(i, line);
                        }
                        ItemStack warpItem = new ItemBuilder(warp.password() != null ? Material.IRON_BLOCK : Material.GOLD_BLOCK)
                                .withName(name)
                                .withLore(lore)
                                .withAction("warp_item")
                                .withData("warp_name", warp.name())
                                .build();
                        inventory.setItem(slot++, warpItem);
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
    public Inventory getInventory() {
        return inventory;
    }
    public Team getTeam() {
        return team;
    }
}
