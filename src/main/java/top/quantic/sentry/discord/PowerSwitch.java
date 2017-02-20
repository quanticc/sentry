package top.quantic.sentry.discord;

import org.ocpsoft.prettytime.shade.edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;

import java.util.List;

import static top.quantic.sentry.config.Constants.INSTANCE_KEY;
import static top.quantic.sentry.discord.util.DiscordUtil.*;

@Component
public class PowerSwitch implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(PowerSwitch.class);

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(logout());
    }

    private Command logout() {
        return CommandBuilder.of("logout")
            .describedAs("Disconnect from Discord")
            .in("Administrative")
            .nonParsed()
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String[] args = context.getArgs();
                if (args != null) {
                    String id = ourBotId(message.getClient());
                    String[] array = args[0].split(" ");
                    // shutdown if the args match this bot but not this instance
                    if (array.length >= 2 && array[0].equals(id) && !array[1].equals(INSTANCE_KEY)) {
                        deleteMessage(message);
                        doLogout(context);
                    } else {
                        log.debug("Ignoring: Expected args {} and anything but {}", id, INSTANCE_KEY);
                    }
                } else {
                    deleteMessage(message);
                    answer(message, ":wave:");
                    doLogout(context);
                }
            }).build();
    }

    private void doLogout(CommandContext context) {
        IDiscordClient client = context.getMessage().getClient();
        log.info("[{}] Logging out via command", client.getOurUser().getName());
        if (client.isLoggedIn()) {
            try {
                client.logout();
            } catch (DiscordException e) {
                log.warn("Could not logout bot", e);
            }
        }
    }
}
