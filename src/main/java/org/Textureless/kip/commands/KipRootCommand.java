package org.Textureless.kip.commands;

import org.Textureless.kip.builders.KIPCommand;

import java.util.List;

public class KipRootCommand extends KIPCommand {
    public KipRootCommand() {
        super(
                meta("kip")
                        .description("Main KIP command")
                        .build()
        );
    }

    @Override
    protected List<KIPCommand> subcommands() {
        return List.of(new ReloadCommand());
    }
}

