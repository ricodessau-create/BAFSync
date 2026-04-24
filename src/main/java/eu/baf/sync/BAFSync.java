package eu.baf.sync;

import org.bukkit.plugin.java.JavaPlugin;

public class BAFSync extends JavaPlugin {

    private static BAFSync instance;

    @Override
    public void onEnable() {
        instance = this;

        getCommand("biersync").setExecutor(new SyncCommand());
        getLogger().info("BAF Sync Plugin aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BAF Sync Plugin deaktiviert.");
    }

    public static BAFSync getInstance() {
        return instance;
    }
}
