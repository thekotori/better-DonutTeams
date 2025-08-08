package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class AliasManager {

    private final DonutTeams plugin;
    private FileConfiguration commandsConfig;

    public AliasManager(DonutTeams plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File commandsFile = new File(plugin.getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            plugin.saveResource("commands.yml", false);
        }
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
    }

    public void reload() {
        loadConfig();
    }

    public void registerAliases() {
        try {
            CommandMap commandMap = Bukkit.getCommandMap();

            String primaryCommand = commandsConfig.getString("primary-command", "team").toLowerCase();
            List<String> allMainCommands = new ArrayList<>(Arrays.asList("team", "guild", "clan", "party"));
            registerAliasGroup(commandMap, allMainCommands, primaryCommand);

            String primaryMessageCommand = commandsConfig.getString("primary-message-command", "teammsg").toLowerCase();
            List<String> allMessageCommands = new ArrayList<>(Arrays.asList("teammsg", "guildmsg", "clanmsg", "partymsg"));
            registerAliasGroup(commandMap, allMessageCommands, primaryMessageCommand);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to dynamically register command aliases.", e);
        }
    }

    private void registerAliasGroup(CommandMap commandMap, List<String> allCommands, String primaryCommand) {
        PluginCommand mainPluginCommand = plugin.getCommand(primaryCommand);
        if (mainPluginCommand == null) {
            plugin.getLogger().severe("Primary command '" + primaryCommand + "' is not registered in plugin.yml! Aliases will not be set up correctly.");
            return;
        }

        List<String> newAliases = new ArrayList<>(mainPluginCommand.getAliases());

        for (String cmdName : allCommands) {
            if (!cmdName.equals(primaryCommand)) {
                Command aliasCmd = commandMap.getCommand(cmdName);
                if (aliasCmd instanceof PluginCommand) {
                    newAliases.add(aliasCmd.getName());
                    newAliases.addAll(aliasCmd.getAliases());
                    commandMap.getKnownCommands().remove(aliasCmd.getName());
                    aliasCmd.getAliases().forEach(alias -> commandMap.getKnownCommands().remove(alias));
                }
            }
        }
        mainPluginCommand.setAliases(newAliases);
    }
}