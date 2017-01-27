package top.quantic.sentry.event;

import de.androidpit.colorthief.ColorThief;
import de.androidpit.colorthief.MMCQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.domain.Streamer;
import top.quantic.sentry.web.rest.vm.TwitchStream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Override
    public String asContent(Map<String, Object> dataMap) {
        TwitchStream stream = getSource();
        if (announcement.contains("{{") || announcement.contains("}}")) {
            log.warn("Announcement appears to be badly formatted: {}", announcement);
            announcement = null;
        }
        if (announcement == null) {
            return "@here " + stream.getChannel().getDisplayName() + getDivisionContent(dataMap)
                + " is now live on <" + stream.getChannel().getUrl() + "> !";
        } else {
            return announcement;
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
            .withAuthorIcon(stream.getChannel().getLogo())
            .withAuthorName(stream.getChannel().getDisplayName())
            .withTitle(stream.getChannel().getStatus())
            .withColor(getDominantColor(stream.getChannel().getLogo()))
            .withThumbnail(stream.getChannel().getLogo())
            .withUrl(stream.getChannel().getUrl())
            .withImage(stream.getPreview().get("medium"))
            .withFooterIcon("https://www.twitch.tv/favicon.ico")
            .withFooterText("twitch.tv")
            .appendField("Playing", stream.getGame(), true)
            .appendField("Viewers", stream.getViewers() + "", true)
            .ignoreNullEmptyFields()
            .appendField("League", league, true)
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

    private Color getDominantColor(String urlStr) {
        try {
            URL url = new URL(urlStr);
            BufferedImage image = ImageIO.read(url);
            MMCQ.CMap result = ColorThief.getColorMap(image, 5);
            MMCQ.VBox vBox = result.vboxes.get(0);
            int[] rgb = vBox.avg(false);
            return new Color(rgb[0], rgb[1], rgb[2]);
        } catch (Exception e) {
            log.warn("Could not analyze image", e);
        }
        return new Color(0x6441A4);
    }
}
