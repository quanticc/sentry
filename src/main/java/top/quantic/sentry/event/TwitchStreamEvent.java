package top.quantic.sentry.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.discord.util.MarkdownUtil;
import top.quantic.sentry.domain.Streamer;
import top.quantic.sentry.web.rest.vm.TwitchStream;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static top.quantic.sentry.service.util.DateUtil.formatRelative;
import static top.quantic.sentry.service.util.MiscUtil.getDominantColor;

public class TwitchStreamEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(TwitchStreamEvent.class);

    private final Streamer streamer;

    private String announcement;
    private Map<String, String> resolvedFields;

    public TwitchStreamEvent(TwitchStream source, Streamer streamer) {
        super(source);
        this.streamer = streamer;
    }

    @Override
    public TwitchStream getSource() {
        return (TwitchStream) super.getSource();
    }

    public Streamer getStreamer() {
        return streamer;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    public Map<String, String> getResolvedFields() {
        return resolvedFields;
    }

    public void setResolvedFields(Map<String, String> resolvedFields) {
        this.resolvedFields = resolvedFields;
    }

    @Override
    public String getContentId() {
        return "@" + System.currentTimeMillis(); // de-dupe is handled elsewhere
    }

    private boolean include(Map<String, Object> flowDataMap) {
        String league = getAsString(flowDataMap.get("league"));
        return league == null || streamer.getLeague().equals(league);
    }

    private String getAsString(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        if (!include(dataMap)) {
            return null;
        }
        TwitchStream stream = getSource();
        if (announcement != null && (announcement.contains("{{") || announcement.contains("}}"))) {
            log.warn("Announcement appears to be badly formatted: {}", announcement);
            announcement = null;
        }
        String displayName = stream.getChannel().getDisplayName();
        String url = stream.getChannel().getUrl();
        if (announcement == null) {
            if (dataMap.containsKey("defaultAnnounce")) {
                return "@here " + MarkdownUtil.escape(displayName) + getDivisionContent(dataMap) + " is now live on <" + url + "> !";
            } else {
                return null;
            }
        } else {
            return announcement.replace(displayName, MarkdownUtil.escape(displayName));
        }
    }

    private String getDivisionContent(Map<String, Object> dataMap) {
        String league = streamer.getLeague();
        String division = streamer.getDivision();
        String hideDivisionRegex = null;
        if (dataMap != null) {
            hideDivisionRegex = (String) dataMap.get("hideDivisionRegex");
        }
        if (division != null) {
            if (hideDivisionRegex != null && division.matches(hideDivisionRegex)) {
                division = null;
            }
        }
        if (league != null) {
            return " (" + league + (division != null ? " " + division : "") + ")";
        }
        return "";
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        if (!include(dataMap)) {
            return null;
        }
        String league = streamer.getLeague();
        String division = streamer.getDivision();
        String hideDivisionRegex = null;
        if (league != null && (league.equalsIgnoreCase("all")
            || league.equalsIgnoreCase("any")
            || league.equalsIgnoreCase("none")
            || league.equalsIgnoreCase("*"))) {
            league = null;
            division = null;
        } else {
            if (dataMap != null) {
                hideDivisionRegex = (String) dataMap.get("hideDivisionRegex");
            }
            if (division != null) {
                if (hideDivisionRegex != null && division.matches(hideDivisionRegex)) {
                    division = null;
                }
                if (league == null) {
                    division = null;
                }
            }
        }

        TwitchStream stream = getSource();
        EmbedBuilder builder = new EmbedBuilder()
            .setLenient(true)
            .withAuthorIcon(stream.getChannel().getLogo())
            .withAuthorName(stream.getChannel().getDisplayName())
            .withTitle(stream.getChannel().getStatus())
            .withColor(getDominantColor(stream.getChannel().getLogo(), new Color(0x6441A4)))
            .withThumbnail(stream.getChannel().getLogo())
            .withUrl(stream.getChannel().getUrl())
            .withImage(stream.getPreview().get("medium"))
            .withFooterText("twitch.tv")
            .appendField("Playing", stream.getGame(), true);
        if (stream.getViewers() >= 10) {
            builder.appendField("Viewers", stream.getViewers() + "", true);
        } else {
            builder.appendField("Started", formatRelative(stream.getCreatedAt()), true);
        }
        builder.appendField("League", league, true)
            .appendField("Division", division, true);
        if (resolvedFields != null) {
            resolvedFields.forEach((title, content) -> {
                if (content.contains("{{") || content.contains("}}")) {
                    log.warn("Field with key {} was incorrectly resolved: {}", title, content);
                } else {
                    builder.appendField(title, content, true);
                }
            });
        }
        return builder.build();
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        TwitchStream stream = getSource();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("avatar", stream.getChannel().getLogo());
        map.put("name", stream.getChannel().getDisplayName());
        map.put("title", stream.getChannel().getStatus());
        map.put("game", stream.getGame());
        map.put("viewers", stream.getViewers());
        map.put("preview", stream.getPreview().get("medium"));
        map.put("url", stream.getChannel().getUrl());
        map.put("createdAt", stream.getCreatedAt());
        map.put("league", streamer.getLeague());
        map.put("division", streamer.getDivision());
        return map;
    }
}
