package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.member.GuildMemberEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.event.ForwardedDiscordEvent;
import top.quantic.sentry.event.LogoutRequestEvent;
import top.quantic.sentry.event.ReconnectEvent;
import top.quantic.sentry.event.ReconnectFailedEvent;

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
        publisher.publishEvent(new ForwardedDiscordEvent(event));
    }

    @EventSubscriber
    public void onGuildMemberEvent(GuildMemberEvent event) {
        publisher.publishEvent(new ForwardedDiscordEvent(event));
    }
}
