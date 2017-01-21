package top.quantic.sentry.event;

import sx.blah.discord.api.IDiscordClient;

import java.util.HashMap;
import java.util.Map;

import static top.quantic.sentry.config.Constants.INSTANCE_KEY;
import static top.quantic.sentry.discord.util.DiscordUtil.ourBotHash;

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
        return ".logout " + ourBotHash(getSource()) + " " + INSTANCE_KEY;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", getSource().getOurUser().getName());
        map.put("botHash", ourBotHash(getSource()));
        map.put("instanceKey", INSTANCE_KEY);
        return map;
    }
}
