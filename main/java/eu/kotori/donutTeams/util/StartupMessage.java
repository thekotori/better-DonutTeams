package eu.kotori.donutTeams.util;

import eu.kotori.donutTeams.DonutTeams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class StartupMessage {

    public static void send() {
        DonutTeams plugin = DonutTeams.getInstance();
        MiniMessage mm = plugin.getMiniMessage();
        CommandSender console = Bukkit.getConsoleSender();

        String mainColor = plugin.getConfigManager().getMainColor();
        String accentColor = plugin.getConfigManager().getAccentColor();
        String version = plugin.getDescription().getVersion();

        String check = "<gradient:" + accentColor + ":" + mainColor + ">✔</gradient>";
        String cross = "<gradient:" + mainColor + ":#ff3030>✖</gradient>";
        String pluginName = "ᴅᴏɴᴜᴛᴛᴇᴀᴍs";

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("version", version),
                Placeholder.unparsed("author", plugin.getDescription().getAuthors().get(0))
        );

        console.sendMessage(mm.deserialize("<dark_gray><strikethrough>                                                                                "));
        console.sendMessage(mm.deserialize(""));
        console.sendMessage(mm.deserialize("  <color:" + mainColor + ">█╗  ██╗   <white>" + pluginName + " <gray>v<version>", placeholders));
        console.sendMessage(mm.deserialize("  <color:" + mainColor + ">██║ ██╔╝   <gray>ʙʏ <white><author>", placeholders));
        console.sendMessage(mm.deserialize("  <color:" + mainColor + ">█████╔╝    <white>sᴛᴀᴛᴜs: " + check));
        console.sendMessage(mm.deserialize("  <color:" + accentColor + ">█╔═██╗    <white>sᴛᴏʀᴀɢᴇ: " + (plugin.getStorageManager().isConnected() ? check : cross)));
        console.sendMessage(mm.deserialize("  <color:" + accentColor + ">█║  ██╗   <white>ᴘᴀᴘɪ: " + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? check : cross)));
        console.sendMessage(mm.deserialize("  <color:" + accentColor + ">█║  ╚═╝"));
        console.sendMessage(mm.deserialize(""));
        console.sendMessage(mm.deserialize("<dark_gray><strikethrough>                                                                                "));
    }
}