package eu.kotori.donutTeams.gui;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import eu.kotori.donutTeams.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class BankGUI implements InventoryHolder {
    private final DonutTeams plugin;
    private final Player viewer;
    private final Team team;
    private final Inventory inventory;

    public BankGUI(DonutTeams plugin, Player viewer, Team team) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.team = team;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("ᴛᴇᴀᴍ ʙᴀɴᴋ"));
        initializeItems();
    }

    private void initializeItems() {
        inventory.setItem(11, new ItemBuilder(Material.GREEN_WOOL)
                .withName("<green><bold>ᴅᴇᴘᴏsɪᴛ</bold></green>")
                .withLore(
                        "<gray>Click to deposit money into the team bank.",
                        "",
                        "<yellow>You will be prompted in chat.</yellow>"
                ).build());

        inventory.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
                .withName("<gold><bold>ᴄᴜʀʀᴇɴᴛ ʙᴀʟᴀɴᴄᴇ</bold></gold>")
                .withLore(
                        "<gray>Balance: <white>" + String.format("%,.2f", team.getBalance()) + "</white>"
                ).withGlow().build());

        inventory.setItem(15, new ItemBuilder(Material.RED_WOOL)
                .withName("<red><bold>ᴡɪᴛʜᴅʀᴀᴡ</bold></red>")
                .withLore(
                        "<gray>Click to withdraw money from the team bank.",
                        "",
                        "<yellow>You will be prompted in chat.</yellow>"
                ).build());

        inventory.setItem(26, new ItemBuilder(Material.ARROW)
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