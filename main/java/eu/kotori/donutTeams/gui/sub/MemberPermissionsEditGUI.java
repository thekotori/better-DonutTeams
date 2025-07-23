package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MemberPermissionsEditGUI implements InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Team team;
    private final TeamPlayer targetMember;
    private final Inventory inventory;

    public MemberPermissionsEditGUI(DonutTeams plugin, Player viewer, Team team, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.targetMember = team.getMember(targetUuid);
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴘᴇʀᴍs: " + targetName));
        initializeItems();
    }

    public void initializeItems() {
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);


        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();

        inventory.setItem(12, new ItemBuilder(targetMember.canWithdraw() ? Material.LIME_DYE : Material.GRAY_DYE)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ʙᴀɴᴋ ᴡɪᴛʜᴅʀᴀᴡ</bold></gradient>")
                .withLore(
                        "<gray>Allows this member to withdraw from the team bank.",
                        "<gray>Status: " + (targetMember.canWithdraw() ? "<green>ENABLED" : "<red>DISABLED"),
                        "",
                        "<yellow>Click to toggle.</yellow>"
                ).build());

        inventory.setItem(14, new ItemBuilder(targetMember.canUseEnderChest() ? Material.LIME_DYE : Material.GRAY_DYE)
                .withName("<gradient:" + mainColor + ":" + accentColor + "><bold>ᴇɴᴅᴇʀ ᴄʜᴇsᴛ ᴀᴄᴄᴇss</bold></gradient>")
                .withLore(
                        "<gray>Allows this member to use the team ender chest.",
                        "<gray>Status: " + (targetMember.canUseEnderChest() ? "<green>ENABLED" : "<red>DISABLED"),
                        "",
                        "<yellow>Click to toggle.</yellow>"
                ).build());

        inventory.setItem(22, new ItemBuilder(Material.ARROW).withName("<gray><bold>ʙᴀᴄᴋ</bold></gray>").build());
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

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}