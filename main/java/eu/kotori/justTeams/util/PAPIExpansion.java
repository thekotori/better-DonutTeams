package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import eu.kotori.justTeams.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.text.DecimalFormat;
public class PAPIExpansion extends PlaceholderExpansion {
    private final JustTeams plugin;
    private final DecimalFormat kdrFormat = new DecimalFormat("#.##");
    public PAPIExpansion(JustTeams plugin) {
        this.plugin = plugin;
    }
    public @NotNull String getIdentifier() {
        return "justteams";
    }
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    public boolean persist() {
        return true;
    }
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        try {
            if (params.equalsIgnoreCase("has_team")) {
                return plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null ? "yes" : "no";
            }
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (params.equalsIgnoreCase("display")) {
                if (team == null) {
                    return plugin.getGuiConfigManager().getPlaceholder("team_display.no_team", "<gray>No Team</gray>");
                } else {
                    String format = plugin.getGuiConfigManager().getPlaceholder("team_display.format", "<team_color><team_icon><team_tag></team_color>");
                    String teamIcon = plugin.getGuiConfigManager().getPlaceholder("team_display.team_icon", "⚔ ");
                    String teamColor = plugin.getGuiConfigManager().getPlaceholder("team_display.team_color", "#4C9DDE");
                    String tagPrefix = plugin.getGuiConfigManager().getPlaceholder("team_display.tag_prefix", "[");
                    String tagSuffix = plugin.getGuiConfigManager().getPlaceholder("team_display.tag_suffix", "]");
                    String tagColor = plugin.getGuiConfigManager().getPlaceholder("team_display.tag_color", "#FFD700");
                    boolean showIcon = plugin.getGuiConfigManager().getPlaceholder("team_display.show_icon", "true").equals("true");
                    boolean showTag = plugin.getGuiConfigManager().getPlaceholder("team_display.show_tag", "true").equals("true");
                    boolean showName = plugin.getGuiConfigManager().getPlaceholder("team_display.show_name", "false").equals("true");
                    String result = format;
                    result = result.replace("%team_name%", team.getColoredName());
                    result = result.replace("%team_tag%", showTag ? tagPrefix + team.getColoredTag() + tagSuffix : "");
                    result = result.replace("%team_color%", teamColor);
                    result = result.replace("%team_icon%", showIcon ? teamIcon : "");
                    if (showTag) {
                        result = result.replace(tagPrefix + team.getColoredTag() + tagSuffix,
                            "<" + tagColor + ">" + tagPrefix + team.getColoredTag() + tagSuffix + "</" + tagColor + ">");
                    }
                    return result;
                }
            }
            if (team == null) {
                return plugin.getMessageManager().getRawMessage("no_team_placeholder");
            }
            switch (params.toLowerCase()) {
                case "name":
                    return team.getName();
                case "tag":
                    return team.getTag();
                case "color_name":
                    return team.getColoredName();
                case "color_tag":
                    return team.getColoredTag();
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
                    var member = team.getMember(player.getUniqueId());
                    return member != null ? member.getRole().name() : "Unknown";
                case "kills":
                    return String.valueOf(team.getKills());
                case "deaths":
                    return String.valueOf(team.getDeaths());
                case "kdr":
                    if (team.getDeaths() == 0) return String.valueOf(team.getKills());
                    return kdrFormat.format((double) team.getKills() / team.getDeaths());
                case "bank_balance":
                    DecimalFormat formatter = new DecimalFormat(plugin.getConfigManager().getCurrencyFormat());
                    return formatter.format(team.getBalance());
                case "is_owner":
                    return team.getOwnerUuid().equals(player.getUniqueId()) ? "yes" : "no";
                case "is_co_owner":
                    var memberRole = team.getMember(player.getUniqueId());
                    return (memberRole != null && memberRole.getRole().name().equals("CO_OWNER")) ? "yes" : "no";
                case "is_member":
                    return team.getMember(player.getUniqueId()) != null ? "yes" : "no";
                case "team_size":
                    return String.valueOf(team.getMembers().size());
                case "team_capacity":
                    return String.valueOf(plugin.getConfigManager().getMaxTeamSize());
                case "team_full":
                    return team.getMembers().size() >= plugin.getConfigManager().getMaxTeamSize() ? "yes" : "no";
                case "pvp_enabled":
                    return team.isPvpEnabled() ? "yes" : "no";
                case "is_public":
                    return team.isPublic() ? "yes" : "no";
                case "plain_name":
                    return team.getPlainName();
                case "plain_tag":
                    return team.getPlainTag();
                case "join_date":
                    var memberInfo = team.getMember(player.getUniqueId());
                    if (memberInfo != null) {
                        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
                        return memberInfo.getJoinDate().atZone(java.time.ZoneId.systemDefault()).format(dateFormatter);
                    }
                    return "Unknown";
                case "created_at":
                    return "Unknown";
                default:
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing PlaceholderAPI request: " + params + " for player: " + player.getName() + " - " + e.getMessage());
            return "";
        }
    }
}
