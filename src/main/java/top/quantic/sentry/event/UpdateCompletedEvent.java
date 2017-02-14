package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateCompletedEvent extends SentryEvent {

    public UpdateCompletedEvent(Integer latestVersion) {
        super(latestVersion);
    }

    @Override
    public Integer getSource() {
        return (Integer) super.getSource();
    }

    @Override
    public String getContentId() {
        return "updated-to-" + getSource();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        return "GameServers updated to v" + getSource();
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return new EmbedBuilder()
            .withTitle("GameServers Updated")
            .appendField("Version", "" + getSource(), true)
            .withColor(new Color(0x00aa00))
            .build();
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", getSource());
        map.put("versionStr", "v" + getSource());
        return map;
    }
}
