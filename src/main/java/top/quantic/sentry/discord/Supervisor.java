package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.ReconnectSuccessEvent;
import sx.blah.discord.handle.obj.IChannel;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.discord.module.DiscordSubscriber;

import static top.quantic.sentry.discord.util.DiscordUtil.ourBotHash;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final SentryProperties sentryProperties;

    @Autowired
    public Supervisor(SentryProperties sentryProperties) {
        this.sentryProperties = sentryProperties;
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", getOurName(event));
        signalReady(event.getClient());
    }

    private void signalReady(IDiscordClient client) {
        IChannel channel = client.getChannelByID(sentryProperties.getDiscord().getCoordinatorChannelId());
        if (channel != null) {
            sendMessage(channel, ".logout " + ourBotHash(client));
        }
    }

    @EventSubscriber
    public void onReconnectSuccess(ReconnectSuccessEvent event) {
        log.info("[{}] Discord bot reconnect succeeded", getOurName(event));
    }

    @EventSubscriber
    public void onReconnectFailure(ReconnectFailureEvent event) {
        log.warn("[{}] Discord bot reconnect failed after {} attempt{} ***", getOurName(event), event.getCurAttempt() + 1, event.getCurAttempt() + 1 == 1 ? "" : "s");
    }

    @EventSubscriber
    public void onDisconnect(DisconnectedEvent event) {
        log.warn("[{}] Discord bot disconnected due to {}", getOurName(event), event.getReason());
    }

    private String getOurName(Event event) {
        return event.getClient().getOurUser().getName();
    }
}
