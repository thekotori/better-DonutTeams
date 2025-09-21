package eu.kotori.justTeams.gui.admin;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.util.GuiConfigManager;
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
import java.util.List;
public class AdminTeamListGUI implements InventoryHolder {
    private final JustTeams plugin;
    private final Player viewer;
    private final Inventory inventory;
    private final List<Team> allTeams;
    private int page;
    public AdminTeamListGUI(JustTeams plugin, Player viewer, List<Team> allTeams, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.allTeams = allTeams;
        this.page = page;
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        ConfigurationSection guiConfig = guiManager.getGUI("admin-team-list-gui");
        String title = guiConfig != null ? guiConfig.getString("title", "All Teams - Page " + (page + 1)) : "All Teams - Page " + (page + 1);
        int size = guiConfig != null ? guiConfig.getInt("size", 54) : 54;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
        initializeItems();
    }
    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);
        int maxItemsPerPage = 36;
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allTeams.size());
        GuiConfigManager guiManager = plugin.getGuiConfigManager();
        for (int i = startIndex; i < endIndex; i++) {
            Team team = allTeams.get(i);
            String ownerName = Bukkit.getOfflinePlayer(team.getOwnerUuid()).getName();
            String teamNameFormat = guiManager.getPlaceholder("admin.team_name_format", "<gold><bold>%team%</bold></gold>");
            String ownerFormat = guiManager.getPlaceholder("admin.owner_format", "<gray>Owner: <white>%owner%");
            String membersFormat = guiManager.getPlaceholder("admin.members_format", "<gray>Members: <white>%count%");
            String clickFormat = guiManager.getPlaceholder("admin.click_format", "<yellow>Click to manage this team.</yellow>");
            inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD)
                    .asPlayerHead(team.getOwnerUuid())
                    .withName(teamNameFormat.replace("%team%", team.getName()))
                    .withLore(
                            ownerFormat.replace("%owner%", ownerName != null ? ownerName : "Unknown"),
                            membersFormat.replace("%count%", String.valueOf(team.getMembers().size())),
                            "",
                            clickFormat
                    )
                    .withAction("team-head")
                    .build());
        }
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).withName("<gray>Previous Page").withAction("previous-page").build());
        }
        if (endIndex < allTeams.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).withName("<gray>Next Page").withAction("next-page").build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).withName("<red>Back to Admin Menu").withAction("back-button").build());
    }
    public void open() {
        viewer.openInventory(inventory);
    }
    public List<Team> getAllTeams() {
        return allTeams;
    }
    public int getPage() {
        return page;
    }
    public Inventory getInventory() {
        return inventory;
    }
}
