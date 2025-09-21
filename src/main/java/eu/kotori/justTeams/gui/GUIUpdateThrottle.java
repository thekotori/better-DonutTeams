package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.util.CancellableTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIUpdateThrottle {
    private final JustTeams plugin;
    private final Map<UUID, CancellableTask> pendingUpdates = new ConcurrentHashMap<>();
    private final long throttleMs;

    public GUIUpdateThrottle(JustTeams plugin) {
        this.plugin = plugin;
        this.throttleMs = plugin.getConfigManager().getGuiUpdateThrottleMs();
    }

    public void scheduleUpdate(UUID playerUuid, Runnable updateTask) {
        if (playerUuid == null || updateTask == null) {
            plugin.getLogger().warning("GUIUpdateThrottle: Cannot schedule update with null parameters");
            return;
        }

        CancellableTask existingTask = pendingUpdates.get(playerUuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        CancellableTask newTask = plugin.getTaskRunner().runLater(() -> {
            try {
                updateTask.run();
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing GUI update task: " + e.getMessage());
            } finally {
                pendingUpdates.remove(playerUuid);
            }
        }, throttleMs / 50);

        pendingUpdates.put(playerUuid, newTask);
    }

    public void cancelPendingUpdate(UUID playerUuid) {
        if (playerUuid == null) return;

        CancellableTask task = pendingUpdates.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void cleanup() {
        for (CancellableTask task : pendingUpdates.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pendingUpdates.clear();
    }

    public int getPendingUpdateCount() {
        return pendingUpdates.size();
    }
}
