package eu.kotori.donutTeams.team;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.config.ConfigManager;
import eu.kotori.donutTeams.config.MessageManager;
import eu.kotori.donutTeams.storage.IDataStorage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TeamManager {

    private final DonutTeams plugin;
    private final IDataStorage storage;
    private final MessageManager messageManager;
    private final ConfigManager configManager;

    private final Map<String, Team> teamNameCache = new ConcurrentHashMap<>();
    private final Map<UUID, Team> playerTeamCache = new ConcurrentHashMap<>();
    private final Cache<UUID, List<String>> teamInvites;
    private final Map<UUID, Instant> homeCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();

    public TeamManager(DonutTeams plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorageManager().getStorage();
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
        this.teamInvites = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    public void loadPlayerTeam(Player player) {
        if (playerTeamCache.containsKey(player.getUniqueId())) {
            return;
        }
        storage.findTeamByPlayer(player.getUniqueId()).ifPresent(this::loadTeamIntoCache);
    }

    public void unloadPlayer(Player player) {
        if (teleportTasks.containsKey(player.getUniqueId())) {
            teleportTasks.get(player.getUniqueId()).cancel();
        }
        Team team = getPlayerTeam(player.getUniqueId());
        if (team != null) {
            playerTeamCache.remove(player.getUniqueId());
            boolean isTeamEmptyOnline = team.getMembers().stream()
                    .allMatch(member -> member.getPlayerUuid().equals(player.getUniqueId()) || !member.isOnline());
            if (isTeamEmptyOnline) {
                teamNameCache.remove(team.getName().toLowerCase());
            }
        }
    }

    private void loadTeamIntoCache(Team team) {
        String lowerCaseName = team.getName().toLowerCase();
        if (teamNameCache.containsKey(lowerCaseName)) {
            Team cachedTeam = teamNameCache.get(lowerCaseName);
            cachedTeam.getMembers().forEach(member -> playerTeamCache.put(member.getPlayerUuid(), cachedTeam));
            return;
        }

        team.getMembers().clear();
        team.getMembers().addAll(storage.getTeamMembers(team.getId()));
        teamNameCache.put(lowerCaseName, team);
        team.getMembers().forEach(member -> playerTeamCache.put(member.getPlayerUuid(), team));
    }

    private void uncacheTeam(Team team) {
        teamNameCache.remove(team.getName().toLowerCase());
        team.getMembers().forEach(member -> playerTeamCache.remove(member.getPlayerUuid()));
    }

    public Team getPlayerTeam(UUID playerUuid) {
        return playerTeamCache.get(playerUuid);
    }

    public Team getTeamByName(String name) {
        Team cachedTeam = teamNameCache.get(name.toLowerCase());
        if (cachedTeam != null) {
            return cachedTeam;
        }

        Optional<Team> dbTeam = storage.findTeamByName(name);
        dbTeam.ifPresent(this::loadTeamIntoCache);
        return dbTeam.orElse(null);
    }

    public void createTeam(Player owner, String name, String tag) {
        if (getPlayerTeam(owner.getUniqueId()) != null) {
            messageManager.sendMessage(owner, "already_in_team");
            return;
        }
        if (name.length() < configManager.getMinNameLength()) {
            messageManager.sendMessage(owner, "name_too_short", Placeholder.unparsed("min_length", String.valueOf(configManager.getMinNameLength())));
            return;
        }
        if (name.length() > configManager.getMaxNameLength()) {
            messageManager.sendMessage(owner, "name_too_long", Placeholder.unparsed("max_length", String.valueOf(configManager.getMaxNameLength())));
            return;
        }
        if (tag.length() > configManager.getMaxTagLength() || !tag.matches("[a-zA-Z0-9]+")) {
            messageManager.sendMessage(owner, "tag_invalid");
            return;
        }
        if (storage.findTeamByName(name).isPresent() || teamNameCache.containsKey(name.toLowerCase())) {
            messageManager.sendMessage(owner, "team_name_exists", Placeholder.unparsed("team", name));
            return;
        }

        boolean defaultPvp = configManager.getDefaultPvpStatus();
        storage.createTeam(name, tag, owner.getUniqueId(), defaultPvp).ifPresent(team -> {
            loadTeamIntoCache(team);
            messageManager.sendMessage(owner, "team_created", Placeholder.unparsed("team", team.getName()));
        });
    }

    public void disbandTeam(Player owner) {
        Team team = getPlayerTeam(owner.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(owner, "player_not_in_team");
            return;
        }
        if (!team.isOwner(owner.getUniqueId())) {
            messageManager.sendMessage(owner, "not_owner");
            return;
        }

        storage.deleteTeam(team.getId());
        team.broadcast("team_disbanded_broadcast", Placeholder.unparsed("team", team.getName()));
        uncacheTeam(team);
        messageManager.sendMessage(owner, "team_disbanded");
    }

    public void invitePlayer(Player inviter, Player target) {
        Team team = getPlayerTeam(inviter.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(inviter, "player_not_in_team");
            return;
        }
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            messageManager.sendMessage(inviter, "invite_self");
            return;
        }
        if (getPlayerTeam(target.getUniqueId()) != null) {
            messageManager.sendMessage(inviter, "target_already_in_team", Placeholder.unparsed("target", target.getName()));
            return;
        }
        if (team.getMembers().size() >= configManager.getMaxTeamSize()) {
            messageManager.sendMessage(inviter, "team_is_full");
            return;
        }

        List<String> invites = teamInvites.getIfPresent(target.getUniqueId());
        if (invites != null && invites.contains(team.getName().toLowerCase())) {
            messageManager.sendMessage(inviter, "invite_spam");
            return;
        }

        if (invites == null) {
            invites = new ArrayList<>();
        }
        invites.add(team.getName().toLowerCase());
        teamInvites.put(target.getUniqueId(), invites);

        messageManager.sendMessage(inviter, "invite_sent", Placeholder.unparsed("target", target.getName()));
        messageManager.sendRawMessage(target, messageManager.getRawMessage("prefix") + messageManager.getRawMessage("invite_received")
                .replace("<team>", team.getName()));
    }

    public void acceptInvite(Player player, String teamName) {
        if (getPlayerTeam(player.getUniqueId()) != null) {
            messageManager.sendMessage(player, "already_in_team");
            return;
        }

        List<String> invites = teamInvites.getIfPresent(player.getUniqueId());
        if (invites == null || !invites.contains(teamName.toLowerCase())) {
            messageManager.sendMessage(player, "no_pending_invite");
            return;
        }

        Team team = getTeamByName(teamName);
        if (team == null) {
            messageManager.sendMessage(player, "team_not_found");
            return;
        }
        if (team.getMembers().size() >= configManager.getMaxTeamSize()) {
            messageManager.sendMessage(player, "team_is_full");
            return;
        }

        invites.remove(teamName.toLowerCase());
        if (invites.isEmpty()) {
            teamInvites.invalidate(player.getUniqueId());
        }

        storage.addMemberToTeam(team.getId(), player.getUniqueId());

        team.addMember(new TeamPlayer(player.getUniqueId(), TeamRole.MEMBER, Instant.now()));
        playerTeamCache.put(player.getUniqueId(), team);

        messageManager.sendMessage(player, "invite_accepted", Placeholder.unparsed("team", team.getName()));
        team.broadcast("invite_accepted_broadcast", Placeholder.unparsed("player", player.getName()));
    }

    public void denyInvite(Player player, String teamName) {
        List<String> invites = teamInvites.getIfPresent(player.getUniqueId());
        if (invites == null || !invites.contains(teamName.toLowerCase())) {
            messageManager.sendMessage(player, "no_pending_invite");
            return;
        }

        invites.remove(teamName.toLowerCase());
        if (invites.isEmpty()) {
            teamInvites.invalidate(player.getUniqueId());
        }

        messageManager.sendMessage(player, "invite_denied", Placeholder.unparsed("team", teamName));

        Team team = getTeamByName(teamName);
        if (team != null) {
            Player owner = Bukkit.getPlayer(team.getOwnerUuid());
            if (owner != null) {
                messageManager.sendMessage(owner, "invite_denied_broadcast", Placeholder.unparsed("player", player.getName()));
            }
        }
    }

    public void leaveTeam(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (team.isOwner(player.getUniqueId())) {
            messageManager.sendMessage(player, "owner_must_disband");
            return;
        }

        storage.removeMemberFromTeam(player.getUniqueId());
        team.removeMember(player.getUniqueId());
        playerTeamCache.remove(player.getUniqueId());

        messageManager.sendMessage(player, "you_left_team", Placeholder.unparsed("team", team.getName()));
        team.broadcast("player_left_broadcast", Placeholder.unparsed("player", player.getName()));
    }

    public void kickPlayer(Player kicker, UUID targetUuid) {
        Team team = getPlayerTeam(kicker.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(kicker, "player_not_in_team");
            return;
        }
        if (!team.isOwner(kicker.getUniqueId())) {
            messageManager.sendMessage(kicker, "not_owner");
            return;
        }
        if (kicker.getUniqueId().equals(targetUuid)) {
            messageManager.sendMessage(kicker, "cannot_kick_owner");
            return;
        }
        if (!team.isMember(targetUuid)) {
            String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
            messageManager.sendMessage(kicker, "target_not_in_your_team", Placeholder.unparsed("target", targetName != null ? targetName : "Unknown"));
            return;
        }

        storage.removeMemberFromTeam(targetUuid);
        team.removeMember(targetUuid);
        playerTeamCache.remove(targetUuid);

        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        messageManager.sendMessage(kicker, "player_kicked", Placeholder.unparsed("target", targetName != null ? targetName : "Unknown"));
        team.broadcast("player_left_broadcast", Placeholder.unparsed("player", targetName != null ? targetName : "Unknown"));

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            messageManager.sendMessage(targetPlayer, "you_were_kicked", Placeholder.unparsed("team", team.getName()));
        }
    }

    public void setTeamTag(Player player, String newTag) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.isOwner(player.getUniqueId())) {
            messageManager.sendMessage(player, "not_owner");
            return;
        }
        if (newTag.length() > configManager.getMaxTagLength() || !newTag.matches("[a-zA-Z0-9]+")) {
            messageManager.sendMessage(player, "tag_invalid");
            return;
        }
        team.setTag(newTag);
        storage.setTeamTag(team.getId(), newTag);
        messageManager.sendMessage(player, "tag_set", Placeholder.unparsed("tag", newTag));
    }

    public void transferOwnership(Player oldOwner, UUID newOwnerUuid) {
        Team team = getPlayerTeam(oldOwner.getUniqueId());
        if (team == null || !team.isOwner(oldOwner.getUniqueId())) {
            messageManager.sendMessage(oldOwner, "not_owner");
            return;
        }
        if (oldOwner.getUniqueId().equals(newOwnerUuid)) {
            messageManager.sendMessage(oldOwner, "cannot_transfer_to_self");
            return;
        }
        if (!team.isMember(newOwnerUuid)) {
            String targetName = Bukkit.getOfflinePlayer(newOwnerUuid).getName();
            messageManager.sendMessage(oldOwner, "target_not_in_your_team", Placeholder.unparsed("target", targetName != null ? targetName : "Unknown"));
            return;
        }

        storage.transferOwnership(team.getId(), newOwnerUuid, oldOwner.getUniqueId());
        team.setOwnerUuid(newOwnerUuid);

        team.getMembers().stream().filter(p -> p.getPlayerUuid().equals(newOwnerUuid)).findFirst().ifPresent(p -> p.setRole(TeamRole.OWNER));
        team.getMembers().stream().filter(p -> p.getPlayerUuid().equals(oldOwner.getUniqueId())).findFirst().ifPresent(p -> p.setRole(TeamRole.MEMBER));

        Player newOwnerPlayer = Bukkit.getPlayer(newOwnerUuid);
        String newOwnerName = newOwnerPlayer != null ? newOwnerPlayer.getName() : Bukkit.getOfflinePlayer(newOwnerUuid).getName();

        messageManager.sendMessage(oldOwner, "transfer_success", Placeholder.unparsed("player", newOwnerName));
        team.broadcast("transfer_broadcast", Placeholder.unparsed("owner", oldOwner.getName()), Placeholder.unparsed("player", newOwnerName));
    }

    public void togglePvp(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.isOwner(player.getUniqueId())) {
            messageManager.sendMessage(player, "not_owner");
            return;
        }
        boolean newStatus = !team.isPvpEnabled();
        team.setPvpEnabled(newStatus);
        storage.setPvpStatus(team.getId(), newStatus);
        if (newStatus) {
            team.broadcast("team_pvp_enabled");
        } else {
            team.broadcast("team_pvp_disabled");
        }
    }

    public void setTeamHome(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.isOwner(player.getUniqueId())) {
            messageManager.sendMessage(player, "not_owner");
            return;
        }
        Location home = player.getLocation();
        team.setHomeLocation(home);
        storage.setTeamHome(team.getId(), home);
        messageManager.sendMessage(player, "home_set");
    }

    public void teleportToHome(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (team.getHomeLocation() == null) {
            messageManager.sendMessage(player, "home_not_set");
            return;
        }

        if (homeCooldowns.containsKey(player.getUniqueId())) {
            Instant cooldownEnd = homeCooldowns.get(player.getUniqueId());
            if (Instant.now().isBefore(cooldownEnd)) {
                long secondsLeft = Duration.between(Instant.now(), cooldownEnd).toSeconds();
                messageManager.sendMessage(player, "teleport_cooldown", Placeholder.unparsed("time", secondsLeft + "s"));
                return;
            }
        }

        int warmup = plugin.getConfig().getInt("team_home.warmup_seconds", 5);
        if (warmup <= 0) {
            player.teleport(team.getHomeLocation());
            messageManager.sendMessage(player, "teleport_success");
            setCooldown(player);
            return;
        }

        Location startLocation = player.getLocation();

        BukkitTask task = new BukkitRunnable() {
            int countdown = warmup;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (player.getLocation().distanceSquared(startLocation) > 1) {
                    messageManager.sendMessage(player, "teleport_moved");
                    cancel();
                    return;
                }
                if (countdown > 0) {
                    messageManager.sendMessage(player, "teleport_warmup", Placeholder.unparsed("seconds", String.valueOf(countdown)));
                    countdown--;
                } else {
                    player.teleport(team.getHomeLocation());
                    messageManager.sendMessage(player, "teleport_success");
                    setCooldown(player);
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                teleportTasks.remove(player.getUniqueId());
            }
        }.runTaskTimer(plugin, 0L, 20L);
        teleportTasks.put(player.getUniqueId(), task);
    }

    private void setCooldown(Player player) {
        int cooldownSeconds = plugin.getConfig().getInt("team_home.cooldown_seconds", 300);
        if (cooldownSeconds > 0) {
            homeCooldowns.put(player.getUniqueId(), Instant.now().plusSeconds(cooldownSeconds));
        }
    }
}