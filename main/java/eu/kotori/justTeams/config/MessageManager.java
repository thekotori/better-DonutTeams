package eu.kotori.justTeams.config;

import eu.kotori.justTeams.JustTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageManager {

    private final JustTeams plugin;
    private final MiniMessage miniMessage;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private String prefix;

    public MessageManager(JustTeams plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        reload();
    }

    public void reload() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Could not load messages.yml!");
            e.printStackTrace();
        }
        this.prefix = messagesConfig.getString("prefix", "<bold><gradient:#4C9DDE:#4C96D2>ᴛᴇᴀᴍs</gradient></bold> <dark_gray>| <gray>");
    }

    public void sendMessage(CommandSender target, String key, TagResolver... resolvers) {
        String messageString = messagesConfig.getString(key, "<red>Message key not found: " + key + "</red>");
        Component message = miniMessage.deserialize(prefix + messageString, resolvers);
        target.sendMessage(message);
    }

    public void sendRawMessage(CommandSender target, String message, TagResolver... resolvers) {
        Component component = miniMessage.deserialize(message, resolvers);
        target.sendMessage(component);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString(key, "Message key not found: " + key);
    }
}