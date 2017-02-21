package top.quantic.sentry.event;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserBannedEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(UserBannedEvent.class);

    public UserBannedEvent(UserBanEvent event) {
        super(event);
    }

    @Override
    public UserBanEvent getSource() {
        return (UserBanEvent) source;
    }

    @Override
    public String getContentId() {
        return "ban:" + getSource().getClient().getOurUser().getID()
            + ":" + getSource().getGuild().getID()
            + ":" + getSource().getUser().getID()
            + "@" + getTimestamp();
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        String guildSpec = (String) dataMap.get("guilds");
        List<String> guilds = null;
        if (guildSpec != null) {
            guilds = Arrays.asList(guildSpec.split(",|;"));
        }
        if (guilds == null) {
            log.info("No guilds specified - Add 'guilds' to dataMap");
            return null;
        }
        if (guilds.contains(getSource().getGuild().getID())) {
            Emoji hammer = EmojiManager.getForAlias("hammer");
            return hammer.getUnicode() + " " + getSource().getUser().getName();
        } else {
            return null;
        }
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        return new LinkedHashMap<>();
    }
}
