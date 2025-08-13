package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final NamespacedKey ACTION_KEY = JustTeams.getActionKey();

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public ItemBuilder withName(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Component component = miniMessage.deserialize(name)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(component);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder withLore(String... loreLines) {
        return withLore(Arrays.asList(loreLines));
    }

    public ItemBuilder withLore(List<String> loreLines) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<Component> lore = loreLines.stream()
                    .map(line -> miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList());
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder asPlayerHead(UUID playerUuid) {
        if (itemStack.getType() == Material.PLAYER_HEAD && itemStack.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setPlayerProfile(Bukkit.createProfile(playerUuid));
            itemStack.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemBuilder withGlow() {
        itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder withAction(String action) {
        if (action == null || action.isEmpty()) {
            return this;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }
}