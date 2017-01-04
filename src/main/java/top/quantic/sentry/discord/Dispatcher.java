package top.quantic.sentry.discord;

import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandContext;
import top.quantic.sentry.discord.command.CommandRegistry;
import top.quantic.sentry.discord.module.ListenerSupplier;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.service.SettingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static top.quantic.sentry.discord.util.DiscordLimiter.acquireDelete;
import static top.quantic.sentry.discord.util.DiscordUtil.getRoles;
import static top.quantic.sentry.discord.util.DiscordUtil.humanize;

@Component
public class Dispatcher implements ListenerSupplier, IListener<MessageReceivedEvent> {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private static final Pattern PATTERN = Pattern.compile("[^ \\t\"']+|\"([^\"]*)\"|'([^']*)'");

    private final CommandRegistry commandRegistry;
    private final SettingService settingService;
    private final PermissionService permissionService;

    @Autowired
    public Dispatcher(CommandRegistry commandRegistry, SettingService settingService, PermissionService permissionService) {
        this.commandRegistry = commandRegistry;
        this.settingService = settingService;
        this.permissionService = permissionService;
    }

    @Override
    public List<IListener<?>> getListeners() {
        return Lists.newArrayList(this);
    }

    @Override
    public void handle(MessageReceivedEvent event) {
        IMessage message = event.getMessage();
        String content = message.getContent();
        String guild = message.getChannel().isPrivate() ? Constants.ANY : message.getChannel().getGuild().getID();
        List<String> prefixes = settingService.getPrefixes(guild);
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
                boolean allowed = true;
                if (command.get().isSecured()) {
                    allowed = permissionService.hasPermission(getRoles(message.getAuthor()),
                        Operations.BOT_COMMAND_EXECUTE, command.get().getName());
                }
                boolean hasPermissions = event.getMessage().getChannel()
                    .getModifiedPermissions(event.getMessage().getAuthor())
                    .containsAll(command.get().getRequiredPermissions());
                if (allowed && hasPermissions) {
                    // clean up and parse arguments
                    String args = content.substring(content.indexOf(command.get().getName()) + command.get().getName().length());
                    args = args.startsWith(" ") ? args.split(" ", 2)[1] : null; // nullify if ran with no args
                    OptionParser parser = command.get().getParser();
                    if (parser == null || parser.recognizedOptions().isEmpty()) {
                        if (args != null) {
                            // commands without parser but with args present, will delegate to onExecute
                            // args will be retrievable from context.getArgs()[0]
                            String[] array = {args};
                            context.setArgs(array);
                        }
                    } else {
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
                    }

                    // finally, execute the command
                    log.info("User {} executing command {}{}", humanize(message.getAuthor()), command.get().getName(),
                        args == null ? "" : " with args: " + args);
                    command.get().onExecute().accept(context);
                    if (command.get().isDeleteRequest()) {
                        RequestBuffer.request(() -> {
                            try {
                                acquireDelete();
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
                } else {
                    log.info("User {} was denied command execution: {}", humanize(message.getAuthor()), command.get().getName());
                    command.get().onAuthorDenied().accept(context);
                }
            }
        }
    }

    private String[] splitAndConvert(String args, int limit, boolean unquote, Map<String, String> parameterAliases) {
        Matcher matcher = PATTERN.matcher(args);
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
            return matches.stream().map(s -> parameterAliases.getOrDefault(s, s))
                .collect(Collectors.toList()).toArray(new String[matches.size()]);
        } else {
            return matches.toArray(new String[matches.size()]);
        }
    }
}
