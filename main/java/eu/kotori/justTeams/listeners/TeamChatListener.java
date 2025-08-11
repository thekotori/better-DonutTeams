package eu.kotori.justTeams.listeners;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.config.MessageManager;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
        if (!teamChatEnabled.contains(player.getUniqueId())) {
            return;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            teamChatEnabled.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        String format = messageManager.getRawMessage("team_chat_format");
        Component formattedMessage = miniMessage.deserialize(format,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("message", event.message())
        );

        team.getMembers().stream()
                .map(member -> member.getBukkitPlayer())
                .filter(onlinePlayer -> onlinePlayer != null)
                .forEach(onlinePlayer -> onlinePlayer.sendMessage(formattedMessage));
    }
}