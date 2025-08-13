package eu.kotori.justTeams.util;

import eu.kotori.justTeams.JustTeams;

import java.util.logging.Logger;

public class DebugLogger {

    private final JustTeams plugin;
    private final Logger logger;
    private boolean debugEnabled;

    public DebugLogger(JustTeams plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        this.debugEnabled = plugin.getConfigManager().isDebugEnabled();
    }

    public void log(String message) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + message);
        }
    }
}