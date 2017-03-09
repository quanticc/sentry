package top.quantic.sentry.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.member.UserPardonEvent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static top.quantic.sentry.discord.util.DiscordUtil.emoji;

public class UserPardonedEvent extends SentryEvent {

    private static final Logger log = LoggerFactory.getLogger(UserPardonedEvent.class);

    public UserPardonedEvent(UserPardonEvent event) {
        super(event);
    }

    @Override
    public UserPardonEvent getSource() {
        return (UserPardonEvent) source;
    }

    @Override
    public String getContentId() {
        return "pardon:" + getSource().getClient().getOurUser().getID()
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
            return getSource().getUser().getName() + " " + emoji("angel");
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
