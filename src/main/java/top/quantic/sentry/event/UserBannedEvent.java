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
        return "ban:" + getSource().getClient().getOurUser().getStringID()
            + ":" + getSource().getGuild().getStringID()
            + ":" + getSource().getUser().getStringID()
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
        if (guilds.contains(getSource().getGuild().getStringID())) {
            IUser user = getSource().getUser();
            String imagesSpec = (String) dataMap.get("images");
            String image = "";
            if (imagesSpec != null) {
                String[] images = imagesSpec.split(";");
                image = "\n" + images[random.nextInt(images.length)];
            }
            String emojiSpec = (String) dataMap.get("emoji");
	        String emoji = emoji("hammer");
	        if (emojiSpec != null) {
		        String[] emojis = emojiSpec.split(";");
		        emoji = "\n" + emojis[random.nextInt(emojis.length)];
		        if (emoji.startsWith("alias:")) {
		        	emoji = emoji(emoji);
		        }
	        }
            return user.getName() + "#" + user.getDiscriminator() + " " + emoji + image;
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
