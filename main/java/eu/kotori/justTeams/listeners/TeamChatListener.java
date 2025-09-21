package eu.kotori.justTeams.listeners;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.config.MessageManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public class TeamChatListener implements Listener {
    private final TeamManager teamManager;
    private final MessageManager messageManager;
    private final Set<UUID> teamChatEnabled = new HashSet<>();
    private final MiniMessage miniMessage;
    public TeamChatListener(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
        this.messageManager = plugin.getMessageManager();
        this.miniMessage = plugin.getMiniMessage();
    }
    public void toggleTeamChat(Player player) {
        UUID uuid = player.getUniqueId();
        if (teamChatEnabled.contains(uuid)) {
            teamChatEnabled.remove(uuid);
            messageManager.sendMessage(player, "team_chat_disabled");
        } else {
            if (teamManager.getPlayerTeam(uuid) == null) {
                messageManager.sendMessage(player, "player_not_in_team");
                return;
            }
            teamChatEnabled.add(uuid);
            messageManager.sendMessage(player, "team_chat_enabled");
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String messageContent = PlainTextComponentSerializer.plainText().serialize(event.message());
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            return;
        }
        boolean isCharacterBasedTeamChat = false;
        boolean isToggleTeamChat = teamChatEnabled.contains(player.getUniqueId());
        if (JustTeams.getInstance().getConfigManager().getBoolean("team_chat.character_enabled", true)) {
            String character = JustTeams.getInstance().getConfigManager().getString("team_chat.character", "#");
            boolean requireSpace = JustTeams.getInstance().getConfigManager().getBoolean("team_chat.require_space", false);
            if (requireSpace) {
                isCharacterBasedTeamChat = messageContent.startsWith(character + " ");
            } else {
                isCharacterBasedTeamChat = messageContent.startsWith(character);
            }
        }
        if (!isToggleTeamChat && !isCharacterBasedTeamChat) {
            return;
        }
        event.setCancelled(true);
        final String finalMessageContent;
        if (isCharacterBasedTeamChat) {
            String character = JustTeams.getInstance().getConfigManager().getString("team_chat.character", "#");
            boolean requireSpace = JustTeams.getInstance().getConfigManager().getBoolean("team_chat.require_space", false);
            if (requireSpace) {
                finalMessageContent = messageContent.substring(character.length() + 1);
            } else {
                finalMessageContent = messageContent.substring(character.length());
            }
        } else {
            finalMessageContent = messageContent;
        }
        if (finalMessageContent.toLowerCase().contains("password") || finalMessageContent.toLowerCase().contains("pass")) {
            messageManager.sendMessage(player, "team_chat_password_warning");
            return;
        }
        String format = messageManager.getRawMessage("team_chat_format");
        Component formattedMessage = miniMessage.deserialize(format,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("team_name", team.getName()),
                Placeholder.unparsed("message", finalMessageContent)
        );
        team.getMembers().stream()
                .map(member -> member.getBukkitPlayer())
                .filter(onlinePlayer -> onlinePlayer != null)
                .forEach(onlinePlayer -> onlinePlayer.sendMessage(formattedMessage));
        if (JustTeams.getInstance().getConfigManager().isCrossServerSyncEnabled()) {
            JustTeams.getInstance().getTaskRunner().runAsync(() -> {
                try {
                    JustTeams.getInstance().getStorageManager().getStorage().addCrossServerMessage(
                        team.getId(),
                        player.getUniqueId().toString(),
                        finalMessageContent,
                        JustTeams.getInstance().getConfigManager().getServerIdentifier()
                    );
                } catch (Exception e) {
                    JustTeams.getInstance().getLogger().warning("Failed to store cross-server message: " + e.getMessage());
                }
            });
        }
    }
}
