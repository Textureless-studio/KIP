package org.Textureless.kip.builders;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class KIPCommandBuilder {
    private final String name;
    private final String description;
    private String permission;
    private Command<CommandSourceStack> executor;
    private final List<NodeBuilder<?>> children = new ArrayList<>();

    private KIPCommandBuilder(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static KIPCommandBuilder command(String name, String description) {
        return new KIPCommandBuilder(name, description);
    }

    public KIPCommandBuilder requiresPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public KIPCommandBuilder executes(Command<CommandSourceStack> executor) {
        this.executor = executor;
        return this;
    }

    public KIPCommandBuilder literal(String literal, Consumer<LiteralNodeBuilder> consumer) {
        LiteralNodeBuilder child = new LiteralNodeBuilder(literal);
        consumer.accept(child);
        children.add(child);
        return this;
    }

    public <T> KIPCommandBuilder argument(String argumentName, ArgumentType<T> type, Consumer<ArgumentNodeBuilder<T>> consumer) {
        ArgumentNodeBuilder<T> child = new ArgumentNodeBuilder<>(argumentName, type);
        consumer.accept(child);
        children.add(child);
        return this;
    }

    public LiteralArgumentBuilder<CommandSourceStack> toBrigadier() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name);
        applyNodeConfiguration(root, permission, executor, children);
        return root;
    }

    public String getDescription() {
        return description;
    }

    private static void applyNodeConfiguration(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            String permission,
            Command<CommandSourceStack> executor,
            List<NodeBuilder<?>> children
    ) {
        if (permission != null && !permission.isBlank()) {
            builder.requires(source -> source.getSender().hasPermission(permission));
        }

        if (executor != null) {
            builder.executes(executor);
        }

        for (NodeBuilder<?> child : children) {
            builder.then(child.toBrigadierNode());
        }
    }

    public abstract static class NodeBuilder<T extends NodeBuilder<T>> {
        private String permission;
        private Command<CommandSourceStack> executor;
        private final List<NodeBuilder<?>> children = new ArrayList<>();

        protected abstract T self();

        public T requiresPermission(String permission) {
            this.permission = permission;
            return self();
        }

        public T executes(Command<CommandSourceStack> executor) {
            this.executor = executor;
            return self();
        }

        public T literal(String literal, Consumer<LiteralNodeBuilder> consumer) {
            LiteralNodeBuilder child = new LiteralNodeBuilder(literal);
            consumer.accept(child);
            children.add(child);
            return self();
        }

        public <A> T argument(String argumentName, ArgumentType<A> type, Consumer<ArgumentNodeBuilder<A>> consumer) {
            ArgumentNodeBuilder<A> child = new ArgumentNodeBuilder<>(argumentName, type);
            consumer.accept(child);
            children.add(child);
            return self();
        }

        protected final void apply(ArgumentBuilder<CommandSourceStack, ?> builder) {
            applyNodeConfiguration(builder, permission, executor, children);
        }

        protected abstract ArgumentBuilder<CommandSourceStack, ?> toBrigadierNode();
    }

    public static final class LiteralNodeBuilder extends NodeBuilder<LiteralNodeBuilder> {
        private final String literal;

        private LiteralNodeBuilder(String literal) {
            this.literal = literal;
        }

        @Override
        protected LiteralNodeBuilder self() {
            return this;
        }

        @Override
        protected ArgumentBuilder<CommandSourceStack, ?> toBrigadierNode() {
            LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(literal);
            apply(node);
            return node;
        }
    }

    public static final class ArgumentNodeBuilder<T> extends NodeBuilder<ArgumentNodeBuilder<T>> {
        private final String argumentName;
        private final ArgumentType<T> argumentType;

        private ArgumentNodeBuilder(String argumentName, ArgumentType<T> argumentType) {
            this.argumentName = argumentName;
            this.argumentType = argumentType;
        }

        @Override
        protected ArgumentNodeBuilder<T> self() {
            return this;
        }

        @Override
        protected ArgumentBuilder<CommandSourceStack, ?> toBrigadierNode() {
            RequiredArgumentBuilder<CommandSourceStack, T> node = Commands.argument(argumentName, argumentType);
            apply(node);
            return node;
        }
    }
}

