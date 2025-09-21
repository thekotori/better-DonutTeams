package eu.kotori.justTeams.util;
import eu.kotori.justTeams.JustTeams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
public class StartupMessage {
    public static void send() {
        JustTeams plugin = JustTeams.getInstance();
        MiniMessage mm = plugin.getMiniMessage();
        CommandSender console = Bukkit.getConsoleSender();
        PluginManager pm = Bukkit.getPluginManager();
        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();
        String version = plugin.getDescription().getVersion();
        String author = plugin.getDescription().getAuthors().get(0);
        String check = "<gradient:#00ff88:#00cc66><bold>✓</bold></gradient>";
        String cross = "<gradient:#ff4444:#cc0000><bold>✗</bold></gradient>";
        String warning = "<gradient:#ffaa00:#ff6600><bold>⚠</bold></gradient>";
        String info = "<gradient:#4C9DDE:#4C96D2><bold>◆</bold></gradient>";
        String star = "<gradient:#ffdd00:#ffaa00><bold>✦</bold></gradient>";
        String diamond = "<gradient:#00ffff:#0088ff><bold>◆</bold></gradient>";
        String crown = "<gradient:#ffd700:#ffaa00><bold>♔</bold></gradient>";
        String sparkle = "<gradient:#ffffff:#ffdd00><bold>✨</bold></gradient>";
        String pluginName = "ᴊᴜsᴛᴛᴇᴀᴍs";
        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("version", version),
                Placeholder.unparsed("author", author)
        );
        String line = "<dark_gray>────────────────────────────────────────────────────────────────────────────────";
        String storageType = plugin.getConfig().getString("storage.type", "H2").toUpperCase();
        boolean storageConnected = plugin.getStorageManager().isConnected();
        boolean vaultEnabled = pm.isPluginEnabled("Vault");
        boolean papiEnabled = pm.isPluginEnabled("PlaceholderAPI");
        boolean pvpManagerEnabled = pm.isPluginEnabled("PvPManager");
        String storageHook = "    <dark_gray>║ <white>Storage <gray>(" + storageType + "): " + (storageConnected ? check : cross) + " <gray>" + (storageConnected ? "Connected" : "Disconnected") + "\n";
        String vaultHook = "    <dark_gray>║ <white>Vault: " + (vaultEnabled ? check : cross) + " <gray>" + (vaultEnabled ? "Economy Ready" : "Not Found") + "\n";
        String papiHook = "    <dark_gray>║ <white>PlaceholderAPI: " + (papiEnabled ? check : cross) + " <gray>" + (papiEnabled ? "16 Placeholders" : "Not Found") + "\n";
        String pvpManagerHook = "    <dark_gray>║ <white>PvPManager: " + (pvpManagerEnabled ? check : cross) + " <gray>" + (pvpManagerEnabled ? "PvP Control" : "Not Found") + "\n";
        String serverName = Bukkit.getServer().getName();
        String serverVersion = Bukkit.getVersion();
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        int cpuCores = Runtime.getRuntime().availableProcessors();
        String javaVendor = System.getProperty("java.vendor");
        String javaVersionShort = System.getProperty("java.version").split("\\.")[0];
        String startupBlock =
                line + "\n" +
                        " \n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>█╗  ██╗   <white>" + pluginName + " <gray>v<version></bold></gradient>\n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>██║ ██╔╝   <gray>ʙʏ <white><author></bold></gradient>\n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>█████╔╝    <white>sᴛᴀᴛᴜs: " + check + " <green>ʀᴇᴀᴅʏ</bold></gradient>\n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>█╔═██╗    <white>ᴘʟᴀᴛғᴏʀᴍ: <#00DFFB>" + serverName + "</bold></gradient>\n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>█║  ██╗    <white>ᴊᴀᴠᴀ: <#00DFFB>" + javaVendor + " " + javaVersionShort + "</bold></gradient>\n" +
                        "  <gradient:#4C9DDE:#4C96D2><bold>█║  ╚═╝    <white>ᴍɪɴᴇᴄʀᴀғᴛ: <#00DFFB>" + serverVersion + "</bold></gradient>\n" +
                        " \n" +
                        "  " + info + " <white>System: <#00DFFB>" + osName + " <gray>(" + osArch + ")</gray>  <white>|</white>  <white>Memory: <#00DFFB>" + totalMemory + "MB/" + maxMemory + "MB</#00DFFB></white>  <white>|</white>  <white>CPU: <#00DFFB>" + cpuCores + "</#00DFFB></white>\n" +
                        "  " + info + " <white>Storage: <#00DFFB>" + storageType + "</#00DFFB></white>  <white>|</white>  <white>Vault: " + (vaultEnabled ? check : cross) + "</white>  <white>|</white>  <white>PAPI: " + (papiEnabled ? check : cross) + "</white>  <white>|</white>  <white>PvPManager: " + (pvpManagerEnabled ? check : cross) + "</white>\n" +
                        "  " + info + " <white>Commands: <#00DFFB>/team</#00DFFB></white>  <white>|</white>  <white><#00DFFB>/teammsg</#00DFFB></white>  <white>|</white>  <white><#00DFFB>/team help</#00DFFB></white>  <white>|</white>  <white>Max Teams: <#00DFFB>" + plugin.getConfig().getInt("team.max-members", 10) + "</#00DFFB></white>\n" +
                        " \n" +
                        "  <gradient:#00ff88:#00cc66><bold>✦ Plugin Successfully Loaded ✦</bold></gradient>\n" +
                        " \n" +
                        line;
        console.sendMessage(mm.deserialize(startupBlock, placeholders));
    }
}
