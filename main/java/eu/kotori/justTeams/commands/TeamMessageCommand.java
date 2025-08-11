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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamMessageCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final MessageManager messageManager;
    private final MiniMessage miniMessage;

    public TeamMessageCommand(JustTeams plugin) {
        this.teamManager = plugin.getTeamManager();
        this.messageManager = plugin.getMessageManager();
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return true;
        }

        if (args.length == 0) {
            messageManager.sendRawMessage(player, "<gray>Usage: /" + label + " <message>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return true;
        }

        String message = String.join(" ", args);
        String format = messageManager.getRawMessage("team_chat_format");

        Component formattedMessage = miniMessage.deserialize(format,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("message", message)
        );

        team.getMembers().stream()
                .map(member -> member.getBukkitPlayer())
                .filter(onlinePlayer -> onlinePlayer != null)
                .forEach(onlinePlayer -> onlinePlayer.sendMessage(formattedMessage));

        return true;
    }
}