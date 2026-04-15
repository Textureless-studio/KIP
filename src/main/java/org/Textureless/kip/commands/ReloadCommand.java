package org.Textureless.kip.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.Textureless.kip.KIP;
import org.Textureless.kip.builders.KIPCommand;
import org.Textureless.kip.utils.PrefixUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadCommand extends KIPCommand {
    public ReloadCommand() {
        super(
                meta("reload")
                        .description("Reloads KIP configuration")
                        .permission("kip.command.reload")
                        .aliases("rl")
                        .build()
        );
    }

    @Override
    protected void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(ctx -> {
            Component prefix = PrefixUtil.prefix(JavaPlugin.getPlugin(KIP.class));

            ctx.getSource().getSender().sendMessage(
                    prefix.append(Component.text("Reloading...").color(NamedTextColor.YELLOW))
            );

            return Command.SINGLE_SUCCESS;
        });
    }
}

