package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import static top.quantic.sentry.discord.util.DiscordUtil.humanize;

public class BotDisconnectedEvent extends SentryEvent {

    public BotDisconnectedEvent(DisconnectedEvent event) {
        super(event);
    }

    @Override
    public DisconnectedEvent getSource() {
        return (DisconnectedEvent) source;
    }

    @Override
    public String getContentId() {
        return getSource().getClient().getOurUser().getID()
            + ":" + getSource().getShard().getInfo()[0]
            + ":" + getSource().getReason();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        int[] shardInfo = getSource().getShard().getInfo();
        return String.format("[%s] Disconnected from shard %d/%d due to %s",
            humanize(getSource().getClient().getOurUser()),
            shardInfo[0] + 1, shardInfo[1] + 1, getSource().getReason().toString());
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
