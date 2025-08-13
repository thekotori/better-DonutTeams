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
import java.util.stream.Collectors;

public class Team implements InventoryHolder {

    private final int id;
    private String name;
    private String tag;
    private String description;
    private UUID ownerUuid;
    private Location homeLocation;
    private String homeServer;
    private boolean pvpEnabled;
    private double balance;
    private int kills;
    private int deaths;
    private final List<TeamPlayer> members;
    private Inventory enderChest;

    public Team(int id, String name, String tag, UUID ownerUuid, boolean pvpEnabled) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.pvpEnabled = pvpEnabled;
        this.members = new CopyOnWriteArrayList<>();
        this.balance = 0.0;
        this.description = "A new Team!";
        this.kills = 0;
        this.deaths = 0;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag != null ? tag : ""; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description != null ? description : "A new Team!"; }
    public void setDescription(String description) { this.description = description; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    public Location getHomeLocation() { return homeLocation; }
    public void setHomeLocation(Location homeLocation) { this.homeLocation = homeLocation; }
    public String getHomeServer() { return homeServer; }
    public void setHomeServer(String homeServer) { this.homeServer = homeServer; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public void addBalance(double amount) { this.balance += amount; }
    public void removeBalance(double amount) { this.balance -= amount; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void incrementKills() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void incrementDeaths() { this.deaths++; }
    public List<TeamPlayer> getMembers() { return members; }
    public Inventory getEnderChest() { return enderChest; }
    public void setEnderChest(Inventory enderChest) { this.enderChest = enderChest; }

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

    @NotNull
    @Override
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
            return comparator;
        }
    }
}