package eu.kotori.justTeams.storage;
import eu.kotori.justTeams.JustTeams;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
public class DatabaseHealthChecker {
    private final JustTeams plugin;
    private final DatabaseStorage databaseStorage;
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicLong lastHealthCheck = new AtomicLong(0);
    private final AtomicLong lastRepairAttempt = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private static final long HEALTH_CHECK_COOLDOWN = 30000;
    private static final long REPAIR_COOLDOWN = 300000;
    private static final long MAX_CONSECUTIVE_FAILURES = 3;
    public DatabaseHealthChecker(JustTeams plugin, DatabaseStorage databaseStorage) {
        this.plugin = plugin;
        this.databaseStorage = databaseStorage;
    }
    public boolean performHealthCheck() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheck.get() < HEALTH_CHECK_COOLDOWN) {
            return isHealthy.get();
        }
        lastHealthCheck.set(currentTime);
        try {
            if (!databaseStorage.isConnected()) {
                plugin.getLogger().warning("Database health check failed: Not connected");
                isHealthy.set(false);
                consecutiveFailures.incrementAndGet();
                return false;
            }
            try (Connection conn = databaseStorage.getConnection()) {
                if (!conn.isValid(5)) {
                    plugin.getLogger().warning("Database health check failed: Connection invalid");
                    isHealthy.set(false);
                    consecutiveFailures.incrementAndGet();
                    return false;
                }
                try (var stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
            }
            isHealthy.set(true);
            consecutiveFailures.set(0);
            plugin.getLogger().fine("Database health check passed");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database health check failed with SQL exception: " + e.getMessage(), e);
            isHealthy.set(false);
            consecutiveFailures.incrementAndGet();
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Database health check failed with unexpected exception: " + e.getMessage(), e);
            isHealthy.set(false);
            consecutiveFailures.incrementAndGet();
            return false;
        }
    }
    public boolean forceHealthCheck() {
        lastHealthCheck.set(0);
        return performHealthCheck();
    }
    public boolean performEmergencyRepair() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRepairAttempt.get() < REPAIR_COOLDOWN) {
            plugin.getLogger().info("Emergency repair skipped due to cooldown");
            return false;
        }
        lastRepairAttempt.set(currentTime);
        plugin.getLogger().info("Performing emergency database repair...");
        try {
            if (databaseStorage.isConnected()) {
                plugin.getLogger().info("Attempting to refresh database connections...");
                try (Connection conn = databaseStorage.getConnection()) {
                    if (conn.isValid(5)) {
                        plugin.getLogger().info("Emergency repair successful: Database connection restored");
                        isHealthy.set(true);
                        consecutiveFailures.set(0);
                        return true;
                    }
                }
            }
            plugin.getLogger().warning("Emergency repair failed: Could not restore database connection");
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Emergency repair failed with exception: " + e.getMessage(), e);
            return false;
        }
    }
    public boolean isHealthy() {
        return isHealthy.get();
    }
    public String getStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Database Health: ").append(isHealthy.get() ? "HEALTHY" : "UNHEALTHY");
        summary.append(", Last Check: ").append(lastHealthCheck.get() == 0 ? "Never" :
            java.time.Instant.ofEpochMilli(lastHealthCheck.get()).toString());
        summary.append(", Consecutive Failures: ").append(consecutiveFailures.get());
        summary.append(", Last Repair: ").append(lastRepairAttempt.get() == 0 ? "Never" :
            java.time.Instant.ofEpochMilli(lastRepairAttempt.get()).toString());
        if (databaseStorage.isConnected()) {
            summary.append(", Connection: ACTIVE");
        } else {
            summary.append(", Connection: INACTIVE");
        }
        return summary.toString();
    }
    public long getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
    public boolean shouldAttemptRepair() {
        return consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES;
    }
    public void resetHealthStatus() {
        isHealthy.set(true);
        consecutiveFailures.set(0);
        lastHealthCheck.set(0);
        lastRepairAttempt.set(0);
    }
}
