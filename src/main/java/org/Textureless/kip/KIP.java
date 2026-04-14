package org.Textureless.kip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class KIP extends JavaPlugin {

    @Override
    public void onEnable() {
       saveResource("config.yml", false);
       saveDefaultConfig();

        getComponentLogger().info(Component.text("KIP has been enabled!").color(NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getComponentLogger().info(Component.text("KIP has been disabled!").color(NamedTextColor.RED));
    }
}
