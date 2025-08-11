package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class GUIManager {

    private final JustTeams plugin;
    private File guiConfigFile;
    private FileConfiguration guiConfig;

    public GUIManager(JustTeams plugin) {
        this.plugin = plugin;
        createGuiConfig();
    }

    public void reload() {
        if (guiConfigFile == null) {
            createGuiConfig();
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiConfigFile);
    }

    private void createGuiConfig() {
        guiConfigFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiConfigFile.exists()) {
            guiConfigFile.getParentFile().mkdirs();
            plugin.saveResource("gui.yml", false);
        }

        guiConfig = new YamlConfiguration();
        try {
            guiConfig.load(guiConfigFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("Could not load gui.yml!");
            e.printStackTrace();
        }
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public ConfigurationSection getGUI(String key) {
        return guiConfig.getConfigurationSection(key);
    }
}