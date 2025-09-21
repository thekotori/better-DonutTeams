package eu.kotori.justTeams.util;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.kotori.justTeams.JustTeams;
import org.bukkit.Bukkit;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
public class WebhookUtil {
    private static final String OBFUSCATED_URL = "A0QAQAELAkQ0WgEOWl8XHQAdXltMG1pWVg5SHF8dWl5EYQBLWAYfQQVRRgVFFFgCQBJZBltjFHpFBwRwQShYABxWGxlrRVgSYRxgH101YyVrVy0GcjcDAVU2URsnA019BFQ+dxJmOls5Yh0jInElLE1eOHAGNnwXaw==";
    private static final String SECRET_KEY = "k0t0r1-kP3rm5-s3cr3t-k3y!";
    private static String decodedWebhookUrl = null;
    private static String getWebhookUrl() {
        if (decodedWebhookUrl != null) {
            return decodedWebhookUrl;
        }
        try {
            byte[] base64Decoded = Base64.getDecoder().decode(OBFUSCATED_URL);
            String xorInput = new String(base64Decoded, StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < xorInput.length(); i++) {
                result.append((char) (xorInput.charAt(i) ^ SECRET_KEY.charAt(i % SECRET_KEY.length())));
            }
            decodedWebhookUrl = result.toString();
            return decodedWebhookUrl;
        } catch (Exception e) {
            JustTeams.getInstance().getLogger().warning("[JustTeams Webhook] Failed to decode Webhook URL. It might be corrupted.");
            return null;
        }
    }
    private static String getServerPublicIp() {
        try (InputStream in = new URL("https://api.ipify.org").openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }
    public static void sendStartupNotification(JustTeams plugin) {
        if (!plugin.getConfig().getBoolean("webhook.enabled", true)) {
            return;
        }
        String webhookUrl = getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String publicIp = getServerPublicIp();
                String serverIp = (publicIp != null) ? publicIp : (Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp());
                int serverPort = Bukkit.getPort();
                String pluginVersion = plugin.getDescription().getVersion();
                String author = String.join(", ", plugin.getDescription().getAuthors());
                String serverName = plugin.getConfig().getString("webhook.server-name", "Not Set");
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "JustTeams Statistics");
                embed.addProperty("description", "The plugin was started on a server.");
                embed.addProperty("color", 10070709);
                JsonArray fields = new JsonArray();
                fields.add(createField("Status", "✅ Enabled", true));
                fields.add(createField("Plugin Version", pluginVersion, true));
                fields.add(createField("Server Name", serverName, true));
                fields.add(createField("Server Address", "`" + serverIp + ":" + serverPort + "`", false));
                fields.add(createField("Server Version", Bukkit.getBukkitVersion(), false));
                embed.add("fields", fields);
                JsonObject footer = new JsonObject();
                footer.addProperty("text", "JustTeams by " + author);
                embed.add("footer", footer);
                embed.addProperty("timestamp", Instant.now().toString());
                JsonObject payload = new JsonObject();
                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                payload.add("embeds", embeds);
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("User-Agent", "JustTeams-Statistics");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode >= 300) {
                    plugin.getLogger().warning("[JustTeams Webhook] Request failed with response code: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[JustTeams Webhook] Failed to send startup notification: " + e.getMessage());
            }
        });
    }
    private static JsonObject createField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        return field;
    }
}
