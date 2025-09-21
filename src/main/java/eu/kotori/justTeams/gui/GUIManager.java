package eu.kotori.justTeams.gui;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
public class GUIManager {
    private final JustTeams plugin;
    private File guiConfigFile;
    private FileConfiguration guiConfig;
    private final GUIUpdateThrottle updateThrottle;

    public GUIManager(JustTeams plugin) {
        this.plugin = plugin;
        this.updateThrottle = new GUIUpdateThrottle(plugin);
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
            plugin.getLogger().log(Level.SEVERE, "GUI config load error details", e);
        }
    }
    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }
    public ConfigurationSection getGUI(String key) {
        return guiConfig.getConfigurationSection(key);
    }
    public ConfigurationSection getNoTeamGUI() {
        return getGUI("no-team-gui");
    }
    public String getNoTeamGUITitle() {
        return getString("no-team-gui.title", "ᴛᴇᴀᴍ ᴍᴇɴᴜ");
    }
    public int getNoTeamGUISize() {
        return getInt("no-team-gui.size", 27);
    }
    public ConfigurationSection getCreateTeamButton() {
        return getGUI("no-team-gui.items.create-team");
    }
    public ConfigurationSection getLeaderboardsButton() {
        return getGUI("no-team-gui.items.leaderboards");
    }
    public ConfigurationSection getNoTeamGUIFillItem() {
        return getGUI("no-team-gui.fill-item");
    }
    public ConfigurationSection getTeamGUI() {
        return getGUI("team-gui");
    }
    public String getTeamGUITitle() {
        return getString("team-gui.title", "ᴛᴇᴀᴍ - <members>/<max_members>");
    }
    public int getTeamGUISize() {
        return getInt("team-gui.size", 54);
    }
    public ConfigurationSection getPlayerHeadSection() {
        return getGUI("team-gui.items.player-head");
    }
    public String getOnlineNameFormat() {
        return getString("team-gui.items.player-head.online-name-format", "<gradient:#4C9DDE:#4C96D2><status_indicator><role_icon><player></gradient>");
    }
    public String getOfflineNameFormat() {
        return getString("team-gui.items.player-head.offline-name-format", "<gray><status_indicator><role_icon><player>");
    }
    public List<String> getPlayerHeadLore() {
        return getStringList("team-gui.items.player-head.lore");
    }
    public String getCanEditPrompt() {
        return getString("team-gui.items.player-head.can-edit-prompt", "<yellow>Click to edit this member.</yellow>");
    }
    public String getCanViewPrompt() {
        return getString("team-gui.items.player-head.can-view-prompt", "<yellow>Click to view your information.</yellow>");
    }
    public String getCannotEditPrompt() {
        return getString("team-gui.items.player-head.cannot-edit-prompt", "");
    }
    public ConfigurationSection getJoinRequestsButton() {
        return getGUI("team-gui.items.join-requests");
    }
    public ConfigurationSection getJoinRequestsLockedButton() {
        return getGUI("team-gui.items.join-requests-locked");
    }
    public ConfigurationSection getWarpsButton() {
        return getGUI("team-gui.items.warps");
    }
    public ConfigurationSection getBankButton() {
        return getGUI("team-gui.items.bank");
    }
    public ConfigurationSection getBankLockedButton() {
        return getGUI("team-gui.items.bank-locked");
    }
    public ConfigurationSection getHomeButton() {
        return getGUI("team-gui.items.home");
    }
    public ConfigurationSection getTeamSettingsButton() {
        return getGUI("team-gui.items.team-settings");
    }
    public ConfigurationSection getTeamSettingsGUI() {
        return getGUI("team-settings-gui");
    }
    public String getTeamSettingsGUITitle() {
        return getString("team-settings-gui.title", "ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs");
    }
    public int getTeamSettingsGUISize() {
        return getInt("team-settings-gui.size", 27);
    }
    public ConfigurationSection getMemberEditGUI() {
        return getGUI("member-edit-gui");
    }
    public String getMemberEditGUITitle() {
        return getString("member-edit-gui.title", "ᴇᴅɪᴛ ᴍᴇᴍʙᴇʀ");
    }
    public int getMemberEditGUISize() {
        return getInt("member-edit-gui.size", 27);
    }
    public ConfigurationSection getMemberPermissionsGUI() {
        return getGUI("member-permissions-gui");
    }
    public String getMemberPermissionsGUITitle() {
        return getString("member-permissions-gui.title", "ᴍᴇᴍʙᴇʀ ᴘᴇʀᴍɪssɪᴏɴs");
    }
    public int getMemberPermissionsGUISize() {
        return getInt("member-permissions-gui.size", 27);
    }
    public ConfigurationSection getBankGUI() {
        return getGUI("bank-gui");
    }
    public String getBankGUITitle() {
        return getString("bank-gui.title", "ᴛᴇᴀᴍ ʙᴀɴᴋ");
    }
    public int getBankGUISize() {
        return getInt("bank-gui.size", 27);
    }
    public ConfigurationSection getWarpsGUI() {
        return getGUI("warps-gui");
    }
    public String getWarpsGUITitle() {
        return getString("warps-gui.title", "ᴛᴇᴀᴍ ᴡᴀʀᴘs");
    }
    public int getWarpsGUISize() {
        return getInt("warps-gui.size", 27);
    }
    public ConfigurationSection getLeaderboardGUI() {
        return getGUI("leaderboard-gui");
    }
    public String getLeaderboardGUITitle() {
        return getString("leaderboard-gui.title", "ᴛᴇᴀᴍ ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ");
    }
    public int getLeaderboardGUISize() {
        return getInt("leaderboard-gui.size", 27);
    }
    public ConfigurationSection getAdminGUI() {
        return getGUI("admin-gui");
    }
    public String getAdminGUITitle() {
        return getString("admin-gui.title", "ᴀᴅᴍɪɴ ᴘᴀɴᴇʟ");
    }
    public int getAdminGUISize() {
        return getInt("admin-gui.size", 27);
    }
    public String getString(String path, String defaultValue) {
        return guiConfig.getString(path, defaultValue);
    }
    public int getInt(String path, int defaultValue) {
        return guiConfig.getInt(path, defaultValue);
    }
    public boolean getBoolean(String path, boolean defaultValue) {
        return guiConfig.getBoolean(path, defaultValue);
    }
    public List<String> getStringList(String path) {
        return guiConfig.getStringList(path);
    }
    public Material getMaterial(String path, Material defaultValue) {
        String materialName = guiConfig.getString(path, defaultValue.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material " + materialName + " found in gui.yml at path " + path + ". Using default: " + defaultValue.name());
            return defaultValue;
        }
    }
    public Material getMaterial(String path) {
        return getMaterial(path, Material.STONE);
    }
    public boolean hasGUI(String key) {
        return guiConfig.contains(key);
    }
    public java.util.Set<String> getGUIKeys() {
        return guiConfig.getKeys(true);
    }

    public GUIUpdateThrottle getUpdateThrottle() {
        return updateThrottle;
    }
    public ConfigurationSection getItemConfig(String guiKey, String itemKey) {
        ConfigurationSection guiSection = getGUI(guiKey);
        if (guiSection != null) {
            return guiSection.getConfigurationSection("items." + itemKey);
        }
        return null;
    }
    public int getItemSlot(String guiKey, String itemKey, int defaultValue) {
        ConfigurationSection itemSection = getItemConfig(guiKey, itemKey);
        if (itemSection != null) {
            return itemSection.getInt("slot", defaultValue);
        }
        return defaultValue;
    }
    public Material getItemMaterial(String guiKey, String itemKey, Material defaultValue) {
        ConfigurationSection itemSection = getItemConfig(guiKey, itemKey);
        if (itemSection != null) {
            return getMaterial("items." + itemKey + ".material", defaultValue);
        }
        return defaultValue;
    }
    public String getItemName(String guiKey, String itemKey, String defaultValue) {
        ConfigurationSection itemSection = getItemConfig(guiKey, itemKey);
        if (itemSection != null) {
            return itemSection.getString("name", defaultValue);
        }
        return defaultValue;
    }
    public List<String> getItemLore(String guiKey, String itemKey) {
        ConfigurationSection itemSection = getItemConfig(guiKey, itemKey);
        if (itemSection != null) {
            return itemSection.getStringList("lore");
        }
        return java.util.Collections.emptyList();
    }
}
