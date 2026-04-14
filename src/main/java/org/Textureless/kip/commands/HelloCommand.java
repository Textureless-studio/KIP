package org.Textureless.kip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.Textureless.kip.KIP;
import org.Textureless.kip.builders.KIPCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class HelloCommand extends KIPCommand {
    public HelloCommand() {
        super("hello", "Says hello to a player");
    }

    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal(this.getName())
                .then(Commands.argument("target", ArgumentTypes.player())
                        .executes(ctx -> {
                            final PlayerSelectorArgumentResolver playerSelector = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                            final Player targetPlayer = playerSelector.resolve(ctx.getSource()).getFirst();
                            final CommandSender sender = ctx.getSource().getSender();
                            final Plugin plugin = JavaPlugin.getPlugin(KIP.class);

                            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(Objects.requireNonNull(plugin.getConfig().getString("prefix")));

                            targetPlayer.sendMessage(component.append(Component.text(sender.getName() + " has said hello to you!").color(NamedTextColor.GREEN)));
                            sender.sendMessage(component.append(Component.text("You have said hello to " + targetPlayer.getName() + "!").color(NamedTextColor.GREEN)));

                            return Command.SINGLE_SUCCESS;
                        }));
    }
}
