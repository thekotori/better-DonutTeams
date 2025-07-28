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
import org.bukkit.OfflinePlayer;
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
import java.util.function.BiConsumer;
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
            case "disband" -> handleDisband(sender, args);
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
            case "setdescription", "setdesc" -> handleSetDescription(sender, args);
            case "promote" -> handlePromote(sender, args);
            case "demote" -> handleDemote(sender, args);
            default -> handleHelp(sender);
        }
        return true;
    }

    private void executeOnPlayer(CommandSender sender, BiConsumer<Player, String[]> action, String[] args) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        action.accept(player, args);
    }

    private void executeOnOfflinePlayer(CommandSender sender, String playerName, BiConsumer<Player, UUID> action) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "player_only");
            return;
        }
        plugin.getTaskRunner().runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (!target.hasPlayedBefore() && target.getPlayer() == null) {
                plugin.getTaskRunner().runOnEntity(player, () ->
                        messageManager.sendMessage(player, "player_not_found", Placeholder.unparsed("target", playerName))
                );
                return;
            }
            plugin.getTaskRunner().runOnEntity(player, () -> action.accept(player, target.getUniqueId()));
        });
    }

    private void handleCreate(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 3) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team create <name> <tag>");
                return;
            }
            String name = cmdArgs[1];
            String tag = cmdArgs[2];
            teamManager.createTeam(player, name, tag);
        }, args);
    }

    private void handleDisband(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            boolean confirmed = cmdArgs.length > 1 && cmdArgs[1].equalsIgnoreCase("confirm");
            teamManager.disbandTeam(player, confirmed);
        }, args);
    }

    private void handleInvite(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team invite <player>");
                return;
            }
            Player target = Bukkit.getPlayer(cmdArgs[1]);
            if (target == null) {
                messageManager.sendMessage(player, "player_not_found", Placeholder.unparsed("target", cmdArgs[1]));
                return;
            }
            teamManager.invitePlayer(player, target);
        }, args);
    }

    private void handleAccept(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team accept <teamName>");
                return;
            }
            teamManager.acceptInvite(player, cmdArgs[1]);
        }, args);
    }

    private void handleDeny(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team deny <teamName>");
                return;
            }
            teamManager.denyInvite(player, cmdArgs[1]);
        }, args);
    }

    private void handleLeave(CommandSender sender) {
        executeOnPlayer(sender, (player, cmdArgs) -> teamManager.leaveTeam(player), null);
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team kick <player>");
            return;
        }
        executeOnOfflinePlayer(sender, args[1], (player, targetUuid) -> teamManager.kickPlayer(player, targetUuid));
    }

    private void handleInfo(CommandSender sender, String... args) {
        if (args.length > 1) {
            plugin.getTaskRunner().runAsync(() -> {
                Team team = teamManager.getTeamByName(args[1]);
                plugin.getTaskRunner().run(() -> {
                    if (team == null) {
                        messageManager.sendMessage(sender, "team_not_found", Placeholder.unparsed("team", args[1]));
                        return;
                    }
                    displayTeamInfo(sender, team);
                });
            });
        } else {
            if (!(sender instanceof Player player)) {
                handleHelp(sender);
                return;
            }
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) {
                messageManager.sendMessage(player, "player_not_in_team");
                return;
            }
            displayTeamInfo(sender, team);
        }
    }

    private void displayTeamInfo(CommandSender sender, Team team) {
        String ownerName = Bukkit.getOfflinePlayer(team.getOwnerUuid()).getName();
        String safeOwnerName = ownerName != null ? ownerName : "Unknown";

        String coOwners = team.getCoOwners().stream()
                .map(co -> Bukkit.getOfflinePlayer(co.getPlayerUuid()).getName())
                .filter(name -> name != null)
                .collect(Collectors.joining(", "));

        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_header"), Placeholder.unparsed("team", team.getName()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_tag"), Placeholder.unparsed("tag", team.getTag()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_description"), Placeholder.unparsed("description", team.getDescription()));
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_owner"), Placeholder.unparsed("owner", safeOwnerName));

        if (!coOwners.isEmpty()) {
            messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_co_owners"), Placeholder.unparsed("co_owners", coOwners));
        }
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
            String safeMemberName = memberName != null ? memberName : "Unknown";
            messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_member_list"), Placeholder.unparsed("player", safeMemberName));
        }
        messageManager.sendRawMessage(sender, messageManager.getRawMessage("team_info_footer"));
    }


    private void handleChatToggle(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> teamChatListener.toggleTeamChat(player), null);
    }

    private void handleGui(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> {
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) {
                messageManager.sendMessage(player, "player_not_in_team");
                return;
            }
            new TeamGUI(plugin, team, player).open();
        }, null);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("donutteams.admin.reload")) {
            messageManager.sendMessage(sender, "no_permission");
            return;
        }
        plugin.getConfigManager().reloadConfig();
        messageManager.sendMessage(sender, "reload");
    }

    private void handleSetHome(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> teamManager.setTeamHome(player), null);
    }

    private void handleHome(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> teamManager.teleportToHome(player), null);
    }

    private void handleSetTag(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team settag <newTag>");
                return;
            }
            teamManager.setTeamTag(player, cmdArgs[1]);
        }, args);
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team transfer <player>");
            return;
        }
        executeOnOfflinePlayer(sender, args[1], (player, targetUuid) -> teamManager.transferOwnership(player, targetUuid));
    }

    private void handlePvpToggle(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> teamManager.togglePvp(player), null);
    }

    private void handleBank(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) {
                messageManager.sendMessage(player, "player_not_in_team");
                return;
            }

            if (cmdArgs.length == 1) {
                new BankGUI(plugin, player, team).open();
                return;
            }
            if (cmdArgs.length < 3) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount>");
                return;
            }

            try {
                double amount = Double.parseDouble(cmdArgs[2]);
                if (cmdArgs[1].equalsIgnoreCase("deposit")) {
                    teamManager.deposit(player, amount);
                } else if (cmdArgs[1].equalsIgnoreCase("withdraw")) {
                    teamManager.withdraw(player, amount);
                } else {
                    messageManager.sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount>");
                }
            } catch (NumberFormatException e) {
                messageManager.sendMessage(player, "bank_invalid_amount");
            }
        }, args);
    }

    private void handleLeaderboard(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> new LeaderboardCategoryGUI(plugin, player).open(), null);
    }

    private void handleEnderChest(CommandSender sender) {
        executeOnPlayer(sender, (player, args) -> teamManager.openEnderChest(player), null);
    }

    private void handleSetDescription(CommandSender sender, String[] args) {
        executeOnPlayer(sender, (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                messageManager.sendRawMessage(sender, "<gray>Usage: /team setdescription <description...>");
                return;
            }
            String description = String.join(" ", Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length));
            teamManager.setTeamDescription(player, description);
        }, args);
    }

    private void handlePromote(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team promote <player>");
            return;
        }
        executeOnOfflinePlayer(sender, args[1], (player, targetUuid) -> teamManager.promotePlayer(player, targetUuid));
    }

    private void handleDemote(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageManager.sendRawMessage(sender, "<gray>Usage: /team demote <player>");
            return;
        }
        executeOnOfflinePlayer(sender, args[1], (player, targetUuid) -> teamManager.demotePlayer(player, targetUuid));
    }

    private void handleHelp(CommandSender sender) {
        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        messageManager.sendRawMessage(sender, "<strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯</strikethrough> <gradient:" + mainColor + ":" + accentColor + "><bold>BetterDonutTeams Help</bold></gradient> <strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯");
        messageManager.sendRawMessage(sender, "<gray>/team create <name> <tag> <white>- Creates a team.");
        messageManager.sendRawMessage(sender, "<gray>/team disband [confirm] <white>- Disbands your team.");
        messageManager.sendRawMessage(sender, "<gray>/team invite <player> <white>- Invites a player.");
        messageManager.sendRawMessage(sender, "<gray>/team kick <player> <white>- Kicks a player.");
        messageManager.sendRawMessage(sender, "<gray>/team leave <white>- Leaves your team.");
        messageManager.sendRawMessage(sender, "<gray>/team promote <player> <white>- Promotes a member to co-owner.");
        messageManager.sendRawMessage(sender, "<gray>/team demote <player> <white>- Demotes a co-owner to member.");
        messageManager.sendRawMessage(sender, "<gray>/team info [team] <white>- Shows team info.");
        messageManager.sendRawMessage(sender, "<gray>/team sethome <white>- Sets the team home.");
        messageManager.sendRawMessage(sender, "<gray>/team home <white>- Teleports to the team home.");
        messageManager.sendRawMessage(sender, "<gray>/team settag <tag> <white>- Changes the team tag.");
        messageManager.sendRawMessage(sender, "<gray>/team setdesc <desc> <white>- Changes the team description.");
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
            List<String> subcommands = new ArrayList<>(Arrays.asList("create", "disband", "invite", "accept", "deny", "kick", "leave", "info", "gui", "chat", "sethome", "home", "settag", "transfer", "pvp", "bank", "top", "enderchest", "setdescription", "promote", "demote"));
            if (sender.hasPermission("donutteams.admin.reload")) {
                subcommands.add("reload");
            }
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("invite", "kick", "transfer", "promote", "demote").contains(subCommand)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}