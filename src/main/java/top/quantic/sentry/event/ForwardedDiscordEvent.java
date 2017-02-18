package top.quantic.sentry.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class ForwardedDiscordEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(ForwardedDiscordEvent.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Handlebars handlebars = new Handlebars();

    public ForwardedDiscordEvent(Event event) {
        super(event);
    }

    @Override
    public Event getSource() {
        return (Event) source;
    }

    @Override
    public String getContentId() {
        return getSource().getClass().getSimpleName() + "@" + getTimestamp();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        String type = (String) dataMap.get("type");
        String eventType = getSource().getClass().getSimpleName();
        boolean included = type == null || type.equals(eventType);
        if (included) {
            String template = (String) dataMap.get("template");
            if (template != null) {
                Context context = Context.newBuilder(getSource())
                    .resolver(
                        MapValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE
                    )
                    .combine("type", eventType)
                    .combine("dataMap", dataMap)
                    .build();
                try {
                    Template compiled = handlebars.compileInline(template);
                    return compiled.apply(context);
                } catch (IOException e) {
                    log.warn("Could not compile announcement", e);
                } finally {
                    context.destroy();
                }
            }
        }
        String content = null;
        try {
            content = "[" + eventType + "] " +
                (dataMap.containsKey("asMap") ? humanize(asMap(dataMap)) : mapper.writeValueAsString(getSource()));
        } catch (JsonProcessingException e) {
            log.warn("Could not convert to json", e);
        }
        if (content != null) {
            if (included) {
                return content;
            } else {
                log.debug("Filtered because type {} did not match {}: {}", eventType, type, content);
            }
        }
        return null;
    }

    private String humanize(Map<String, Object> map) {
        return map.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue().toString())
            .collect(Collectors.joining("\n"));
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        return mapper.convertValue(getSource(), new TypeReference<Map<String, ?>>() {
        });
    }
}
