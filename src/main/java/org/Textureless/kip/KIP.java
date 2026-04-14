package org.Textureless.kip;

import org.bukkit.plugin.java.JavaPlugin;

public final class KIP extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info("[KIP] Enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
