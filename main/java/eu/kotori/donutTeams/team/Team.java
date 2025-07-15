package eu.kotori.donutTeams.team;

import eu.kotori.donutTeams.DonutTeams;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Team {

    private final int id;
    private String name;
    private String tag;
    private UUID ownerUuid;
    private Location homeLocation;
    private boolean pvpEnabled;
    private final List<TeamPlayer> members;

    public Team(int id, String name, String tag, UUID ownerUuid, boolean pvpEnabled) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.pvpEnabled = pvpEnabled;
        this.members = new CopyOnWriteArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public Location getHomeLocation() {
        return homeLocation;
    }

    public void setHomeLocation(Location homeLocation) {
        this.homeLocation = homeLocation;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public List<TeamPlayer> getMembers() {
        return members;
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

    public void broadcast(String messageKey, TagResolver... resolvers) {
        members.forEach(member -> {
            if (member.isOnline()) {
                DonutTeams.getInstance().getMessageManager().sendMessage(member.getBukkitPlayer(), messageKey, resolvers);
            }
        });
    }

    public enum SortType {
        JOIN_DATE(Comparator.comparing(TeamPlayer::getJoinDate)),
        ALPHABETICAL(Comparator.comparing(p -> Bukkit.getOfflinePlayer(p.getPlayerUuid()).getName() != null ? Bukkit.getOfflinePlayer(p.getPlayerUuid()).getName().toLowerCase() : "")),
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