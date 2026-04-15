package org.Textureless.kip.builders;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        subcommands().stream().flatMap(subcommand -> subcommand.literals().stream()).forEach(builder::then);
        return builder;
    }

    public final LiteralArgumentBuilder<CommandSourceStack> build() {
        return buildLiteral(name());
    }

    public final List<LiteralArgumentBuilder<CommandSourceStack>> literals() {
        return Stream.concat(Stream.of(name()), aliases().stream()).map(this::buildLiteral).toList();
    }

    public final List<LiteralCommandNode<CommandSourceStack>> nodes() {
        return literals().stream().map(LiteralArgumentBuilder::build).toList();
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
                this.description = normalizeText(description);
                return this;
            }

            public Builder permission(String permission) {
                this.permission = normalizeText(permission);
                return this;
            }

            public Builder aliases(String... aliases) {
                this.aliases = normalizeAliases(aliases);
                return this;
            }

            public CommandMeta build() {
                return new CommandMeta(name, description, permission, List.copyOf(aliases));
            }

            private static String normalizeText(String value) {
                return value == null ? "" : value;
            }

            private static List<String> normalizeAliases(String... aliases) {
                if (aliases == null) return List.of();
                LinkedHashSet<String> normalized = Arrays.stream(aliases).filter(alias -> alias != null && !alias.isBlank()).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
                return List.copyOf(normalized);
            }
        }
    }
}
