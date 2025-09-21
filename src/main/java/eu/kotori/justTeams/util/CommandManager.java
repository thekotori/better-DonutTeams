package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.commands.TeamMessageCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
public class CommandManager {
    private final JustTeams plugin;
    private File commandsFile;
    private FileConfiguration commandsConfig;
    public CommandManager(JustTeams plugin) {
        this.plugin = plugin;
        createCommandsConfig();
    }
    private void createCommandsConfig() {
        commandsFile = new File(plugin.getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            commandsFile.getParentFile().mkdirs();
            plugin.saveResource("commands.yml", false);
        }
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
    }
    public void reload() {
        createCommandsConfig();
    }
    public void registerCommands() {
        try {
            String primaryCommand = getPrimaryCommand();
            List<String> aliases = getCommandAliases();
            registerCommand(primaryCommand, aliases, new TeamCommand(plugin), new TeamCommand(plugin));
            registerCommand("teammsg", List.of("tm", "tmsg"), new TeamMessageCommand(plugin), null);
            plugin.getLogger().info("CommandManager: All commands registered successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register commands.", e);
        }
    }
    private void registerCommand(String name, List<String> aliases, CommandExecutor executor, TabCompleter completer) {
        try {
            final Field serverCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            serverCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) serverCommandMap.get(Bukkit.getServer());
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand pluginCommand = constructor.newInstance(name, plugin);
            pluginCommand.setAliases(aliases);
            pluginCommand.setExecutor(executor);
            if (completer != null) {
                pluginCommand.setTabCompleter(completer);
            }
            commandMap.register(plugin.getName(), pluginCommand);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to dynamically register command: " + name, e);
        }
    }
    public String getPrimaryCommand() {
        return commandsConfig.getString("primary-command", "team");
    }
    public List<String> getCommandAliases() {
        List<String> aliases = commandsConfig.getStringList("aliases");
        if (aliases.isEmpty()) {
            aliases = List.of("guild", "clan", "party");
        }
        return aliases;
    }
    public boolean isCommandEnabled(String commandName) {
        return commandsConfig.getBoolean("commands." + commandName + ".enabled", true);
    }
    public String getCommandPermission(String commandName) {
        return commandsConfig.getString("commands." + commandName + ".permission", "justteams." + commandName);
    }
    public String getCommandDescription(String commandName) {
        return commandsConfig.getString("commands." + commandName + ".description", "No description available");
    }
    public String getCommandUsage(String commandName) {
        return commandsConfig.getString("commands." + commandName + ".usage", "/" + commandName);
    }
    public List<String> getCommandAliases(String commandName) {
        return commandsConfig.getStringList("commands." + commandName + ".aliases");
    }
    public boolean isTeamCreateEnabled() {
        return isCommandEnabled("create");
    }
    public boolean isTeamDisbandEnabled() {
        return isCommandEnabled("disband");
    }
    public boolean isTeamInviteEnabled() {
        return isCommandEnabled("invite");
    }
    public boolean isTeamKickEnabled() {
        return isCommandEnabled("kick");
    }
    public boolean isTeamLeaveEnabled() {
        return isCommandEnabled("leave");
    }
    public boolean isTeamPromoteEnabled() {
        return isCommandEnabled("promote");
    }
    public boolean isTeamDemoteEnabled() {
        return isCommandEnabled("demote");
    }
    public boolean isTeamInfoEnabled() {
        return isCommandEnabled("info");
    }
    public boolean isTeamSethomeEnabled() {
        return isCommandEnabled("sethome");
    }
    public boolean isTeamHomeEnabled() {
        return isCommandEnabled("home");
    }
    public boolean isTeamSettagEnabled() {
        return isCommandEnabled("settag");
    }
    public boolean isTeamSetdescriptionEnabled() {
        return isCommandEnabled("setdescription");
    }
    public boolean isTeamTransferEnabled() {
        return isCommandEnabled("transfer");
    }
    public boolean isTeamPvpEnabled() {
        return isCommandEnabled("pvp");
    }
    public boolean isTeamBankEnabled() {
        return isCommandEnabled("bank");
    }
    public boolean isTeamEnderchestEnabled() {
        return isCommandEnabled("enderchest");
    }
    public boolean isTeamTopEnabled() {
        return isCommandEnabled("top");
    }
    public boolean isTeamJoinEnabled() {
        return isCommandEnabled("join");
    }
    public boolean isTeamUnjoinEnabled() {
        return isCommandEnabled("unjoin");
    }
    public boolean isTeamPublicEnabled() {
        return isCommandEnabled("public");
    }
    public boolean isTeamRequestsEnabled() {
        return isCommandEnabled("requests");
    }
    public boolean isTeamSetwarpEnabled() {
        return isCommandEnabled("setwarp");
    }
    public boolean isTeamDelwarpEnabled() {
        return isCommandEnabled("delwarp");
    }
    public boolean isTeamWarpEnabled() {
        return isCommandEnabled("warp");
    }
    public boolean isTeamWarpsEnabled() {
        return isCommandEnabled("warps");
    }
    public boolean isTeamAdminEnabled() {
        return isCommandEnabled("admin");
    }
    public int getCommandCooldown(String commandName) {
        return commandsConfig.getInt("commands." + commandName + ".cooldown", 0);
    }
    public boolean isCommandCooldownEnabled(String commandName) {
        return getCommandCooldown(commandName) > 0;
    }
    public int getMaxCommandUses(String commandName) {
        return commandsConfig.getInt("commands." + commandName + ".max_uses", -1);
    }
    public boolean isCommandUsageLimited(String commandName) {
        return getMaxCommandUses(commandName) > 0;
    }
    public List<String> getCommandCategories() {
        return commandsConfig.getStringList("categories");
    }
    public List<String> getCommandsInCategory(String category) {
        return commandsConfig.getStringList("categories." + category + ".commands");
    }
    public String getCategoryDescription(String category) {
        return commandsConfig.getString("categories." + category + ".description", "No description available");
    }
    public String getCategoryPermission(String category) {
        return commandsConfig.getString("categories." + category + ".permission", "justteams.category." + category);
    }
    public String getString(String path, String defaultValue) {
        return commandsConfig.getString(path, defaultValue);
    }
    public int getInt(String path, int defaultValue) {
        return commandsConfig.getInt(path, defaultValue);
    }
    public boolean getBoolean(String path, boolean defaultValue) {
        return commandsConfig.getBoolean(path, defaultValue);
    }
    public List<String> getStringList(String path) {
        return commandsConfig.getStringList(path);
    }
    public ConfigurationSection getConfigurationSection(String path) {
        return commandsConfig.getConfigurationSection(path);
    }
    public boolean hasCommand(String commandName) {
        return commandsConfig.contains("commands." + commandName);
    }
    public boolean hasCategory(String categoryName) {
        return commandsConfig.contains("categories." + categoryName);
    }
    public java.util.Set<String> getCommandNames() {
        java.util.Set<String> commands = new java.util.HashSet<>();
        ConfigurationSection commandsSection = commandsConfig.getConfigurationSection("commands");
        if (commandsSection != null) {
            commands.addAll(commandsSection.getKeys(false));
        }
        return commands;
    }
    public java.util.Set<String> getConfigurationKeys() {
        return commandsConfig.getKeys(true);
    }
    public String getCommandHelp(String commandName) {
        if (!hasCommand(commandName)) {
            return "Command not found: " + commandName;
        }
        StringBuilder help = new StringBuilder();
        help.append("§6=== ").append(commandName.toUpperCase()).append(" ===\n");
        help.append("§7Description: §f").append(getCommandDescription(commandName)).append("\n");
        help.append("§7Usage: §f").append(getCommandUsage(commandName)).append("\n");
        help.append("§7Permission: §f").append(getCommandPermission(commandName)).append("\n");
        if (isCommandCooldownEnabled(commandName)) {
            help.append("§7Cooldown: §f").append(getCommandCooldown(commandName)).append(" seconds\n");
        }
        if (isCommandUsageLimited(commandName)) {
            help.append("§7Max Uses: §f").append(getMaxCommandUses(commandName)).append("\n");
        }
        List<String> aliases = getCommandAliases(commandName);
        if (!aliases.isEmpty()) {
            help.append("§7Aliases: §f").append(String.join(", ", aliases));
        }
        return help.toString();
    }
}
