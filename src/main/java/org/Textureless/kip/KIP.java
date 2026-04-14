package org.Textureless.kip;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.Textureless.kip.builders.KIPCommand;
import org.Textureless.kip.commands.HelloCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class KIP extends JavaPlugin {
    private final List<KIPCommand> commands = List.of(
            new HelloCommand()
    );

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveResource("profiles.yml", false);
        saveDefaultConfig();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (KIPCommand command : commands) {
                event.registrar().register(command.build(), command.getDescription());
            }
        });

        getComponentLogger().info(Component.text("KIP has been enabled!").color(NamedTextColor.GREEN));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getComponentLogger().info(Component.text("KIP has been disabled!").color(NamedTextColor.RED));
    }

}
