package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.event.LogoutRequestEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final ApplicationEventPublisher publisher;
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    @Autowired
    public Supervisor(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        readyLatch.countDown();
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", getOurName(event));
        CompletableFuture.runAsync(this::await)
            .thenRun(() -> publisher.publishEvent(new LogoutRequestEvent(event.getClient())));
    }

    private void await() {
        if (readyLatch.getCount() > 0) {
            log.debug("Waiting until the application is ready...");
        }
        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    @EventSubscriber
    public void onReconnectSuccess(ReconnectSuccessEvent event) {
        log.info("[{}] Discord bot reconnect succeeded", getOurName(event));
    }

    @EventSubscriber
    public void onReconnectFailure(ReconnectFailureEvent event) {
        log.warn("[{}] Discord bot reconnect failed after {} ***", getOurName(event),
            inflect(event.getCurrentAttempt() + 1, "attempt"));
    }

    @EventSubscriber
    public void onDisconnect(DisconnectedEvent event) {
        log.warn("[{}] Discord bot disconnected due to {}", getOurName(event), event.getReason());
    }

    private String getOurName(Event event) {
        return event.getClient().getOurUser().getName();
    }
}
