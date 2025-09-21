package eu.kotori.justTeams.gui;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.gui.admin.AdminGUI;
import eu.kotori.justTeams.gui.admin.AdminTeamListGUI;
import eu.kotori.justTeams.gui.admin.AdminTeamManageGUI;
import eu.kotori.justTeams.gui.sub.TeamSettingsGUI;
import eu.kotori.justTeams.gui.BlacklistGUI;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.util.EffectsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class TeamGUIListener implements Listener {
    private final JustTeams plugin;
    private final TeamManager teamManager;
    private final NamespacedKey actionKey;
    private final ConcurrentHashMap<String, Long> actionCooldowns = new ConcurrentHashMap<>();
    private final Object actionLock = new Object();
    public TeamGUIListener(JustTeams plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.actionKey = JustTeams.getActionKey();
    }
    private boolean checkActionCooldown(Player player, String action, long cooldownMs) {
        if (player == null || action == null) {
            return false;
        }
        String key = player.getUniqueId() + ":" + action;
        long currentTime = System.currentTimeMillis();
        synchronized (actionLock) {
            return actionCooldowns.compute(key, (k, lastActionTime) -> {
                if (lastActionTime == null || (currentTime - lastActionTime) >= cooldownMs) {
                    return currentTime;
                }
                return lastActionTime;
            }) == currentTime;
        }
    }
    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        try {
            InventoryHolder holder = event.getView().getTopInventory().getHolder();
            boolean isOurGui = holder instanceof IRefreshableGUI || holder instanceof NoTeamGUI || holder instanceof ConfirmGUI ||
                    holder instanceof AdminGUI || holder instanceof AdminTeamListGUI || holder instanceof AdminTeamManageGUI ||
                    holder instanceof TeamSettingsGUI || holder instanceof LeaderboardCategoryGUI || holder instanceof LeaderboardViewGUI ||
                    holder instanceof JoinRequestGUI || holder instanceof WarpsGUI || holder instanceof BlacklistGUI;
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
            if (holder instanceof BlacklistGUI) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getDebugLogger().log("=== BLACKLIST GUI MAIN CLICK DEBUG ===");
                    plugin.getDebugLogger().log("Player: " + player.getName());
                    plugin.getDebugLogger().log("Clicked item type: " + clickedItem.getType());
                    plugin.getDebugLogger().log("Clicked item has meta: " + (meta != null));
                    plugin.getDebugLogger().log("PDC has action key: " + pdc.has(actionKey, PersistentDataType.STRING));
                    if (pdc.has(actionKey, PersistentDataType.STRING)) {
                        String action = pdc.get(actionKey, PersistentDataType.STRING);
                        plugin.getDebugLogger().log("Action found: " + action);
                    } else {
                        plugin.getLogger().warning("No action key found in blacklist item!");
                        for (NamespacedKey key : pdc.getKeys()) {
                            plugin.getDebugLogger().log("PDC key found: " + key.toString());
                        }
                    }
                    plugin.getDebugLogger().log("=== END BLACKLIST GUI MAIN CLICK DEBUG ===");
                }
            }
            if (!pdc.has(actionKey, PersistentDataType.STRING)) {
                plugin.getDebugLogger().log("GUI click without valid action key from " + player.getName());
                return;
            }
            String action = pdc.get(actionKey, PersistentDataType.STRING);
            if (action == null || action.isEmpty() || action.length() > 50) {
                plugin.getDebugLogger().log("Invalid action in GUI click from " + player.getName() + ": " + action);
                return;
            }
            if ("back-button".equals(action)) {
                plugin.getDebugLogger().log("Back button clicked by " + player.getName() + " in " + holder.getClass().getSimpleName());
            }
        if (holder instanceof TeamGUI gui) onTeamGUIClick(player, gui, clickedItem, pdc);
        else if (holder instanceof MemberEditGUI gui) onMemberEditGUIClick(player, gui, pdc);
        else if (holder instanceof BankGUI gui) onBankGUIClick(player, gui, pdc);
        else if (holder instanceof TeamSettingsGUI gui) onTeamSettingsGUIClick(player, gui, pdc);
        else if (holder instanceof JoinRequestGUI gui) onJoinRequestGUIClick(player, gui, event.getClick(), clickedItem);
        else if (holder instanceof LeaderboardCategoryGUI) onLeaderboardCategoryGUIClick(player, pdc);
        else if (holder instanceof LeaderboardViewGUI) onLeaderboardViewGUIClick(player, pdc);
        else if (holder instanceof NoTeamGUI) onNoTeamGUIClick(player, pdc);
        else if (holder instanceof AdminGUI) onAdminGUIClick(player, pdc);
        else if (holder instanceof AdminTeamListGUI gui) onAdminTeamListGUIClick(player, gui, clickedItem, pdc);
        else if (holder instanceof AdminTeamManageGUI gui) onAdminTeamManageGUIClick(player, gui, pdc);
        else if (holder instanceof ConfirmGUI gui) onConfirmGUIClick(gui, pdc);
        else if (holder instanceof WarpsGUI) onWarpsGUIClick(player, (WarpsGUI) holder, event.getClick(), clickedItem, pdc);
        else if (holder instanceof BlacklistGUI gui) onBlacklistGUIClick(player, gui, event.getClick(), clickedItem, pdc);
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling GUI click for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().severe("Error in GUI action: " + e.getMessage());
            }
            plugin.getMessageManager().sendMessage(player, "gui_error");
            event.setCancelled(true);
        }
    }
    private void onTeamGUIClick(Player player, TeamGUI gui, ItemStack clickedItem, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        Team team = gui.getTeam();
        if (team == null) {
            plugin.getDebugLogger().log("TeamGUI click with null team for " + player.getName());
            return;
        }
        if (!team.isMember(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "player_not_in_team");
            player.closeInventory();
            return;
        }
        switch (action) {
            case "player-head" -> {
                if (clickedItem.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getPlayerProfile() != null) {
                    Object profileId = skullMeta.getPlayerProfile().getId();
                    UUID targetUuid = null;
                    if (profileId instanceof UUID) {
                        targetUuid = (UUID) profileId;
                    } else if (profileId instanceof String) {
                        try {
                            targetUuid = UUID.fromString((String) profileId);
                        } catch (IllegalArgumentException e) {
                            plugin.getDebugLogger().log("Invalid UUID format in player-head click from " + player.getName());
                            return;
                        }
                    }
                    if (targetUuid != null) {
                        if (targetUuid.equals(player.getUniqueId())) {
                            plugin.getMessageManager().sendMessage(player, "cannot_edit_own_permissions");
                            return;
                        }
                        TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                        TeamPlayer targetMember = team.getMember(targetUuid);
                        if (viewerMember == null || targetMember == null) {
                            plugin.getMessageManager().sendMessage(player, "player_not_in_team");
                            return;
                        }
                        boolean canEdit = false;
                        if (viewerMember.getRole() == TeamRole.OWNER) {
                            canEdit = true;
                        } else if (viewerMember.getRole() == TeamRole.CO_OWNER) {
                            canEdit = targetMember.getRole() == TeamRole.MEMBER;
                        }
                        if (canEdit) {
                            new MemberEditGUI(plugin, team, player, targetUuid).open();
                        } else {
                            plugin.getMessageManager().sendMessage(player, "no_permission");
                        }
                    }
                }
            }
            case "join-requests" -> {
                new JoinRequestGUI(plugin, player, team).open();
            }
            case "join-requests-locked" -> {
                plugin.getMessageManager().sendMessage(player, "join_requests_permission_denied");
            }
            case "warps" -> {
                try {
                    Class.forName("eu.kotori.justTeams.gui.WarpsGUI");
                    teamManager.openWarpsGUI(player);
                } catch (ClassNotFoundException e) {
                    teamManager.listTeamWarps(player);
                }
            }
            case "bank" -> {
                TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                if (viewerMember == null || (!viewerMember.canWithdraw() && !player.hasPermission("justteams.bypass.bank.use"))) {
                    plugin.getMessageManager().sendMessage(player, "no_permission");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                    return;
                }
                new BankGUI(plugin, player, team).open();
            }
            case "bank-locked" -> {
                plugin.getMessageManager().sendMessage(player, "bank_permission_denied");
            }
            case "home" -> {
                TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                if (viewerMember == null || (!viewerMember.canUseHome() && !player.hasPermission("justteams.bypass.home.use"))) {
                    plugin.getMessageManager().sendMessage(player, "no_permission");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                    return;
                }
                if (!checkActionCooldown(player, "home", 5000)) {
                    return;
                }
                teamManager.teleportToHome(player);
            }
            case "ender-chest" -> {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Enderchest clicked by " + player.getName() + " in team " + team.getName());
                }
                TeamPlayer viewerMember = team.getMember(player.getUniqueId());
                if (viewerMember == null) {
                    plugin.getLogger().warning("Player " + player.getName() + " not found in team " + team.getName());
                    plugin.getMessageManager().sendMessage(player, "player_not_in_team");
                    return;
                }
                boolean hasPermission = viewerMember.canUseEnderChest();
                boolean hasBypass = player.hasPermission("justteams.bypass.enderchest.use");
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Enderchest permission check for " + player.getName() +
                        " - canUseEnderChest: " + hasPermission +
                        ", hasBypass: " + hasBypass +
                        ", member: " + viewerMember.getPlayerUuid() +
                        ", team: " + team.getName() +
                        ", teamId: " + team.getId());
                }
                if (!hasPermission && !hasBypass) {
                    plugin.getLogger().warning("Player " + player.getName() + " attempted to access enderchest without permission!");
                    plugin.getLogger().warning("Permission details - canUseEnderChest: " + hasPermission + ", hasBypass: " + hasBypass);
                    plugin.getMessageManager().sendMessage(player, "no_permission");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                    return;
                }
                if (!checkActionCooldown(player, "enderchest", 2000)) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Enderchest action blocked by cooldown for " + player.getName());
                    }
                    return;
                }
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Opening enderchest for " + player.getName());
                }
                teamManager.openEnderChest(player);
            }
            case "ender-chest-locked" -> {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Enderchest-locked clicked by " + player.getName() + " in team " + team.getName());
                }
                plugin.getMessageManager().sendMessage(player, "enderchest_permission_denied");
                EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            }
            case "sort" -> {
                team.cycleSortType();
                new TeamGUI(plugin, team, player).open();
            }
            case "pvp-toggle" -> {
                if (!checkActionCooldown(player, "pvp-toggle", 2000)) {
                    return;
                }
                teamManager.togglePvpStatus(player);
                gui.initializeItems();
            }
            case "team-settings" -> {
                new TeamSettingsGUI(plugin, player, team).open();
            }
            case "settings" -> {
                new TeamSettingsGUI(plugin, player, team).open();
            }
            case "settings-locked" -> {
                plugin.getMessageManager().sendMessage(player, "settings_permission_denied");
            }
            case "disband-button" -> {
                if (!checkActionCooldown(player, "disband", 10000)) {
                    return;
                }
                new ConfirmGUI(plugin, player, "Are you sure you want to disband your team? This cannot be undone.", confirmed -> {
                    if (confirmed) {
                        teamManager.disbandTeam(player);
                    }
                }).open();
            }
            case "leave-button" -> {
                if (!checkActionCooldown(player, "leave", 5000)) {
                    return;
                }
                new ConfirmGUI(plugin, player, "Are you sure you want to leave the team?", confirmed -> {
                    if (confirmed) {
                        teamManager.leaveTeam(player);
                    }
                }).open();
            }
            case "blacklist" -> {
                new BlacklistGUI(plugin, team, player).open();
            }
            default -> {
                plugin.getDebugLogger().log("Unknown TeamGUI action: " + action + " from " + player.getName());
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
            case "promote-button" -> {
                if (!checkActionCooldown(player, "promote", 2000)) {
                    return;
                }
                teamManager.promotePlayer(player, gui.getTargetUuid());
            }
            case "demote-button" -> {
                if (!checkActionCooldown(player, "demote", 2000)) {
                    return;
                }
                teamManager.demotePlayer(player, gui.getTargetUuid());
            }
            case "kick-button" -> {
                if (!checkActionCooldown(player, "kick", 2000)) {
                    return;
                }
                teamManager.kickPlayer(player, gui.getTargetUuid());
            }
            case "transfer-button" -> {
                if (!checkActionCooldown(player, "transfer", 5000)) {
                    return;
                }
                teamManager.transferOwnership(player, gui.getTargetUuid());
            }
            case "back-button" -> {
                new TeamGUI(plugin, gui.getTeam(), player).open();
            }
            case "withdraw-permission", "enderchest-permission", "sethome-permission", "usehome-permission" -> {
                if (!checkActionCooldown(player, "permission-change", 1000)) {
                    return;
                }
                boolean isSelfView = gui.getTargetUuid().equals(player.getUniqueId());
                if (isSelfView) {
                    plugin.getMessageManager().sendMessage(player, "cannot_edit_own_permissions");
                    return;
                }
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
            case "withdraw-permission-view", "enderchest-permission-view", "sethome-permission-view", "usehome-permission-view" -> {
                plugin.getMessageManager().sendMessage(player, "view_only_mode");
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
                if (!checkActionCooldown(player, "bank-action", 1000)) {
                    return;
                }
                player.closeInventory();
                boolean isDeposit = action.equals("deposit");
                String promptAction = isDeposit ? "deposit" : "withdraw";
                String prompt = plugin.getMessageManager().getRawMessage("prompt_bank_amount").replace("<action>", promptAction);
                plugin.getMessageManager().sendRawMessage(player, prompt);
                plugin.getChatInputManager().awaitInput(player, gui, input -> {
                    try {
                        double amount = Double.parseDouble(input);
                        if (amount <= 0) {
                            plugin.getMessageManager().sendMessage(player, "bank_invalid_amount");
                        } else if (amount > 1_000_000_000) {
                            plugin.getMessageManager().sendMessage(player, "bank_amount_too_large");
                        } else {
                            if (isDeposit) {
                                teamManager.deposit(player, amount);
                            } else {
                                teamManager.withdraw(player, amount);
                            }
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
            case "back-button" -> {
                new TeamGUI(plugin, gui.getTeam(), player).open();
            }
            case "toggle-public" -> {
                if (!checkActionCooldown(player, "toggle-public", 2000)) {
                    return;
                }
                plugin.getTeamManager().togglePublicStatus(player);
                gui.initializeItems();
            }
            case "change-tag", "change-description" -> {
                String actionType = action.equals("change-tag") ? "change-tag" : "change-description";
                if (!checkActionCooldown(player, actionType, 2000)) {
                    return;
                }
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
    private void onJoinRequestGUIClick(Player player, JoinRequestGUI gui, ClickType click, ItemStack clickedItem) {
        if (clickedItem == null) return;
        ItemMeta meta = clickedItem.getItemMeta();
        if(meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(actionKey, PersistentDataType.STRING)) {
            String action = pdc.get(actionKey, PersistentDataType.STRING);
            if (action.equals("back-button")) {
                new TeamGUI(plugin, gui.getTeam(), player).open();
            } else if (action.equals("player-head")) {
                String playerUuidStr = pdc.get(new NamespacedKey(JustTeams.getInstance(), "player_uuid"), PersistentDataType.STRING);
                if (playerUuidStr != null) {
                    try {
                        UUID targetUuid = UUID.fromString(playerUuidStr);
                        if (click.isLeftClick()) {
                            teamManager.acceptJoinRequest(gui.getTeam(), targetUuid);
                        } else if (click.isRightClick()) {
                            teamManager.denyJoinRequest(gui.getTeam(), targetUuid);
                        }
                        gui.initializeItems();
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in join request GUI: " + playerUuidStr);
                    }
                }
            }
            return;
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
                plugin.getChatInputManager().awaitInput(player, null, teamName -> {
                    String validationError = teamManager.validateTeamName(teamName);
                    if (validationError != null) {
                        plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prefix") + validationError);
                        plugin.getTaskRunner().runOnEntity(player, () -> new NoTeamGUI(plugin, player).open());
                        return;
                    }
                    plugin.getMessageManager().sendRawMessage(player, plugin.getMessageManager().getRawMessage("prompt_team_tag"));
                    plugin.getChatInputManager().awaitInput(player, null, teamTag -> {
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
        switch (action) {
            case "back-button", "close" -> {
                player.closeInventory();
            }
            case "manage-teams" -> {
                plugin.getTaskRunner().runAsync(() -> {
                    List<Team> allTeams = teamManager.getAllTeams();
                    plugin.getTaskRunner().runOnEntity(player, () -> new AdminTeamListGUI(plugin, player, allTeams, 0).open());
                });
            }
            case "view-enderchest" -> {
                player.closeInventory();
                plugin.getChatInputManager().awaitInput(player, null, (input) -> {
                    if (input == null || input.trim().isEmpty()) {
                        plugin.getMessageManager().sendMessage(player, "invalid_input");
                        return;
                    }
                    teamManager.adminOpenEnderChest(player, input.trim());
                });
                plugin.getMessageManager().sendMessage(player, "admin_enderchest_input_prompt");
            }
            case "reload-plugin" -> {
                player.closeInventory();
                plugin.getTaskRunner().runAsync(() -> {
                    try {
                        plugin.getConfigManager().reloadConfig();
                        plugin.getTaskRunner().runOnEntity(player, () -> {
                            plugin.getMessageManager().sendMessage(player, "admin_reload_success");
                            EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
                        });
                    } catch (Exception e) {
                        plugin.getTaskRunner().runOnEntity(player, () -> {
                            plugin.getMessageManager().sendMessage(player, "admin_reload_failed");
                            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                        });
                    }
                });
            }
            default -> {
                plugin.getDebugLogger().log("Unknown admin GUI action: " + action + " from " + player.getName());
            }
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
    private void onWarpsGUIClick(Player player, WarpsGUI gui, ClickType click, ItemStack clickedItem, PersistentDataContainer pdc) {
        if (!pdc.has(actionKey, PersistentDataType.STRING)) return;
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (action.equals("back-button")) {
            new TeamGUI(plugin, gui.getTeam(), player).open();
        } else if (action.equals("warp_item")) {
            String warpName = pdc.get(new NamespacedKey(JustTeams.getInstance(), "warp_name"), PersistentDataType.STRING);
            if (warpName != null) {
                if (click.isLeftClick()) {
                    plugin.getTeamManager().teleportToTeamWarp(player, warpName, null);
                    player.closeInventory();
                } else if (click.isRightClick()) {
                    plugin.getTeamManager().deleteTeamWarp(player, warpName);
                    gui.initializeItems();
                }
            }
        }
    }
    private void onBlacklistGUIClick(Player player, BlacklistGUI gui, ClickType click, ItemStack clickedItem, PersistentDataContainer pdc) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().log("=== BLACKLIST GUI CLICK DEBUG ===");
            plugin.getDebugLogger().log("Player: " + player.getName());
            plugin.getDebugLogger().log("Click type: " + click);
            plugin.getDebugLogger().log("Clicked item: " + (clickedItem != null ? clickedItem.getType() : "null"));
            plugin.getDebugLogger().log("PDC has action key: " + pdc.has(actionKey, PersistentDataType.STRING));
        }
        if (!pdc.has(actionKey, PersistentDataType.STRING)) {
            plugin.getLogger().warning("No action key found in PDC for blacklist click");
            return;
        }
        String action = pdc.get(actionKey, PersistentDataType.STRING);
        plugin.getLogger().info("Action retrieved: " + action);
        if (action.equals("back-button")) {
            plugin.getLogger().info("Back button clicked, opening team GUI");
            new TeamGUI(plugin, gui.getTeam(), player).open();
        } else if (action.startsWith("remove-blacklist:")) {
            plugin.getLogger().info("Remove blacklist action detected: " + action);
            if (!checkActionCooldown(player, "remove-blacklist", 2000)) {
                plugin.getLogger().info("Rate limit hit for blacklist removal by " + player.getName());
                return;
            }
            String uuidString = action.substring("remove-blacklist:".length());
            plugin.getLogger().info("UUID string extracted: " + uuidString);
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(uuidString);
                plugin.getLogger().info("UUID parsed successfully: " + targetUuid);
                plugin.getLogger().info("Team ID: " + gui.getTeam().getId());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID format in blacklist removal action: " + uuidString);
                return;
            }
            final BlacklistGUI finalGui = gui;
            final UUID finalTargetUuid = targetUuid;
            plugin.getLogger().info("Starting async blacklist removal...");
            plugin.getTaskRunner().runAsync(() -> {
                try {
                    plugin.getLogger().info("Executing blacklist removal in async thread for " + finalTargetUuid);
                    plugin.getLogger().info("Storage manager: " + plugin.getStorageManager());
                    plugin.getLogger().info("Storage: " + plugin.getStorageManager().getStorage());
                    boolean success = plugin.getStorageManager().getStorage().removePlayerFromBlacklist(
                        finalGui.getTeam().getId(), finalTargetUuid);
                    plugin.getLogger().info("Blacklist removal result: " + success + " for " + finalTargetUuid);
                    if (success) {
                        plugin.getLogger().info("Blacklist removal successful, refreshing GUI...");
                        plugin.getTaskRunner().runOnEntity(player, () -> {
                            try {
                                plugin.getMessageManager().sendMessage(player, "player_removed_from_blacklist",
                                    Placeholder.unparsed("target", Bukkit.getOfflinePlayer(finalTargetUuid).getName()));
                                plugin.getLogger().info("Success message sent, now refreshing GUI for " + player.getName());
                                finalGui.refresh();
                                plugin.getLogger().info("GUI refresh called successfully");
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error in sync thread for blacklist removal: " + e.getMessage());
                                plugin.getLogger().severe("Error in GUI action: " + e.getMessage());
                            }
                        });
                    } else {
                        plugin.getLogger().warning("Blacklist removal failed in database");
                        plugin.getTaskRunner().runOnEntity(player, () -> {
                            plugin.getMessageManager().sendMessage(player, "remove_blacklist_failed");
                        });
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error removing player from blacklist: " + e.getMessage());
                    plugin.getLogger().severe("Error in GUI action: " + e.getMessage());
                    plugin.getTaskRunner().runOnEntity(player, () -> {
                        plugin.getMessageManager().sendMessage(player, "remove_blacklist_failed");
                    });
                }
            });
        } else {
            plugin.getLogger().warning("Unknown action in blacklist GUI: " + action);
        }
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().log("=== END BLACKLIST GUI CLICK DEBUG ===");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        boolean isOurGui = holder instanceof IRefreshableGUI || holder instanceof NoTeamGUI || holder instanceof ConfirmGUI ||
                holder instanceof AdminGUI || holder instanceof AdminTeamListGUI || holder instanceof AdminTeamManageGUI ||
                holder instanceof TeamSettingsGUI || holder instanceof LeaderboardCategoryGUI || holder instanceof LeaderboardViewGUI ||
                holder instanceof JoinRequestGUI || holder instanceof WarpsGUI || holder instanceof BlacklistGUI;

        if (isOurGui) {
            event.setCancelled(true);
        }
    }
}
