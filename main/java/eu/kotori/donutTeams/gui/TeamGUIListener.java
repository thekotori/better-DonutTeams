package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.gui.admin.AdminGUI;
import eu.kotori.donutTeams.gui.admin.AdminTeamListGUI;
import eu.kotori.donutTeams.gui.admin.AdminTeamManageGUI;
import eu.kotori.donutTeams.gui.sub.TeamSettingsGUI;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamManager;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.team.TeamRole;
import eu.kotori.donutTeams.util.EffectsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamGUIListener implements Listener {

    private final DonutTeams plugin;
    private final TeamManager teamManager;

    public TeamGUIListener(DonutTeams plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        boolean isOurGui = holder instanceof IRefreshableGUI || holder instanceof NoTeamGUI || holder instanceof ConfirmGUI ||
                holder instanceof AdminGUI || holder instanceof AdminTeamListGUI || holder instanceof AdminTeamManageGUI ||
                holder instanceof TeamSettingsGUI || holder instanceof LeaderboardCategoryGUI || holder instanceof LeaderboardViewGUI;

        if (!isOurGui) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (holder instanceof TeamGUI gui) onTeamGUIClick(player, gui, clickedItem);
        else if (holder instanceof MemberEditGUI gui) onMemberEditGUIClick(player, gui, clickedItem);
        else if (holder instanceof BankGUI gui) onBankGUIClick(player, gui, clickedItem);
        else if (holder instanceof TeamSettingsGUI gui) onTeamSettingsGUIClick(player, gui, clickedItem);
        else if (holder instanceof LeaderboardCategoryGUI) onLeaderboardCategoryGUIClick(player, clickedItem);
        else if (holder instanceof LeaderboardViewGUI) onLeaderboardViewGUIClick(player, clickedItem);
        else if (holder instanceof NoTeamGUI) onNoTeamGUIClick(player, clickedItem);
        else if (holder instanceof AdminGUI) onAdminGUIClick(player, clickedItem);
        else if (holder instanceof AdminTeamListGUI gui) onAdminTeamListGUIClick(player, gui, clickedItem);
        else if (holder instanceof AdminTeamManageGUI gui) onAdminTeamManageGUIClick(player, gui, clickedItem);
        else if (holder instanceof ConfirmGUI gui) onConfirmGUIClick(gui, clickedItem);
    }

    private void onTeamGUIClick(Player player, TeamGUI gui, ItemStack clicked) {
        Team team = gui.getTeam();
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                UUID targetUuid = skullMeta.getPlayerProfile().getId();
                TeamPlayer target = team.getMember(targetUuid);
                if (target == null) return;

                TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                if (viewerMember == null) return;

                boolean canEdit = (viewerMember.getRole() == TeamRole.OWNER && target.getRole() != TeamRole.OWNER) ||
                        (viewerMember.getRole() == TeamRole.CO_OWNER && target.getRole() == TeamRole.MEMBER);

                if (canEdit) {
                    new MemberEditGUI(plugin, team, player, targetUuid).open();
                }
            }
            return;
        }

        switch (clicked.getType()) {
            case TNT -> teamManager.disbandTeam(player);
            case BARRIER -> teamManager.leaveTeam(player);
            case ENDER_PEARL -> teamManager.teleportToHome(player);
            case GOLD_NUGGET -> new BankGUI(plugin, player, team).open();
            case ENDER_CHEST -> teamManager.openEnderChest(player);
            case HOPPER -> gui.cycleSort();
            case COMPARATOR -> {
                if(team.hasElevatedPermissions(player.getUniqueId())) {
                    new TeamSettingsGUI(plugin, player, team).open();
                } else {
                    plugin.getMessageManager().sendMessage(player, "gui_action_locked");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                }
            }
            case IRON_SWORD -> {
                teamManager.togglePvp(player);
                gui.initializeItems();
            }
            default -> {}
        }
    }

    private void onConfirmGUIClick(ConfirmGUI gui, ItemStack clicked) {
        if (clicked.getType() == Material.GREEN_WOOL) {
            gui.handleConfirm();
        } else if (clicked.getType() == Material.RED_WOOL) {
            gui.handleCancel();
        }
    }

    private void onNoTeamGUIClick(Player player, ItemStack clicked) {
        switch (clicked.getType()) {
            case WRITABLE_BOOK -> {
                player.closeInventory();
                plugin.getMessageManager().sendRawMessage(player, "<gray>Please type your desired team name in chat, or type 'cancel' to abort.</gray>");
                plugin.getChatInputManager().awaitInput(player, new NoTeamGUI(plugin, player), teamName -> {
                    if (teamName.equalsIgnoreCase("cancel")) {
                        plugin.getMessageManager().sendRawMessage(player, "<red>Team creation cancelled.</red>");
                        return;
                    }
                    String validationError = teamManager.validateTeamName(teamName);
                    if (validationError != null) {
                        plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prefix") + validationError);
                        return;
                    }
                    plugin.getMessageManager().sendRawMessage(player, "<gray>Please type your desired team tag in chat, or type 'cancel' to abort.</gray>");
                    plugin.getChatInputManager().awaitInput(player, new NoTeamGUI(plugin, player), teamTag -> {
                        if (teamTag.equalsIgnoreCase("cancel")) {
                            plugin.getMessageManager().sendRawMessage(player, "<red>Team creation cancelled.</red>");
                            return;
                        }
                        teamManager.createTeam(player, teamName, teamTag);
                    });
                });
            }
            case EMERALD -> new LeaderboardCategoryGUI(plugin, player).open();
            default -> {}
        }
    }

    private void onMemberEditGUIClick(Player player, MemberEditGUI gui, ItemStack clicked) {
        TeamPlayer targetMember = gui.getTargetMember();
        if (targetMember == null) {
            player.closeInventory();
            return;
        }

        switch (clicked.getType()) {
            case LIME_DYE -> teamManager.promotePlayer(player, gui.getTargetUuid());
            case GRAY_DYE -> teamManager.demotePlayer(player, gui.getTargetUuid());
            case RED_WOOL -> teamManager.kickPlayer(player, gui.getTargetUuid());
            case BEACON -> teamManager.transferOwnership(player, gui.getTargetUuid());
            case ARROW -> new TeamGUI(plugin, gui.getTeam(), player).open();
            case GOLD_INGOT, ENDER_CHEST, GRASS_BLOCK, ENDER_PEARL -> {
                boolean canWithdraw = targetMember.canWithdraw();
                boolean canUseEC = targetMember.canUseEnderChest();
                boolean canSetHome = targetMember.canSetHome();
                boolean canUseHome = targetMember.canUseHome();

                if (clicked.getType() == Material.GOLD_INGOT) canWithdraw = !canWithdraw;
                if (clicked.getType() == Material.ENDER_CHEST) canUseEC = !canUseEC;
                if (clicked.getType() == Material.GRASS_BLOCK) canSetHome = !canSetHome;
                if (clicked.getType() == Material.ENDER_PEARL) canUseHome = !canUseHome;

                teamManager.updateMemberPermissions(player, targetMember.getPlayerUuid(), canWithdraw, canUseEC, canSetHome, canUseHome);
            }
            default -> { return; }
        }
        gui.initializeItems();
    }

    private void onBankGUIClick(Player player, BankGUI gui, ItemStack clicked) {
        switch (clicked.getType()) {
            case ARROW -> {
                new TeamGUI(plugin, gui.getTeam(), player).open();
                return;
            }
            case BARRIER -> {
                plugin.getMessageManager().sendMessage(player, "gui_action_locked");
                EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                return;
            }
            case GREEN_WOOL, RED_WOOL -> {
                player.closeInventory();
                boolean isDeposit = clicked.getType() == Material.GREEN_WOOL;
                String action = isDeposit ? "deposit" : "withdraw";

                plugin.getMessageManager().sendRawMessage(player, "<gray>Please type the amount to " + action + " in chat, or type 'cancel' to abort.</gray>");

                plugin.getChatInputManager().awaitInput(player, gui, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        plugin.getMessageManager().sendRawMessage(player, "<red>Action cancelled.</red>");
                        gui.open();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(input);
                        if (isDeposit) {
                            teamManager.deposit(player, amount);
                        } else {
                            teamManager.withdraw(player, amount);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getMessageManager().sendMessage(player, "bank_invalid_amount");
                    }
                    Bukkit.getScheduler().runTask(plugin, gui::open);
                });
            }
            default -> {}
        }
    }

    private void onTeamSettingsGUIClick(Player player, TeamSettingsGUI gui, ItemStack clicked) {
        switch (clicked.getType()) {
            case ARROW -> {
                new TeamGUI(plugin, gui.getTeam(), player).open();
                return;
            }
            case NAME_TAG, OAK_SIGN -> {
                player.closeInventory();
                boolean isTag = clicked.getType() == Material.NAME_TAG;
                String action = isTag ? "tag" : "description";

                plugin.getMessageManager().sendRawMessage(player, "<gray>Please type the new team " + action + " in chat, or type 'cancel' to abort.</gray>");

                plugin.getChatInputManager().awaitInput(player, gui, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        plugin.getMessageManager().sendRawMessage(player, "<red>Action cancelled.</red>");
                        gui.open();
                        return;
                    }
                    if (isTag) {
                        teamManager.setTeamTag(player, input);
                    } else {
                        teamManager.setTeamDescription(player, input);
                    }
                    Bukkit.getScheduler().runTask(plugin, gui::open);
                });
            }
            default -> {}
        }
    }

    private void onLeaderboardCategoryGUIClick(Player player, ItemStack clicked) {
        LeaderboardViewGUI.LeaderboardType type;
        String title;

        switch (clicked.getType()) {
            case DIAMOND_SWORD -> {
                type = LeaderboardViewGUI.LeaderboardType.KILLS;
                title = "ᴛᴏᴘ 10 ᴛᴇᴀᴍs ʙʏ ᴋɪʟʟs";
            }
            case GOLD_INGOT -> {
                type = LeaderboardViewGUI.LeaderboardType.BALANCE;
                title = "ᴛᴏᴘ 10 ᴛᴇᴀᴍs ʙʏ ʙᴀʟᴀɴᴄᴇ";
            }
            case PLAYER_HEAD -> {
                type = LeaderboardViewGUI.LeaderboardType.MEMBERS;
                title = "ᴛᴏᴘ 10 ᴛᴇᴀᴍs ʙʏ ᴍᴇᴍʙᴇʀs";
            }
            default -> { return; }
        }

        plugin.getTaskRunner().runAsync(() -> {
            Map<Integer, Team> topTeams;
            switch(type) {
                case KILLS -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByKills(10);
                case BALANCE -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByBalance(10);
                case MEMBERS -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByMembers(10);
                default -> topTeams = Map.of();
            }
            Map<Integer, Team> finalTopTeams = topTeams;
            plugin.getTaskRunner().runOnEntity(player, () -> new LeaderboardViewGUI(plugin, player, title, finalTopTeams, type).open());
        });
    }

    private void onLeaderboardViewGUIClick(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new LeaderboardCategoryGUI(plugin, player).open();
        }
    }

    private void onAdminGUIClick(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ANVIL) {
            plugin.getTaskRunner().runAsync(() -> {
                List<Team> allTeams = teamManager.getAllTeams();
                plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamListGUI(plugin, player, allTeams, 0).open());
            });
        }
    }

    private void onAdminTeamListGUIClick(Player player, AdminTeamListGUI gui, ItemStack clicked) {
        switch (clicked.getType()) {
            case ARROW -> {
                Component displayName = clicked.getItemMeta().displayName();
                if (displayName == null) return;
                String name = PlainTextComponentSerializer.plainText().serialize(displayName);
                if (name.contains("Next")) {
                    new AdminTeamListGUI(plugin, player, gui.getAllTeams(), gui.getPage() + 1).open();
                } else if (name.contains("Previous")) {
                    new AdminTeamListGUI(plugin, player, gui.getAllTeams(), gui.getPage() - 1).open();
                }
            }
            case BARRIER -> new AdminGUI(plugin, player).open();
            case PLAYER_HEAD -> {
                Component displayName = clicked.getItemMeta().displayName();
                if (displayName == null) return;
                String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
                plugin.getTaskRunner().runAsync(() -> {
                    Team targetTeam = teamManager.getTeamByName(plainName);
                    if (targetTeam != null) {
                        plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamManageGUI(plugin, player, targetTeam).open());
                    }
                });
            }
            default -> {}
        }
    }

    private void onAdminTeamManageGUIClick(Player player, AdminTeamManageGUI gui, ItemStack clicked) {
        switch (clicked.getType()) {
            case ARROW -> plugin.getTaskRunner().runAsync(() -> {
                List<Team> allTeams = teamManager.getAllTeams();
                plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamListGUI(plugin, player, allTeams, 0).open());
            });
            case TNT -> new ConfirmGUI(plugin, player, "Disband " + gui.getTargetTeam().getName() + "?", confirmed -> {
                if (confirmed) {
                    teamManager.adminDisbandTeam(player, gui.getTargetTeam().getName());
                } else {
                    gui.open();
                }
            }).open();
            default -> {}
        }
    }
}