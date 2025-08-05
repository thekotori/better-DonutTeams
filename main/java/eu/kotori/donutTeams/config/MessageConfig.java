package eu.kotori.donutTeams.config;

import eu.kotori.donutTeams.DonutTeams;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageConfig {

    private final DonutTeams plugin;
    private File customConfigFile;
    private FileConfiguration customConfig;

    public MessageConfig(DonutTeams plugin) {
        this.plugin = plugin;
        createCustomConfig();
    }

    public FileConfiguration getCustomConfig() {
        return this.customConfig;
    }

    public void reload() {
        customConfigFile = new File(plugin.getDataFolder(), "messages.yml");
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    private void createCustomConfig() {
        customConfigFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}