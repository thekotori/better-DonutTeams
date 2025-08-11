package eu.kotori.justTeams.commands;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.gui.BankGUI;
import eu.kotori.justTeams.gui.LeaderboardCategoryGUI;
import eu.kotori.justTeams.gui.NoTeamGUI;
import eu.kotori.justTeams.gui.TeamGUI;
import eu.kotori.justTeams.gui.admin.AdminGUI;
import eu.kotori.justTeams.listeners.TeamChatListener;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final JustTeams plugin;
    private final TeamChatListener teamChatListener;

    public TeamCommand(JustTeams plugin, TeamChatListener teamChatListener) {
        this.plugin = plugin;
        this.teamChatListener = teamChatListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            handleBaseCommand(sender);
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
            case "reload" -> handleReload(sender);
            case "admin" -> handleAdmin(sender, args);
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

    private void executeOnPlayer(CommandSender sender, String permission, BiConsumer<Player, String[]> action, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage(sender, "player_only");
            return;
        }
        if (!player.hasPermission(permission)) {
            plugin.getMessageManager().sendMessage(player, "no_permission");
            return;
        }
        action.accept(player, args);
    }

    private void executeOnOfflinePlayer(CommandSender sender, String permission, String playerName, BiConsumer<Player, UUID> action) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendMessage(sender, "player_only");
            return;
        }
        if (!player.hasPermission(permission)) {
            plugin.getMessageManager().sendMessage(player, "no_permission");
            return;
        }
        plugin.getTaskRunner().runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (!target.hasPlayedBefore() && target.getPlayer() == null) {
                plugin.getTaskRunner().runOnEntity(player, () ->
                        plugin.getMessageManager().sendMessage(player, "player_not_found", Placeholder.unparsed("target", playerName))
                );
                return;
            }
            plugin.getTaskRunner().runOnEntity(player, () -> action.accept(player, target.getUniqueId()));
        });
    }

    private void handleBaseCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            handleHelp(sender);
            return;
        }
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            new NoTeamGUI(plugin, player).open();
        } else {
            new TeamGUI(plugin, team, player).open();
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.admin", (player, cmdArgs) -> {
            if (cmdArgs.length == 1) {
                new AdminGUI(plugin, player).open();
                return;
            }
            if (cmdArgs.length >= 3 && cmdArgs[1].equalsIgnoreCase("disband")) {
                String teamName = cmdArgs[2];
                plugin.getTeamManager().adminDisbandTeam(player, teamName);
            } else {
                plugin.getMessageManager().sendRawMessage(player, "<gray>Usage: /team admin [disband <teamName>]</gray>");
            }
        }, args);
    }

    private void handleCreate(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.create", (player, cmdArgs) -> {
            if (cmdArgs.length < 3) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team create <name> <tag></gray>");
                return;
            }
            String name = cmdArgs[1];
            String tag = cmdArgs[2];
            plugin.getTeamManager().createTeam(player, name, tag);
        }, args);
    }

    private void handleDisband(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.disband", (player, cmdArgs) -> {
            plugin.getTeamManager().disbandTeam(player);
        }, null);
    }

    private void handleInvite(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.invite", (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team invite <player></gray>");
                return;
            }
            Player target = Bukkit.getPlayer(cmdArgs[1]);
            if (target == null) {
                plugin.getMessageManager().sendMessage(player, "player_not_found", Placeholder.unparsed("target", cmdArgs[1]));
                return;
            }
            plugin.getTeamManager().invitePlayer(player, target);
        }, args);
    }

    private void handleAccept(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.accept", (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team accept <teamName></gray>");
                return;
            }
            plugin.getTeamManager().acceptInvite(player, cmdArgs[1]);
        }, args);
    }

    private void handleDeny(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.deny", (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team deny <teamName></gray>");
                return;
            }
            plugin.getTeamManager().denyInvite(player, cmdArgs[1]);
        }, args);
    }

    private void handleLeave(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.leave", (player, cmdArgs) -> plugin.getTeamManager().leaveTeam(player), null);
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team kick <player></gray>");
            return;
        }
        executeOnOfflinePlayer(sender, "justteams.command.kick", args[1], (player, targetUuid) -> {
            plugin.getTeamManager().kickPlayer(player, targetUuid);
        });
    }

    private void handleInfo(CommandSender sender, String... args) {
        if (!sender.hasPermission("justteams.command.info")) {
            plugin.getMessageManager().sendMessage(sender, "no_permission");
            return;
        }
        if (args.length > 1) {
            plugin.getTaskRunner().runAsync(() -> {
                Team team = plugin.getTeamManager().getTeamByName(args[1]);
                plugin.getTaskRunner().run(() -> {
                    if (team == null) {
                        plugin.getMessageManager().sendMessage(sender, "team_not_found", Placeholder.unparsed("team", args[1]));
                        return;
                    }
                    displayTeamInfo(sender, team);
                });
            });
        } else {
            handleBaseCommand(sender);
        }
    }

    private void displayTeamInfo(CommandSender sender, Team team) {
        String ownerName = Bukkit.getOfflinePlayer(team.getOwnerUuid()).getName();
        String safeOwnerName = ownerName != null ? ownerName : "Unknown";

        String coOwners = team.getCoOwners().stream()
                .map(co -> Bukkit.getOfflinePlayer(co.getPlayerUuid()).getName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_header"), Placeholder.unparsed("team", team.getName()));
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_tag"), Placeholder.unparsed("tag", team.getTag()));
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_description"), Placeholder.unparsed("description", team.getDescription()));
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_owner"), Placeholder.unparsed("owner", safeOwnerName));

        if (!coOwners.isEmpty()) {
            plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_co_owners"), Placeholder.unparsed("co_owners", coOwners));
        }
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_bank"), Placeholder.unparsed("balance", String.format("%,.2f", team.getBalance())));

        double kdr = (team.getDeaths() == 0) ? team.getKills() : (double) team.getKills() / team.getDeaths();
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_stats"),
                Placeholder.unparsed("kills", String.valueOf(team.getKills())),
                Placeholder.unparsed("deaths", String.valueOf(team.getDeaths())),
                Placeholder.unparsed("kdr", String.format("%.2f", kdr))
        );

        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_members"),
                Placeholder.unparsed("member_count", String.valueOf(team.getMembers().size())),
                Placeholder.unparsed("max_members", String.valueOf(plugin.getConfigManager().getMaxTeamSize()))
        );

        for (TeamPlayer member : team.getMembers()) {
            String memberName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
            String safeMemberName = memberName != null ? memberName : "Unknown";
            plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_member_list"), Placeholder.unparsed("player", safeMemberName));
        }
        plugin.getMessageManager().sendRawMessage(sender, plugin.getMessageManager().getRawMessage("team_info_footer"));
    }

    private void handleChatToggle(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.chat", (player, args) -> teamChatListener.toggleTeamChat(player), null);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("justteams.command.reload")) {
            plugin.getMessageManager().sendMessage(sender, "no_permission");
            return;
        }
        plugin.reloadPluginConfigs();
        plugin.getMessageManager().sendMessage(sender, "reload");
        plugin.getMessageManager().sendMessage(sender, "reload_commands_notice");
    }

    private void handleSetHome(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.sethome", (player, args) -> plugin.getTeamManager().setTeamHome(player), null);
    }

    private void handleHome(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.home", (player, args) -> plugin.getTeamManager().teleportToHome(player), null);
    }

    private void handleSetTag(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.settag", (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team settag <newTag></gray>");
                return;
            }
            plugin.getTeamManager().setTeamTag(player, cmdArgs[1]);
        }, args);
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team transfer <player></gray>");
            return;
        }
        executeOnOfflinePlayer(sender, "justteams.command.transfer", args[1], (player, targetUuid) -> plugin.getTeamManager().transferOwnership(player, targetUuid));
    }

    private void handlePvpToggle(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.pvp", (player, args) -> plugin.getTeamManager().togglePvp(player), null);
    }

    private void handleBank(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.bank", (player, cmdArgs) -> {
            if (!plugin.getConfigManager().isBankEnabled()) {
                plugin.getMessageManager().sendMessage(player, "feature_disabled");
                return;
            }
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) {
                plugin.getMessageManager().sendMessage(player, "player_not_in_team");
                return;
            }

            if (cmdArgs.length == 1) {
                new BankGUI(plugin, player, team).open();
                return;
            }
            if (cmdArgs.length < 3) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount></gray>");
                return;
            }

            try {
                double amount = Double.parseDouble(cmdArgs[2]);
                if (cmdArgs[1].equalsIgnoreCase("deposit")) {
                    plugin.getTeamManager().deposit(player, amount);
                } else if (cmdArgs[1].equalsIgnoreCase("withdraw")) {
                    plugin.getTeamManager().withdraw(player, amount);
                } else {
                    plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team bank <deposit|withdraw> <amount></gray>");
                }
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(player, "bank_invalid_amount");
            }
        }, args);
    }

    private void handleLeaderboard(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.top", (player, args) -> new LeaderboardCategoryGUI(plugin, player).open(), null);
    }

    private void handleEnderChest(CommandSender sender) {
        executeOnPlayer(sender, "justteams.command.enderchest", (player, args) -> {
            if (!plugin.getConfigManager().isEnderChestEnabled()) {
                plugin.getMessageManager().sendMessage(player, "feature_disabled");
                return;
            }
            plugin.getTeamManager().openEnderChest(player);
        }, null);
    }

    private void handleSetDescription(CommandSender sender, String[] args) {
        executeOnPlayer(sender, "justteams.command.setdescription", (player, cmdArgs) -> {
            if (cmdArgs.length < 2) {
                plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team setdescription <description...></gray>");
                return;
            }
            String description = String.join(" ", Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length));
            plugin.getTeamManager().setTeamDescription(player, description);
        }, args);
    }

    private void handlePromote(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team promote <player></gray>");
            return;
        }
        executeOnOfflinePlayer(sender, "justteams.command.promote", args[1], (player, targetUuid) -> plugin.getTeamManager().promotePlayer(player, targetUuid));
    }

    private void handleDemote(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(sender, "<gray>Usage: /team demote <player></gray>");
            return;
        }
        executeOnOfflinePlayer(sender, "justteams.command.demote", args[1], (player, targetUuid) -> plugin.getTeamManager().demotePlayer(player, targetUuid));
    }

    private void handleHelp(CommandSender sender) {
        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        plugin.getMessageManager().sendRawMessage(sender, "<strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯</strikethrough> <gradient:" + mainColor + ":" + accentColor + "><bold>Betterjustteams Help</bold></gradient> <strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team <white>- Opens the team GUI.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team create <name> <tag> <white>- Creates a team.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team disband <white>- Disbands your team.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team invite <player> <white>- Invites a player.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team kick <player> <white>- Kicks a player.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team leave <white>- Leaves your team.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team promote <player> <white>- Promotes a member to co-owner.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team demote <player> <white>- Demotes a co-owner to member.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team info [team] <white>- Shows team info.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team sethome <white>- Sets the team home.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team home <white>- Teleports to the team home.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team settag <tag> <white>- Changes the team tag.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team setdesc <desc> <white>- Changes the team description.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team transfer <player> <white>- Transfers ownership.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team pvp <white>- Toggles team PvP.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team bank [deposit|withdraw] [amount] <white>- Manage team bank.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team enderchest <white>- Opens the team enderchest.");
        plugin.getMessageManager().sendRawMessage(sender, "<gray>/team top <white>- Shows leaderboards.");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList("create", "disband", "invite", "accept", "deny", "kick", "leave", "info", "chat", "sethome", "home", "settag", "transfer", "pvp", "bank", "top", "enderchest", "setdescription", "promote", "demote"));
            if (sender.hasPermission("justteams.command.admin")) {
                subcommands.add("admin");
            }
            if (sender.hasPermission("justteams.command.reload")) {
                subcommands.add("reload");
            }
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
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
            if (subCommand.equals("admin") && sender.hasPermission("justteams.command.admin")) {
                return List.of("disband").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("disband") && sender.hasPermission("justteams.command.admin")) {
                return plugin.getTeamManager().getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}