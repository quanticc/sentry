package top.quantic.sentry.discord.command;

import com.google.common.collect.Sets;
import joptsimple.OptionParser;
import sx.blah.discord.handle.obj.Permissions;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Constructs Commands in a builder pattern fashion.
 */
public class CommandBuilder {

    private String name;
    private Set<String> aliases = new HashSet<>();
    private String category = "";
    private String description = "";
    private OptionParser parser;
    private Map<String, String> parameterAliases;
    private EnumSet<Permissions> requiredPermissions = EnumSet.noneOf(Permissions.class);
    private boolean secured = false;
    private boolean deleteRequest = false;
    private boolean preserveQuotes = false;
    private int argumentLimit = 0;
    private Consumer<CommandContext> onExecute = c -> {
    };
    private Consumer<CommandContext> onBotDenied = c -> {
    };
    private Consumer<CommandContext> onAuthorDenied = c -> {
    };

    public static CommandBuilder of(String name) {
        return new CommandBuilder()
            .name(name);
    }

    public static CommandBuilder of(String name, String... aliases) {
        return new CommandBuilder()
            .name(name)
            .aliasedBy(Sets.newHashSet(aliases));
    }

    public CommandBuilder name(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        this.name = name;
        return this;
    }

    /**
     * Defines a set of aliases for this command.
     *
     * @param aliases a set of string aliases
     * @return this builder
     */
    public CommandBuilder aliasedBy(Set<String> aliases) {
        requireNonNull(aliases, "Alias set must not be null");
        this.aliases = aliases;
        return this;
    }

    /**
     * Sets the category of this command.
     *
     * @param category category of this command
     * @return this builder
     */
    public CommandBuilder in(String category) {
        this.category = category == null ? "" : category;
        return this;
    }

    /**
     * Sets the description for this command.
     *
     * @param description description of this command
     * @return this builder
     */
    public CommandBuilder describedAs(String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    /**
     * Set the jopt-simple OptionParser to use for parsing this command's arguments.
     *
     * @param parser the parser for this command
     * @return this builder
     */
    public CommandBuilder parsedBy(OptionParser parser) {
        this.parser = parser;
        this.parser.acceptsAll(asList("?", "h", "help"), "Display the help").forHelp();
        this.parser.allowsUnrecognizedOptions();
        parameterAliases = new HashMap<>();
        parser.recognizedOptions().keySet().stream()
            .filter(k -> k.length() > 1)
            .forEach(k -> parameterAliases.put(k, "--" + k));
        return this;
    }

    /**
     * Do not use a OptionParser for parsing this command's arguments.
     *
     * @return this builder
     */
    public CommandBuilder nonParsed() {
        this.parser = null;
        return this;
    }

    /**
     * Requires a set of Discord4J permissions from the calling user in order to execute this command.
     *
     * @param requiredPermissions a set of permissions
     * @return this builder
     */
    public CommandBuilder requires(EnumSet<Permissions> requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
        return this;
    }

    /**
     * Requires an explicit internal permission to use this command.
     *
     * @return this builder
     */
    public CommandBuilder secured() {
        this.secured = true;
        return this;
    }

    /**
     * Delete the message that invoked this command after its execution.
     *
     * @return this builder
     */
    public CommandBuilder deleteRequest() {
        this.deleteRequest = true;
        return this;
    }

    /**
     * Keep the quotes the user might introduce to separate individual arguments.
     *
     * @return this builder
     */
    public CommandBuilder preserveQuotes() {
        this.preserveQuotes = true;
        return this;
    }

    /**
     * Parse only the specified amount of arguments, grouping the following ones in the last.
     *
     * @param argumentLimit number of arguments to parse
     * @return this builder
     */
    public CommandBuilder argumentLimit(int argumentLimit) {
        this.argumentLimit = Math.max(0, argumentLimit);
        return this;
    }

    /**
     * Defines the action to follow upon command execution.
     *
     * @param onExecute a consumer of CommandContext
     * @return this builder
     */
    public CommandBuilder onExecute(Consumer<CommandContext> onExecute) {
        this.onExecute = onExecute;
        return this;
    }

    /**
     * Defines the action to follow if the bot is missing permissions to execute the command.
     *
     * @param onBotDenied a consumer of CommandContext
     * @return this builder
     */
    public CommandBuilder onBotDenied(Consumer<CommandContext> onBotDenied) {
        this.onBotDenied = onBotDenied;
        return this;
    }

    /**
     * Defines the action to follow if the user is missing permissions to execute the command.
     *
     * @param onAuthorDenied a consumer of CommandContext
     * @return this builder
     */
    public CommandBuilder onAuthorDenied(Consumer<CommandContext> onAuthorDenied) {
        this.onAuthorDenied = onAuthorDenied;
        return this;
    }

    public Command build() {
        return createCommand();
    }

    public Command createCommand() {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        return new Command(name, aliases, category, description, parser, parameterAliases, requiredPermissions, secured,
            deleteRequest, preserveQuotes, argumentLimit, onExecute, onBotDenied, onAuthorDenied);
    }
}
