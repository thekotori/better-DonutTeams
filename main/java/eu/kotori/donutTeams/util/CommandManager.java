package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.commands.TeamCommand;
import eu.kotori.donutTeams.commands.TeamMessageCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

public class CommandManager {

    private final DonutTeams plugin;
    private File commandsFile;
    private FileConfiguration commandsConfig;

    public CommandManager(DonutTeams plugin) {
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

    public void registerCommands() {
        TeamCommand teamExecutor = new TeamCommand(plugin, plugin.getTeamChatListener());
        TeamMessageCommand teamMessageExecutor = new TeamMessageCommand(plugin);

        plugin.getCommand("team").setExecutor(teamExecutor);
        plugin.getCommand("team").setTabCompleter(teamExecutor);
        plugin.getCommand("teammsg").setExecutor(teamMessageExecutor);

        ConfigurationSection additionalCommands = commandsConfig.getConfigurationSection("additional-commands");
        if (additionalCommands == null) return;

        for (String key : additionalCommands.getKeys(false)) {
            ConfigurationSection commandGroup = additionalCommands.getConfigurationSection(key);
            if (commandGroup != null && commandGroup.getBoolean("enabled", false)) {
                ConfigurationSection mainCmdSection = commandGroup.getConfigurationSection("main-command");
                if (mainCmdSection != null) {
                    String name = mainCmdSection.getString("name");
                    List<String> aliases = mainCmdSection.getStringList("aliases");
                    if (name != null && !name.isEmpty()) {
                        registerCommand(name, aliases, teamExecutor, teamExecutor);
                    }
                }

                ConfigurationSection msgCmdSection = commandGroup.getConfigurationSection("message-command");
                if (msgCmdSection != null) {
                    String name = msgCmdSection.getString("name");
                    List<String> aliases = msgCmdSection.getStringList("aliases");
                    if (name != null && !name.isEmpty()) {
                        registerCommand(name, aliases, teamMessageExecutor, null);
                    }
                }
            }
        }
    }

    private void registerCommand(String name, List<String> aliases, CommandExecutor executor, TabCompleter completer) {
        try {
            final Field serverCommandMap = SimplePluginManager.class.getDeclaredField("commandMap");
            serverCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) serverCommandMap.get(Bukkit.getPluginManager());

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
}