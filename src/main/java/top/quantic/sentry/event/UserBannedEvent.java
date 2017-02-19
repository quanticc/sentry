package top.quantic.sentry.event;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserBannedEvent extends SentryEvent {

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
        Emoji hammer = EmojiManager.getForAlias("hammer");
        return hammer.getUnicode() + " " + getSource().getUser().getName();
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
