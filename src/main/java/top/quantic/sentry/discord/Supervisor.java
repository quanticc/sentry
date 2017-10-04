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
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserPardonEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.event.*;

import java.util.concurrent.atomic.AtomicReference;

import static top.quantic.sentry.discord.util.DiscordUtil.ourBotName;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final ApplicationEventPublisher publisher;

    @Autowired
    public Supervisor(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", ourBotName(event));
        publisher.publishEvent(new LogoutRequestEvent(event.getClient()));
    }

    @EventSubscriber
    public void onReconnectSuccess(ReconnectSuccessEvent event) {
        publisher.publishEvent(new ReconnectEvent(event));
    }

    @EventSubscriber
    public void onReconnectFailure(ReconnectFailureEvent event) {
        publisher.publishEvent(new ReconnectFailedEvent(event));
    }

    @EventSubscriber
    public void onDisconnect(DisconnectedEvent event) {
        log.info("[{}] Discord bot disconnected due to {}", ourBotName(event), event.getReason());

	    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    context.getLogger("org.eclipse.jetty.websocket").setLevel(Level.TRACE);
	    context.getLogger(Discord4J.class).setLevel(Level.TRACE);
    }

    @EventSubscriber
    public void onUserBanned(UserBanEvent event) {
        publisher.publishEvent(new UserBannedEvent(event));
    }

    @EventSubscriber
    public void onUserPardon(UserPardonEvent event) {
        publisher.publishEvent(new UserPardonedEvent(event));
    }

    @EventSubscriber
    public void onMessageDelete(MessageDeleteEvent event) {
    	publisher.publishEvent(new MessageDeletedEvent(event));
    }

}
