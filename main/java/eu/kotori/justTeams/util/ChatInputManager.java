package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.gui.IRefreshableGUI;
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

    private final JustTeams plugin;
    private final Map<UUID, InputData> pendingInput = new ConcurrentHashMap<>();

    public ChatInputManager(JustTeams plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void awaitInput(Player player, IRefreshableGUI previousGui, Consumer<String> onInput) {
        pendingInput.put(player.getUniqueId(), new InputData(onInput, previousGui));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingInput.containsKey(uuid)) {
            event.setCancelled(true);
            InputData data = pendingInput.remove(uuid);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            plugin.getTaskRunner().runOnEntity(player, () -> {
                if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("abort")) {
                    plugin.getMessageManager().sendRawMessage(player, "<red>Action cancelled.</red>");
                    if (data.getPreviousGui() != null) {
                        data.getPreviousGui().open();
                    }
                    return;
                }
                data.getOnInput().accept(message);
            });
        }
    }

    private static class InputData {
        private final Consumer<String> onInput;
        private final IRefreshableGUI previousGui;

        public InputData(Consumer<String> onInput, IRefreshableGUI previousGui) {
            this.onInput = onInput;
            this.previousGui = previousGui;
        }

        public Consumer<String> getOnInput() {
            return onInput;
        }

        public IRefreshableGUI getPreviousGui() {
            return previousGui;
        }
    }
}