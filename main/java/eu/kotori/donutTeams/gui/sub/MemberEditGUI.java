package eu.kotori.donutTeams.gui.sub;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.team.TeamPlayer;
import eu.kotori.donutTeams.team.TeamRole;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
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
        inventory.clear();
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).withName(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

        TeamPlayer targetMember = team.getMember(targetUuid);
        if(targetMember == null) return;

        if (targetMember.getRole() == TeamRole.MEMBER) {
            inventory.setItem(10, new ItemBuilder(Material.LIME_DYE)
                    .withName("<green><bold>PROMOTE TO CO-OWNER</bold></green>")
                    .withLore("<gray>Gives this player more permissions.", "", "<yellow>Click to promote.</yellow>")
                    .build());
        } else if (targetMember.getRole() == TeamRole.CO_OWNER) {
            inventory.setItem(10, new ItemBuilder(Material.GRAY_DYE)
                    .withName("<gray><bold>DEMOTE TO MEMBER</bold></gray>")
                    .withLore("<gray>Removes co-owner permissions.", "", "<yellow>Click to demote.</yellow>")
                    .build());
        }

        inventory.setItem(12, new ItemBuilder(Material.RED_WOOL)
                .withName("<red><bold>KICK MEMBER</bold></red>")
                .withLore(
                        "<gray>Removes this player from the team.",
                        "",
                        "<yellow>Click to kick</yellow>"
                ).build());

        inventory.setItem(14, new ItemBuilder(Material.BEACON)
                .withName("<gold><bold>TRANSFER OWNERSHIP</bold></gold>")
                .withLore(
                        "<gray>Makes this player the new team owner.",
                        "<dark_red>You will become a regular member!</dark_red>",
                        "",
                        "<yellow>Click to transfer</yellow>"
                ).build());

        inventory.setItem(22, new ItemBuilder(Material.ARROW)
                .withName("<gray><bold>BACK</bold></gray>")
                .withLore("<yellow>Click to return to the main menu.</yellow>").build());
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public TeamPlayer getTargetMember() {
        return team.getMember(targetUuid);
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