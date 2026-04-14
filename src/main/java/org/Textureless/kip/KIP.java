package org.Textureless.kip;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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
        getComponentLogger().info("[KIP] Enabled");

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (KIPCommand command : commands) {
                event.registrar().register(command.toBrigadier().build(), command.getDescription());
            }
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
