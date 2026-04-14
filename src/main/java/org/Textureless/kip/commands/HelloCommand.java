package org.Textureless.kip.commands;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.Textureless.kip.builders.KIPCommand;
import org.Textureless.kip.builders.KIPCommandBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HelloCommand extends KIPCommand {

    public HelloCommand() {
        super("hello", "Send a hello message to a player!");
    }

    @Override
    protected void configure(KIPCommandBuilder command) {
        command.requiresPermission("kip.hello")
                .argument("target", ArgumentTypes.player(), target -> target.executes(ctx -> {
                    PlayerSelectorArgumentResolver playerSelector = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                    Player targetPlayer = playerSelector.resolve(ctx.getSource()).getFirst();
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    targetPlayer.sendMessage(Component.text("Hello from " + sender.getName() + "!").color(NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Sent a hello message to " + targetPlayer.getName() + "!").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }));
    }
}

