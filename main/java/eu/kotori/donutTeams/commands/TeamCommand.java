package eu.kotori.donutTeams.commands;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.config.MessageManager;
import eu.kotori.donutTeams.gui.BankGUI;
import eu.kotori.donutTeams.gui.LeaderboardCategoryGUI;
import eu.kotori.donutTeams.gui.TeamGUI;
import eu.kotori.donutTeams.listeners.TeamChatListener;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamManager;
import eu.kotori.donutTeams.team.TeamPlayer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final DonutTeams plugin;
    private final TeamManager teamManager;
    private final MessageManager messageManager;
    private final TeamChatListener teamChatListener;

    public TeamCommand(DonutTeams plugin, TeamChatListener teamChatListener) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.messageManager = plugin.getMessageManager();
        this.teamChatListener = teamChatListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            handleInfo(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "disband" -> handleDisband(sender);
            case "invite" -> handleInvite(sender, args);
            case "accept" -> handleAccept(sender, args);
            case "deny" -> handleDeny(sender, args);
            case "leave" -> handleLeave(sender);
            case "kick" -> handleKick(sender, args);
            case "info" -> handleInfo(sender, args);
            case "chat", "c" -> handleChatToggle(sender);
            case "gui" -> handleGui(sender);
            case "reload" -> handleReload(sender);
            case "sethome" -> handleSetHome(sender);
            case "home" -> handleHome(sender);
            case "settag" -> handleSetTag(sender, args);
            case "transfer" -> handleTransfer(sender, args);
            case "pvp" -> handlePvpToggle(sender);
            case "bank" -> handleBank(sender, args);
            case "top", "leaderboard" -> handleLeaderboard(sender);
            case "enderchest", "ec" -> handleEnderChest(sender);
            case "setdescription" -> handleSetDescription(sender, args);
            default -> handleHelp(sender);
        }
        return true;
    }

    private void handleBank(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }

        if (args.length == 1) {
            new BankGUI(plugin, player, team).open();
            return;
        }
        if (args.length < 3) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount>");
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (args[1].equalsIgnoreCase("deposit")) {
                teamManager.deposit(player, amount);
            } else if (args[1].equalsIgnoreCase("withdraw")) {
                teamManager.withdraw(player, amount);
            } else {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount>");
            }
        } catch (NumberFormatException e) {
            messageManager.sendMessage(player, "bank_invalid_amount");
        }
    }

    private void handleLeaderboard(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        new LeaderboardCategoryGUI(plugin, player).open();
    }

    private void handleEnderChest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.openEnderChest(player);
    }

    private void handleSetDescription(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team setdescription <description...>");
            return;
        }
        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        teamManager.setTeamDescription(player, description);
    }

    private void handleSetHome(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.setTeamHome(player);
    }

    private void handleHome(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.teleportToHome(player);
    }

    private void handleSetTag(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team settag <newTag>");
            return;
        }
        teamManager.setTeamTag(player, args[1]);
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team transfer <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.sendMessage(player, "player_not_found", Placeholder.unparsed("target", args[1]));
            return;
        }
        teamManager.transferOwnership(player, target.getUniqueId());
    }

    private void handlePvpToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.togglePvp(player);
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        new TeamGUI(plugin, team, player).open();
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 3) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team create <name> <tag>");
            return;
        }
        String name = args[1];
        String tag = args[2];
        teamManager.createTeam(player, name, tag);
    }

    private void handleDisband(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.disbandTeam(player);
    }

    private void handleInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team invite <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.sendMessage(player, "player_not_found", Placeholder.unparsed("target", args[1]));
            return;
        }
        teamManager.invitePlayer(player, target);
    }

    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team accept <teamName>");
            return;
        }
        teamManager.acceptInvite(player, args[1]);
    }

    private void handleDeny(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team deny <teamName>");
            return;
        }
        teamManager.denyInvite(player, args[1]);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamManager.leaveTeam(player);
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team kick <player>");
            return;
        }
        UUID targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
        teamManager.kickPlayer(player, targetUuid);
    }

    private void handleInfo(CommandSender sender, String... args) {
        Team team;
        if (args.length > 1) {
            team = teamManager.getTeamByName(args[1]);
            if (team == null) {
                messageManager.sendMessage(sender, "team_not_found", Placeholder.unparsed("team", args[1]));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                handleHelp(sender);
                return;
            }
            team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) {
                messageManager.sendMessage(player, "player_not_in_team");
                return;
            }
        }

        String ownerName = Bukkit.getOfflinePlayer(team.getOwnerUuid()).getName();

        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_header"), Placeholder.unparsed("team", team.getName()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_tag"), Placeholder.unparsed("tag", team.getTag()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_description"), Placeholder.unparsed("description", team.getDescription()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_owner"), Placeholder.unparsed("owner", ownerName != null ? ownerName : "Unknown"));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_bank"), Placeholder.unparsed("balance", String.format("%,.2f", team.getBalance())));

        double kdr = (team.getDeaths() == 0) ? team.getKills() : (double) team.getKills() / team.getDeaths();
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_stats"),
                Placeholder.unparsed("kills", String.valueOf(team.getKills())),
                Placeholder.unparsed("deaths", String.valueOf(team.getDeaths())),
                Placeholder.unparsed("kdr", String.format("%.2f", kdr))
        );

        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_members"),
                Placeholder.unparsed("member_count", String.valueOf(team.getMembers().size())),
                Placeholder.unparsed("max_members", String.valueOf(plugin.getConfigManager().getMaxTeamSize()))
        );

        for (TeamPlayer member : team.getMembers()) {
            String memberName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
            messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_member_list"), Placeholder.unparsed("player", memberName != null ? memberName : "Unknown"));
        }
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_footer"));
    }

    private void handleChatToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        teamChatListener.toggleTeamChat(player);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("donutteams.admin.reload")) {
            messageManager.sendMessage(sender, "no_permission");
            return;
        }
        plugin.getConfigManager().reloadConfig();
        messageManager.sendMessage(sender, "reload");
    }

    private void handleHelp(CommandSender sender) {
        messageManager.sendRawMessage(sender, "<strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯</strikethrough> <gradient:#95FD95:#FFFFFF><bold>DonutTeams Help</bold></gradient> <strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯");
        messageManager.sendRawMessage(sender, "<gray>/team create <name> <tag> <white>- Creates a team.");
        messageManager.sendRawMessage(sender, "<gray>/team invite <player> <white>- Invites a player.");
        messageManager.sendRawMessage(sender, "<gray>/team kick <player> <white>- Kicks a player.");
        messageManager.sendRawMessage(sender, "<gray>/team leave <white>- Leaves your team.");
        messageManager.sendRawMessage(sender, "<gray>/team info [team] <white>- Shows team info.");
        messageManager.sendRawMessage(sender, "<gray>/team sethome <white>- Sets the team home.");
        messageManager.sendRawMessage(sender, "<gray>/team home <white>- Teleports to the team home.");
        messageManager.sendRawMessage(sender, "<gray>/team settag <tag> <white>- Changes the team tag.");
        messageManager.sendRawMessage(sender, "<gray>/team setdescription <desc> <white>- Changes the team description.");
        messageManager.sendRawMessage(sender, "<gray>/team transfer <player> <white>- Transfers ownership.");
        messageManager.sendRawMessage(sender, "<gray>/team pvp <white>- Toggles team PvP.");
        messageManager.sendRawMessage(sender, "<gray>/team bank [deposit|withdraw] [amount] <white>- Manage team bank.");
        messageManager.sendRawMessage(sender, "<gray>/team enderchest <white>- Opens the team enderchest.");
        messageManager.sendRawMessage(sender, "<gray>/team top <white>- Shows leaderboards.");
        messageManager.sendRawMessage(sender, "<gray>/team gui <white>- Opens the team GUI.");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("create", "disband", "invite", "accept", "deny", "kick", "leave", "info", "gui", "chat", "sethome", "home", "settag", "transfer", "pvp", "bank", "top", "enderchest", "setdescription"));
            if (sender.hasPermission("donutteams.admin.reload")) {
                subcommands.add("reload");
            }
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (Arrays.asList("invite", "kick", "transfer").contains(args[0].toLowerCase())) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}