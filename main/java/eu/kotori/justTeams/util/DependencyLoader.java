package eu.kotori.justTeams.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

public class DependencyLoader {

    public static void loadDependencies(JavaPlugin plugin) {
        File libsFolder = new File(plugin.getDataFolder(), "libs");

        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
            plugin.getLogger().warning("Created 'libs' folder. Please add the required dependency JARs there and restart the server.");
            return;
        }

        File[] libFiles = libsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (libFiles == null || libFiles.length == 0) {
            plugin.getLogger().severe("No dependency JARs found in the 'libs' folder. This plugin will not function correctly. Please download the dependencies.zip and extract its contents into the libs folder.");
            return;
        }

        ClassLoader classLoader = plugin.getClass().getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            plugin.getLogger().severe("Unsupported ClassLoader. Cannot load dependencies from 'libs' folder.");
            return;
        }

        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        Method addURLMethod;
        try {
            addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURLMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get addURL method from URLClassLoader.", e);
            return;
        }

        for (File libFile : libFiles) {
            try {
                URL url = libFile.toURI().toURL();
                addURLMethod.invoke(urlClassLoader, url);
                plugin.getLogger().info("Loaded dependency: " + libFile.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load dependency: " + libFile.getName(), e);
            }
        }
    }
}