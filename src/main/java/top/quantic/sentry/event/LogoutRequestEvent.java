package top.quantic.sentry.event;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static top.quantic.sentry.config.Constants.INSTANCE_KEY;
import static top.quantic.sentry.discord.util.DiscordUtil.ourBotId;
import static top.quantic.sentry.service.util.Maps.entriesToMap;
import static top.quantic.sentry.service.util.Maps.entry;

public class LogoutRequestEvent extends SentryEvent {

    public LogoutRequestEvent(IDiscordClient source) {
        super(source);
    }

    @Override
    public IDiscordClient getSource() {
        return (IDiscordClient) super.getSource();
    }

    @Override
    public String getContentId() {
        return "@" + System.currentTimeMillis(); // cache buster
    }

    @Override
    public String asContent() {
        return ".logout " + ourBotId(getSource()) + " " + INSTANCE_KEY;
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(
            Stream.of(
                entry("name", getSource().getOurUser().getName()),
                entry("botHash", ourBotId(getSource())),
                entry("instanceKey", INSTANCE_KEY)
            ).collect(entriesToMap()));
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }
}
