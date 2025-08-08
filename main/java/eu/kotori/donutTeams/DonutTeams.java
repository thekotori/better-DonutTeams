package eu.kotori.donutTeams;

import eu.kotori.donutTeams.commands.TeamCommand;
import eu.kotori.donutTeams.commands.TeamMessageCommand;
import eu.kotori.donutTeams.config.ConfigManager;
import eu.kotori.donutTeams.config.MessageManager;
import eu.kotori.donutTeams.gui.TeamGUIListener;
import eu.kotori.donutTeams.listeners.*;
import eu.kotori.donutTeams.storage.StorageManager;
import eu.kotori.donutTeams.team.TeamManager;
import eu.kotori.donutTeams.util.*;
import me.NoChance.PvPManager.PvPManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class DonutTeams extends JavaPlugin {

    private static DonutTeams instance;
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
    private static boolean IS_FOLIA = false;
    public boolean updateAvailable = false;
    public String latestVersion = "";

    @Override
    public void onEnable() {
        instance = this;
        this.miniMessage = MiniMessage.miniMessage();

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
        this.messageManager = new MessageManager(this);
        this.guiConfigManager = new GuiConfigManager(this);
        this.aliasManager = new AliasManager(this);

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

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        }

        new VersionChecker(this).check();

        StartupMessage.send();
        WebhookUtil.sendStartupNotification(this);
    }

    @Override
    public void onDisable() {
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
        for(String cmd : mainCommands) {
            if(getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(teamExecutor);
                getCommand(cmd).setTabCompleter(teamExecutor);
            }
        }

        List<String> msgCommands = Arrays.asList("teammsg", "guildmsg", "clanmsg", "partymsg");
        for(String cmd : msgCommands) {
            if(getCommand(cmd) != null) {
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
    }

    public static DonutTeams getInstance() {
        return instance;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
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
}