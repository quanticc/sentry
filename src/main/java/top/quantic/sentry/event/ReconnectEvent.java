package top.quantic.sentry.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import static top.quantic.sentry.discord.util.DiscordUtil.ourBotId;
import static top.quantic.sentry.discord.util.DiscordUtil.ourBotName;

public class ReconnectEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(ReconnectEvent.class);

    public ReconnectEvent(ReconnectSuccessEvent event) {
        super(event);
    }

    @Override
    public ReconnectSuccessEvent getSource() {
        return (ReconnectSuccessEvent) source;
    }

    @Override
    public String getContentId() {
        return ourBotName(getSource()) + "@" + getTimestamp();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        IDiscordClient client = getSource().getClient();
        String bot = (String) dataMap.get("bot");
        if (bot == null || (client.isReady() && (ourBotId(client).equals(bot) || ourBotName(client).equals(bot)))) {
            return String.format("[%s] Discord bot reconnect succeeded", ourBotName(client));
        } else {
            log.debug("Message filtered - neither {} nor {} matched {}", ourBotId(client), ourBotName(client), bot);
            return null;
        }
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        return new LinkedHashMap<>();
    }
}
