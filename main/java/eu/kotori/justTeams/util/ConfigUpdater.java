package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
public class ConfigUpdater {
    private static final List<String> CONFIG_FILES = List.of(
        "config.yml",
        "messages.yml",
        "gui.yml",
        "commands.yml",
        "placeholders.yml"
    );
    public static void updateAllConfigs(JustTeams plugin) {
        plugin.getLogger().info("Starting automatic configuration update process...");
        performConfigHealthCheck(plugin);
        int successCount = 0;
        int failCount = 0;
        for (String fileName : CONFIG_FILES) {
            try {
                File configFile = new File(plugin.getDataFolder(), fileName);
                if (configFile.exists()) {
                    try {
                        YamlConfiguration.loadConfiguration(configFile);
                    } catch (Exception e) {
                        plugin.getLogger().warning("YAML syntax error detected in " + fileName + ", attempting repair...");
                        if (IntelligentConfigHelper.performYamlAutoRepair(configFile)) {
                            plugin.getLogger().info("Successfully auto-repaired " + fileName + " using intelligent repair");
                        } else if (repairYamlFile(configFile)) {
                            plugin.getLogger().info("Successfully repaired " + fileName + " using fallback method");
                        } else {
                            plugin.getLogger().severe("Failed to repair " + fileName + ", will recreate from defaults");
                        }
                    }
                }
                boolean needsUpdate = needsUpdate(plugin, fileName);
                if (needsUpdate) {
                    plugin.getLogger().info(fileName + " needs update, processing...");
                    boolean updated = updateConfig(plugin, fileName);
                    if (updated) {
                        successCount++;
                        plugin.getLogger().info("Successfully updated " + fileName);
                    } else {
                        plugin.getLogger().warning("Failed to update " + fileName + " despite needing update");
                        failCount++;
                    }
                } else {
                    plugin.getLogger().fine(fileName + " is already up to date");
                }
            } catch (Exception e) {
                failCount++;
                plugin.getLogger().log(Level.SEVERE, "Failed to update " + fileName + ": " + e.getMessage(), e);
                try {
                    plugin.getLogger().warning("Creating backup and force update for " + fileName);
                    createBackupAndForceUpdate(plugin, fileName);
                    successCount++;
                    plugin.getLogger().info("Successfully recovered " + fileName + " with force update");
                } catch (Exception recoveryException) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to recover " + fileName + ": " + recoveryException.getMessage(), recoveryException);
                }
            }
        }
        plugin.getLogger().info("Configuration update process completed! Success: " + successCount + ", Failed: " + failCount);
    }
    private static boolean updateConfig(JustTeams plugin, String fileName) throws IOException {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created " + fileName + " from default template.");
            return true;
        }
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        try (InputStream defaultConfigStream = plugin.getResource(fileName)) {
            if (defaultConfigStream == null) {
                plugin.getLogger().warning("Could not find default " + fileName + " in plugin resources!");
                return false;
            }
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            String versionKey = getVersionKey(fileName);
            boolean versionMismatch = false;
            if (versionKey != null && defaultConfig.contains(versionKey)) {
                int currentVersion = currentConfig.getInt(versionKey, 0);
                int defaultVersion = defaultConfig.getInt(versionKey);
                if (currentVersion != defaultVersion) {
                    versionMismatch = true;
                    plugin.getLogger().info("Version mismatch detected for " + fileName + ": current=" + currentVersion + ", default=" + defaultVersion);
                }
            }
            boolean updated = performComprehensiveUpdate(currentConfig, defaultConfig, fileName);
            if (updated || versionMismatch) {
                if (versionMismatch) {
                    File backupFolder = new File(plugin.getDataFolder(), "backups");
                    if (!backupFolder.exists()) {
                        backupFolder.mkdirs();
                    }
                    File backupFile = new File(backupFolder, fileName + ".backup." + System.currentTimeMillis());
                    java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Created backup before version update: " + backupFile.getName());
                }
                currentConfig.save(configFile);
                plugin.getLogger().info(fileName + " has been automatically updated with new configuration options.");
                return true;
            } else {
                plugin.getLogger().fine(fileName + " is already up to date.");
                return false;
            }
        }
    }
    private static boolean performComprehensiveUpdate(FileConfiguration currentConfig, FileConfiguration defaultConfig, String fileName) {
        boolean updated = false;
        try {
            updated |= addMissingKeys(currentConfig, defaultConfig, "");
            updated |= updateVersionNumbers(currentConfig, defaultConfig, fileName);
            updated |= removeObsoleteKeys(currentConfig, defaultConfig, "");
            updated |= validateConfiguration(currentConfig, defaultConfig, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform comprehensive update for " + fileName + ": " + e.getMessage(), e);
        }
        return updated;
    }
    private static boolean validateConfiguration(FileConfiguration currentConfig, FileConfiguration defaultConfig, String fileName) {
        boolean updated = false;
        String versionKey = getVersionKey(fileName);
        if (versionKey != null) {
            if (!currentConfig.contains(versionKey)) {
                currentConfig.set(versionKey, defaultConfig.getInt(versionKey, 1));
                updated = true;
            }
        }
        if (fileName.equals("config.yml")) {
            updated |= validateConfigFile(currentConfig, defaultConfig);
        } else if (fileName.equals("messages.yml")) {
            updated |= validateMessagesFile(currentConfig, defaultConfig);
        } else if (fileName.equals("gui.yml")) {
            updated |= validateGuiFile(currentConfig, defaultConfig);
        } else if (fileName.equals("commands.yml")) {
            updated |= validateCommandsFile(currentConfig, defaultConfig);
        } else if (fileName.equals("placeholders.yml")) {
            updated |= validatePlaceholdersFile(currentConfig, defaultConfig);
        }
        return updated;
    }
    private static boolean validateConfigFile(FileConfiguration currentConfig, FileConfiguration defaultConfig) {
        boolean updated = false;
        if (!currentConfig.contains("config-version")) {
            currentConfig.set("config-version", defaultConfig.getInt("config-version", 19));
            updated = true;
        }
        if (!currentConfig.contains("storage.type")) {
            currentConfig.set("storage.type", "h2");
            updated = true;
        }
        if (!currentConfig.contains("team_chat")) {
            currentConfig.set("team_chat.character_enabled", true);
            currentConfig.set("team_chat.character", "#");
            currentConfig.set("team_chat.require_space", false);
            updated = true;
        }
        if (!currentConfig.contains("features")) {
            currentConfig.set("features.team_creation", true);
            currentConfig.set("features.team_disband", true);
            currentConfig.set("features.team_invites", true);
            currentConfig.set("features.team_home", true);
            currentConfig.set("features.team_home_set", true);
            currentConfig.set("features.team_home_teleport", true);
            currentConfig.set("features.team_warps", true);
            currentConfig.set("features.team_warp_set", true);
            currentConfig.set("features.team_warp_delete", true);
            currentConfig.set("features.team_warp_teleport", true);
            currentConfig.set("features.team_pvp", true);
            currentConfig.set("features.team_bank", true);
            currentConfig.set("features.team_enderchest", true);
            currentConfig.set("features.team_chat", true);
            currentConfig.set("features.member_leave", true);
            currentConfig.set("features.member_kick", true);
            currentConfig.set("features.member_promote", true);
            currentConfig.set("features.member_demote", true);
            currentConfig.set("features.join_requests", true);
            updated = true;
        }
        return updated;
    }
    private static boolean validateMessagesFile(FileConfiguration currentConfig, FileConfiguration defaultConfig) {
        boolean updated = false;
        if (!currentConfig.contains("messages-version")) {
            currentConfig.set("messages-version", defaultConfig.getInt("messages-version", 5));
            updated = true;
        }
        if (!currentConfig.contains("prefix")) {
            currentConfig.set("prefix", defaultConfig.getString("prefix"));
            updated = true;
        }
        if (!currentConfig.contains("feature_disabled")) {
            currentConfig.set("feature_disabled", "<red>This feature is disabled on this server.</red>");
            updated = true;
        }
        return updated;
    }
    private static boolean validateGuiFile(FileConfiguration currentConfig, FileConfiguration defaultConfig) {
        boolean updated = false;
        if (!currentConfig.contains("gui-version")) {
            currentConfig.set("gui-version", defaultConfig.getInt("gui-version", 10));
            updated = true;
        }
        if (!currentConfig.contains("team-gui.items.pvp-toggle-locked")) {
            currentConfig.set("team-gui.items.pvp-toggle-locked.material", "BARRIER");
            currentConfig.set("team-gui.items.pvp-toggle-locked.slot", 12);
            currentConfig.set("team-gui.items.pvp-toggle-locked.name", "<red>PvP Toggle (Disabled)</red>");
            currentConfig.set("team-gui.items.pvp-toggle-locked.lore", List.of(
                "<gray>This feature has been disabled",
                "<gray>by the server administrator."
            ));
            updated = true;
        }
        if (!currentConfig.contains("team-gui.items.warps-locked")) {
            currentConfig.set("team-gui.items.warps-locked.material", "BARRIER");
            currentConfig.set("team-gui.items.warps-locked.slot", 14);
            currentConfig.set("team-gui.items.warps-locked.name", "<red>Team Warps (Disabled)</red>");
            currentConfig.set("team-gui.items.warps-locked.lore", List.of(
                "<gray>This feature has been disabled",
                "<gray>by the server administrator."
            ));
            updated = true;
        }
        return updated;
    }
    private static boolean validateCommandsFile(FileConfiguration currentConfig, FileConfiguration defaultConfig) {
        boolean updated = false;
        if (!currentConfig.contains("commands-version")) {
            currentConfig.set("commands-version", defaultConfig.getInt("commands-version", 4));
            updated = true;
        }
        if (!currentConfig.contains("primary-command")) {
            currentConfig.set("primary-command", "team");
            updated = true;
        }
        return updated;
    }
    private static boolean validatePlaceholdersFile(FileConfiguration currentConfig, FileConfiguration defaultConfig) {
        boolean updated = false;
        if (!currentConfig.contains("placeholders-version")) {
            currentConfig.set("placeholders-version", defaultConfig.getInt("placeholders-version", 3));
            updated = true;
        }
        String[] requiredSections = {
            "colors", "roles", "status", "sort", "date_time",
            "numbers", "gui", "indicators", "admin"
        };
        for (String section : requiredSections) {
            if (!currentConfig.contains(section)) {
                currentConfig.set(section, defaultConfig.getConfigurationSection(section));
                updated = true;
            }
        }
        return updated;
    }
    public static void migrateToPlaceholderSystem(JustTeams plugin) {
        plugin.getLogger().info("Starting migration to placeholder system...");
        try {
            migrateGuiToPlaceholders(plugin);
            updateExistingConfigurations(plugin);

            plugin.getLogger().info("Placeholder system migration completed successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to migrate to placeholder system: " + e.getMessage());
            plugin.getLogger().severe("Failed to update config: " + e.getMessage());
        }
    }
    private static void migrateGuiToPlaceholders(JustTeams plugin) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui.yml");
            if (!guiFile.exists()) {
                plugin.getLogger().info("gui.yml not found, skipping GUI migration");
                return;
            }
            FileConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            boolean updated = false;
            updated |= migrateTeamGuiElements(guiConfig);
            updated |= migrateAdminGuiElements(guiConfig);
            updated |= migrateOtherGuiElements(guiConfig);
            if (updated) {
                guiConfig.save(guiFile);
                plugin.getLogger().info("GUI configuration migrated to use placeholder system");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate GUI to placeholders: " + e.getMessage());
        }
    }
    private static boolean migrateTeamGuiElements(FileConfiguration guiConfig) {
        boolean updated = false;
        ConfigurationSection teamGui = guiConfig.getConfigurationSection("team-gui");
        if (teamGui != null) {
            ConfigurationSection items = teamGui.getConfigurationSection("items");
            if (items != null) {
                ConfigurationSection sortItem = items.getConfigurationSection("sort");
                if (sortItem != null) {
                    String currentName = sortItem.getString("name", "");
                    if (currentName.contains("Sort by") && !currentName.contains("<placeholder:")) {
                        sortItem.set("name", "<placeholder:sort.sort_button.name>");
                        updated = true;
                    }
                }
            }
        }
        return updated;
    }
    private static boolean migrateAdminGuiElements(FileConfiguration guiConfig) {
        boolean updated = false;
        if (!guiConfig.contains("admin-team-list-gui")) {
            guiConfig.set("admin-team-list-gui.title", "All Teams - Page %page%");
            guiConfig.set("admin-team-list-gui.size", 54);
            updated = true;
        }
        if (!guiConfig.contains("admin-team-manage-gui")) {
            guiConfig.set("admin-team-manage-gui.title", "Manage: %team%");
            guiConfig.set("admin-team-manage-gui.size", 27);
            updated = true;
        }
        return updated;
    }
    private static boolean migrateOtherGuiElements(FileConfiguration guiConfig) {
        boolean updated = false;
        String[] guiTypes = {
            "join-requests-gui", "warps-gui", "blacklist-gui",
            "leaderboard-view-gui", "leaderboard-category-gui"
        };
        for (String guiType : guiTypes) {
            if (!guiConfig.contains(guiType)) {
                guiConfig.set(guiType + ".title", "Default Title");
                guiConfig.set(guiType + ".size", 54);
                updated = true;
            }
        }
        return updated;
    }
    private static void updateExistingConfigurations(JustTeams plugin) {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (configFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                boolean updated = false;
                if (!config.contains("team_chat")) {
                    config.set("team_chat.character_enabled", true);
                    config.set("team_chat.character", "#");
                    config.set("team_chat.require_space", false);
                    updated = true;
                }
                if (updated) {
                    config.save(configFile);
                    plugin.getLogger().info("Updated config.yml with new team chat settings");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update existing configurations: " + e.getMessage());
        }
    }
    private static void ensurePlaceholdersFile(JustTeams plugin) {
        try {
            File placeholdersFile = new File(plugin.getDataFolder(), "placeholders.yml");
            if (!placeholdersFile.exists()) {
                plugin.saveResource("placeholders.yml", false);
                plugin.getLogger().info("Created placeholders.yml from template");
            } else {
                FileConfiguration placeholders = YamlConfiguration.loadConfiguration(placeholdersFile);
                boolean updated = false;
        if (!placeholders.contains("placeholders-version")) {
            placeholders.set("placeholders-version", 4);
            updated = true;
        }

                if (updated) {
                    placeholders.save(placeholdersFile);
                    plugin.getLogger().info("Updated placeholders.yml with missing sections");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ensure placeholders file: " + e.getMessage());
        }
    }
    private static boolean addMissingKeys(FileConfiguration currentConfig, FileConfiguration defaultConfig, String path) {
        boolean updated = false;
        for (String key : defaultConfig.getKeys(true)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (!currentConfig.contains(key)) {
                if (shouldSkipGuiItem(fullPath)) {
                    continue;
                }
                Object defaultValue = defaultConfig.get(key);
                currentConfig.set(key, defaultValue);
                updated = true;
            } else if (defaultConfig.isConfigurationSection(key) && currentConfig.isConfigurationSection(key)) {
                updated |= addMissingKeys(currentConfig.getConfigurationSection(key),
                                        defaultConfig.getConfigurationSection(key), fullPath);
            }
        }
        return updated;
    }
    private static boolean shouldSkipGuiItem(String fullPath) {
        String[] userRemovableGuiItems = {
            "no-team-gui.items.create-team",
            "team-gui.items.create-team"
        };
        for (String removableItem : userRemovableGuiItems) {
            if (fullPath.startsWith(removableItem)) {
                return true;
            }
        }
        return false;
    }
    private static boolean addMissingKeys(org.bukkit.configuration.ConfigurationSection currentConfig,
                                        org.bukkit.configuration.ConfigurationSection defaultConfig, String path) {
        boolean updated = false;
        for (String key : defaultConfig.getKeys(true)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (!currentConfig.contains(key)) {
                if (shouldSkipGuiItem(fullPath)) {
                    continue;
                }
                Object defaultValue = defaultConfig.get(key);
                currentConfig.set(key, defaultValue);
                updated = true;
            } else if (defaultConfig.isConfigurationSection(key) && currentConfig.isConfigurationSection(key)) {
                updated |= addMissingKeys(currentConfig.getConfigurationSection(key),
                                        defaultConfig.getConfigurationSection(key), fullPath);
            }
        }
        return updated;
    }
    private static boolean updateVersionNumbers(FileConfiguration currentConfig, FileConfiguration defaultConfig, String fileName) {
        boolean updated = false;
        String versionKey = getVersionKey(fileName);
        if (versionKey != null && defaultConfig.contains(versionKey)) {
            int currentVersion = currentConfig.getInt(versionKey, 0);
            int defaultVersion = defaultConfig.getInt(versionKey);
            if (currentVersion != defaultVersion) {
                currentConfig.set(versionKey, defaultVersion);
                updated = true;
            }
        }
        return updated;
    }
    public static String getVersionKey(String fileName) {
        return switch (fileName) {
            case "config.yml" -> "config-version";
            case "gui.yml" -> "gui-version";
            case "messages.yml" -> "messages-version";
            case "commands.yml" -> "commands-version";
            case "placeholders.yml" -> "placeholders-version";
            default -> null;
        };
    }
    private static boolean removeObsoleteKeys(FileConfiguration currentConfig, FileConfiguration defaultConfig, String path) {
        boolean updated = false;
        Set<String> currentKeys = currentConfig.getKeys(true);
        Set<String> defaultKeys = defaultConfig.getKeys(true);
        for (String key : currentKeys) {
            if (!defaultKeys.contains(key)) {
                if (!isUserCustomizedValue(currentConfig, key)) {
                    currentConfig.set(key, null);
                    updated = true;
                }
            }
        }
        return updated;
    }
    private static boolean isUserCustomizedValue(FileConfiguration config, String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("server-identifier") ||
               lowerKey.contains("username") ||
               lowerKey.contains("password") ||
               lowerKey.contains("host") ||
               lowerKey.contains("database") ||
               lowerKey.contains("custom") ||
               lowerKey.contains("user") ||
               lowerKey.contains("personal");
    }
    public static void forceUpdateConfig(JustTeams plugin, String fileName) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);
            File backupFolder = new File(plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            if (configFile.exists()) {
                File backupFile = new File(backupFolder, fileName + ".backup." + System.currentTimeMillis());
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created backup: " + backupFile.getName());
                configFile.delete();
                plugin.getLogger().info("Deleted existing " + fileName + " for forced update.");
            }
            updateConfig(plugin, fileName);
            cleanupOldBackups(plugin, backupFolder, fileName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to force update " + fileName + ": " + e.getMessage(), e);
        }
    }
    private static void createBackupAndForceUpdate(JustTeams plugin, String fileName) throws IOException {
        File configFile = new File(plugin.getDataFolder(), fileName);
        File backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        File backupFile = new File(backupFolder, fileName + ".backup." + System.currentTimeMillis());
        if (configFile.exists()) {
            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created backup: " + backupFile.getName());
            configFile.delete();
            plugin.getLogger().info("Deleted corrupted " + fileName + " for recovery.");
        }
        plugin.saveResource(fileName, false);
        plugin.getLogger().info("Recovered " + fileName + " from template.");
        cleanupOldBackups(plugin, backupFolder, fileName);
    }
    public static boolean needsUpdate(JustTeams plugin, String fileName) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);
            if (!configFile.exists()) {
                return true;
            }
            FileConfiguration currentConfig = null;
            try {
                currentConfig = YamlConfiguration.loadConfiguration(configFile);
            } catch (Exception e) {
                plugin.getLogger().warning("YAML syntax error detected in " + fileName + ", attempting repair...");
                if (repairYamlFile(configFile)) {
                    plugin.getLogger().info("Successfully repaired " + fileName);
                    currentConfig = YamlConfiguration.loadConfiguration(configFile);
                } else {
                    plugin.getLogger().severe("Failed to repair " + fileName + ", using default configuration");
                    return true;
                }
            }
            try (InputStream defaultConfigStream = plugin.getResource(fileName)) {
                if (defaultConfigStream == null) {
                    return false;
                }
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
                for (String key : defaultConfig.getKeys(true)) {
                    if (!currentConfig.contains(key)) {
                        plugin.getLogger().info("Missing key detected in " + fileName + ": " + key);
                        return true;
                    }
                }
                String versionKey = getVersionKey(fileName);
                if (versionKey != null && defaultConfig.contains(versionKey)) {
                    int currentVersion = currentConfig.getInt(versionKey, 0);
                    int defaultVersion = defaultConfig.getInt(versionKey);
                    if (currentVersion != defaultVersion) {
                        plugin.getLogger().info("Version mismatch in " + fileName + ": current=" + currentVersion + ", default=" + defaultVersion);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if " + fileName + " needs update: " + e.getMessage());
            return true;
        }
    }
    public static List<String> getConfigsNeedingUpdate(JustTeams plugin) {
        List<String> needsUpdate = new ArrayList<>();
        for (String fileName : CONFIG_FILES) {
            if (needsUpdate(plugin, fileName)) {
                needsUpdate.add(fileName);
            }
        }
        return needsUpdate;
    }
    public static void testConfigurationSystem(JustTeams plugin) {
        plugin.getLogger().info("Testing configuration system...");
        for (String fileName : CONFIG_FILES) {
            try {
                boolean needsUpdate = needsUpdate(plugin, fileName);
                plugin.getLogger().info("Config " + fileName + " needs update: " + needsUpdate);
                File configFile = new File(plugin.getDataFolder(), fileName);
                if (configFile.exists()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    String versionKey = getVersionKey(fileName);
                    if (versionKey != null && config.contains(versionKey)) {
                        int version = config.getInt(versionKey);
                        plugin.getLogger().info("Config " + fileName + " current version: " + version);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error testing " + fileName + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Configuration system test completed!");
    }
    public static void forceUpdateAllConfigs(JustTeams plugin) {
        plugin.getLogger().info("=== FORCE UPDATING ALL CONFIGURATION FILES ===");
        int successCount = 0;
        int failCount = 0;
        for (String fileName : CONFIG_FILES) {
            try {
                plugin.getLogger().info("Force updating " + fileName + "...");
                boolean updated = updateConfig(plugin, fileName);
                if (updated) {
                    successCount++;
                    plugin.getLogger().info("Successfully force updated " + fileName);
                } else {
                    plugin.getLogger().warning("Failed to force update " + fileName);
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                plugin.getLogger().log(Level.SEVERE, "Failed to force update " + fileName + ": " + e.getMessage(), e);
            }
        }
        plugin.getLogger().info("Force update completed! Success: " + successCount + ", Failed: " + failCount);
    }
    public static boolean isConfigurationSystemHealthy(JustTeams plugin) {
        try {
            for (String fileName : CONFIG_FILES) {
                File configFile = new File(plugin.getDataFolder(), fileName);
                if (!configFile.exists()) {
                    plugin.getLogger().warning("Missing configuration file: " + fileName);
                    return false;
                }
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                String versionKey = getVersionKey(fileName);
                if (versionKey != null && !config.contains(versionKey)) {
                    plugin.getLogger().warning("Configuration file " + fileName + " missing version key: " + versionKey);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Configuration system health check failed: " + e.getMessage());
            return false;
        }
    }
    private static void cleanupOldBackups(JustTeams plugin, File backupFolder, String fileName) {
        try {
            if (!backupFolder.exists()) {
                return;
            }
            File[] backupFiles = backupFolder.listFiles((dir, name) ->
                name.startsWith(fileName + ".backup.") && name.endsWith(".yml"));
            if (backupFiles == null || backupFiles.length <= 5) {
                return;
            }
            java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = 5; i < backupFiles.length; i++) {
                if (backupFiles[i].delete()) {
                    plugin.getLogger().info("Cleaned up old backup: " + backupFiles[i].getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up old backups for " + fileName + ": " + e.getMessage());
        }
    }
    public static void cleanupAllOldBackups(JustTeams plugin) {
        try {
            File backupFolder = new File(plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                return;
            }
            plugin.getLogger().info("Cleaning up old backup files...");
            for (String fileName : CONFIG_FILES) {
                cleanupOldBackups(plugin, backupFolder, fileName);
            }
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            File[] allBackupFiles = backupFolder.listFiles((dir, name) -> {
                if (!name.contains(".backup.")) return false;
                try {
                    String timestampStr = name.substring(name.lastIndexOf(".backup.") + 8);
                    long timestamp = Long.parseLong(timestampStr);
                    return timestamp < sevenDaysAgo;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
            if (allBackupFiles != null) {
                for (File oldBackup : allBackupFiles) {
                    if (oldBackup.delete()) {
                        plugin.getLogger().info("Cleaned up old backup: " + oldBackup.getName());
                    }
                }
            }
            plugin.getLogger().info("Backup cleanup completed!");
        } catch (Exception e) {
            plugin.getLogger().warning("Error during backup cleanup: " + e.getMessage());
        }
    }
    public static int getBackupCount(JustTeams plugin, String fileName) {
        try {
            File backupFolder = new File(plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                return 0;
            }
            File[] backupFiles = backupFolder.listFiles((dir, name) ->
                name.startsWith(fileName + ".backup.") && name.endsWith(".yml"));
            return backupFiles != null ? backupFiles.length : 0;
        } catch (Exception e) {
            plugin.getLogger().warning("Error counting backups for " + fileName + ": " + e.getMessage());
            return 0;
        }
    }
    private static boolean repairYamlFile(File configFile) {
        try {
            String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            content = content.replaceAll("\"<gradient:\\s*$", "\"<gradient:#4C9DDE:#4C96D2>JustTeams</gradient>\"");
            content = content.replaceAll("\"<gradient:\\s*\\n", "\"<gradient:#4C9DDE:#4C96D2>JustTeams</gradient>\"\\n");
            content = content.replaceAll("team_chat_password_warning: \"<red>Warning: Please do not shar\\s*$", "team_chat_password_warning: \"<red>Warning: Please do not share your team password with anyone!</red>\"");
            content = content.replaceAll("online-name-format: \"<gradient:\\s*$", "online-name-format: \"<gradient:#4C9DDE:#4C96D2><player></gradient>\"");
            content = content.replaceAll("offline-name-format: \"<gray><status_indicator><role_ic\\s*$", "offline-name-format: \"<gray><status_indicator><role_icon> <player></gray>\"");
            content = content.replaceAll(": \"[^\"]*$", ": \"Fixed incomplete string\"");
            java.nio.file.Files.write(configFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try {
                YamlConfiguration.loadConfiguration(configFile);
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    public static void createConfigBackup(JustTeams plugin, String fileName) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);
            File backupFolder = new File(plugin.getDataFolder(), "backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            if (configFile.exists()) {
                File backupFile = new File(backupFolder, fileName + ".backup." + System.currentTimeMillis());
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created manual backup: " + backupFile.getName());
                cleanupOldBackups(plugin, backupFolder, fileName);
            } else {
                plugin.getLogger().warning("Cannot backup " + fileName + " - file does not exist");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup for " + fileName + ": " + e.getMessage());
        }
    }
    public static final Map<String, Set<String>> USER_CUSTOMIZABLE_KEYS = new HashMap<String, Set<String>>() {{
        put("config.yml", Set.of(
            "storage.mysql.host", "storage.mysql.port", "storage.mysql.database",
            "storage.mysql.username", "storage.mysql.password", "storage.mysql.useSSL",
            "server-identifier", "main_color", "accent_color", "currency_format",
            "max_team_size", "max_teams_per_player", "team_creation.min_tag_length",
            "team_creation.max_tag_length", "team_creation.min_name_length", "team_creation.max_name_length",
            "debug.enabled", "webhook.url", "webhook.enabled"
        ));
        put("messages.yml", Set.of(
            "prefix", "team_chat_format", "help_header"
        ));
        put("gui.yml", Set.of(
            "no-team-gui.title", "team-gui.title", "admin-gui.title",
            "no-team-gui.items.create-team", "no-team-gui.items.leaderboards"
        ));
        put("placeholders.yml", Set.of(
            "colors.primary", "colors.secondary", "colors.accent", "colors.success",
            "colors.error", "colors.warning", "colors.info",
            "team_display.format", "team_display.team_icon", "team_display.team_color",
            "team_display.show_icon", "team_display.show_tag", "team_display.show_name",
            "team_display.no_team", "team_display.tag_prefix", "team_display.tag_suffix", "team_display.tag_color"
        ));
        put("commands.yml", Set.of());
    }};
    public static final Map<String, Pattern> VALUE_VALIDATORS = new HashMap<String, Pattern>() {{
        put("storage.mysql.port", Pattern.compile("^\\d{1,5}$"));
        put("max_team_size", Pattern.compile("^\\d+$"));
        put("max_teams_per_player", Pattern.compile("^\\d+$"));
        put("team_creation.min_tag_length", Pattern.compile("^\\d+$"));
        put("team_creation.max_tag_length", Pattern.compile("^\\d+$"));
        put("colors.primary", Pattern.compile("^#[0-9A-Fa-f]{6}$"));
        put("colors.secondary", Pattern.compile("^#[0-9A-Fa-f]{6}$"));
        put("colors.accent", Pattern.compile("^#[0-9A-Fa-f]{6}$"));
    }};
    public static void performIntelligentUpdate(JustTeams plugin) {
        plugin.getLogger().info("Starting intelligent configuration update system...");
        LocalDateTime updateTime = LocalDateTime.now();
        String timestamp = updateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        IntelligentConfigHelper.createUpdateSnapshot(plugin, timestamp);
        restoreCreateTeamSection(plugin);
        int successCount = 0;
        int failCount = 0;
        for (String fileName : CONFIG_FILES) {
            try {
                if (IntelligentConfigHelper.performIntelligentFileUpdate(plugin, fileName, timestamp)) {
                    successCount++;
                    plugin.getLogger().info("Successfully performed intelligent update on " + fileName);
                } else {
                    plugin.getLogger().info(fileName + " was already up to date");
                }
            } catch (Exception e) {
                failCount++;
                plugin.getLogger().log(Level.SEVERE, "Failed intelligent update for " + fileName + ": " + e.getMessage(), e);
                try {
                    IntelligentConfigHelper.performEmergencyRecovery(plugin, fileName);
                    successCount++;
                } catch (Exception recoveryError) {
                    plugin.getLogger().log(Level.SEVERE, "Emergency recovery failed for " + fileName, recoveryError);
                }
            }
        }
        IntelligentConfigHelper.generateUpdateReport(plugin, successCount, failCount, timestamp);
        plugin.getLogger().info("Intelligent update system completed! Success: " + successCount + ", Failed: " + failCount);
    }
    public static void performConfigHealthCheck(JustTeams plugin) {
        plugin.getLogger().info("Performing configuration health check...");
        boolean allHealthy = true;
        List<String> issues = new ArrayList<>();
        for (String fileName : CONFIG_FILES) {
            File configFile = new File(plugin.getDataFolder(), fileName);
            if (!configFile.exists()) {
                issues.add(fileName + ": File missing");
                allHealthy = false;
                continue;
            }
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                String versionKey = getVersionKey(fileName);
                if (versionKey != null && !config.contains(versionKey)) {
                    issues.add(fileName + ": Missing version key");
                    allHealthy = false;
                }
                if (IntelligentConfigHelper.hasCorruptedValues(config, fileName)) {
                    issues.add(fileName + ": Contains corrupted values");
                    allHealthy = false;
                }
            } catch (Exception e) {
                issues.add(fileName + ": YAML syntax error - " + e.getMessage());
                allHealthy = false;
            }
        }
        if (allHealthy) {
            plugin.getLogger().info("✓ All configuration files are healthy");
        } else {
            plugin.getLogger().warning("✗ Configuration health check found issues:");
            for (String issue : issues) {
                plugin.getLogger().warning("  - " + issue);
            }
            plugin.getLogger().info("Running intelligent auto-repair...");
            performIntelligentUpdate(plugin);
        }
    }
    private static void restoreCreateTeamSection(JustTeams plugin) {
        try {
            File guiFile = new File(plugin.getDataFolder(), "gui.yml");
            if (!guiFile.exists()) {
                return;
            }
            FileConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
            ConfigurationSection noTeamGui = guiConfig.getConfigurationSection("no-team-gui");
            if (noTeamGui == null) {
                plugin.getLogger().info("no-team-gui section missing, restoring...");
                return;
            }
            ConfigurationSection items = noTeamGui.getConfigurationSection("items");
            if (items == null) {
                plugin.getLogger().info("no-team-gui.items section missing, restoring...");
                return;
            }
            if (!items.contains("create-team")) {
                plugin.getLogger().info("create-team item missing, restoring...");
                items.set("create-team.slot", 12);
                items.set("create-team.material", "WRITABLE_BOOK");
                items.set("create-team.name", "<gradient:#4C9DDE:#4C96D2><bold>ᴄʀᴇᴀᴛᴇ ᴀ ᴛᴇᴀᴍ</bold></gradient>");
                items.set("create-team.lore", List.of(
                    "<gray>Start your own team and invite your friends!</gray>",
                    "",
                    "<yellow>Click to begin the creation process.</yellow>"
                ));
                guiConfig.save(guiFile);
                plugin.getLogger().info("Successfully restored create-team section");
            }
            if (!items.contains("leaderboards")) {
                plugin.getLogger().info("leaderboards item missing, restoring...");
                items.set("leaderboards.slot", 14);
                items.set("leaderboards.material", "EMERALD");
                items.set("leaderboards.name", "<gradient:#4C9DDE:#4C96D2><bold>ᴠɪᴇᴡ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅs</bold></gradient>");
                items.set("leaderboards.lore", List.of(
                    "<gray>See the top teams on the server.</gray>",
                    "",
                    "<yellow>Click to view leaderboards.</yellow>"
                ));
                guiConfig.save(guiFile);
                plugin.getLogger().info("Successfully restored leaderboards section");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore create-team section: " + e.getMessage());
        }
    }
}
