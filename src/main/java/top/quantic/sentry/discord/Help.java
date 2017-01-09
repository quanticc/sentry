package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandBuilder;
import top.quantic.sentry.discord.command.CommandContext;
import top.quantic.sentry.discord.command.CommandRegistry;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.DiscordHelpFormatter;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.service.SettingService;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.rightPad;
import static top.quantic.sentry.discord.util.DiscordUtil.answerPrivately;

@Component
public class Help implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Help.class);

    private final CommandRegistry commandRegistry;
    private final PermissionService permissionService;
    private final SettingService settingService;

    @Autowired
    public Help(CommandRegistry commandRegistry, PermissionService permissionService, SettingService settingService) {
        this.commandRegistry = commandRegistry;
        this.permissionService = permissionService;
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(help());
    }

    private Command help() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> helpNonOptionSpec = parser.nonOptions("Command to get help about").ofType(String.class);
        OptionSpec<Void> helpFullSpec = parser.accepts("full", "Display all commands in a list with their description");
        return CommandBuilder.of("help")
            .describedAs("Show help about commands")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                OptionSet o = context.getOptionSet();
                boolean full = o.has(helpFullSpec);
                List<String> keys = o.valuesOf(helpNonOptionSpec);
                IMessage message = context.getMessage();
                List<Command> commandList = commandRegistry.getCommands(message.getClient());
                String response;
                if (keys.isEmpty()) {
                    if (full) {
                        response = "*Commands available to you*\n" + commandList.stream()
                            .filter(c -> canExecute(c, message))
                            .sorted(Comparator.naturalOrder())
                            .map(c -> rightPad("**" + c.getName() + "**", 20) + "\t\t" + c.getDescription())
                            .collect(Collectors.joining("\n"));
                    } else {
                        String prefix = settingService.getPrefixes(message).stream().findAny().orElse("");
                        response = "*Commands available to you*: " + commandList.stream()
                            .filter(c -> canExecute(c, message))
                            .sorted(Comparator.naturalOrder())
                            .map(Command::getName)
                            .collect(Collectors.joining(", ")) + " (more with `" + prefix + "help full`)";
                    }
                } else {
                    List<Command> requested = commandList.stream()
                        .filter(c -> isRequested(keys, c))
                        .filter(c -> canExecute(c, message))
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

    private boolean isRequested(List<String> keys, Command c) {
        return keys.contains(c.getName().toLowerCase()) || (!c.getAliases().isEmpty()
            && keys.stream().anyMatch(k -> c.getAliases().stream().anyMatch(a -> k.contains(a.toLowerCase()))));
    }

    private boolean canExecute(Command command, IMessage message) {
        return (!command.isSecured() || permissionService.hasPermission(message, Operations.EXECUTE, command))
            && message.getChannel().getModifiedPermissions(message.getAuthor()).containsAll(command.getRequiredPermissions());
    }

    void replyWithHelp(Command command, CommandContext context) {
        replyWithHelp(command, context, null);
    }

    void replyWithHelp(Command command, CommandContext context, String comment) {
        StringBuilder response = new StringBuilder();
        if (comment != null) {
            response.append(comment).append("\n");
        }
        String content = appendHelp(response, command).toString();
        RequestBuffer.request(() -> {
            try {
                context.getMessage().getAuthor().getOrCreatePMChannel().sendMessage(content);
            } catch (MissingPermissionsException | DiscordException e) {
                log.warn("Could not reply with help", e);
            }
        });
    }

    private StringBuilder appendHelp(StringBuilder builder, Command command) {
        builder.append("• Help for **").append(command.getName()).append("**: ")
            .append(command.getDescription()).append('\n');
        if (command.getParser() == null) {
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                command.getParser().formatHelpWith(new DiscordHelpFormatter(140, 5));
                command.getParser().printHelpOn(stream);
                builder.append(new String(stream.toByteArray(), "UTF-8")).append('\n');
            } catch (Exception e) {
                log.warn("Could not show help", e);
            }
        }
        return builder;
    }
}
