package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.core.CommandRegistry;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.DiscordHelpFormatter;
import top.quantic.sentry.service.PermissionService;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static top.quantic.sentry.discord.util.DiscordUtil.answerPrivately;

@Component
public class Help implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Help.class);

    private final CommandRegistry commandRegistry;
    private final PermissionService permissionService;

    @Autowired
    public Help(CommandRegistry commandRegistry, PermissionService permissionService) {
        this.commandRegistry = commandRegistry;
        this.permissionService = permissionService;
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(help(), examples());
    }

    private Command help() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Command to get help about").ofType(String.class);
        OptionSpec<Void> moreSpec = parser.accepts("more", "Display all commands in a list with their description");
        OptionSpec<Void> fullSpec = parser.accepts("full", "Calculate permissions from all of your known guild roles");
        return CommandBuilder.of("help")
            .describedAs("Show help about commands")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                boolean withDescription = o.has(moreSpec);
                boolean deep = o.has(fullSpec);
                List<String> keys = o.valuesOf(nonOptSpec);
                IMessage message = context.getMessage();
                List<Command> commandList = commandRegistry.getCommands(message.getClient());
                String response;
                String prefix = context.getPrefix();
                if (keys.isEmpty()) {
                    if (withDescription || deep) {
                        response = "*Commands available to you*\n";
                        if (!deep) {
                            response += "Note: This list does not take all of your known guild roles into account, " +
                                "use `" + prefix + "help full` for that\n\n";
                        }
                        List<Command> executable = commandList.stream()
                            .filter(c -> canExecute(c, message, deep))
                            .collect(Collectors.toList());
                        Map<String, List<Command>> categories = new HashMap<>();
                        executable.forEach(cmd -> categories.computeIfAbsent(cmd.getCategory(), k -> new ArrayList<>()).add(cmd));
                        response += categories.entrySet().stream()
                            .map(e -> "__" + e.getKey() + "__\n" +
                                e.getValue().stream()
                                    .sorted(Comparator.naturalOrder())
                                    .map(c -> rightPad("**" + c.getName() + "**", 20) + "\t\t" + c.getDescription())
                                    .collect(Collectors.joining("\n")))
                            .collect(Collectors.joining("\n\n"));
                    } else {
                        response = "*Commands available to you*\n" + commandList.stream()
                            .filter(c -> canExecute(c, message, false))
                            .sorted(Comparator.naturalOrder())
                            .map(Command::getName)
                            .collect(Collectors.joining(", ")) +
                            "\n\n• Get a more detailed view using **" + prefix + "help more**" +
                            "\n• Get help for an specific command use **" + prefix + "help** [__command__]";
                    }
                } else {
                    List<Command> requested = commandList.stream()
                        .filter(c -> isRequested(keys, c))
                        .filter(c -> canExecute(c, message, true))
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList());
                    StringBuilder builder = new StringBuilder();
                    for (Command command : requested) {
                        appendHelp(builder, command);
                    }
                    response = builder.toString();
                }
                answerPrivately(message, response);
            }).build();
    }

    private Command examples() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> helpNonOptionSpec = parser.nonOptions("Command to get examples for").ofType(String.class);
        return CommandBuilder.of("examples")
            .describedAs("Show examples for commands")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                List<String> keys = o.valuesOf(helpNonOptionSpec);
                IMessage message = context.getMessage();
                List<Command> commandList = commandRegistry.getCommands(message.getClient());
                if (keys.isEmpty()) {
                    answerPrivately(message, "Please specify at least one command");
                } else {
                    List<Command> requested = commandList.stream()
                        .filter(c -> isRequested(keys, c))
                        .filter(c -> canExecute(c, message, true))
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList());
                    StringBuilder builder = new StringBuilder();
                    for (Command command : requested) {
                        appendExamples(builder, command);
                    }
                    answerPrivately(message, builder.toString());
                }
            }).build();
    }

    private boolean isRequested(List<String> keys, Command c) {
        return keys.stream().anyMatch(k -> k.equalsIgnoreCase(c.getName())) ||
            (!c.getAliases().isEmpty() && keys.stream().anyMatch(k -> c.getAliases().stream().anyMatch(k::equalsIgnoreCase)));
    }

    private boolean canExecute(Command command, IMessage message, boolean deep) {
        return (!command.isSecured() || permissionService.hasPermission(message, Operations.EXECUTE, command, deep))
            && message.getChannel().getModifiedPermissions(message.getAuthor()).containsAll(command.getRequiredPermissions());
    }

    public void replyWithHelp(Command command, CommandContext context) {
        replyWithHelp(command, context, null);
    }

    public void replyWithHelp(Command command, CommandContext context, String comment) {
        StringBuilder response = new StringBuilder();
        if (comment != null) {
            response.append(comment).append("\n");
        }
        String content = appendHelp(response, command).toString();
        answerPrivately(context.getMessage(), content);
    }

    private StringBuilder appendExamples(StringBuilder builder, Command command) {
        builder.append("• Examples for **").append(command.getName()).append("**: ")
            .append(command.getDescription()).append('\n');
        if (!isBlank(command.getExamples())) {
            builder.append(command.getExamples());
        } else {
            builder.append("No examples defined for this command, check **help ")
                .append(command.getName()).append("**\n");
        }
        return builder.append('\n');
    }

    private StringBuilder appendHelp(StringBuilder builder, Command command) {
        builder.append("• Help for **").append(command.getName()).append("**: ")
            .append(command.getDescription()).append('\n');
        if (!isBlank(command.getExamples())) {
            builder.append("\n*Examples*\n").append(command.getExamples()).append("\n");
        }
        if (command.getParser() != null) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                command.getParser().formatHelpWith(new DiscordHelpFormatter(280, 5));
                command.getParser().printHelpOn(stream);
                builder.append(stream.toString("UTF-8")).append('\n');
            } catch (Exception e) {
                log.warn("Could not show help", e);
            }
        }
        return builder;
    }
}
