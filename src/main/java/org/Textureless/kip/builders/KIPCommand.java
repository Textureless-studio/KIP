package org.Textureless.kip.builders;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public abstract class KIPCommand {
    private final String name;
    private final String description;

    protected KIPCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected abstract void configure(KIPCommandBuilder command);

    public final LiteralArgumentBuilder<CommandSourceStack> toBrigadier() {
        KIPCommandBuilder builder = KIPCommandBuilder.command(name, description);
        configure(builder);
        return builder.toBrigadier();
    }

    public final String getDescription() {
        return description;
    }
}
