package eu.kotori.donutTeams.team;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.config.ConfigManager;
import eu.kotori.donutTeams.config.MessageManager;
import eu.kotori.donutTeams.storage.IDataStorage;
import eu.kotori.donutTeams.util.EffectsUtil;
import eu.kotori.donutTeams.util.InventoryUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TeamManager {

    private final DonutTeams plugin;
    private final IDataStorage storage;
    private final MessageManager messageManager;
    private final ConfigManager configManager;
    private final Economy economy;

    private final Map<String, Team> teamNameCache = new ConcurrentHashMap<>();
    private final Map<UUID, Team> playerTeamCache = new ConcurrentHashMap<>();
    private final Cache<UUID, List<String>> teamInvites;
    private final Cache<UUID, Long> disbandConfirmations;
    private final Map<UUID, Instant> homeCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();

    public TeamManager(DonutTeams plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorageManager().getStorage();
        this.messageManager = plugin.getMessageManager();
        this.configManager = plugin.getConfigManager();
        this.economy = plugin.getEconomy();
        this.teamInvites = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        this.disbandConfirmations = CacheBuilder.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
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
        loadEnderChest(team);
        teamNameCache.put(lowerCaseName, team);
        team.getMembers().forEach(member -> playerTeamCache.put(member.getPlayerUuid(), team));
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
                saveEnderChest(team);
                teamNameCache.remove(team.getName().toLowerCase());
            }
        }
    }

    public void loadPlayerTeam(Player player) {
        if (playerTeamCache.containsKey(player.getUniqueId())) {
            return;
        }
        storage.findTeamByPlayer(player.getUniqueId()).ifPresent(this::loadTeamIntoCache);
    }

    private void uncacheTeam(Team team) {
        saveEnderChest(team);
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
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (name.length() < configManager.getMinNameLength()) {
            messageManager.sendMessage(owner, "name_too_short", Placeholder.unparsed("min_length", String.valueOf(configManager.getMinNameLength())));
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (name.length() > configManager.getMaxNameLength()) {
            messageManager.sendMessage(owner, "name_too_long", Placeholder.unparsed("max_length", String.valueOf(configManager.getMaxNameLength())));
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            messageManager.sendMessage(owner, "name_invalid");
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (tag.length() > configManager.getMaxTagLength() || !tag.matches("[a-zA-Z0-9]+")) {
            messageManager.sendMessage(owner, "tag_invalid");
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (storage.findTeamByName(name).isPresent() || teamNameCache.containsKey(name.toLowerCase())) {
            messageManager.sendMessage(owner, "team_name_exists", Placeholder.unparsed("team", name));
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }

        boolean defaultPvp = configManager.getDefaultPvpStatus();
        storage.createTeam(name, tag, owner.getUniqueId(), defaultPvp).ifPresent(team -> {
            loadTeamIntoCache(team);
            messageManager.sendMessage(owner, "team_created", Placeholder.unparsed("team", team.getName()));
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.SUCCESS);
        });
    }

    public void disbandTeam(Player owner, boolean confirmed) {
        Team team = getPlayerTeam(owner.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(owner, "player_not_in_team");
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (!team.isOwner(owner.getUniqueId())) {
            messageManager.sendMessage(owner, "not_owner");
            EffectsUtil.playSound(owner, EffectsUtil.SoundType.ERROR);
            return;
        }

        if (!confirmed) {
            disbandConfirmations.put(owner.getUniqueId(), System.currentTimeMillis());
            messageManager.sendMessage(owner, "disband_confirm");
            return;
        }

        if (disbandConfirmations.getIfPresent(owner.getUniqueId()) == null) {
            messageManager.sendMessage(owner, "disband_confirm");
            return;
        }

        disbandConfirmations.invalidate(owner.getUniqueId());
        storage.deleteTeam(team.getId());
        team.broadcast("team_disbanded_broadcast", Placeholder.unparsed("team", team.getName()));
        uncacheTeam(team);
        messageManager.sendMessage(owner, "team_disbanded");
        EffectsUtil.playSound(owner, EffectsUtil.SoundType.SUCCESS);
    }

    public void invitePlayer(Player inviter, Player target) {
        Team team = getPlayerTeam(inviter.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(inviter, "player_not_in_team");
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (!team.hasElevatedPermissions(inviter.getUniqueId())) {
            messageManager.sendMessage(inviter, "must_be_owner_or_co_owner");
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            messageManager.sendMessage(inviter, "invite_self");
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (getPlayerTeam(target.getUniqueId()) != null) {
            messageManager.sendMessage(inviter, "target_already_in_team", Placeholder.unparsed("target", target.getName()));
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (team.getMembers().size() >= configManager.getMaxTeamSize()) {
            messageManager.sendMessage(inviter, "team_is_full");
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
            return;
        }

        List<String> invites = teamInvites.getIfPresent(target.getUniqueId());
        if (invites != null && invites.contains(team.getName().toLowerCase())) {
            messageManager.sendMessage(inviter, "invite_spam");
            EffectsUtil.playSound(inviter, EffectsUtil.SoundType.ERROR);
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
        EffectsUtil.playSound(inviter, EffectsUtil.SoundType.SUCCESS);
        EffectsUtil.playSound(target, EffectsUtil.SoundType.SUCCESS);
    }

    public void acceptInvite(Player player, String teamName) {
        if (getPlayerTeam(player.getUniqueId()) != null) {
            messageManager.sendMessage(player, "already_in_team");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }

        List<String> invites = teamInvites.getIfPresent(player.getUniqueId());
        if (invites == null || !invites.contains(teamName.toLowerCase())) {
            messageManager.sendMessage(player, "no_pending_invite");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }

        Team team = getTeamByName(teamName);
        if (team == null) {
            messageManager.sendMessage(player, "team_not_found");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (team.getMembers().size() >= configManager.getMaxTeamSize()) {
            messageManager.sendMessage(player, "team_is_full");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }

        invites.remove(teamName.toLowerCase());
        if (invites.isEmpty()) {
            teamInvites.invalidate(player.getUniqueId());
        }

        storage.addMemberToTeam(team.getId(), player.getUniqueId());

        team.addMember(new TeamPlayer(player.getUniqueId(), TeamRole.MEMBER, Instant.now(), false, true));
        playerTeamCache.put(player.getUniqueId(), team);

        messageManager.sendMessage(player, "invite_accepted", Placeholder.unparsed("team", team.getName()));
        team.broadcast("invite_accepted_broadcast", Placeholder.unparsed("player", player.getName()));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void denyInvite(Player player, String teamName) {
        List<String> invites = teamInvites.getIfPresent(player.getUniqueId());
        if (invites == null || !invites.contains(teamName.toLowerCase())) {
            messageManager.sendMessage(player, "no_pending_invite");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }

        invites.remove(teamName.toLowerCase());
        if (invites.isEmpty()) {
            teamInvites.invalidate(player.getUniqueId());
        }

        messageManager.sendMessage(player, "invite_denied", Placeholder.unparsed("team", teamName));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);

        Team team = getTeamByName(teamName);
        if (team != null) {
            team.getMembers().stream()
                    .filter(member -> team.hasElevatedPermissions(member.getPlayerUuid()) && member.isOnline())
                    .forEach(privilegedMember -> {
                        messageManager.sendMessage(privilegedMember.getBukkitPlayer(), "invite_denied_broadcast", Placeholder.unparsed("player", player.getName()));
                        EffectsUtil.playSound(privilegedMember.getBukkitPlayer(), EffectsUtil.SoundType.ERROR);
                    });
        }
    }

    public void leaveTeam(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (team.isOwner(player.getUniqueId())) {
            messageManager.sendMessage(player, "owner_must_disband");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }

        storage.removeMemberFromTeam(player.getUniqueId());
        team.removeMember(player.getUniqueId());
        playerTeamCache.remove(player.getUniqueId());

        messageManager.sendMessage(player, "you_left_team", Placeholder.unparsed("team", team.getName()));
        team.broadcast("player_left_broadcast", Placeholder.unparsed("player", player.getName()));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void kickPlayer(Player kicker, UUID targetUuid) {
        Team team = getPlayerTeam(kicker.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(kicker, "player_not_in_team");
            EffectsUtil.playSound(kicker, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (!team.hasElevatedPermissions(kicker.getUniqueId())) {
            messageManager.sendMessage(kicker, "must_be_owner_or_co_owner");
            EffectsUtil.playSound(kicker, EffectsUtil.SoundType.ERROR);
            return;
        }

        TeamPlayer targetMember = team.getMember(targetUuid);
        if (targetMember == null) {
            String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
            messageManager.sendMessage(kicker, "target_not_in_your_team", Placeholder.unparsed("target", targetName != null ? targetName : "Unknown"));
            EffectsUtil.playSound(kicker, EffectsUtil.SoundType.ERROR);
            return;
        }

        if (targetMember.getRole() == TeamRole.OWNER) {
            messageManager.sendMessage(kicker, "cannot_kick_owner");
            EffectsUtil.playSound(kicker, EffectsUtil.SoundType.ERROR);
            return;
        }

        if (targetMember.getRole() == TeamRole.CO_OWNER && !team.isOwner(kicker.getUniqueId())) {
            messageManager.sendMessage(kicker, "cannot_kick_co_owner");
            EffectsUtil.playSound(kicker, EffectsUtil.SoundType.ERROR);
            return;
        }

        storage.removeMemberFromTeam(targetUuid);
        team.removeMember(targetUuid);
        playerTeamCache.remove(targetUuid);

        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        String finalTargetName = targetName != null ? targetName : "Unknown";

        messageManager.sendMessage(kicker, "player_kicked", Placeholder.unparsed("target", finalTargetName));
        team.broadcast("player_left_broadcast", Placeholder.unparsed("player", finalTargetName));
        EffectsUtil.playSound(kicker, EffectsUtil.SoundType.SUCCESS);

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null) {
            messageManager.sendMessage(targetPlayer, "you_were_kicked", Placeholder.unparsed("team", team.getName()));
            EffectsUtil.playSound(targetPlayer, EffectsUtil.SoundType.ERROR);
        }
    }

    public void promotePlayer(Player promoter, UUID targetUuid) {
        Team team = getPlayerTeam(promoter.getUniqueId());
        if (team == null || !team.isOwner(promoter.getUniqueId())) {
            messageManager.sendMessage(promoter, "not_owner");
            EffectsUtil.playSound(promoter, EffectsUtil.SoundType.ERROR);
            return;
        }

        TeamPlayer target = team.getMember(targetUuid);
        if (target == null) {
            messageManager.sendMessage(promoter, "target_not_in_your_team", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
            return;
        }

        if (target.getRole() == TeamRole.CO_OWNER) {
            messageManager.sendMessage(promoter, "already_that_role", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
            return;
        }

        if (target.getRole() == TeamRole.OWNER) {
            messageManager.sendMessage(promoter, "cannot_promote_owner");
            return;
        }

        target.setRole(TeamRole.CO_OWNER);
        storage.updateMemberRole(team.getId(), targetUuid, TeamRole.CO_OWNER);
        team.broadcast("player_promoted", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
        EffectsUtil.playSound(promoter, EffectsUtil.SoundType.SUCCESS);
    }

    public void demotePlayer(Player demoter, UUID targetUuid) {
        Team team = getPlayerTeam(demoter.getUniqueId());
        if (team == null || !team.isOwner(demoter.getUniqueId())) {
            messageManager.sendMessage(demoter, "not_owner");
            EffectsUtil.playSound(demoter, EffectsUtil.SoundType.ERROR);
            return;
        }

        TeamPlayer target = team.getMember(targetUuid);
        if (target == null) {
            messageManager.sendMessage(demoter, "target_not_in_your_team", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
            return;
        }

        if (target.getRole() == TeamRole.MEMBER) {
            messageManager.sendMessage(demoter, "already_that_role", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
            return;
        }

        if (target.getRole() == TeamRole.OWNER) {
            messageManager.sendMessage(demoter, "cannot_demote_owner");
            return;
        }

        target.setRole(TeamRole.MEMBER);
        storage.updateMemberRole(team.getId(), targetUuid, TeamRole.MEMBER);
        team.broadcast("player_demoted", Placeholder.unparsed("target", Bukkit.getOfflinePlayer(targetUuid).getName()));
        EffectsUtil.playSound(demoter, EffectsUtil.SoundType.SUCCESS);
    }

    public void setTeamTag(Player player, String newTag) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.hasElevatedPermissions(player.getUniqueId())) {
            messageManager.sendMessage(player, "must_be_owner_or_co_owner");
            return;
        }
        if (newTag.length() > configManager.getMaxTagLength() || !newTag.matches("[a-zA-Z0-9]+")) {
            messageManager.sendMessage(player, "tag_invalid");
            return;
        }
        team.setTag(newTag);
        storage.setTeamTag(team.getId(), newTag);
        messageManager.sendMessage(player, "tag_set", Placeholder.unparsed("tag", newTag));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void setTeamDescription(Player player, String newDescription) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.hasElevatedPermissions(player.getUniqueId())) {
            messageManager.sendMessage(player, "must_be_owner_or_co_owner");
            return;
        }
        if (newDescription.length() > configManager.getMaxDescriptionLength()) {
            messageManager.sendMessage(player, "description_too_long", Placeholder.unparsed("max_length", String.valueOf(configManager.getMaxDescriptionLength())));
            return;
        }
        team.setDescription(newDescription);
        storage.setTeamDescription(team.getId(), newDescription);
        messageManager.sendMessage(player, "description_set");
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void transferOwnership(Player oldOwner, UUID newOwnerUuid) {
        Team team = getPlayerTeam(oldOwner.getUniqueId());
        if (team == null || !team.isOwner(oldOwner.getUniqueId())) {
            messageManager.sendMessage(oldOwner, "not_owner");
            EffectsUtil.playSound(oldOwner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (oldOwner.getUniqueId().equals(newOwnerUuid)) {
            messageManager.sendMessage(oldOwner, "cannot_transfer_to_self");
            EffectsUtil.playSound(oldOwner, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (!team.isMember(newOwnerUuid)) {
            String targetName = Bukkit.getOfflinePlayer(newOwnerUuid).getName();
            messageManager.sendMessage(oldOwner, "target_not_in_your_team", Placeholder.unparsed("target", targetName != null ? targetName : "Unknown"));
            EffectsUtil.playSound(oldOwner, EffectsUtil.SoundType.ERROR);
            return;
        }

        storage.transferOwnership(team.getId(), newOwnerUuid, oldOwner.getUniqueId());
        team.setOwnerUuid(newOwnerUuid);

        TeamPlayer newOwnerMember = team.getMember(newOwnerUuid);
        if (newOwnerMember != null) {
            newOwnerMember.setRole(TeamRole.OWNER);
            newOwnerMember.setCanWithdraw(true);
        }
        TeamPlayer oldOwnerMember = team.getMember(oldOwner.getUniqueId());
        if (oldOwnerMember != null) {
            oldOwnerMember.setRole(TeamRole.MEMBER);
        }

        Player newOwnerPlayer = Bukkit.getPlayer(newOwnerUuid);
        String newOwnerName = newOwnerPlayer != null ? newOwnerPlayer.getName() : Bukkit.getOfflinePlayer(newOwnerUuid).getName();

        messageManager.sendMessage(oldOwner, "transfer_success", Placeholder.unparsed("player", newOwnerName));
        team.broadcast("transfer_broadcast", Placeholder.unparsed("owner", oldOwner.getName()), Placeholder.unparsed("player", newOwnerName));
        EffectsUtil.playSound(oldOwner, EffectsUtil.SoundType.SUCCESS);
    }

    public void togglePvp(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (!team.hasElevatedPermissions(player.getUniqueId())) {
            messageManager.sendMessage(player, "must_be_owner_or_co_owner");
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
        if (!team.hasElevatedPermissions(player.getUniqueId())) {
            messageManager.sendMessage(player, "must_be_owner_or_co_owner");
            return;
        }
        Location home = player.getLocation();
        team.setHomeLocation(home);
        storage.setTeamHome(team.getId(), home);
        messageManager.sendMessage(player, "home_set");
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
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
            teleportPlayer(player, team.getHomeLocation());
            setCooldown(player);
            return;
        }

        Location startLocation = player.getLocation();

        BukkitTask task = plugin.getTaskRunner().runTaskTimer(new Runnable() {
            int countdown = warmup;

            @Override
            public void run() {
                if (!player.isOnline() || player.getLocation().distanceSquared(startLocation) > 1) {
                    if(player.isOnline()) {
                        messageManager.sendMessage(player, "teleport_moved");
                        EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                    }
                    teleportTasks.get(player.getUniqueId()).cancel();
                    return;
                }

                if (countdown > 0) {
                    messageManager.sendMessage(player, "teleport_warmup", Placeholder.unparsed("seconds", String.valueOf(countdown)));
                    EffectsUtil.spawnParticles(player.getLocation().add(0, 1, 0), Particle.valueOf(configManager.getWarmupParticle()), 10);
                    countdown--;
                } else {
                    teleportPlayer(player, team.getHomeLocation());
                    setCooldown(player);
                    teleportTasks.get(player.getUniqueId()).cancel();
                }
            }
        }, 0L, 20L);

        teleportTasks.put(player.getUniqueId(), task);
    }

    private void teleportPlayer(Player player, Location location) {
        plugin.getTaskRunner().runAtLocation(location, () -> {
            player.teleportAsync(location).thenAccept(success -> {
                if(success) {
                    messageManager.sendMessage(player, "teleport_success");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.TELEPORT);
                    EffectsUtil.spawnParticles(player.getLocation(), Particle.valueOf(configManager.getSuccessParticle()), 30);
                } else {
                    messageManager.sendMessage(player, "teleport_moved");
                    EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
                }
            });
        });
    }

    private void setCooldown(Player player) {
        int cooldownSeconds = plugin.getConfig().getInt("team_home.cooldown_seconds", 300);
        if (cooldownSeconds > 0) {
            homeCooldowns.put(player.getUniqueId(), Instant.now().plusSeconds(cooldownSeconds));
        }
    }

    public void deposit(Player player, double amount) {
        if (economy == null) {
            messageManager.sendMessage(player, "economy_not_found");
            return;
        }
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        if (amount <= 0) {
            messageManager.sendMessage(player, "bank_invalid_amount");
            return;
        }
        if (economy.getBalance(player) < amount) {
            messageManager.sendMessage(player, "player_insufficient_funds");
            return;
        }
        double maxBalance = configManager.getMaxBankBalance();
        if (maxBalance != -1 && team.getBalance() + amount > maxBalance) {
            messageManager.sendMessage(player, "bank_max_balance_reached");
            return;
        }

        economy.withdrawPlayer(player, amount);
        team.addBalance(amount);
        storage.updateTeamBalance(team.getId(), team.getBalance());
        messageManager.sendMessage(player, "bank_deposit_success", Placeholder.unparsed("amount", String.format("%,.2f", amount)));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void withdraw(Player player, double amount) {
        if (economy == null) {
            messageManager.sendMessage(player, "economy_not_found");
            return;
        }
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        TeamPlayer member = team.getMember(player.getUniqueId());
        if (member == null || !member.canWithdraw()) {
            messageManager.sendMessage(player, "no_permission");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }
        if (amount <= 0) {
            messageManager.sendMessage(player, "bank_invalid_amount");
            return;
        }
        if (team.getBalance() < amount) {
            messageManager.sendMessage(player, "bank_insufficient_funds");
            return;
        }

        team.removeBalance(amount);
        economy.depositPlayer(player, amount);
        storage.updateTeamBalance(team.getId(), team.getBalance());
        messageManager.sendMessage(player, "bank_withdraw_success", Placeholder.unparsed("amount", String.format("%,.2f", amount)));
        EffectsUtil.playSound(player, EffectsUtil.SoundType.SUCCESS);
    }

    public void openEnderChest(Player player) {
        Team team = getPlayerTeam(player.getUniqueId());
        if (team == null) {
            messageManager.sendMessage(player, "player_not_in_team");
            return;
        }
        TeamPlayer member = team.getMember(player.getUniqueId());
        if (member == null || !member.canUseEnderChest()) {
            messageManager.sendMessage(player, "no_permission");
            EffectsUtil.playSound(player, EffectsUtil.SoundType.ERROR);
            return;
        }
        player.openInventory(team.getEnderChest());
    }

    private void loadEnderChest(Team team) {
        int rows = configManager.getEnderChestRows();
        Inventory enderChest = Bukkit.createInventory(team, rows * 9, Component.text("ᴛᴇᴀᴍ ᴇɴᴅᴇʀ ᴄʜᴇsᴛ"));
        String data = storage.getEnderChest(team.getId());
        if (data != null && !data.isEmpty()) {
            try {
                InventoryUtil.deserializeInventory(enderChest, data);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not deserialize ender chest for team " + team.getName() + ": " + e.getMessage());
            }
        }
        team.setEnderChest(enderChest);
    }

    public void saveEnderChest(Team team) {
        if (team == null || team.getEnderChest() == null) return;
        try {
            String data = InventoryUtil.serializeInventory(team.getEnderChest());
            storage.saveEnderChest(team.getId(), data);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save ender chest for team " + team.getName() + ": " + e.getMessage());
        }
    }

    public void saveAllOnlineTeamEnderChests() {
        teamNameCache.values().forEach(this::saveEnderChest);
    }

    public void updateMemberPermissions(Player owner, UUID targetUuid, boolean canWithdraw, boolean canUseEnderChest) {
        Team team = getPlayerTeam(owner.getUniqueId());
        if (team == null || !team.isOwner(owner.getUniqueId())) {
            messageManager.sendMessage(owner, "not_owner");
            return;
        }
        TeamPlayer member = team.getMember(targetUuid);
        if (member == null) {
            return;
        }
        member.setCanWithdraw(canWithdraw);
        member.setCanUseEnderChest(canUseEnderChest);
        storage.updateMemberPermissions(team.getId(), targetUuid, canWithdraw, canUseEnderChest);
    }
}