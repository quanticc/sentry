package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.obj.IUser;

import java.util.*;

import static top.quantic.sentry.discord.util.DiscordUtil.emoji;

public class UserBannedEvent extends SentryEvent {

    private Random random = new Random();

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
            guilds = Arrays.asList(guildSpec.split("[,;]"));
        }
        if (guilds == null) {
            return null;
        }
        if (guilds.contains(getSource().getGuild().getID())) {
            IUser user = getSource().getUser();
            String imagesSpec = (String) dataMap.get("images");
            String image;
            if (imagesSpec != null) {
                String[] images = imagesSpec.split(";");
                image = "\n" + images[random.nextInt(images.length)];
            } else {
                image = " " + emoji("hammer");
            }
            return user.getName() + "#" + user.getDiscriminator() + image;
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
