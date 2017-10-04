package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.member.UserPardonEvent;
import sx.blah.discord.handle.obj.IUser;

import java.util.*;

import static top.quantic.sentry.discord.util.DiscordUtil.emoji;

public class UserPardonedEvent extends SentryEvent {

	private Random random = new Random();

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
            guilds = Arrays.asList(guildSpec.split("[,;]"));
        }
        if (guilds == null) {
            return null;
        }

        if (guilds.contains(getSource().getGuild().getID())) {
            IUser user = getSource().getUser();
	        String imagesSpec = (String) dataMap.get("images");
	        String image = "";
	        if (imagesSpec != null) {
		        String[] images = imagesSpec.split(";");
		        image = "\n" + images[random.nextInt(images.length)];
	        }
	        String emojiSpec = (String) dataMap.get("emoji");
	        String emoji = emoji("angel");
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
