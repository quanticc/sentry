package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.obj.IChannel;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.service.SettingService;

import static top.quantic.sentry.discord.util.DiscordUtil.ourBotHash;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Supervisor implements DiscordSubscriber {

    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final SentryProperties sentryProperties;
    private final SettingService settingService;

    @Autowired
    public Supervisor(SentryProperties sentryProperties, SettingService settingService) {
        this.sentryProperties = sentryProperties;
        this.settingService = settingService;
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        log.info("[{}] Discord bot is ready", getOurName(event));
        signalReady(event.getClient());
    }

    private void signalReady(IDiscordClient client) {
        if (sentryProperties.getDiscord().isAnnounceReady()) {
            IChannel channel = client.getChannelByID(sentryProperties.getDiscord().getCoordinatorChannelId());
            if (channel != null) {
            settingService.getPrefixes(channel.getGuild().getID()).stream()
                .findAny()
                .ifPresent(pre -> sendMessage(channel, pre + "logout " + ourBotHash(client)));
            }
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
