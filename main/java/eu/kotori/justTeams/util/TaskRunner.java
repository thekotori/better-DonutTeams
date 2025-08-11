package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeams;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public class TaskRunner {

    private final JustTeams plugin;

    public TaskRunner(JustTeams plugin) {
        this.plugin = plugin;
    }

    public void run(Runnable task) {
        if (JustTeams.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (JustTeams.isFolia()) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runAtLocation(Location location, Runnable task) {
        if (JustTeams.isFolia()) {
            plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            run(task);
        }
    }

    public void runOnEntity(Entity entity, Runnable task) {
        if (JustTeams.isFolia()) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            run(task);
        }
    }

    public BukkitTask runTaskLater(Runnable task, long delay) {
        return plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
    }

    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
    }
}