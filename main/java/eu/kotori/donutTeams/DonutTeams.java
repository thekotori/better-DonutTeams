package eu.kotori.donutTeams;

import eu.kotori.donutTeams.commands.TeamCommand;
import eu.kotori.donutTeams.commands.TeamMessageCommand;
import eu.kotori.donutTeams.config.ConfigManager;
import eu.kotori.donutTeams.config.MessageConfig;
import eu.kotori.donutTeams.config.MessageManager;
import eu.kotori.donutTeams.gui.TeamGUIListener;
import eu.kotori.donutTeams.listeners.PlayerConnectionListener;
import eu.kotori.donutTeams.listeners.PvPListener;
import eu.kotori.donutTeams.listeners.TeamChatListener;
import eu.kotori.donutTeams.storage.StorageManager;
import eu.kotori.donutTeams.team.TeamManager;
import eu.kotori.donutTeams.util.StartupMessage;
import eu.kotori.donutTeams.util.WebhookUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutTeams extends JavaPlugin {

    private static DonutTeams instance;
    private ConfigManager configManager;
    private MessageConfig messageConfig;
    private MessageManager messageManager;
    private StorageManager storageManager;
    private TeamManager teamManager;
    private TeamChatListener teamChatListener;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();

        this.configManager = new ConfigManager(this);
        this.messageConfig = new MessageConfig(this);
        this.messageManager = new MessageManager(this);

        this.storageManager = new StorageManager(this);
        if (!storageManager.init()) {
            getLogger().severe("Failed to initialize storage. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.teamManager = new TeamManager(this);

        registerListeners();
        registerCommands();

        StartupMessage.send();
        WebhookUtil.sendStartupNotification(this);

    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.shutdown();
        }
    }

    private void registerCommands() {
        TeamCommand teamCommand = new TeamCommand(this, this.teamChatListener);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

        getCommand("teammsg").setExecutor(new TeamMessageCommand(this));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvPListener(this), this);

        this.teamChatListener = new TeamChatListener(this);
        Bukkit.getPluginManager().registerEvents(this.teamChatListener, this);
    }

    public static DonutTeams getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}