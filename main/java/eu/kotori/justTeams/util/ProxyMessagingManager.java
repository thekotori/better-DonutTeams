package eu.kotori.justTeams.util;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
public class ProxyMessagingManager implements PluginMessageListener, Listener {
    private final JustTeams plugin;
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String VELOCITY_CHANNEL = "velocity:main";
    public static final String CUSTOM_DATA_CHANNEL = "justteams:main";
    private final Cache<UUID, Location> pendingTeleports = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    public ProxyMessagingManager(JustTeams plugin) {
        this.plugin = plugin;
    }
    public static String getChannel() {
        return CUSTOM_DATA_CHANNEL;
    }
    public void sendPlayerToServerAndTeleport(Player player, String targetServer, Location location) {
        plugin.getDebugLogger().log(
                "Preparing to send " + player.getName() + " to server " + targetServer + " for home teleport via proxy protocol."
        );
        ByteArrayDataOutput dataPayload = ByteStreams.newDataOutput();
        dataPayload.writeUTF("TEAM_HOME_TELEPORT");
        dataPayload.writeUTF(player.getUniqueId().toString());
        dataPayload.writeUTF(location.getWorld().getName());
        dataPayload.writeDouble(location.getX());
        dataPayload.writeDouble(location.getY());
        dataPayload.writeDouble(location.getZ());
        dataPayload.writeFloat(location.getYaw());
        dataPayload.writeFloat(location.getPitch());
        ByteArrayDataOutput forwardMessage = ByteStreams.newDataOutput();
        forwardMessage.writeUTF("Forward");
        forwardMessage.writeUTF(targetServer);
        forwardMessage.writeUTF(CUSTOM_DATA_CHANNEL);
        forwardMessage.writeShort(dataPayload.toByteArray().length);
        forwardMessage.write(dataPayload.toByteArray());
        String proxyChannel = BUNGEE_CHANNEL;
        player.sendPluginMessage(plugin, proxyChannel, forwardMessage.toByteArray());
        plugin.getDebugLogger().log("Proxy: Forward message sent to " + targetServer + ".");
        plugin.getTaskRunner().runEntityTaskLater(player, () -> {
            ByteArrayDataOutput connectMessage = ByteStreams.newDataOutput();
            connectMessage.writeUTF("Connect");
            connectMessage.writeUTF(targetServer);
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, connectMessage.toByteArray());
            plugin.getDebugLogger().log("Connect message sent to proxy to move " + player.getName() + " to " + targetServer + ".");
        }, 5L);
    }
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (channel.equals(BUNGEE_CHANNEL)) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            if (!subChannel.equals(CUSTOM_DATA_CHANNEL)) return;
            short len = in.readShort();
            byte[] data = new byte[len];
            in.readFully(data);
            handleTeleportData(data);
        }
        else if (channel.equals(CUSTOM_DATA_CHANNEL)) {
            handleTeleportData(message);
        }
    }
    private void handleTeleportData(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String messageType = in.readUTF();
        plugin.getDebugLogger().log("Received plugin message type: " + messageType);
        if ("TEAM_HOME_TELEPORT".equals(messageType)) {
            try {
                UUID playerUuid = UUID.fromString(in.readUTF());
                String worldName = in.readUTF();
                double x = in.readDouble();
                double y = in.readDouble();
                double z = in.readDouble();
                float yaw = in.readFloat();
                float pitch = in.readFloat();
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World " + worldName + " not found for teleport of " + playerUuid);
                    return;
                }
                Location destination = new Location(world, x, y, z, yaw, pitch);
                pendingTeleports.put(playerUuid, destination);
                plugin.getDebugLogger().log("Stored pending teleport for " + playerUuid + " -> " + destination);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid teleport plugin message: " + e.getMessage());
            }
        }
    }
    public Location getAndRemovePendingTeleport(UUID playerUuid) {
        Location loc = pendingTeleports.getIfPresent(playerUuid);
        if (loc != null) {
            pendingTeleports.invalidate(playerUuid);
            plugin.getDebugLogger().log("Pending teleport for " + playerUuid + " retrieved and removed.");
        }
        return loc;
    }
}
