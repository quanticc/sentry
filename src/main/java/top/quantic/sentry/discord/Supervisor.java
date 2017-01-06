package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.ReconnectSuccessEvent;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static top.quantic.sentry.discord.util.DiscordLimiter.acquireWebhook;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final SentryProperties sentryProperties;
    private final Executor taskExecutor;

    @Autowired
    public Supervisor(SentryProperties sentryProperties, Executor taskExecutor) {
        this.sentryProperties = sentryProperties;
        this.taskExecutor = taskExecutor;
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", getOurName(event));
        SentryProperties.Discord.Coordinator coordinator = sentryProperties.getDiscord().getCoordinator();
        String webhookUrl = coordinator.getWebhookUrl();
        if (webhookUrl != null) {
            CompletableFuture.runAsync(() -> {
                DiscordWebhook webhook = new DiscordWebhook();
                webhook.setContent(".logout " + Operations.COORDINATOR_KEY);
                webhook.setUsername(coordinator.getUsername());
                webhook.setAvatarUrl(coordinator.getAvatarUrl());
                try {
                    execute(webhook, webhookUrl);
                } catch (RestClientException e) {
                    log.warn("Could not execute webhook", e);
                }
            }, taskExecutor);
        }
    }

    private ResponseEntity<Map<String, ?>> execute(DiscordWebhook webhook, String webhookUrl) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("User-Agent", "curl"); // cloudflare!
        acquireWebhook();
        return restTemplate.exchange(webhookUrl,
            HttpMethod.POST,
            new HttpEntity<>(webhook, headers),
            new ParameterizedTypeReference<Map<String, ?>>() {});
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
