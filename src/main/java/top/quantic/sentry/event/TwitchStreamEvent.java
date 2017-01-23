package top.quantic.sentry.event;

import de.androidpit.colorthief.ColorThief;
import de.androidpit.colorthief.MMCQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.web.rest.vm.TwitchStream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TwitchStreamEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(TwitchStreamEvent.class);

    public TwitchStreamEvent(TwitchStream source) {
        super(source);
    }

    @Override
    public TwitchStream getSource() {
        return (TwitchStream) super.getSource();
    }

    @Override
    public String getContentId() {
        return Integer.toHexString(Objects.hash(getSource().getId()));
    }

    @Override
    public String asContent() {
        TwitchStream stream = getSource();
        return "@here " + stream.getChannel().getDisplayName() + " is now live on <" + stream.getChannel().getUrl() + "> !";
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        TwitchStream stream = getSource();
        return new EmbedBuilder()
            .withAuthorIcon(stream.getChannel().getLogo())
            .withAuthorName(stream.getChannel().getDisplayName())
            .withTitle(stream.getChannel().getStatus())
            .withColor(getDominantColor(stream.getChannel().getLogo()))
            .withThumbnail(stream.getChannel().getLogo())
            .withUrl(stream.getChannel().getUrl())
            .withImage(stream.getPreview().get("medium"))
            .withFooterIcon("https://www.twitch.tv/favicon.ico")
            .withFooterText("twitch.tv")
            .appendField("Playing", stream.getChannel().getStatus(), true)
            .appendField("Viewers", stream.getViewers() + "", true)
            .build();
    }

    @Override
    public Map<String, Object> asMap() {
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
