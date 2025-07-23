package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private final DonutTeams plugin;
    private final Map<UUID, Consumer<String>> pendingInput = new ConcurrentHashMap<>();

    public ChatInputManager(DonutTeams plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void awaitInput(Player player, Consumer<String> onInput) {
        pendingInput.put(player.getUniqueId(), onInput);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingInput.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> consumer = pendingInput.remove(uuid);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            plugin.getTaskRunner().runOnEntity(player, () -> consumer.accept(message));
        }
    }
}