package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MemberPermissionsListGUI implements InventoryHolder {
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;

    public MemberPermissionsListGUI(Player viewer, Team team) {
        this.viewer = viewer;
        this.team = team;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Select Member to Edit"));
        initializeItems();
    }

    private void initializeItems() {
        int slot = 0;
        for (TeamPlayer member : team.getMembers()) {
            if (slot >= 45 || team.isOwner(member.getPlayerUuid())) continue;

            String memberName = Bukkit.getOfflinePlayer(member.getPlayerUuid()).getName();
            inventory.setItem(slot++, new ItemBuilder(Material.PLAYER_HEAD)
                    .asPlayerHead(member.getPlayerUuid())
                    .withName("<gradient:#95FD95:#FFFFFF><bold>" + memberName + "</bold></gradient>")
                    .withLore(
                            "<gray>Bank Withdraw: " + (member.canWithdraw() ? "<green>Yes" : "<red>No"),
                            "<gray>Use Ender Chest: " + (member.canUseEnderChest() ? "<green>Yes" : "<red>No"),
                            "",
                            "<yellow>Click to edit permissions.</yellow>"
                    ).build());
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW).withName("<gray><bold>BACK</bold></gray>").build());
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