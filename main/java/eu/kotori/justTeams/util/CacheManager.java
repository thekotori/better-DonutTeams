package eu.kotori.justTeams.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.BlacklistedPlayer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class CacheManager {
    private final JustTeams plugin;
    private final Cache<Integer, List<BlacklistedPlayer>> blacklistCache;
    private final Cache<Integer, List<UUID>> joinRequestCache;
    private final Cache<String, Boolean> permissionCache;
    private final Cache<UUID, String> playerNameCache;
    private final Map<Integer, Long> lastDatabaseSync = new ConcurrentHashMap<>();
    private final long cacheExpiry;
    private final long syncCooldown;

    public CacheManager(JustTeams plugin) {
        this.plugin = plugin;
        this.cacheExpiry = plugin.getConfig().getLong("settings.sync_optimization.team_cache_ttl", 900);
        this.syncCooldown = plugin.getConfig().getLong("settings.sync_optimization.cache_cleanup_interval", 600) * 1000;

        this.blacklistCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
                .build();

        this.joinRequestCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
                .build();

        this.permissionCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
                .build();

        this.playerNameCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1800, TimeUnit.SECONDS)
                .build();
    }

    public List<BlacklistedPlayer> getTeamBlacklist(int teamId) {
        return blacklistCache.getIfPresent(teamId);
    }

    public void cacheTeamBlacklist(int teamId, List<BlacklistedPlayer> blacklist) {
        blacklistCache.put(teamId, blacklist);
        lastDatabaseSync.put(teamId, System.currentTimeMillis());
    }

    public void invalidateTeamBlacklist(int teamId) {
        blacklistCache.invalidate(teamId);
        lastDatabaseSync.remove(teamId);
    }

    public List<UUID> getJoinRequests(int teamId) {
        return joinRequestCache.getIfPresent(teamId);
    }

    public void cacheJoinRequests(int teamId, List<UUID> requests) {
        joinRequestCache.put(teamId, requests);
    }

    public void invalidateJoinRequests(int teamId) {
        joinRequestCache.invalidate(teamId);
    }

    public Boolean getPermissionResult(String key) {
        return permissionCache.getIfPresent(key);
    }

    public void cachePermissionResult(String key, boolean result) {
        permissionCache.put(key, result);
    }

    public String getPlayerName(UUID playerUuid) {
        return playerNameCache.getIfPresent(playerUuid);
    }

    public void cachePlayerName(UUID playerUuid, String name) {
        playerNameCache.put(playerUuid, name);
    }

    public boolean needsDatabaseSync(int teamId) {
        Long lastSync = lastDatabaseSync.get(teamId);
        return lastSync == null || (System.currentTimeMillis() - lastSync) > syncCooldown;
    }

    public void markSynced(int teamId) {
        lastDatabaseSync.put(teamId, System.currentTimeMillis());
    }

    public void cleanup() {
        blacklistCache.cleanUp();
        joinRequestCache.cleanUp();
        permissionCache.cleanUp();
        playerNameCache.cleanUp();

        long cutoff = System.currentTimeMillis() - (syncCooldown * 2);
        lastDatabaseSync.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    public void invalidateAll() {
        blacklistCache.invalidateAll();
        joinRequestCache.invalidateAll();
        permissionCache.invalidateAll();
        playerNameCache.invalidateAll();
        lastDatabaseSync.clear();
    }

    public CacheStats getStats() {
        return new CacheStats(
            blacklistCache.size(),
            joinRequestCache.size(),
            permissionCache.size(),
            playerNameCache.size(),
            lastDatabaseSync.size()
        );
    }

    public static class CacheStats {
        public final long blacklistCacheSize;
        public final long joinRequestCacheSize;
        public final long permissionCacheSize;
        public final long playerNameCacheSize;
        public final int syncTrackingSize;

        public CacheStats(long blacklistCacheSize, long joinRequestCacheSize,
                         long permissionCacheSize, long playerNameCacheSize,
                         int syncTrackingSize) {
            this.blacklistCacheSize = blacklistCacheSize;
            this.joinRequestCacheSize = joinRequestCacheSize;
            this.permissionCacheSize = permissionCacheSize;
            this.playerNameCacheSize = playerNameCacheSize;
            this.syncTrackingSize = syncTrackingSize;
        }
    }
}
