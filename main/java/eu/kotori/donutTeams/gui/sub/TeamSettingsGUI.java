package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TeamSettingsGUI implements InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;

    public TeamSettingsGUI(DonutTeams plugin, Player viewer, Team team) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴛᴇᴀᴍ sᴇᴛᴛɪɴɢs"));
        initializeItems();
    }

    private void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        inventory.setItem(11, new ItemBuilder(Material.NAME_TAG)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴄʜᴀɴɢᴇ ᴛᴀɢ</bold></gradient>")
                .withLore(
                        "<gray>Current: <white>" + team.getTag() + "</white>",
                        "",
                        "<yellow>Click to set a new tag in chat.</yellow>"
                ).build());

        inventory.setItem(13, new ItemBuilder(Material.WRITABLE_BOOK)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴄʜᴀɴɢᴇ ᴅᴇsᴄʀɪᴘᴛɪᴏɴ</bold></gradient>")
                .withLore(
                        "<gray>Current: <white>" + team.getDescription() + "</white>",
                        "",
                        "<yellow>Click to set a new description in chat.</yellow>"
                ).build());

        if (team.isOwner(viewer.getUniqueId())) {
            inventory.setItem(15, new ItemBuilder(Material.COMPARATOR)
                    .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴍᴇᴍʙᴇʀ ᴘᴇʀᴍɪssɪᴏɴs</bold></gradient>")
                    .withLore(
                            "<gray>Manage individual member permissions.",
                            "",
                            "<yellow>Click to open the member list.</yellow>"
                    ).build());
        }


        inventory.setItem(22, new ItemBuilder(Material.ARROW)
                .withName("<gray><bold>ʙᴀᴄᴋ</bold></gray>")
                .withLore("<yellow>Click to return to the main menu.</yellow>").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public Team getTeam() {
        return team;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}