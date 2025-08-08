package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class GuiConfigManager {

    private final DonutTeams plugin;
    private File guiConfigFile;
    private FileConfiguration guiConfig;

    public GuiConfigManager(DonutTeams plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        guiConfigFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiConfigFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiConfigFile);
    }

    public ConfigurationSection getGUI(String key) {
        return guiConfig.getConfigurationSection(key);
    }

    public String getString(String path, String def) {
        return guiConfig.getString(path, def);
    }

    public List<String> getStringList(String path) {
        if (!guiConfig.isSet(path)) {
            return Collections.emptyList();
        }
        return guiConfig.getStringList(path);
    }

    public Material getMaterial(String path, Material def) {
        String materialName = guiConfig.getString(path, def.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material '" + materialName + "' found in gui.yml at path '" + path + "'. Using default: " + def.name());
            return def;
        }
    }
}