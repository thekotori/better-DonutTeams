package eu.kotori.justTeams.commands;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.config.MessageManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
public class TeamMessageCommand implements CommandExecutor, TabCompleter {
    private final TeamManager teamManager;
    private final MessageManager messageManager;
    private final MiniMessage miniMessage;
    private final ConcurrentHashMap<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> messageCounts = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 2000;
    private static final int MAX_MESSAGES_PER_MINUTE = 20;
    private static final int MAX_MESSAGE_LENGTH = 200;
    public TeamMessageCommand(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
        this.messageManager = plugin.getMessageManager();
        this.miniMessage = plugin.getMiniMessage();
        plugin.getTaskRunner().runTimer(() -> {
            messageCounts.clear();
        }, 20L * 60, 20L * 60);
    }
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return true;
        }
        if (args.length == 0) {
            messageManager.sendRawMessage(player, "<gray>Usage: /" + label + " <message>");
            return true;
        }
        if (!checkMessageSpam(player)) {
            messageManager.sendMessage(player, "message_spam_protection");
            return true;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return true;
        }
        String message = String.join(" ", args);
        if (message.length() > MAX_MESSAGE_LENGTH) {
            messageManager.sendMessage(player, "message_too_long");
            return true;
        }
        if (containsInappropriateContent(message)) {
            messageManager.sendMessage(player, "inappropriate_message");
            return true;
        }
        String format = messageManager.getRawMessage("team_chat_format");
        Component formattedMessage = miniMessage.deserialize(format,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("team_name", team.getName()),
                Placeholder.unparsed("message", message)
        );
        team.getMembers().stream()
                .map(member -> member.getBukkitPlayer())
                .filter(onlinePlayer -> onlinePlayer != null)
                .forEach(onlinePlayer -> onlinePlayer.sendMessage(formattedMessage));
        return true;
    }
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
    private boolean checkMessageSpam(Player player) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long lastMessage = messageCooldowns.get(playerId);
        if (lastMessage != null && currentTime - lastMessage < MESSAGE_COOLDOWN) {
            return false;
        }
        int count = messageCounts.getOrDefault(playerId, 0);
        if (count >= MAX_MESSAGES_PER_MINUTE) {
            return false;
        }
        messageCooldowns.put(playerId, currentTime);
        messageCounts.put(playerId, count + 1);
        return true;
    }
    private boolean containsInappropriateContent(String message) {
        String lowerMessage = message.toLowerCase();
        String[] inappropriate = {
            "admin", "mod", "staff", "owner", "server", "minecraft", "bukkit", "spigot",
            "hack", "cheat", "exploit", "bug", "glitch", "dupe", "duplicate"
        };
        for (String word : inappropriate) {
            if (lowerMessage.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
