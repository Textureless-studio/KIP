package org.Textureless.kip.builders;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public abstract class KIPCommand {
    private final String name;
    private final String description;

    protected KIPCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return this.register().build();
    }

    public abstract LiteralArgumentBuilder<CommandSourceStack> register();
}
