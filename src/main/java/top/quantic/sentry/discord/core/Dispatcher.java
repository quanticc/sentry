package top.quantic.sentry.discord.core;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.discord.Help;
import top.quantic.sentry.discord.module.ListenerSupplier;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.service.SettingService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static top.quantic.sentry.discord.util.DiscordUtil.humanize;

@Component
public class Dispatcher implements ListenerSupplier, IListener<MessageReceivedEvent> {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private static final Pattern QUOTE_PATTERN = Pattern.compile("[^ \\t\"']+|\"([^\"]*)\"|'([^']*)'");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^[\\w]+=[\\w]+$");

    private final CommandRegistry commandRegistry;
    private final SettingService settingService;
    private final PermissionService permissionService;
    private final Help help;
    private final Executor taskExecutor;

    @Autowired
    public Dispatcher(CommandRegistry commandRegistry, SettingService settingService,
                      PermissionService permissionService, Help help, Executor taskExecutor) {
        this.commandRegistry = commandRegistry;
        this.settingService = settingService;
        this.permissionService = permissionService;
        this.help = help;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public List<IListener<?>> getListeners() {
        return Collections.singletonList(this);
    }

    @Override
    public void handle(MessageReceivedEvent event) {
        CompletableFuture.runAsync(() -> execute(event), taskExecutor)
            .exceptionally(t -> {
                log.warn("Command executor terminated exceptionally", t);
                return null;
            });
    }

    private void execute(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String content = message.getContent();
        String guild = message.getChannel().isPrivate() ? Constants.ANY : message.getChannel().getGuild().getID();
        Set<String> prefixes = settingService.getPrefixes(guild);
        Optional<String> prefix = prefixes.stream().filter(content::startsWith).findAny();
        if (prefix.isPresent()) {
            List<Command> commands = commandRegistry.getCommands(event.getClient());
            String name = content.substring(prefix.get().length(), content.contains(" ") ? content.indexOf(" ") : content.length());
            Optional<Command> command = commands.stream()
                .filter(c -> c.getName().equals(name.toLowerCase()) || c.getAliases().contains(name.toLowerCase()))
                .findAny();
            if (command.isPresent()) {
                CommandContext context = new CommandContext();
                context.setMessage(message);
                context.setCommand(command.get());
                context.setPrefix(prefix.get());

                // check permissions to execute the command
                // will check for "execute" permission on "*", "command" and "command category" resources
                Set<PermissionType> perms = permissionService.check(message, Operations.EXECUTE, command.get());
                boolean isAllowed = perms.contains(PermissionType.ALLOW);
                boolean isDenied = perms.contains(PermissionType.DENY);
                boolean isSecured = command.get().isSecured();
                boolean hasPermission = message.getChannel().getModifiedPermissions(message.getAuthor())
                    .containsAll(command.get().getRequiredPermissions());
                boolean canExecute = (isSecured && isAllowed && !isDenied) || (!isSecured && !isDenied);
                if (canExecute && hasPermission) {
                    // clean up and parse arguments
                    String args = content.substring(content.indexOf(command.get().getName()) + command.get().getName().length());
                    args = args.startsWith(" ") ? args.split(" ", 2)[1] : null; // nullify if ran with no args
                    OptionParser parser = command.get().getParser();
                    boolean parseError = false;
                    boolean forHelp = false;
                    if (parser == null || parser.recognizedOptions().isEmpty()) {
                        if (args != null) {
                            // commands without parser but with args present, will delegate to onExecute
                            // args will be retrievable from context.getArgs()[0]
                            String[] array = {args};
                            context.setArgs(array);
                        }
                    } else {
                        try {
                            if (args != null) {
                                int limit = command.get().getArgumentLimit();
                                boolean unquote = !command.get().isPreserveQuotes();
                                Map<String, String> aliases = command.get().getParameterAliases();
                                String[] splitArgs = splitAndConvert(args, limit, unquote, aliases);
                                context.setArgs(splitArgs);
                                context.setOptionSet(parser.parse(splitArgs));
                            } else {
                                context.setOptionSet(parser.parse());
                            }
                            forHelp = context.getOptionSet().has("help");
                        } catch (OptionException e) {
                            log.info("[{}] User {} executing command {}{} failed parsing: {}",
                                message.getClient().getOurUser().getName(),
                                humanize(message.getAuthor()), command.get().getName(),
                                args == null ? "" : " with args: " + args, e.toString());
                            help.replyWithHelp(command.get(), context, e.getMessage());
                            parseError = true;
                        }
                    }

                    // finally, execute the command
                    log.info("[{}] User {} executing command {}{}", message.getClient().getOurUser().getName(),
                        humanize(message.getAuthor()), command.get().getName(), args == null ? "" : " with args: " + args);

                    // intercept with help if parse failed or was explicitly requested
                    if (forHelp) {
                        help.replyWithHelp(command.get(), context);
                    } else if (!parseError) {
                        command.get().onExecute().accept(context);
                    }
                    if (command.get().isDeleteRequest()) {
                        RequestBuffer.request(() -> {
                            try {
                                message.delete();
                            } catch (MissingPermissionsException e) {
                                log.warn("[{}] Missing permissions in {}: {}",
                                    event.getClient().getOurUser().getName(),
                                    humanize(message.getChannel()), e.getErrorMessage());
                                command.get().onBotDenied().accept(context);
                            } catch (DiscordException e) {
                                log.warn("[{}] Failed to delete message in {}: {}",
                                    event.getClient().getOurUser().getName(),
                                    humanize(message.getChannel()), e.getErrorMessage());
                            }
                        });
                    }
                    log.info("[{}] User {} completed execution of command {}{}", message.getClient().getOurUser().getName(),
                        humanize(message.getAuthor()), command.get().getName(), args == null ? "" : " with args: " + args);
                } else {
                    log.info("[{}] User {} was denied command execution: {}", message.getClient().getOurUser().getName(),
                        humanize(message.getAuthor()), command.get().getName());
                    command.get().onAuthorDenied().accept(context);
                }
            }
        }
    }

    private String[] splitAndConvert(String args, int limit, boolean unquote, Map<String, String> parameterAliases) {
        Matcher matcher = QUOTE_PATTERN.matcher(args);
        List<String> matches = new ArrayList<>();
        int count = 1;
        while (matcher.find()) {
            if (limit > 0 && ++count > limit) {
                String group = args.substring(matcher.start());
                matches.add(group);
                break;
            } else {
                String group = matcher.group();
                matches.add(unquote ? group.replaceAll("\"|'", "") : group);
            }
        }
        if (parameterAliases != null && !parameterAliases.isEmpty()) {
            return matches.stream().map(s -> {
                // if this match is in the form key=value and starts with a key present, prepend "--" to it
                if (KEY_VALUE_PATTERN.matcher(s).matches()
                    && parameterAliases.keySet().stream().anyMatch(s::startsWith)) {
                    return "--" + s;
                } else {
                    return parameterAliases.getOrDefault(s, s);
                }
            }).collect(Collectors.toList()).toArray(new String[matches.size()]);
        } else {
            return matches.toArray(new String[matches.size()]);
        }
    }
}
