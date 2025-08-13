package eu.kotori.justTeams;
import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.commands.TeamMessageCommand;
import eu.kotori.justTeams.config.ConfigManager;
import eu.kotori.justTeams.config.MessageManager;
import eu.kotori.justTeams.gui.TeamGUIListener;
import eu.kotori.justTeams.listeners.*;
import eu.kotori.justTeams.storage.StorageManager;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.util.*;
import me.NoChance.PvPManager.PvPManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
public final class JustTeams extends JavaPlugin {
    public enum ProxyType { BUNGEECORD, VELOCITY, NONE }

    private static JustTeams instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private GuiConfigManager guiConfigManager;
    private StorageManager storageManager;
    private TeamManager teamManager;
    private AliasManager aliasManager;
    private TeamChatListener teamChatListener;
    private MiniMessage miniMessage;
    private Economy economy;
    private ChatInputManager chatInputManager;
    private TaskRunner taskRunner;
    private PvPManager pvpManager;
    private ProxyMessagingManager proxyMessagingManager;
    private DebugLogger debugLogger;
    private ProxyType proxyType = ProxyType.NONE;
    private static boolean IS_FOLIA = false;
    public boolean updateAvailable = false;
    public String latestVersion = "";
    private static NamespacedKey actionKey;
    private Object heartbeatTask;

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();
        actionKey = new NamespacedKey(this, "gui-action");

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            IS_FOLIA = true;
            getLogger().info("Folia detected. Enabling Folia support.");
        } catch (ClassNotFoundException e) {
            IS_FOLIA = false;
            getLogger().info("Folia not detected. Using standard Bukkit scheduler.");
        }

        this.taskRunner = new TaskRunner(this);

        ConfigUpdater.update(this);

        this.configManager = new ConfigManager(this);
        this.debugLogger = new DebugLogger(this);
        this.messageManager = new MessageManager(this);
        this.guiConfigManager = new GuiConfigManager(this);
        this.aliasManager = new AliasManager(this);

        initializeProxy();

        if (!setupEconomy()) {
            getLogger().warning("Vault or an Economy plugin not found! The team bank feature will be disabled.");
        }
        setupPvpManager();

        this.storageManager = new StorageManager(this);
        if (!storageManager.init()) {
            getLogger().severe("Failed to initialize storage. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.chatInputManager = new ChatInputManager(this);
        this.teamManager = new TeamManager(this);

        registerListeners();
        registerCommands();
        registerChannels();
        startHeartbeat();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        }

        new VersionChecker(this).check();

        StartupMessage.send();
        WebhookUtil.sendStartupNotification(this);
    }

    public void initializeProxy() {
        String proxyConfig = configManager.getString("proxy_settings.type", "NONE").toUpperCase();
        try {
            this.proxyType = ProxyType.valueOf(proxyConfig);
        } catch (IllegalArgumentException e) {
            getLogger().severe("Invalid 'proxy_type' in config.yml! Defaulting to NONE.");
            this.proxyType = ProxyType.NONE;
        }

        this.proxyMessagingManager = new ProxyMessagingManager(this);

        if (proxyType != ProxyType.NONE) {
            getLogger().info("Proxy mode enabled: " + proxyType.name() + ". Cross-server features are active.");
        } else {
            getLogger().warning("Proxy mode is NONE. Cross-server features are disabled.");
        }
    }

    @Override
    public void onDisable() {
        cancelHeartbeat();
        if (teamManager != null) {
            teamManager.saveAllOnlineTeamEnderChests();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
    }

    public void reloadPluginConfigs() {
        configManager.reloadConfig();
        messageManager.reload();
        guiConfigManager.reload();
        aliasManager.reload();
        debugLogger.reload();
        initializeProxy();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupPvpManager() {
        if (Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {
            this.pvpManager = (PvPManager) Bukkit.getPluginManager().getPlugin("PvPManager");
            getLogger().info("Successfully hooked into PvPManager.");
        } else {
            this.pvpManager = null;
        }
    }

    private void registerCommands() {
        TeamCommand teamExecutor = new TeamCommand(this, this.teamChatListener);
        TeamMessageCommand teamMessageExecutor = new TeamMessageCommand(this);

        List<String> mainCommands = Arrays.asList("team", "guild", "clan", "party");
        for (String cmd : mainCommands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(teamExecutor);
                getCommand(cmd).setTabCompleter(teamExecutor);
            }
        }

        List<String> msgCommands = Arrays.asList("teammsg", "guildmsg", "clanmsg", "partymsg");
        for (String cmd : msgCommands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(teamMessageExecutor);
            }
        }

        aliasManager.registerAliases();
    }

    private void registerListeners() {
        this.teamChatListener = new TeamChatListener(this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvPListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerStatsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TeamEnderChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.teamChatListener, this);
        if (this.proxyMessagingManager != null) {
            Bukkit.getPluginManager().registerEvents(this.proxyMessagingManager, this);
        }
    }

    private void registerChannels() {
        if (proxyType == ProxyType.BUNGEECORD) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", proxyMessagingManager);
            getDebugLogger().log("Registered BungeeCord channels.");
        } else if (proxyType == ProxyType.VELOCITY) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:main");
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, ProxyMessagingManager.CUSTOM_DATA_CHANNEL, proxyMessagingManager);
            getDebugLogger().log("Registered Velocity channels.");
        }
    }

    private void startHeartbeat() {
        if (heartbeatTask != null) {
            cancelHeartbeat();
        }
        Runnable heartbeatRunnable = () -> {
            getStorageManager().getStorage().updateServerHeartbeat(getConfigManager().getServerIdentifier());
            getDebugLogger().log("Sent database heartbeat for server: " + getConfigManager().getServerIdentifier());
        };

        if (isFolia()) {
            this.heartbeatTask = getServer().getAsyncScheduler().runAtFixedRate(this, (task) -> heartbeatRunnable.run(), 1, 1, TimeUnit.MINUTES);
        } else {
            this.heartbeatTask = getServer().getScheduler().runTaskTimerAsynchronously(this, heartbeatRunnable, 20L * 60, 20L * 60);
        }
    }

    private void cancelHeartbeat() {
        if (heartbeatTask instanceof ScheduledFuture) {
            ((ScheduledFuture<?>) heartbeatTask).cancel(false);
        } else if (heartbeatTask instanceof BukkitTask) {
            ((BukkitTask) heartbeatTask).cancel();
        }
        heartbeatTask = null;
    }

    public static JustTeams getInstance() {
        return instance;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
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

    public GuiConfigManager getGuiConfigManager() {
        return guiConfigManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public AliasManager getAliasManager() {
        return aliasManager;
    }

    public TeamChatListener getTeamChatListener() {
        return teamChatListener;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public TaskRunner getTaskRunner() {
        return taskRunner;
    }

    public PvPManager getPvpManager() {
        return pvpManager;
    }

    public ProxyMessagingManager getProxyMessagingManager() {
        return proxyMessagingManager;
    }

    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }
}