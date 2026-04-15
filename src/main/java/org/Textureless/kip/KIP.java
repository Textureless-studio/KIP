package org.Textureless.kip;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.Textureless.kip.commands.KipRootCommand;
import org.Textureless.kip.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class KIP extends JavaPlugin {
    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveResource("profiles.yml", false);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        KipRootCommand rootCommand = new KipRootCommand();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            for (LiteralCommandNode<CommandSourceStack> rootNode : rootCommand.nodes()) {
                event.registrar().register(rootNode, rootCommand.description());
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
