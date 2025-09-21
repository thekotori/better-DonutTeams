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
    private final Object inputLock = new Object();
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
        InputData inputData = pendingInput.get(player.getUniqueId());
        if (inputData == null) return;
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        pendingInput.remove(player.getUniqueId());
        plugin.getTaskRunner().run(() -> {
            inputData.onInput().accept(message);
            if (inputData.previousGui() != null) {
                inputData.previousGui().refresh();
            }
        });
    }
    public void cancelInput(Player player) {
        pendingInput.remove(player.getUniqueId());
    }
    public boolean hasPendingInput(Player player) {
        return pendingInput.containsKey(player.getUniqueId());
    }
    public void clearAllPendingInput() {
        pendingInput.clear();
    }
    private static class InputData {
        private final Consumer<String> onInput;
        private final IRefreshableGUI previousGui;
        public InputData(Consumer<String> onInput, IRefreshableGUI previousGui) {
            this.onInput = onInput;
            this.previousGui = previousGui;
        }
        public Consumer<String> onInput() {
            return onInput;
        }
        public IRefreshableGUI previousGui() {
            return previousGui;
        }
    }
}
