package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static top.quantic.sentry.discord.util.DiscordUtil.messageSummary;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class ChannelPurgeEvent extends SentryEvent {

    private final List<IMessage> purged;

    public ChannelPurgeEvent(IChannel target, List<IMessage> purged) {
        super(target);
        this.purged = purged;
    }

    @Override
    public IChannel getSource() {
        return (IChannel) source;
    }

    @Override
    public String getContentId() {
        return Integer.toHexString(Objects.hash(source, purged));
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        if (purged.isEmpty()) {
            return null;
        }
        return "**Deleted " + inflect(purged.size(), "message") + " from** " + getSource().mention() + "\n" + messageSummary(purged, 50);
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("channel", getSource());
        map.put("channelId", getSource().getID());
        map.put("purged", purged);
        map.put("purgedSize", purged.size());
        return map;
    }
}
