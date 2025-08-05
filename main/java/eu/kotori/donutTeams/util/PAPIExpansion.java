package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import eu.kotori.donutTeams.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class PAPIExpansion extends PlaceholderExpansion {

    private final DonutTeams plugin;
    private final DecimalFormat kdrFormat = new DecimalFormat("#.##");

    public PAPIExpansion(DonutTeams plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "donutteams";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            return plugin.getMessageManager().getRawMessage("no_team_placeholder");
        }

        switch (params.toLowerCase()) {
            case "name":
                return team.getName();
            case "tag":
                return team.getTag();
            case "description":
                return team.getDescription();
            case "owner":
                return plugin.getServer().getOfflinePlayer(team.getOwnerUuid()).getName();
            case "member_count":
                return String.valueOf(team.getMembers().size());
            case "max_members":
                return String.valueOf(plugin.getConfigManager().getMaxTeamSize());
            case "members_online":
                return String.valueOf(team.getMembers().stream().filter(p -> p.isOnline()).count());
            case "role":
                return team.getMember(player.getUniqueId()).getRole().name();
            case "kills":
                return String.valueOf(team.getKills());
            case "deaths":
                return String.valueOf(team.getDeaths());
            case "kdr":
                if (team.getDeaths() == 0) return String.valueOf(team.getKills());
                return kdrFormat.format((double) team.getKills() / team.getDeaths());
            case "bank_balance":
                return String.format("%,.2f", team.getBalance());
            default:
                return null;
        }
    }
}