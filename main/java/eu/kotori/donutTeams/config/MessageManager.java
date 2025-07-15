package eu.kotori.donutTeams.config;

import eu.kotori.donutTeams.DonutTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageManager {

    private final MiniMessage miniMessage;
    private final FileConfiguration messagesConfig;
    private final String prefix;

    public MessageManager(DonutTeams plugin) {
        this.miniMessage = plugin.getMiniMessage();
        this.messagesConfig = plugin.getMessageConfig().getCustomConfig();
        this.prefix = messagesConfig.getString("prefix", "<gradient:#ff8c9f:#ffc2cd><bold>TEAMS</bold></gradient> <dark_gray>| <gray>");
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