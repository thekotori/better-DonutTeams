package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
public class IntelligentConfigHelper {
    public static boolean performIntelligentFileUpdate(JustTeams plugin, String fileName, String timestamp) throws IOException {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created " + fileName + " from template");
            return true;
        }
        FileConfiguration currentConfig = null;
        try {
            currentConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("YAML corruption detected in " + fileName + ", performing auto-repair...");
            if (performYamlAutoRepair(configFile)) {
                currentConfig = YamlConfiguration.loadConfiguration(configFile);
                plugin.getLogger().info("Successfully auto-repaired " + fileName);
            } else {
                throw new IOException("Could not repair corrupted YAML in " + fileName);
            }
        }
        try (InputStream defaultStream = plugin.getResource(fileName)) {
            if (defaultStream == null) {
                plugin.getLogger().warning("No default template found for " + fileName);
                return false;
            }
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            Map<String, Object> userValues = preserveUserCustomizations(currentConfig, fileName);
            boolean needsUpdate = intelligentVersionCheck(currentConfig, defaultConfig, fileName);
            if (!needsUpdate && !hasMissingKeys(currentConfig, defaultConfig)) {
                return false;
            }
            createIntelligentBackup(plugin, configFile, fileName, timestamp);
            boolean updated = performSmartMerge(currentConfig, defaultConfig, userValues, fileName);
            if (updated) {
                validateAndSanitizeConfig(currentConfig, fileName);
                currentConfig.save(configFile);
                plugin.getLogger().info("Intelligently updated " + fileName + " while preserving user customizations");
                return true;
            }
        }
        return false;
    }
    public static Map<String, Object> preserveUserCustomizations(FileConfiguration config, String fileName) {
        Map<String, Object> userValues = new LinkedHashMap<>();
        Set<String> customizableKeys = ConfigUpdater.USER_CUSTOMIZABLE_KEYS.getOrDefault(fileName, Set.of());
        for (String key : customizableKeys) {
            if (config.contains(key)) {
                Object value = config.get(key);
                if (value != null && !isDefaultValue(key, value)) {
                    userValues.put(key, value);
                }
            }
        }
        userValues.putAll(preserveCustomSections(config, fileName));
        return userValues;
    }
    public static Map<String, Object> preserveCustomSections(FileConfiguration config, String fileName) {
        Map<String, Object> customSections = new LinkedHashMap<>();
        if ("config.yml".equals(fileName)) {
            preserveSection(config, "storage.mysql", customSections);
            preserveSection(config, "webhook", customSections);
            preserveSection(config, "team_creation", customSections);
        }
        return customSections;
    }
    public static void preserveSection(FileConfiguration config, String sectionPath, Map<String, Object> storage) {
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        if (section != null) {
            for (String key : section.getKeys(true)) {
                String fullKey = sectionPath + "." + key;
                storage.put(fullKey, section.get(key));
            }
        }
    }
    public static boolean intelligentVersionCheck(FileConfiguration current, FileConfiguration defaultConfig, String fileName) {
        String versionKey = ConfigUpdater.getVersionKey(fileName);
        if (versionKey == null) return false;
        int currentVersion = current.getInt(versionKey, 0);
        int defaultVersion = defaultConfig.getInt(versionKey, 0);
        if (currentVersion < defaultVersion) {
            return true;
        } else if (currentVersion > defaultVersion) {
            current.set(versionKey, defaultVersion);
            return true;
        }
        return false;
    }
    public static boolean hasMissingKeys(FileConfiguration current, FileConfiguration defaultConfig) {
        for (String key : defaultConfig.getKeys(true)) {
            if (!current.contains(key) && !defaultConfig.isConfigurationSection(key)) {
                return true;
            }
        }
        return false;
    }
    public static void createIntelligentBackup(JustTeams plugin, File configFile, String fileName, String timestamp) throws IOException {
        File backupFolder = new File(plugin.getDataFolder(), "backups/intelligent/" + timestamp);
        backupFolder.mkdirs();
        File backupFile = new File(backupFolder, fileName);
        java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(),
                                 java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Created intelligent backup: backups/intelligent/" + timestamp + "/" + fileName);
    }
    public static void createUpdateSnapshot(JustTeams plugin, String timestamp) {
        try {
            File snapshotFolder = new File(plugin.getDataFolder(), "backups/snapshots/" + timestamp);
            snapshotFolder.mkdirs();
            List<String> configFiles = Arrays.asList("config.yml", "messages.yml", "gui.yml", "commands.yml", "placeholders.yml");
            for (String fileName : configFiles) {
                File configFile = new File(plugin.getDataFolder(), fileName);
                if (configFile.exists()) {
                    File snapshotFile = new File(snapshotFolder, fileName);
                    java.nio.file.Files.copy(configFile.toPath(), snapshotFile.toPath());
                }
            }
            plugin.getLogger().info("Created pre-update snapshot: backups/snapshots/" + timestamp);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create update snapshot: " + e.getMessage());
        }
    }
    public static boolean performSmartMerge(FileConfiguration current, FileConfiguration defaultConfig,
                                           Map<String, Object> userValues, String fileName) {
        boolean updated = false;
        String versionKey = ConfigUpdater.getVersionKey(fileName);
        if (versionKey != null) {
            int defaultVersion = defaultConfig.getInt(versionKey);
            current.set(versionKey, defaultVersion);
            updated = true;
        }
        for (String key : defaultConfig.getKeys(true)) {
            if (!defaultConfig.isConfigurationSection(key)) {
                if (!current.contains(key)) {
                    current.set(key, defaultConfig.get(key));
                    updated = true;
                } else if (shouldUpdateKey(key, current.get(key), defaultConfig.get(key), fileName)) {
                    current.set(key, defaultConfig.get(key));
                    updated = true;
                }
            }
        }
        for (Map.Entry<String, Object> entry : userValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isValidUserValue(key, value)) {
                current.set(key, value);
            }
        }
        return updated;
    }
    public static boolean shouldUpdateKey(String key, Object currentValue, Object defaultValue, String fileName) {
        if ("prefix".equals(key) && fileName.equals("messages.yml")) {
            String currentStr = currentValue.toString();
            return currentStr.contains("<gradient:") && !currentStr.contains("</gradient>");
        }
        if (key.contains("name") && fileName.equals("gui.yml")) {
            String currentStr = currentValue.toString();
            return currentStr.contains("<gradient:") && !currentStr.contains("</gradient>");
        }
        if (key.contains("color") && fileName.equals("placeholders.yml")) {
            String currentStr = currentValue.toString();
            return currentStr.trim().isEmpty() || currentStr.equals("\"\"");
        }
        return false;
    }
    public static boolean isDefaultValue(String key, Object value) {
        if (value == null) return true;
        String strValue = value.toString().trim();
        return strValue.isEmpty() ||
               strValue.equals("\"\"") ||
               strValue.equals("__STRING_PLACEHOLDER_0__") ||
               strValue.contains("<gradient:") && !strValue.contains("</gradient>");
    }
    public static boolean isValidUserValue(String key, Object value) {
        if (value == null) return false;
        Pattern validator = ConfigUpdater.VALUE_VALIDATORS.get(key);
        if (validator != null) {
            return validator.matcher(value.toString()).matches();
        }
        return true;
    }
    public static void validateAndSanitizeConfig(FileConfiguration config, String fileName) {
        if ("config.yml".equals(fileName)) {
            validateConfigValues(config);
        } else if ("messages.yml".equals(fileName)) {
            validateMessageFormats(config);
        } else if ("gui.yml".equals(fileName)) {
            validateGuiConfiguration(config);
        } else if ("placeholders.yml".equals(fileName)) {
            validatePlaceholderValues(config);
        }
    }
    public static void validateConfigValues(FileConfiguration config) {
        int maxTeamSize = config.getInt("max_team_size", 10);
        if (maxTeamSize < 1 || maxTeamSize > 100) {
            config.set("max_team_size", 10);
        }
        int mysqlPort = config.getInt("storage.mysql.port", 3306);
        if (mysqlPort < 1 || mysqlPort > 65535) {
            config.set("storage.mysql.port", 3306);
        }
    }
    public static void validateMessageFormats(FileConfiguration config) {
        String prefix = config.getString("prefix", "");
        if (prefix.contains("<gradient:") && !prefix.contains("</gradient>")) {
            config.set("prefix", "<bold><gradient:#4C9DDE:#FFD700>JustTeams</gradient></bold>");
        }
    }
    public static void validateGuiConfiguration(FileConfiguration config) {
    }
    public static void validatePlaceholderValues(FileConfiguration config) {
        validateColorValue(config, "colors.primary", "#4C9DDE");
        validateColorValue(config, "colors.secondary", "#4C96D2");
        validateColorValue(config, "colors.accent", "#FFD700");
        validateColorValue(config, "colors.success", "#00FF00");
        validateColorValue(config, "colors.error", "#FF0000");
        validateColorValue(config, "colors.warning", "#FFA500");
        validateColorValue(config, "colors.info", "#00BFFF");
    }
    public static void validateColorValue(FileConfiguration config, String key, String defaultValue) {
        String value = config.getString(key, "");
        if (value.trim().isEmpty() || !value.matches("^#[0-9A-Fa-f]{6}$")) {
            config.set(key, defaultValue);
        }
    }
    public static boolean performYamlAutoRepair(File configFile) {
        try {
            List<String> lines = java.nio.file.Files.readAllLines(configFile.toPath());
            boolean repaired = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("<gradient:") && !line.contains("</gradient>")) {
                    if (line.contains("prefix:")) {
                        lines.set(i, "prefix: \"<bold><gradient:#4C9DDE:#FFD700>JustTeams</gradient></bold>\"");
                        repaired = true;
                    } else if (line.contains("name:")) {
                        String indent = line.substring(0, line.indexOf("name:"));
                        lines.set(i, indent + "name: \"<gradient:#4C9DDE:#FFD700>Item</gradient>\"");
                        repaired = true;
                    }
                }
                if (line.matches("\\s*color:\\s*\"?\\s*\"?\\s*$")) {
                    String indent = line.substring(0, line.indexOf("color:"));
                    lines.set(i, indent + "color: \"#FFFFFF\"");
                    repaired = true;
                }
            }
            if (repaired) {
                java.nio.file.Files.write(configFile.toPath(), lines);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    public static void performEmergencyRecovery(JustTeams plugin, String fileName) throws IOException {
        plugin.getLogger().warning("Performing emergency recovery for " + fileName);
        File configFile = new File(plugin.getDataFolder(), fileName);
        File backupFolder = new File(plugin.getDataFolder(), "backups");
        File emergencyBackup = new File(backupFolder, fileName + ".emergency." + System.currentTimeMillis());
        if (configFile.exists()) {
            java.nio.file.Files.copy(configFile.toPath(), emergencyBackup.toPath());
        }
        plugin.saveResource(fileName, true);
        plugin.getLogger().info("Emergency recovery completed for " + fileName + " (backup: " + emergencyBackup.getName() + ")");
    }
    public static void generateUpdateReport(JustTeams plugin, int successCount, int failCount, String timestamp) {
        try {
            File reportsFolder = new File(plugin.getDataFolder(), "reports");
            reportsFolder.mkdirs();
            File reportFile = new File(reportsFolder, "update_report_" + timestamp + ".txt");
            List<String> reportLines = new ArrayList<>();
            reportLines.add("=== JustTeams Configuration Update Report ===");
            reportLines.add("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            reportLines.add("Update ID: " + timestamp);
            reportLines.add("");
            reportLines.add("Results:");
            reportLines.add("  Successful updates: " + successCount);
            reportLines.add("  Failed updates: " + failCount);
            reportLines.add("  Total files processed: 5");
            reportLines.add("");
            reportLines.add("Backup locations:");
            reportLines.add("  Snapshot: backups/snapshots/" + timestamp + "/");
            reportLines.add("  Individual backups: backups/intelligent/" + timestamp + "/");
            reportLines.add("");
            reportLines.add("Features applied:");
            reportLines.add("  ✓ User customization preservation");
            reportLines.add("  ✓ Intelligent version management");
            reportLines.add("  ✓ YAML auto-repair");
            reportLines.add("  ✓ Value validation and sanitization");
            reportLines.add("  ✓ Emergency recovery procedures");
            java.nio.file.Files.write(reportFile.toPath(), reportLines);
            plugin.getLogger().info("Update report generated: reports/update_report_" + timestamp + ".txt");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to generate update report: " + e.getMessage());
        }
    }
    public static boolean hasCorruptedValues(FileConfiguration config, String fileName) {
        if ("messages.yml".equals(fileName)) {
            String prefix = config.getString("prefix", "");
            if (prefix.contains("<gradient:") && !prefix.contains("</gradient>")) {
                return true;
            }
        }
        if ("placeholders.yml".equals(fileName)) {
            for (String colorKey : List.of("colors.primary", "colors.secondary", "colors.accent")) {
                String value = config.getString(colorKey, "");
                if (value.trim().isEmpty() || value.equals("\"\"")) {
                    return true;
                }
            }
        }
        return false;
    }
}
