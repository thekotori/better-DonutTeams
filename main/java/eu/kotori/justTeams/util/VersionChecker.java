package eu.kotori.justTeams.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.kotori.justTeams.JustTeams;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionChecker implements Listener {

    private final JustTeams plugin;

    public VersionChecker(JustTeams plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void check() {
        plugin.getTaskRunner().runAsync(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/kotorinet/BetterDonutTeams/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                    String latestTag = jsonObject.get("tag_name").getAsString();
                    String currentVersion = plugin.getDescription().getVersion();

                    if (!latestTag.equalsIgnoreCase(currentVersion)) {
                        plugin.updateAvailable = true;
                        plugin.latestVersion = latestTag;
                        plugin.getLogger().info("A new version is available: " + latestTag);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("justteams.admin") && plugin.updateAvailable) {
            plugin.getTaskRunner().runEntityTaskLater(player, () -> {
                plugin.getMessageManager().sendRawMessage(player,
                        "<strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</strikethrough>\n" +
                                " \n" +
                                "  <gradient:#4C9DDE:#4C96D2><bold>JustTeams</bold></gradient>\n" +
                                "  <gray>A new version is available: <green><latest_version></green>\n" +
                                "  <click:open_url:https://builtbybit.com/resources/better-donut-teams.71401/><yellow>Click here to download</click>\n" +
                                " \n" +
                                "<strikethrough><dark_gray>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯</strikethrough>",
                        Placeholder.unparsed("latest_version", plugin.latestVersion)
                );
            }, 60L);
        }
    }
}