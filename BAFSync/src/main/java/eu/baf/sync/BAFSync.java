package eu.baf.sync;

import org.bukkit.plugin.java.JavaPlugin;

public class BAFSync extends JavaPlugin {

    private static BAFSync instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getCommand("biersync").setExecutor(new SyncCommand());

        getLogger().info("╔══════════════════════════════╗");
        getLogger().info("║    BAF Sync Plugin v1.0.0    ║");
        getLogger().info("║  BierAndFriends Account Sync ║");
        getLogger().info("╚══════════════════════════════╝");
        getLogger().info("Plugin erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BAF Sync Plugin deaktiviert.");
    }

    public static BAFSync getInstance() {
        return instance;
    }

    public String getApiUrl() {
        return getConfig().getString("api-url", "http://localhost:3000/sync");
    }
}
