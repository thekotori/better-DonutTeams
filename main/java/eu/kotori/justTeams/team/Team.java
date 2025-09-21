package eu.kotori.justTeams.team;
import eu.kotori.justTeams.JustTeams;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
public class Team implements InventoryHolder {
    private final int id;
    private volatile String name;
    private volatile String tag;
    private volatile String description;
    private volatile UUID ownerUuid;
    private volatile Location homeLocation;
    private volatile String homeServer;
    private final AtomicBoolean pvpEnabled = new AtomicBoolean(false);
    private final AtomicBoolean isPublic = new AtomicBoolean(false);
    private final AtomicReference<Double> balance = new AtomicReference<>(0.0);
    private final AtomicInteger kills = new AtomicInteger(0);
    private final AtomicInteger deaths = new AtomicInteger(0);
    private final List<TeamPlayer> members;
    private Inventory enderChest;
    private final List<UUID> joinRequests;
    private final AtomicBoolean enderChestLock = new AtomicBoolean(false);
    private final List<UUID> enderChestViewers = new CopyOnWriteArrayList<>();
    private volatile SortType currentSortType = SortType.JOIN_DATE;
    public Team(int id, String name, String tag, UUID ownerUuid,
                boolean defaultPvpStatus, boolean defaultPublicStatus) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.pvpEnabled.set(defaultPvpStatus);
        this.isPublic.set(defaultPublicStatus);
        this.members = new CopyOnWriteArrayList<>();
        this.joinRequests = new CopyOnWriteArrayList<>();
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getTag() { return tag != null ? tag : ""; }
    public String getDescription() { return description != null ? description : "A new Team!"; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public Location getHomeLocation() { return homeLocation; }
    public String getHomeServer() { return homeServer; }
    public boolean isPvpEnabled() { return pvpEnabled.get(); }
    public boolean isPublic() { return isPublic.get(); }
    public double getBalance() { return balance.get(); }
    public void setBalance(double balance) { this.balance.set(balance); }
    public void addBalance(double amount) { this.balance.updateAndGet(current -> current + amount); }
    public void removeBalance(double amount) { this.balance.updateAndGet(current -> current - amount); }
    public int getKills() { return kills.get(); }
    public void setKills(int kills) { this.kills.set(kills); }
    public void incrementKills() { this.kills.incrementAndGet(); }
    public int getDeaths() { return deaths.get(); }
    public void setDeaths(int deaths) { this.deaths.set(deaths); }
    public void incrementDeaths() { this.deaths.incrementAndGet(); }
    public List<TeamPlayer> getMembers() { return members; }
    public Inventory getEnderChest() { return enderChest; }
    public void setEnderChest(Inventory enderChest) { this.enderChest = enderChest; }
    public List<UUID> getJoinRequests() { return joinRequests; }
    public boolean isEnderChestLocked() { return enderChestLock.get(); }
    public boolean tryLockEnderChest() { return enderChestLock.compareAndSet(false, true); }
    public void unlockEnderChest() { enderChestLock.set(false); }
    public List<UUID> getEnderChestViewers() { return enderChestViewers; }
    public void addEnderChestViewer(UUID playerUuid) {
        if (!enderChestViewers.contains(playerUuid)) {
            enderChestViewers.add(playerUuid);
        }
    }
    public void removeEnderChestViewer(UUID playerUuid) {
        enderChestViewers.remove(playerUuid);
    }
    public boolean hasEnderChestViewers() { return !enderChestViewers.isEmpty(); }
    public SortType getCurrentSortType() { return currentSortType; }
    public void setSortType(SortType sortType) { this.currentSortType = sortType; }
    public void cycleSortType() {
        SortType currentSort = getCurrentSortType();
        SortType newSort = switch (currentSort) {
            case JOIN_DATE -> SortType.ALPHABETICAL;
            case ALPHABETICAL -> SortType.ONLINE_STATUS;
            case ONLINE_STATUS -> SortType.JOIN_DATE;
        };
        setSortType(newSort);
    }
    public void addJoinRequest(UUID playerUuid) {
        if (!joinRequests.contains(playerUuid)) {
            joinRequests.add(playerUuid);
        }
    }
    public void removeJoinRequest(UUID playerUuid) {
        joinRequests.remove(playerUuid);
    }
    public List<TeamPlayer> getCoOwners() {
        return members.stream().filter(m -> m.getRole() == TeamRole.CO_OWNER).collect(Collectors.toList());
    }
    public List<TeamPlayer> getSortedMembers(SortType sortType) {
        return members.stream().sorted(sortType.getComparator()).collect(Collectors.toList());
    }
    public void addMember(TeamPlayer player) {
        this.members.add(player);
    }
    public void removeMember(UUID playerUuid) {
        this.members.removeIf(member -> member.getPlayerUuid().equals(playerUuid));
    }
    public boolean isMember(UUID playerUuid) {
        return members.stream().anyMatch(member -> member.getPlayerUuid().equals(playerUuid));
    }
    public boolean isOwner(UUID playerUuid) {
        return this.ownerUuid.equals(playerUuid);
    }
    public boolean hasElevatedPermissions(UUID playerUuid) {
        TeamPlayer member = getMember(playerUuid);
        if (member == null) return false;
        return member.getRole() == TeamRole.OWNER || member.getRole() == TeamRole.CO_OWNER;
    }
    public TeamPlayer getMember(UUID playerUuid) {
        return members.stream().filter(m -> m.getPlayerUuid().equals(playerUuid)).findFirst().orElse(null);
    }
    public void broadcast(String messageKey, TagResolver... resolvers) {
        members.forEach(member -> {
            if (member.isOnline()) {
                JustTeams.getInstance().getMessageManager().sendMessage(member.getBukkitPlayer(), messageKey, resolvers);
            }
        });
    }
    public void setName(String name) { this.name = name; }
    public void setTag(String tag) { this.tag = tag; }
    public String getColoredName() { return name; }
    public String getColoredTag() { return tag != null ? tag : ""; }
    public String getPlainName() { return stripColorCodes(name); }
    public String getPlainTag() { return stripColorCodes(tag != null ? tag : ""); }
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)&[0-9A-FK-OR]", "").replaceAll("(?i)<#[0-9A-F]{6}>", "").replaceAll("(?i)</#[0-9A-F]{6}>", "");
    }
    public void setDescription(String description) { this.description = description; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public void setHomeLocation(Location homeLocation) { this.homeLocation = homeLocation; }
    public void setHomeServer(String homeServer) { this.homeServer = homeServer; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled.set(pvpEnabled); }
    public void setPublic(boolean isPublic) { this.isPublic.set(isPublic); }
    public Inventory getInventory() {
        return this.enderChest;
    }
    public enum SortType {
        JOIN_DATE(Comparator.comparing(TeamPlayer::getJoinDate)),
        ALPHABETICAL(Comparator.comparing(p -> {
            String name = Bukkit.getOfflinePlayer(p.getPlayerUuid()).getName();
            return name != null ? name.toLowerCase() : "";
        })),
        ONLINE_STATUS(Comparator.comparing(TeamPlayer::isOnline).reversed());
        private final Comparator<TeamPlayer> comparator;
        SortType(Comparator<TeamPlayer> comparator) {
            this.comparator = comparator;
        }
        public Comparator<TeamPlayer> getComparator() {
            return this.comparator;
        }
    }
}
