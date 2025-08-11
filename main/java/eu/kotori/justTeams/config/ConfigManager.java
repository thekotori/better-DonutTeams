package eu.kotori.justTeams.config;

import eu.kotori.justTeams.JustTeams;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final JustTeams plugin;
    private FileConfiguration config;

    public ConfigManager(JustTeams plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMainColor() {
        return config.getString("settings.main_color", "#4C9DDE");
    }

    public String getAccentColor() {
        return config.getString("settings.accent_color", "#4C96D2");
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

    public boolean isBankEnabled() {
        return config.getBoolean("team_bank.enabled", true);
    }

    public double getMaxBankBalance() {
        return config.getDouble("team_bank.max_balance", 1000000.0);
    }

    public boolean isEnderChestEnabled() {
        return config.getBoolean("team_enderchest.enabled", true);
    }

    public int getEnderChestRows() {
        int rows = config.getInt("team_enderchest.rows", 3);
        return Math.max(1, Math.min(6, rows));
    }

    public boolean isSoundsEnabled() {
        return config.getBoolean("effects.sounds.enabled", true);
    }

    public String getSuccessSound() {
        return config.getString("effects.sounds.success", "ENTITY_PLAYER_LEVELUP");
    }

    public String getErrorSound() {
        return config.getString("effects.sounds.error", "ENTITY_VILLAGER_NO");
    }

    public String getTeleportSound() {
        return config.getString("effects.sounds.teleport", "ENTITY_ENDERMAN_TELEPORT");
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("effects.particles.enabled", true);
    }

    public String getWarmupParticle() {
        return config.getString("effects.particles.teleport_warmup", "PORTAL");
    }

    public String getSuccessParticle() {
        return config.getString("effects.particles.teleport_success", "END_ROD");
    }
}