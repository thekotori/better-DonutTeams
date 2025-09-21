package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
public class BedrockSupport {
    private final JustTeams plugin;
    private final Map<UUID, Boolean> bedrockPlayerCache = new ConcurrentHashMap<>();
    private boolean floodgateAvailable = false;
    public BedrockSupport(JustTeams plugin) {
        this.plugin = plugin;
        checkFloodgateAvailability();
    }
    private void checkFloodgateAvailability() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("floodgate") != null) {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateAvailable = true;
                plugin.getLogger().info("Floodgate detected! Bedrock support enabled.");
            } else {
                floodgateAvailable = false;
                plugin.getLogger().info("Floodgate not found. Bedrock support disabled.");
            }
        } catch (Exception e) {
            floodgateAvailable = false;
            plugin.getLogger().warning("Error checking Floodgate availability: " + e.getMessage());
        }
    }
    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable) {
            return false;
        }
        Boolean cached = bedrockPlayerCache.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            boolean isBedrock = (Boolean) floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(floodgateApi, player.getUniqueId());
            bedrockPlayerCache.put(player.getUniqueId(), isBedrock);
            return isBedrock;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if player is Bedrock: " + e.getMessage());
            return false;
        }
    }
    public boolean isBedrockPlayer(UUID uuid) {
        if (!floodgateAvailable) {
            return false;
        }
        Boolean cached = bedrockPlayerCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            boolean isBedrock = (Boolean) floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(floodgateApi, uuid);
            bedrockPlayerCache.put(uuid, isBedrock);
            return isBedrock;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if UUID is Bedrock: " + e.getMessage());
            return false;
        }
    }
    public String getBedrockGamertag(Player player) {
        if (!floodgateAvailable || !isBedrockPlayer(player)) {
            return null;
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object floodgatePlayer = floodgateApiClass.getMethod("getPlayer", UUID.class).invoke(floodgateApi, player.getUniqueId());
            if (floodgatePlayer != null) {
                return (String) floodgatePlayer.getClass().getMethod("getUsername").invoke(floodgatePlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting Bedrock gamertag: " + e.getMessage());
        }
        return null;
    }
    public String getBedrockGamertag(UUID uuid) {
        if (!floodgateAvailable || !isBedrockPlayer(uuid)) {
            return null;
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object floodgatePlayer = floodgateApiClass.getMethod("getPlayer", UUID.class).invoke(floodgateApi, uuid);
            if (floodgatePlayer != null) {
                return (String) floodgatePlayer.getClass().getMethod("getUsername").invoke(floodgatePlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting Bedrock gamertag by UUID: " + e.getMessage());
        }
        return null;
    }
    public UUID getJavaEditionUuid(Player player) {
        if (!floodgateAvailable || !isBedrockPlayer(player)) {
            return player.getUniqueId();
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object floodgatePlayer = floodgateApiClass.getMethod("getPlayer", UUID.class).invoke(floodgateApi, player.getUniqueId());
            if (floodgatePlayer != null) {
                return (UUID) floodgatePlayer.getClass().getMethod("getJavaUniqueId").invoke(floodgatePlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting Java edition UUID: " + e.getMessage());
        }
        return player.getUniqueId();
    }
    public UUID getJavaEditionUuid(UUID uuid) {
        if (!floodgateAvailable || !isBedrockPlayer(uuid)) {
            return uuid;
        }
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApi = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object floodgatePlayer = floodgateApiClass.getMethod("getPlayer", UUID.class).invoke(floodgateApi, uuid);
            if (floodgatePlayer != null) {
                return (UUID) floodgatePlayer.getClass().getMethod("getJavaUniqueId").invoke(floodgatePlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting Java edition UUID by UUID: " + e.getMessage());
        }
        return uuid;
    }
    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }
    public void clearPlayerCache(UUID uuid) {
        bedrockPlayerCache.remove(uuid);
    }
    public void clearAllCache() {
        bedrockPlayerCache.clear();
    }
    public String getPlatformDisplayName(Player player) {
        if (isBedrockPlayer(player)) {
            String gamertag = getBedrockGamertag(player);
            if (gamertag != null && !gamertag.equals(player.getName())) {
                return player.getName() + " <gray>(<#00D4FF>Bedrock</#00D4FF>: " + gamertag + "<gray>)";
            } else {
                return player.getName() + " <gray>(<#00D4FF>Bedrock</#00D4FF>)";
            }
        } else {
            return player.getName() + " <gray>(<#00FF00>Java</#00FF00>)";
        }
    }
    public String getPlatformIndicator(Player player) {
        return isBedrockPlayer(player) ? "<#00D4FF>BE</#00D4FF>" : "<#00FF00>JE</#00FF00>";
    }
}
