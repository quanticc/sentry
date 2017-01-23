package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.util.Map;

public interface ContentSupplier {
    String getContentId();
    String asContent();
    EmbedObject asEmbed(Map<String, Object> dataMap);
    Map<String, Object> asMap();
}
