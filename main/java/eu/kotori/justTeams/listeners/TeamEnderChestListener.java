package eu.kotori.justTeams.listeners;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class TeamEnderChestListener implements Listener {
    private final JustTeams plugin;
    private final ConcurrentHashMap<UUID, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private static final long UPDATE_COOLDOWN = 100;
    private static final long SLOT_UPDATE_COOLDOWN = 50;
    public TeamEnderChestListener(JustTeams plugin) {
        this.plugin = plugin;
    }
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Team team)) return;
        Player player = (Player) event.getPlayer();
        team.addEnderChestViewer(player.getUniqueId());
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " opened team enderchest for team " + team.getName() +
                " (viewers: " + team.getEnderChestViewers().size() + ")");
        }
    }
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Team team)) return;
        Player player = (Player) event.getWhoClicked();
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime.containsKey(player.getUniqueId())) {
            if (currentTime - lastUpdateTime.get(player.getUniqueId()) < SLOT_UPDATE_COOLDOWN) {
                return;
            }
        }
        handleInventoryChange(team, player, event.getInventory(), "click");
        lastUpdateTime.put(player.getUniqueId(), currentTime);
    }
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Team team)) return;
        Player player = (Player) event.getWhoClicked();
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime.containsKey(player.getUniqueId())) {
            if (currentTime - lastUpdateTime.get(player.getUniqueId()) < SLOT_UPDATE_COOLDOWN) {
                return;
            }
        }
        handleInventoryChange(team, player, event.getInventory(), "drag");
        lastUpdateTime.put(player.getUniqueId(), currentTime);
    }
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Team team)) return;
        Player player = (Player) event.getPlayer();
        team.removeEnderChestViewer(player.getUniqueId());
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " closed team enderchest for team " + team.getName() +
                " (remaining viewers: " + team.getEnderChestViewers().size() + ")");
        }
        plugin.getTaskRunner().runAsync(() -> {
            try {
                plugin.getTeamManager().saveEnderChest(team);
            } catch (Exception e) {
                plugin.getLogger().warning("Error saving enderchest on close for " + player.getName() + ": " + e.getMessage());
            }
        });
    }
    private void handleInventoryChange(Team team, Player player, Inventory inventory, String changeType) {
        plugin.getTaskRunner().runAsync(() -> {
            try {
                plugin.getTeamManager().saveEnderChest(team);
                if (team.hasEnderChestViewers()) {
                    plugin.getTaskRunner().run(() -> {
                        notifyOtherViewers(team, player, changeType);
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error handling enderchest change for " + player.getName() + ": " + e.getMessage());
            }
        });
    }
    private void notifyOtherViewers(Team team, Player changer, String changeType) {
        for (UUID viewerUuid : team.getEnderChestViewers()) {
            if (viewerUuid.equals(changer.getUniqueId())) continue;
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer != null && viewer.isOnline()) {
                try {
                    refreshViewerInventory(viewer, team);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to refresh enderchest for viewer " + viewer.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    private void refreshViewerInventory(Player viewer, Team team) {
        if (viewer.getOpenInventory().getTopInventory().getHolder() instanceof Team) {
            plugin.getTaskRunner().runOnEntity(viewer, () -> {
                try {
                    viewer.closeInventory();
                    plugin.getTaskRunner().runOnEntity(viewer, () -> {
                        viewer.openInventory(team.getEnderChest());
                    });
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to refresh enderchest inventory for " + viewer.getName() + ": " + e.getMessage());
                }
            });
        }
    }
}
