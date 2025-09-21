package eu.kotori.justTeams.gui.sub;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.gui.IRefreshableGUI;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import eu.kotori.justTeams.team.TeamRole;
public class MemberPermissionsEditGUI implements InventoryHolder, IRefreshableGUI {
    private final JustTeams plugin;
    private final Player viewer;
    private final Team team;
    private final TeamPlayer targetMember;
    private final Inventory inventory;
    private final ConfigurationSection guiConfig;
    public MemberPermissionsEditGUI(JustTeams plugin, Player viewer, Team team, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.targetMember = team.getMember(targetUuid);
        this.guiConfig = plugin.getGuiConfigManager().getGUI("member-permissions-edit-menu");
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        String title = guiConfig.getString("title", "ᴘᴇʀᴍs: <target_name>").replace("<target_name>", targetName != null ? targetName : "Unknown");
        int size = guiConfig.getInt("size", 27);
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        initializeItems();
    }
    public void initializeItems() {
        inventory.clear();
        if (targetMember == null) return;
        boolean isSelfView = viewer.getUniqueId().equals(targetMember.getPlayerUuid());
        boolean canEdit = !isSelfView && (viewer.getUniqueId().equals(team.getOwnerUuid()) ||
                          team.getMember(viewer.getUniqueId()) != null &&
                          team.getMember(viewer.getUniqueId()).getRole() == TeamRole.CO_OWNER);
        setupHeader();
        setupPermissionToggles(canEdit);
        setupRoleManagement(canEdit);
        setupNavigation();
        fillEmptySlots();
    }
    private void setupHeader() {
        String targetName = Bukkit.getOfflinePlayer(targetMember.getPlayerUuid()).getName();
        if (targetName == null) targetName = "Unknown";
        String roleName = plugin.getGuiConfigManager().getRoleName(targetMember.getRole().name());
        String joinDate = formatJoinDate(targetMember.getJoinDate());
        ItemStack playerHead = new ItemBuilder(Material.PLAYER_HEAD)
                .withName("<gold><b>" + targetName)
                .withLore(
                    "<gray>Role: <yellow>" + roleName,
                    "<gray>Join Date: <yellow>" + joinDate,
                    "",
                    "<gray>Click to view permissions"
                )
                .build();
        inventory.setItem(4, playerHead);
    }
    private void setupPermissionToggles(boolean canEdit) {
        if (canEdit) {
            setupToggleButton(19, Material.GOLD_INGOT, "withdraw-permission", "Bank Withdraw", targetMember.canWithdraw());
            setupToggleButton(21, Material.ENDER_CHEST, "enderchest-permission", "Ender Chest", targetMember.canUseEnderChest());
            setupToggleButton(23, Material.RED_BED, "sethome-permission", "Set Home", targetMember.canSetHome());
            setupToggleButton(25, Material.COMPASS, "usehome-permission", "Use Home", targetMember.canUseHome());
        } else {
            setupViewButton(19, Material.GOLD_INGOT, "Bank Withdraw", targetMember.canWithdraw());
            setupViewButton(21, Material.ENDER_CHEST, "Ender Chest", targetMember.canUseEnderChest());
            setupViewButton(23, Material.RED_BED, "Set Home", targetMember.canSetHome());
            setupViewButton(25, Material.COMPASS, "Use Home", targetMember.canUseHome());
        }
    }
    private void setupToggleButton(int slot, Material material, String action, String permissionName, boolean currentStatus) {
        String status = currentStatus ? "<green>ENABLED" : "<red>DISABLED";
        String toggleText = currentStatus ? "<red>Click to DISABLE" : "<green>Click to ENABLE";
        ItemStack item = new ItemBuilder(material)
                .withName("<gold><b>" + permissionName)
                .withLore(
                    "<gray>Current Status: " + status,
                    "",
                    toggleText,
                    "<gray>Action: <yellow>" + action
                )
                .build();
        inventory.setItem(slot, item);
    }
    private void setupViewButton(int slot, Material material, String permissionName, boolean currentStatus) {
        String status = currentStatus ? "<green>ENABLED" : "<red>DISABLED";
        ItemStack item = new ItemBuilder(material)
                .withName("<gray><b>" + permissionName)
                .withLore(
                    "<gray>Current Status: " + status,
                    "",
                    "<gray>View Only Mode"
                )
                .build();
        inventory.setItem(slot, item);
    }
    private String formatJoinDate(Instant joinDate) {
        try {
            if (joinDate != null) {
                String dateFormat = plugin.getGuiConfigManager().getPlaceholder("date_time.join_date_format", "dd MMM yyyy");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat)
                        .withZone(ZoneOffset.UTC);
                return formatter.format(joinDate);
            } else {
                return plugin.getGuiConfigManager().getPlaceholder("date_time.unknown_date", "Unknown");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting join date: " + e.getMessage());
            return plugin.getGuiConfigManager().getPlaceholder("date_time.unknown_date", "Unknown");
        }
    }
    private void setupRoleManagement(boolean canEdit) {
        if (!canEdit) return;
        if (targetMember.getRole() == TeamRole.MEMBER) {
            ItemStack promoteButton = new ItemBuilder(Material.EMERALD)
                    .withName("<green><b>Promote to Co-Owner")
                    .withLore(
                        "<gray>Click to promote",
                        "<gray>player to co-owner role",
                        "",
                        "<yellow>Action: promote"
                    )
                    .build();
            inventory.setItem(37, promoteButton);
        } else if (targetMember.getRole() == TeamRole.CO_OWNER && !targetMember.getPlayerUuid().equals(team.getOwnerUuid())) {
            ItemStack demoteButton = new ItemBuilder(Material.REDSTONE)
                    .withName("<red><b>Demote to Member")
                    .withLore(
                        "<gray>Click to demote",
                        "<gray>player to member role",
                        "",
                        "<yellow>Action: demote"
                    )
                    .build();
            inventory.setItem(37, demoteButton);
        }
    }
    private void setupNavigation() {
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .withName("<yellow><b>← Back to Team")
                .withLore(
                    "<gray>Return to team menu",
                    "",
                    "<yellow>Action: back"
                )
                .build();
        inventory.setItem(40, backButton);
    }
    private void fillEmptySlots() {
        ItemStack fillItem = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .withName(" ")
                .build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }
    public void open() {
        viewer.openInventory(inventory);
    }
    public Team getTeam() {
        return team;
    }
    public TeamPlayer getTargetMember() {
        return targetMember;
    }
    public void refresh() {
        open();
    }
    public Inventory getInventory() {
        return inventory;
    }
}
