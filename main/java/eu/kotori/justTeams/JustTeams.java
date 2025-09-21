package eu.kotori.justTeams;
import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.commands.TeamMessageCommand;
import eu.kotori.justTeams.config.ConfigManager;
import eu.kotori.justTeams.config.MessageManager;
import eu.kotori.justTeams.gui.GUIManager;
import eu.kotori.justTeams.gui.TeamGUIListener;
import eu.kotori.justTeams.listeners.PlayerConnectionListener;
import eu.kotori.justTeams.listeners.PlayerStatsListener;
import eu.kotori.justTeams.listeners.PvPListener;
import eu.kotori.justTeams.listeners.TeamChatListener;
import eu.kotori.justTeams.listeners.TeamEnderChestListener;
import eu.kotori.justTeams.storage.StorageManager;
import eu.kotori.justTeams.storage.DatabaseStorage;
import eu.kotori.justTeams.storage.DatabaseMigrationManager;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.util.AliasManager;
import eu.kotori.justTeams.util.BedrockSupport;
import eu.kotori.justTeams.util.ChatInputManager;
import eu.kotori.justTeams.util.CommandManager;
import eu.kotori.justTeams.util.ConfigUpdater;
import eu.kotori.justTeams.util.DebugLogger;
import eu.kotori.justTeams.util.GuiConfigManager;
import eu.kotori.justTeams.util.ItemBuilder;
import eu.kotori.justTeams.util.StartupManager;
import eu.kotori.justTeams.util.TaskRunner;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import java.util.Map;
import eu.kotori.justTeams.storage.DatabaseStorage;
import eu.kotori.justTeams.util.PAPIExpansion;
import eu.kotori.justTeams.util.StartupMessage;
import me.clip.placeholderapi.PlaceholderAPI;
public final class JustTeams extends JavaPlugin {
    private static JustTeams instance;
    private static NamespacedKey actionKey;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private StorageManager storageManager;
    private TeamManager teamManager;
    private GUIManager guiManager;
    private TaskRunner taskRunner;
    private ChatInputManager chatInputManager;
    private CommandManager commandManager;
    private AliasManager aliasManager;
    private GuiConfigManager guiConfigManager;
    private DebugLogger debugLogger;
    private StartupManager startupManager;
    private BedrockSupport bedrockSupport;
    private TeamChatListener teamChatListener;
    private MiniMessage miniMessage;
    private Economy economy;
    public boolean updateAvailable = false;
    public String latestVersion = "";
    public void onEnable() {
        instance = this;
        actionKey = new NamespacedKey(this, "action");
        Logger logger = getLogger();
        logger.info("Starting JustTeams...");
        if (!Bukkit.getServer().getName().equals("Paper") && !Bukkit.getServer().getName().equals("Folia")) {
            logger.warning("JustTeams is designed for Paper/Folia servers. Some features may not work correctly on other server software.");
        }
        if (Bukkit.getServer().getName().equals("Folia")) {
            logger.info("Folia detected! Using threaded region support.");
        }
        miniMessage = MiniMessage.miniMessage();
        try {
            setupEconomy();
            initializeManagers();
            registerListeners();
            registerCommands();
            registerPlaceholderAPI();
            StartupMessage.send();
            logger.info("JustTeams has been enabled successfully!");
        } catch (Exception e) {
            logger.severe("Failed to enable JustTeams: " + e.getMessage());
            logger.log(java.util.logging.Level.SEVERE, "JustTeams enable error details", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    public void onDisable() {
        Logger logger = getLogger();
        logger.info("Disabling JustTeams...");
        try {
            if (taskRunner != null) {
                taskRunner.cancelAllTasks();
            }
        } catch (Exception e) {
            logger.warning("Error cancelling tasks: " + e.getMessage());
        }
        try {
            if (teamManager != null) {
                teamManager.shutdown();
            }
        } catch (Exception e) {
            logger.warning("Error shutting down team manager: " + e.getMessage());
        }
        try {
            if (guiManager != null && guiManager.getUpdateThrottle() != null) {
                guiManager.getUpdateThrottle().cleanup();
            }
        } catch (Exception e) {
            logger.warning("Error cleaning up GUI throttles: " + e.getMessage());
        }
        try {
            if (storageManager != null) {
                storageManager.shutdown();
            }
        } catch (Exception e) {
            logger.warning("Error shutting down storage manager: " + e.getMessage());
        }
        logger.info("JustTeams has been disabled.");
    }
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        ConfigUpdater.updateAllConfigs(this);
        ConfigUpdater.migrateToPlaceholderSystem(this);
        messageManager = new MessageManager(this);
        storageManager = new StorageManager(this);
        if (!storageManager.init()) {
            throw new RuntimeException("Failed to initialize storage manager");
        }

        if (storageManager.getStorage() instanceof DatabaseStorage) {
            DatabaseMigrationManager migrationManager = new DatabaseMigrationManager(this, (DatabaseStorage) storageManager.getStorage());
            if (!migrationManager.performMigration()) {
                getLogger().warning("Database migration completed with warnings. Some features may not work correctly.");
            }
        }
        teamManager = new TeamManager(this);
        guiManager = new GUIManager(this);
        taskRunner = new TaskRunner(this);
        chatInputManager = new ChatInputManager(this);
        commandManager = new CommandManager(this);
        aliasManager = new AliasManager(this);
        guiConfigManager = new GuiConfigManager(this);
        debugLogger = new DebugLogger(this);
        bedrockSupport = new BedrockSupport(this);
        teamManager.cleanupEnderChestLocksOnStartup();
        if (storageManager.getStorage() instanceof DatabaseStorage) {
            startupManager = new StartupManager(this, (DatabaseStorage) storageManager.getStorage());
            if (!startupManager.performStartup()) {
                throw new RuntimeException("Startup sequence failed! Check logs for details.");
            }
            startupManager.schedulePeriodicHealthChecks();
            startupManager.schedulePeriodicPermissionSaves();
        }
        startCrossServerTasks();
    }
    private void startCrossServerTasks() {
        String serverName = configManager.getServerIdentifier();
        long heartbeatInterval = configManager.getHeartbeatInterval();
        long crossServerInterval = configManager.getCrossServerSyncInterval();
        long criticalInterval = configManager.getCriticalSyncInterval();
        long cacheCleanupInterval = configManager.getCacheCleanupInterval();

        taskRunner.runAsyncTaskTimer(() -> {
            try {
                if (storageManager.getStorage() instanceof DatabaseStorage dbStorage) {
                    dbStorage.updateServerHeartbeat(serverName);
                } else {
                    storageManager.getStorage().updateServerHeartbeat(serverName);
                }
                if (configManager.isDebugLoggingEnabled()) {
                    debugLogger.log("Updated server heartbeat for: " + serverName);
                }
            } catch (Exception e) {
                getLogger().warning("Error updating server heartbeat: " + e.getMessage());
            }
        }, heartbeatInterval, heartbeatInterval);

        if (configManager.isCrossServerSyncEnabled()) {
            taskRunner.runAsyncTaskTimer(() -> {
                try {
                    teamManager.syncCrossServerData();
                    if (configManager.isDebugLoggingEnabled()) {
                        debugLogger.log("Cross-server sync cycle completed");
                    }
                } catch (Exception e) {
                    getLogger().warning("Error in cross-server sync: " + e.getMessage());
                }
            }, crossServerInterval, crossServerInterval);

            taskRunner.runAsyncTaskTimer(() -> {
                try {
                    teamManager.syncCriticalUpdates();
                } catch (Exception e) {
                    getLogger().warning("Error in critical sync: " + e.getMessage());
                }
            }, criticalInterval, criticalInterval);

            taskRunner.runAsyncTaskTimer(() -> {
                try {
                    teamManager.flushCrossServerUpdates();
                    if (configManager.isDebugLoggingEnabled()) {
                        debugLogger.log("Flushed pending cross-server updates");
                    }
                } catch (Exception e) {
                    getLogger().warning("Error flushing cross-server updates: " + e.getMessage());
                }
            }, 120L, 120L);
        }

        taskRunner.runAsyncTaskTimer(() -> {
            try {
                teamManager.cleanupExpiredCache();
                if (configManager.isDebugLoggingEnabled()) {
                    debugLogger.log("Cleaned up expired cache entries");
                }
            } catch (Exception e) {
                getLogger().warning("Error cleaning up cache: " + e.getMessage());
            }
        }, cacheCleanupInterval, cacheCleanupInterval);

        taskRunner.runAsyncTaskTimer(() -> {
            try {
                if (storageManager.getStorage() instanceof DatabaseStorage) {
                    ((DatabaseStorage) storageManager.getStorage()).cleanupOldCrossServerData();
                    if (configManager.isDebugLoggingEnabled()) {
                        debugLogger.log("Cleaned up old cross-server data");
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Error cleaning up old cross-server data: " + e.getMessage());
            }
        }, 1200L, 1200L);

        if (configManager.isConnectionPoolMonitoringEnabled()) {
            taskRunner.runAsyncTaskTimer(() -> {
                try {
                    if (storageManager.getStorage() instanceof DatabaseStorage) {
                        DatabaseStorage dbStorage = (DatabaseStorage) storageManager.getStorage();
                        if (configManager.isDebugEnabled()) {
                            Map<String, Object> stats = dbStorage.getDatabaseStats();
                            debugLogger.log("Database stats: " + stats.toString());
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Error monitoring connection pool: " + e.getMessage());
                }
            }, configManager.getConnectionPoolLogInterval() * 60L, configManager.getConnectionPoolLogInterval() * 60L);
        }
    }
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new TeamGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        teamChatListener = new TeamChatListener(this);
        getServer().getPluginManager().registerEvents(teamChatListener, this);
        getServer().getPluginManager().registerEvents(new TeamEnderChestListener(this), this);
    }
    private void registerCommands() {
        TeamCommand teamCommand = new TeamCommand(this);
        TeamMessageCommand teamMessageCommand = new TeamMessageCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);
        getCommand("guild").setExecutor(teamCommand);
        getCommand("guild").setTabCompleter(teamCommand);
        getCommand("clan").setExecutor(teamCommand);
        getCommand("clan").setTabCompleter(teamCommand);
        getCommand("party").setExecutor(teamCommand);
        getCommand("party").setTabCompleter(teamCommand);
        getCommand("teammsg").setExecutor(teamMessageCommand);
        getCommand("guildmsg").setExecutor(teamMessageCommand);
        getCommand("clanmsg").setExecutor(teamMessageCommand);
        getCommand("partymsg").setExecutor(teamMessageCommand);
        aliasManager.registerAliases();
        commandManager.registerCommands();
    }
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }
    public static JustTeams getInstance() {
        return instance;
    }
    public static NamespacedKey getActionKey() {
        return actionKey;
    }
    public ConfigManager getConfigManager() {
        return configManager;
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
    public TeamChatListener getTeamChatListener() {
        return teamChatListener;
    }
    public GUIManager getGuiManager() {
        return guiManager;
    }
    public TaskRunner getTaskRunner() {
        return taskRunner;
    }
    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
    public CommandManager getCommandManager() {
        return commandManager;
    }
    public AliasManager getAliasManager() {
        return aliasManager;
    }
    public GuiConfigManager getGuiConfigManager() {
        return guiConfigManager;
    }
    public StartupManager getStartupManager() {
        return startupManager;
    }
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }
    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
    public Economy getEconomy() {
        return economy;
    }
    public BedrockSupport getBedrockSupport() {
        return bedrockSupport;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found! Economy features will be disabled.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found! Economy features will be disabled.");
            return false;
        }
        economy = rsp.getProvider();
        if (economy != null) {
            getLogger().info("Economy provider found: " + economy.getName());
        }
        return economy != null;
    }
}
