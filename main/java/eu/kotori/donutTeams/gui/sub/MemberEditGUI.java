package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MemberEditGUI implements InventoryHolder {

    private final DonutTeams plugin;
    private final Team team;
    private final Player viewer;
    private final UUID targetUuid;
    private final Inventory inventory;

    public MemberEditGUI(DonutTeams plugin, Team team, Player viewer, UUID targetUuid) {
        this.plugin = plugin;
        this.team = team;
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Edit: " + target.getName()));
        initializeItems();
    }

    private void initializeItems() {
        inventory.setItem(11, new ItemBuilder(Material.RED_WOOL)
                .withName("<red><bold>ᴋɪᴄᴋ ᴍᴇᴍʙᴇʀ")
                .withLore(
                        "<gray>Removes this player from the team.",
                        "",
                        "<yellow>Click to kick"
                ).build());

        inventory.setItem(13, new ItemBuilder(Material.BEACON)
                .withName("<gold><bold>ᴛʀᴀɴsғᴇʀ ᴏᴡɴᴇʀsʜɪᴘ")
                .withLore(
                        "<gray>Makes this player the new team owner.",
                        "<dark_red>You will become a regular member!",
                        "",
                        "<yellow>Click to transfer"
                ).build());

        inventory.setItem(15, new ItemBuilder(Material.ARROW)
                .withName("<gray><bold>ʙᴀᴄᴋ")
                .withLore("<yellow>Click to return to the main menu.").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}