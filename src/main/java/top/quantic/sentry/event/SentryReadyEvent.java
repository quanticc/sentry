package top.quantic.sentry.event;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SentryReadyEvent extends SentryEvent {

    public SentryReadyEvent(ApplicationReadyEvent source) {
        super(source);
    }

    @Override
    public ApplicationReadyEvent getSource() {
        return (ApplicationReadyEvent) super.getSource();
    }

    @Override
    public String getContentId() {
        return "@" + getTimestamp();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        return Instant.now().toString();
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(dataMap);
        String duration = "durationMinutes";
        if (dataMap.containsKey(duration)) {
            map.put("end", Instant.now().plusSeconds(60 * Long.parseLong((String) dataMap.get(duration))).toEpochMilli() / 1000);
        }
        return map;
    }
}
