package top.quantic.sentry.discord.command;

import joptsimple.OptionParser;
import sx.blah.discord.handle.obj.Permissions;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class Command implements Comparable<Command> {

    private final String name;
    private final Set<String> aliases;
    private final String category;
    private final String description;
    private final OptionParser parser;
    private final Map<String, String> parameterAliases;
    private final EnumSet<Permissions> requiredPermissions;
    private final boolean secured;
    private final boolean deleteRequest;
    private final boolean preserveQuotes;
    private final int argumentLimit;
    private final Consumer<CommandContext> onExecute;
    private final Consumer<CommandContext> onBotDenied;
    private final Consumer<CommandContext> onAuthorDenied;

    Command(String name, Set<String> aliases, String category, String description, OptionParser parser,
            Map<String, String> parameterAliases, EnumSet<Permissions> requiredPermissions, boolean secured,
            boolean deleteRequest, boolean preserveQuotes, int argumentLimit, Consumer<CommandContext> onExecute,
            Consumer<CommandContext> onBotDenied, Consumer<CommandContext> onAuthorDenied) {
        this.name = name;
        this.aliases = aliases;
        this.category = category;
        this.description = description;
        this.parser = parser;
        this.parameterAliases = parameterAliases;
        this.requiredPermissions = requiredPermissions;
        this.secured = secured;
        this.deleteRequest = deleteRequest;
        this.preserveQuotes = preserveQuotes;
        this.argumentLimit = argumentLimit;
        this.onExecute = onExecute;
        this.onBotDenied = onBotDenied;
        this.onAuthorDenied = onAuthorDenied;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public OptionParser getParser() {
        return parser;
    }

    public Map<String, String> getParameterAliases() {
        return parameterAliases;
    }

    public EnumSet<Permissions> getRequiredPermissions() {
        return requiredPermissions;
    }

    public boolean isSecured() {
        return secured;
    }

    public boolean isDeleteRequest() {
        return deleteRequest;
    }

    public boolean isPreserveQuotes() {
        return preserveQuotes;
    }

    public int getArgumentLimit() {
        return argumentLimit;
    }

    public Consumer<CommandContext> onExecute() {
        return onExecute;
    }

    public Consumer<CommandContext> onBotDenied() {
        return onBotDenied;
    }

    public Consumer<CommandContext> onAuthorDenied() {
        return onAuthorDenied;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(name, command.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Command o) {
        return name.compareTo(o.name);
    }
}
