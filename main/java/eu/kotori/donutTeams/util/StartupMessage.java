package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

public class StartupMessage {

    public static void send() {
        DonutTeams plugin = DonutTeams.getInstance();
        MiniMessage mm = plugin.getMiniMessage();
        CommandSender console = Bukkit.getConsoleSender();
        PluginManager pm = Bukkit.getPluginManager();

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();
        String version = plugin.getDescription().getVersion();
        String author = plugin.getDescription().getAuthors().get(0);

        String check = "<gradient:" + accentColor + ":" + mainColor + ">✔</gradient>";
        String cross = "<gradient:" + mainColor + ":#ff3030>✖</gradient>";
        String info = "<gradient:" + mainColor + ":" + accentColor + ">●</gradient>";
        String pluginName = "ᴅᴏɴᴜᴛᴛᴇᴀᴍs";

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("version", version),
                Placeholder.unparsed("author", author)
        );

        String line = "<dark_gray><strikethrough>                                                                                ";

        String storageHook = "    <gray>- Storage (" + plugin.getConfig().getString("storage.type", "H2").toUpperCase() + "): " + (plugin.getStorageManager().isConnected() ? check : cross) + "\n";
        String vaultHook = "    <gray>- Vault: " + (plugin.getEconomy() != null ? check : cross) + "\n";
        String papiHook = "    <gray>- PlaceholderAPI: " + (pm.isPluginEnabled("PlaceholderAPI") ? check : cross) + "\n";
        String pvpManagerHook = "    <gray>- PvPManager: " + (plugin.getPvpManager() != null ? check : cross) + "\n";

        String startupBlock =
                line + "\n" +
                        " \n" +
                        "  <color:" + mainColor + ">█╗  ██╗   <white>" + pluginName + " <gray>v<version>\n" +
                        "  <color:" + mainColor + ">██║ ██╔╝   <gray>ʙʏ <white><author>\n" +
                        "  <color:" + mainColor + ">█████╔╝    <white>sᴛᴀᴛᴜs: " + check + "\n" +
                        "  <color:" + accentColor + ">█╔═██╗\n" +
                        "  <color:" + accentColor + ">█║  ██╗\n" +
                        "  <color:" + accentColor + ">█║  ╚═╝\n" +
                        " \n" +
                        "  " + info + " <white>Server Platform: <#00DFFB>" + (DonutTeams.isFolia() ? "Folia" : "Paper/Spigot") + "\n" +
                        "  " + info + " <white>Dependency Hooks:\n" +
                        storageHook +
                        vaultHook +
                        papiHook +
                        pvpManagerHook +
                        " \n" +
                        line;

        console.sendMessage(mm.deserialize(startupBlock, placeholders));
    }
}