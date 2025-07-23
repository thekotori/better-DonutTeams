package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class TaskRunner {

    private final DonutTeams plugin;

    public TaskRunner(DonutTeams plugin) {
        this.plugin = plugin;
    }

    public void run(Runnable task) {
        if (DonutTeams.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (DonutTeams.isFolia()) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runAtLocation(Location location, Runnable task) {
        if (DonutTeams.isFolia()) {
            plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            run(task);
        }
    }

    public void runOnEntity(Entity entity, Runnable task) {
        if (DonutTeams.isFolia()) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            run(task);
        }
    }

    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delay, period);
    }
}