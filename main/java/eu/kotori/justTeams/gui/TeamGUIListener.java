package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.gui.admin.AdminGUI;
import eu.kotori.justTeams.gui.admin.AdminTeamListGUI;
import eu.kotori.justTeams.gui.admin.AdminTeamManageGUI;
import eu.kotori.justTeams.gui.sub.TeamSettingsGUI;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.util.EffectsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamGUIListener implements Listener {

    private final JustTeams plugin;
    private final TeamManager teamManager;
    private final NamespacedKey actionKey;

    public TeamGUIListener(JustTeams plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.actionKey = JustTeams.getActionKey();
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

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (holder instanceof TeamGUI gui) onTeamGUIClick(player, gui, clickedItem, pdc);
        else if (holder instanceof MemberEditGUI gui) onMemberEditGUIClick(player, gui, pdc);
        else if (holder instanceof BankGUI gui) onBankGUIClick(player, gui, pdc);
        else if (holder instanceof TeamSettingsGUI gui) onTeamSettingsGUIClick(player, gui, pdc);
        else if (holder instanceof LeaderboardCategoryGUI) onLeaderboardCategoryGUIClick(player, pdc);
        else if (holder instanceof LeaderboardViewGUI) onLeaderboardViewGUIClick(player, pdc);
        else if (holder instanceof NoTeamGUI) onNoTeamGUIClick(player, pdc);
        else if (holder instanceof AdminGUI) onAdminGUIClick(player, pdc);
        else if (holder instanceof AdminTeamListGUI gui) onAdminTeamListGUIClick(player, gui, clickedItem, pdc);
        else if (holder instanceof AdminTeamManageGUI gui) onAdminTeamManageGUIClick(player, gui, pdc);
        else if (holder instanceof ConfirmGUI gui) onConfirmGUIClick(gui, pdc);
    }

    private void onTeamGUIClick(Player player, TeamGUI gui, ItemStack clickedItem, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        Team team = gui.getTeam();

        switch (action) {
            case "player-head" -> {
                if (clickedItem.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                    UUID targetUuid = skullMeta.getPlayerProfile().getId();
                    TeamPlayer target = team.getMember(targetUuid);
                    if (target == null) return;
                    TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                    if (viewerMember == null) return;
                    boolean canEdit = (viewerMember.getRole() == TeamRole.OWNER && target.getRole() != TeamRole.OWNER) || (viewerMember.getRole() == TeamRole.CO_OWNER && target.getRole() == TeamRole.MEMBER);
                    if (canEdit) {
                        new MemberEditGUI(plugin, team, player, targetUuid).open();
                    }
                }
            }
            case "bank" -> new BankGUI(plugin, player, team).open();
            case "home" -> teamManager.teleportToHome(player);
            case "ender-chest" -> teamManager.openEnderChest(player);
            case "sort" -> gui.cycleSort();
            case "pvp-toggle" -> {
                teamManager.togglePvp(player);
                gui.initializeItems();
            }
            case "settings" -> new TeamSettingsGUI(plugin, player, team).open();
            case "disband-button" -> teamManager.disbandTeam(player);
            case "leave-button" -> teamManager.leaveTeam(player);
            case "settings-locked", "bank-locked", "ender-chest-locked" -> {
                plugin.getMessageManager().sendMessage(player, "gui_action_locked");
                EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            }
        }
    }

    private void onMemberEditGUIClick(Player player, MemberEditGUI gui, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        TeamPlayer targetMember = gui.getTargetMember();
        if (targetMember == null) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "promote-button" -> teamManager.promotePlayer(player, gui.getTargetUuid());
            case "demote-button" -> teamManager.demotePlayer(player, gui.getTargetUuid());
            case "kick-button" -> teamManager.kickPlayer(player, gui.getTargetUuid());
            case "transfer-button" -> teamManager.transferOwnership(player, gui.getTargetUuid());
            case "back-button" -> new TeamGUI(plugin, gui.getTeam(), player).open();
            case "withdraw-permission", "enderchest-permission", "sethome-permission", "usehome-permission" -> {
                boolean canWithdraw = targetMember.canWithdraw();
                boolean canUseEC = targetMember.canUseEnderChest();
                boolean canSetHome = targetMember.canSetHome();
                boolean canUseHome = targetMember.canUseHome();
                switch (action) {
                    case "withdraw-permission" -> canWithdraw = !canWithdraw;
                    case "enderchest-permission" -> canUseEC = !canUseEC;
                    case "sethome-permission" -> canSetHome = !canSetHome;
                    case "usehome-permission" -> canUseHome = !canUseHome;
                }
                teamManager.updateMemberPermissions(player, targetMember.getPlayerUuid(), canWithdraw, canUseEC, canSetHome, canUseHome);
            }
            default -> { return; }
        }
        gui.initializeItems();
    }

    private void onBankGUIClick(Player player, BankGUI gui, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);

        switch (action) {
            case "back-button" -> {
                new TeamGUI(plugin, gui.getTeam(), player).open();
            }
            case "withdraw-locked" -> {
                plugin.getMessageManager().sendMessage(player, "gui_action_locked");
                EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            }
            case "deposit", "withdraw" -> {
                player.closeInventory();
                boolean isDeposit = action.equals("deposit");
                String promptAction = isDeposit ? "deposit" : "withdraw";
                String prompt = plugin.getMessageManager().getRawMessage("prompt_bank_amount").replace("<action>", promptAction);
                plugin.getMessageManager().sendRawMessage(player, prompt);
                plugin.getChatInputManager().awaitInput(player, gui, input -> {
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
                    plugin.getTaskRunner().runOnEntity(player, gui::open);
                });
            }
        }
    }

    private void onTeamSettingsGUIClick(Player player, TeamSettingsGUI gui, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);

        switch (action) {
            case "back-button" -> new TeamGUI(plugin, gui.getTeam(), player).open();
            case "change-tag", "change-description" -> {
                player.closeInventory();
                boolean isTag = action.equals("change-tag");
                String setting = isTag ? "tag" : "description";
                String prompt = plugin.getMessageManager().getRawMessage("prompt_setting_change").replace("<setting>", setting);
                plugin.getMessageManager().sendRawMessage(player, prompt);
                plugin.getChatInputManager().awaitInput(player, gui, input -> {
                    if (isTag) {
                        teamManager.setTeamTag(player, input);
                    } else {
                        teamManager.setTeamDescription(player, input);
                    }
                    plugin.getTaskRunner().runOnEntity(player, gui::open);
                });
            }
        }
    }

    private void onLeaderboardCategoryGUIClick(Player player, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);

        LeaderboardViewGUI.LeaderboardType type;
        String title;
        switch (action) {
            case "top-kills" -> {
                type = LeaderboardViewGUI.LeaderboardType.KILLS;
                title = "ᴛᴏᴘ 10 ᴛᴇᴀᴍs ʙʏ ᴋɪʟʟs";
            }
            case "top-balance" -> {
                type = LeaderboardViewGUI.LeaderboardType.BALANCE;
                title = "ᴛᴏᴘ 10 ᴛᴇᴀᴍs ʙʏ ʙᴀʟᴀɴᴄᴇ";
            }
            case "top-members" -> {
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

    private void onLeaderboardViewGUIClick(Player player, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action.equals("back-button")) {
            new LeaderboardCategoryGUI(plugin, player).open();
        }
    }

    private void onNoTeamGUIClick(Player player, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);

        switch (action) {
            case "create-team" -> {
                player.closeInventory();
                plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prompt_team_name"));
                plugin.getChatInputManager().awaitInput(player, new NoTeamGUI(plugin, player), teamName -> {
                    String validationError = teamManager.validateTeamName(teamName);
                    if (validationError != null) {
                        plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prefix") + validationError);
                        plugin.getTaskRunner().runOnEntity(player, () -> new NoTeamGUI(plugin, player).open());
                        return;
                    }
                    plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prompt_team_tag"));
                    plugin.getChatInputManager().awaitInput(player, new NoTeamGUI(plugin, player), teamTag -> {
                        teamManager.createTeam(player, teamName, teamTag);
                    });
                });
            }
            case "leaderboards" -> new LeaderboardCategoryGUI(plugin, player).open();
        }
    }

    private void onAdminGUIClick(Player player, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action.equals("manage-teams")) {
            plugin.getTaskRunner().runAsync(() -> {
                List<Team> allTeams = teamManager.getAllTeams();
                plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamListGUI(plugin, player, allTeams, 0).open());
            });
        }
    }

    private void onAdminTeamListGUIClick(Player player, AdminTeamListGUI gui, ItemStack clickedItem, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        switch (action) {
            case "next-page" -> new AdminTeamListGUI(plugin, player, gui.getAllTeams(), gui.getPage() + 1).open();
            case "previous-page" -> new AdminTeamListGUI(plugin, player, gui.getAllTeams(), gui.getPage() - 1).open();
            case "back-button" -> new AdminGUI(plugin, player).open();
            case "team-head" -> {
                Component displayName = clickedItem.getItemMeta().displayName();
                if (displayName == null) return;
                String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
                plugin.getTaskRunner().runAsync(() -> {
                    Team targetTeam = teamManager.getTeamByName(plainName);
                    if (targetTeam != null) {
                        plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamManageGUI(plugin, player, targetTeam).open());
                    }
                });
            }
        }
    }

    private void onAdminTeamManageGUIClick(Player player, AdminTeamManageGUI gui, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        switch (action) {
            case "back-button" -> plugin.getTaskRunner().runAsync(() -> {
                List<Team> allTeams = teamManager.getAllTeams();
                plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamListGUI(plugin, player, allTeams, 0).open());
            });
            case "disband-team" -> new ConfirmGUI(plugin, player, "Disband " + gui.getTargetTeam().getName() + "?", confirmed -> {
                if (confirmed) {
                    teamManager.adminDisbandTeam(player, gui.getTargetTeam().getName());
                } else {
                    gui.open();
                }
            }).open();
        }
    }

    private void onConfirmGUIClick(ConfirmGUI gui, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action.equals("confirm")) {
            gui.handleConfirm();
        } else if (action.equals("cancel")) {
            gui.handleCancel();
        }
    }
}