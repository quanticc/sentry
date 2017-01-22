package top.quantic.sentry.discord.module;

import top.quantic.sentry.discord.core.Command;

import java.util.List;

public interface CommandSupplier {

    /**
     * Provide a list of commands that can be attached to any Discord client.
     *
     * @return a list of commands
     */
    List<Command> getCommands();
}
