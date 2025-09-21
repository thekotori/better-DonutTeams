package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.storage.DatabaseHealthChecker;
import eu.kotori.justTeams.storage.DatabaseStorage;
public class StartupManager {
    private final JustTeams plugin;
    private final DatabaseHealthChecker healthChecker;
    private boolean startupCompleted = false;
    private boolean startupSuccessful = false;
    public StartupManager(JustTeams plugin, DatabaseStorage databaseStorage) {
        this.plugin = plugin;
        this.healthChecker = new DatabaseHealthChecker(plugin, databaseStorage);
    }
    public boolean performStartup() {
        plugin.getLogger().info("Starting comprehensive startup sequence...");
        try {
            ConfigUpdater.updateAllConfigs(plugin);
            ConfigUpdater.cleanupAllOldBackups(plugin);
            boolean healthy = healthChecker.performHealthCheck();
            if (!healthy) {
                healthChecker.performEmergencyRepair();
            }
            startupCompleted = true;
            startupSuccessful = true;
            plugin.getLogger().info("Startup sequence completed successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Startup sequence failed: " + e.getMessage());
            startupCompleted = true;
            startupSuccessful = false;
            return false;
        }
    }
    public boolean isStartupCompleted() { return startupCompleted; }
    public boolean isStartupSuccessful() { return startupSuccessful; }
    public boolean isDatabaseHealthy() { return healthChecker.isHealthy(); }
    public String getDatabaseStatus() { return healthChecker.getStatusSummary(); }
    public boolean forceHealthCheck() { return healthChecker.forceHealthCheck(); }
    public boolean performEmergencyRepair() { return healthChecker.performEmergencyRepair(); }
    public String getStartupSummary() {
        return "Startup Status: " + (startupSuccessful ? "SUCCESS" : "FAILED") + ", Database Health: " + (healthChecker.isHealthy() ? "HEALTHY" : "UNHEALTHY");
    }
    public void schedulePeriodicHealthChecks() {
        plugin.getLogger().info("Scheduling periodic health checks...");
        plugin.getTaskRunner().runTimer(() -> {
            try {
                if (!healthChecker.performHealthCheck()) {
                    plugin.getLogger().warning("Database health check failed, attempting emergency repair...");
                    healthChecker.performEmergencyRepair();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Periodic health check failed: " + e.getMessage());
            }
        }, 20L * 60 * 15, 20L * 60 * 15);
    }
    public void schedulePeriodicPermissionSaves() {
        plugin.getLogger().info("Scheduling periodic permission saves...");
        plugin.getTaskRunner().runTimer(() -> {
            try {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getDebugLogger().log("Performing periodic permission save...");
                }
                plugin.getTeamManager().forceSaveAllTeamData();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getDebugLogger().log("Periodic permission save completed successfully");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Periodic permission save failed: " + e.getMessage());
            }
        }, 20L * 60 * 30, 20L * 60 * 30);
    }
}
