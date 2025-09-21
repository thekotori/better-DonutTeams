package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
public class TaskRunner {
    private final JustTeams plugin;
    private final boolean isFolia;
    private final boolean isPaper;
    private final Map<UUID, CancellableTask> activeTasks = new ConcurrentHashMap<>();
    private final Object taskLock = new Object();

    public TaskRunner(JustTeams plugin) {
        this.plugin = plugin;
        String serverName = plugin.getServer().getName();
        this.isFolia = serverName.equals("Folia");
        this.isPaper = serverName.equals("Paper") || serverName.equals("Purpur") || serverName.equals("Airplane") || serverName.equals("Pufferfish");
    }
    public void run(Runnable task) {
        if (isFolia) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
    public void runAsync(Runnable task) {
        if (task == null) {
            return;
        }
        if (isFolia) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in async task: " + e.getMessage());
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in async task: " + e.getMessage());
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            run(task);
        }
    }
    public void runOnEntity(Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            run(task);
        }
    }
    public CancellableTask runEntityTaskLater(Entity entity, Runnable task, long delay) {
        if (isFolia) {
            ScheduledTask scheduledTask = entity.getScheduler().runDelayed(plugin, scheduledTask1 -> task.run(), null, delay);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
            return bukkitTask::cancel;
        }
    }
    public CancellableTask runEntityTaskTimer(Entity entity, Runnable task, long delay, long period) {
        if (isFolia) {
            ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(plugin, scheduledTask1 -> task.run(), null, delay, period);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
            return bukkitTask::cancel;
        }
    }
    public CancellableTask runTimer(Runnable task, long delay, long period) {
        return runTaskTimer(task, delay, period);
    }
    public CancellableTask runLater(Runnable task, long delay) {
        return runTaskLater(task, delay);
    }
    public CancellableTask runTaskLater(Runnable task, long delay) {
        if (isFolia) {
            ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask1 -> task.run(), delay);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
            return bukkitTask::cancel;
        }
    }
    public CancellableTask runTaskTimer(Runnable task, long delay, long period) {
        if (isFolia) {
            ScheduledTask scheduledTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask1 -> task.run(), delay, period);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
            return bukkitTask::cancel;
        }
    }
    public CancellableTask runAsyncTaskLater(Runnable task, long delay) {
        if (isFolia) {
            ScheduledTask scheduledTask = plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask1 -> task.run(), delay, TimeUnit.MILLISECONDS);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
            return bukkitTask::cancel;
        }
    }
    public CancellableTask runAsyncTaskTimer(Runnable task, long delay, long period) {
        if (isFolia) {
            ScheduledTask scheduledTask = plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, scheduledTask1 -> task.run(), delay, period, TimeUnit.MILLISECONDS);
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
            return bukkitTask::cancel;
        }
    }
    public void addActiveTask(UUID taskId, CancellableTask task) {
        activeTasks.put(taskId, task);
    }
    public void removeActiveTask(UUID taskId) {
        CancellableTask task = activeTasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }
    public void cancelAllTasks() {
        activeTasks.values().forEach(CancellableTask::cancel);
        activeTasks.clear();
    }
    public boolean hasActiveTask(UUID taskId) {
        return activeTasks.containsKey(taskId);
    }

    public boolean isFolia() {
        return isFolia;
    }

    public boolean isPaper() {
        return isPaper;
    }

    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    public void runAsyncWithCatch(Runnable task, String taskName) {
        runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                task.run();
                long duration = System.currentTimeMillis() - startTime;

                if (duration > 100 && plugin.getConfigManager().isSlowQueryLoggingEnabled()) {
                    plugin.getLogger().warning("Slow async task '" + taskName + "' took " + duration + "ms");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async task '" + taskName + "': " + e.getMessage());
                if (plugin.getConfigManager().isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        });
    }
}
