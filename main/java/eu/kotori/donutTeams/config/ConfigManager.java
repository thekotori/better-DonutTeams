package eu.kotori.donutTeams.config;

import eu.kotori.donutTeams.DonutTeams;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final DonutTeams plugin;
    private FileConfiguration config;

    public ConfigManager(DonutTeams plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMainColor() {
        return config.getString("settings.main_color", "#95FD95");
    }

    public String getAccentColor() {
        return config.getString("settings.accent_color", "#FFFFFF");
    }

    public int getMaxTeamSize() {
        return config.getInt("settings.max_team_size", 10);
    }

    public int getMinNameLength() {
        return config.getInt("settings.min_name_length", 3);
    }

    public int getMaxNameLength() {
        return config.getInt("settings.max_name_length", 16);
    }

    public int getMaxTagLength() {
        return config.getInt("settings.max_tag_length", 6);
    }

    public int getMaxDescriptionLength() {
        return config.getInt("settings.max_description_length", 64);
    }

    public boolean getDefaultPvpStatus() {
        return config.getBoolean("settings.default_pvp_status", true);
    }

    public double getMaxBankBalance() {
        return config.getDouble("team_bank.max_balance", 1000000.0);
    }

    public int getEnderChestRows() {
        int rows = config.getInt("team_enderchest.rows", 3);
        return Math.max(1, Math.min(6, rows));
    }
}