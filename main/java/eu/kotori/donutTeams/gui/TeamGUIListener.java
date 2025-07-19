package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.gui.sub.MemberEditGUI;
import eu.kotori.donutTeams.gui.sub.MemberPermissionsEditGUI;
import eu.kotori.donutTeams.gui.sub.MemberPermissionsListGUI;
import eu.kotori.donutTeams.gui.sub.TeamSettingsGUI;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamManager;
import eu.kotori.donutTeams.team.TeamPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
        if (event.getClickedInventory() == null) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Team) {
            return;
        }

        if (holder == null) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (holder instanceof TeamGUI gui) onTeamGUIClick(event, player, gui, clickedItem);
        else if (holder instanceof MemberEditGUI gui) onMemberEditGUIClick(event, player, gui, clickedItem);
        else if (holder instanceof BankGUI gui) onBankGUIClick(player, gui, clickedItem);
        else if (holder instanceof TeamSettingsGUI gui) onTeamSettingsGUIClick(player, gui, clickedItem);
        else if (holder instanceof MemberPermissionsListGUI gui) onMemberPermissionsListGUIClick(player, gui, clickedItem);
        else if (holder instanceof MemberPermissionsEditGUI gui) onMemberPermissionsEditGUIClick(player, gui, clickedItem);
        else if (holder instanceof LeaderboardCategoryGUI gui) onLeaderboardCategoryGUIClick(player, gui, clickedItem);
        else if (holder instanceof LeaderboardViewGUI) onLeaderboardViewGUIClick(player, clickedItem);
    }

    private void onTeamGUIClick(InventoryClickEvent event, Player player, TeamGUI gui, ItemStack clicked) {
        Team team = gui.getTeam();
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                if (team.isOwner(player.getUniqueId()) && !team.isOwner(skullMeta.getPlayerProfile().getId())) {
                    new MemberEditGUI(plugin, team, player, skullMeta.getPlayerProfile().getId()).open();
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
            case COMPARATOR -> new TeamSettingsGUI(plugin, player, team).open();
            case IRON_SWORD -> {
                teamManager.togglePvp(player);
                gui.initializeItems();
            }
            default -> {}
        }
    }

    private void onMemberEditGUIClick(InventoryClickEvent event, Player player, MemberEditGUI gui, ItemStack clicked) {
        switch (clicked.getType()) {
            case RED_WOOL -> teamManager.kickPlayer(player, gui.getTargetUuid());
            case BEACON -> teamManager.transferOwnership(player, gui.getTargetUuid());
            case ARROW -> new TeamGUI(plugin, gui.getTeam(), player).open();
            default -> {}
        }
        if (clicked.getType() != Material.ARROW) player.closeInventory();
    }

    private void onBankGUIClick(Player player, BankGUI gui, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new TeamGUI(plugin, gui.getTeam(), player).open();
            return;
        }

        player.closeInventory();
        boolean isDeposit = clicked.getType() == Material.GREEN_WOOL;
        String action = isDeposit ? "deposit" : "withdraw";

        plugin.getMessageManager().sendRawMessage(player, "<gray>Please type the amount to " + action + " in chat, or type 'cancel' to abort.");

        plugin.getChatInputManager().awaitInput(player, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                plugin.getMessageManager().sendRawMessage(player, "<red>Action cancelled.");
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
        });
    }

    private void onTeamSettingsGUIClick(Player player, TeamSettingsGUI gui, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new TeamGUI(plugin, gui.getTeam(), player).open();
            return;
        }

        if (clicked.getType() == Material.COMPARATOR) {
            new MemberPermissionsListGUI(player, gui.getTeam()).open();
            return;
        }

        player.closeInventory();
        boolean isTag = clicked.getType() == Material.NAME_TAG;
        String action = isTag ? "tag" : "description";

        plugin.getMessageManager().sendRawMessage(player, "<gray>Please type the new team " + action + " in chat, or type 'cancel' to abort.");

        plugin.getChatInputManager().awaitInput(player, input -> {
            if (input.equalsIgnoreCase("cancel")) {
                plugin.getMessageManager().sendRawMessage(player, "<red>Action cancelled.");
                return;
            }
            if (isTag) {
                teamManager.setTeamTag(player, input);
            } else {
                teamManager.setTeamDescription(player, input);
            }
        });
    }

    private void onMemberPermissionsListGUIClick(Player player, MemberPermissionsListGUI gui, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new TeamSettingsGUI(plugin, player, gui.getTeam()).open();
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getPlayerProfile() != null && skullMeta.getPlayerProfile().getId() != null) {
                UUID targetUuid = skullMeta.getPlayerProfile().getId();
                new MemberPermissionsEditGUI(plugin, player, gui.getTeam(), targetUuid).open();
            }
        }
    }

    private void onMemberPermissionsEditGUIClick(Player player, MemberPermissionsEditGUI gui, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new MemberPermissionsListGUI(player, gui.getTeam()).open();
            return;
        }

        TeamPlayer target = gui.getTargetMember();
        if (target == null) return;

        boolean canWithdraw = target.canWithdraw();
        boolean canUseEC = target.canUseEnderChest();

        if (clicked.getItemMeta().getDisplayName().contains("BANK WITHDRAW")) {
            canWithdraw = !canWithdraw;
        } else if (clicked.getItemMeta().getDisplayName().contains("ENDER CHEST ACCESS")) {
            canUseEC = !canUseEC;
        }

        teamManager.updateMemberPermissions(player, target.getPlayerUuid(), canWithdraw, canUseEC);
        gui.initializeItems();
    }

    private void onLeaderboardCategoryGUIClick(Player player, LeaderboardCategoryGUI gui, ItemStack clicked) {
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, Team> topTeams;
            switch(type) {
                case KILLS -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByKills(10);
                case BALANCE -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByBalance(10);
                case MEMBERS -> topTeams = plugin.getStorageManager().getStorage().getTopTeamsByMembers(10);
                default -> topTeams = Map.of();
            }
            Map<Integer, Team> finalTopTeams = topTeams;
            Bukkit.getScheduler().runTask(plugin, () -> new LeaderboardViewGUI(plugin, player, title, finalTopTeams, type).open());
        });
    }

    private void onLeaderboardViewGUIClick(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            new LeaderboardCategoryGUI(plugin, player).open();
        }
    }
}