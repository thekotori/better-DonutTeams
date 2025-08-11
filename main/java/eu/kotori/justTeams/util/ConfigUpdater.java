package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeams;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigUpdater {

    public static void update(JustTeams plugin) {
        updateConfig(plugin, "config.yml");
        updateConfig(plugin, "messages.yml");
    }

    private static void updateConfig(JustTeams plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }

        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);

        try (InputStream defaultConfigStream = plugin.getResource(fileName)) {
            if (defaultConfigStream == null) return;

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));

            boolean updated = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!currentConfig.contains(key)) {
                    currentConfig.set(key, defaultConfig.get(key));
                    updated = true;
                }
            }

            if (currentConfig.getInt("config-version", 0) < defaultConfig.getInt("config-version", 1) && fileName.equals("config.yml")) {
                currentConfig.set("config-version", defaultConfig.getInt("config-version"));
                updated = true;
            }

            if (updated) {
                currentConfig.save(configFile);
                plugin.getLogger().info(fileName + " has been updated with new values.");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getMessage());
        }
    }
}