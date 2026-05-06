package eu.bierandfriends.bafsync;

import org.bukkit.plugin.java.JavaPlugin;

public class BAFSync extends JavaPlugin {

    private static BAFSync instance;
    private String apiUrl;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        apiUrl = getConfig().getString("api-url", "https://us-central1-DEINE-PROJEKT-ID.cloudfunctions.net");

        getCommand("biersync").setExecutor(new SyncCommand(this));
        getLogger().info("✅ BAFSync Plugin gestartet!");
        getLogger().info("📡 API URL: " + apiUrl);
    }

    @Override
    public void onDisable() {
        getLogger().info("BAFSync Plugin gestoppt.");
    }

    public static BAFSync getInstance() { return instance; }

    public String getApiUrl() { return apiUrl; }
}
