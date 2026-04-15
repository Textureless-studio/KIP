package org.Textureless.kip.builders;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class KIPCommand {
    private final CommandMeta meta;

    protected KIPCommand(CommandMeta meta) {
        this.meta = meta;
    }

    public final String name() {
        return meta.name();
    }

    public final String description() {
        return meta.description();
    }

    public final String permission() {
        return meta.permission();
    }

    public final List<String> aliases() {
        return meta.aliases();
    }

    protected final LiteralArgumentBuilder<CommandSourceStack> literal(String literalName) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literalName);
        if (!permission().isBlank()) {
            builder.requires(source -> source.getSender().hasPermission(permission()));
        }
        return builder;
    }

    protected void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
    }

    protected List<KIPCommand> subcommands() {
        return List.of();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildLiteral(String literalName) {
        LiteralArgumentBuilder<CommandSourceStack> builder = literal(literalName);
        configure(builder);
        for (KIPCommand subcommand : subcommands()) {
            for (LiteralArgumentBuilder<CommandSourceStack> childLiteral : subcommand.literals()) {
                builder.then(childLiteral);
            }
        }
        return builder;
    }

    public final LiteralArgumentBuilder<CommandSourceStack> build() {
        return buildLiteral(name());
    }

    public final List<LiteralArgumentBuilder<CommandSourceStack>> literals() {
        List<LiteralArgumentBuilder<CommandSourceStack>> builders = new ArrayList<>();
        builders.add(buildLiteral(name()));
        for (String alias : aliases()) {
            builders.add(buildLiteral(alias));
        }
        return List.copyOf(builders);
    }

    public final List<LiteralCommandNode<CommandSourceStack>> nodes() {
        List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        for (LiteralArgumentBuilder<CommandSourceStack> literal : literals()) {
            nodes.add(literal.build());
        }
        return List.copyOf(nodes);
    }

    protected static CommandMeta.Builder meta(String name) {
        return CommandMeta.builder(name);
    }

    public record CommandMeta(String name, String description, String permission, List<String> aliases) {
        public static Builder builder(String name) {
            return new Builder(name);
        }

        public static final class Builder {
            private final String name;
            private String description = "";
            private String permission = "";
            private List<String> aliases = List.of();

            private Builder(String name) {
                this.name = name;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }

            public Builder aliases(String... aliases) {
                this.aliases = Arrays.stream(aliases)
                        .filter(alias -> alias != null && !alias.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
                return this;
            }

            public CommandMeta build() {
                return new CommandMeta(
                        name,
                        description == null ? "" : description,
                        permission == null ? "" : permission,
                        List.copyOf(aliases)
                );
            }
        }
    }
}
