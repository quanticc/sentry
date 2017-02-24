package top.quantic.sentry.discord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.event.LogoutRequestEvent;
import top.quantic.sentry.event.ReconnectEvent;
import top.quantic.sentry.event.ReconnectFailedEvent;
import top.quantic.sentry.event.UserBannedEvent;

import java.util.concurrent.atomic.AtomicReference;

import static top.quantic.sentry.discord.util.DiscordUtil.ourBotName;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);
    private static final String WEBSOCKET_LOGGER_NAME = "org.eclipse.jetty.websocket";

    private final ApplicationEventPublisher publisher;

    private final AtomicReference<Level> jettyLevel = new AtomicReference<>(Level.WARN);
    private final AtomicReference<Level> d4jLevel = new AtomicReference<>(Level.DEBUG);

    @Autowired
    public Supervisor(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        this.jettyLevel.set(context.getLogger(WEBSOCKET_LOGGER_NAME).getLevel());
        this.d4jLevel.set(context.getLogger(Discord4J.class).getLevel());
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", ourBotName(event));
        publisher.publishEvent(new LogoutRequestEvent(event.getClient()));
    }

    @EventSubscriber
    public void onReconnectSuccess(ReconnectSuccessEvent event) {
        publisher.publishEvent(new ReconnectEvent(event));
        restoreLevel();
    }

    @EventSubscriber
    public void onReconnectFailure(ReconnectFailureEvent event) {
        publisher.publishEvent(new ReconnectFailedEvent(event));
    }

    @EventSubscriber
    public void onDisconnect(DisconnectedEvent event) {
        log.info("[{}] Discord bot disconnected due to {}", ourBotName(event), event.getReason());
        setTraceLevel();
    }

    @EventSubscriber
    public void onUserBanned(UserBanEvent event) {
        publisher.publishEvent(new UserBannedEvent(event));
    }

    private void setTraceLevel() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(WEBSOCKET_LOGGER_NAME).setLevel(Level.TRACE);
        context.getLogger(Discord4J.class).setLevel(Level.TRACE);
    }

    private void restoreLevel() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(WEBSOCKET_LOGGER_NAME).setLevel(jettyLevel.get());
        context.getLogger(Discord4J.class).setLevel(d4jLevel.get());
    }
}
