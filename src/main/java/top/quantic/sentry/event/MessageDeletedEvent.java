package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import top.quantic.sentry.discord.util.DiscordUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageDeletedEvent extends SentryEvent {

	public MessageDeletedEvent(MessageDeleteEvent source) {
		super(source);
	}

	@Override
	public MessageDeleteEvent getSource() {
		return (MessageDeleteEvent) super.getSource();
	}

	@Override
	public String getContentId() {
		return "deleted:" + getSource().getMessage().getID();
	}

	@Override
	public String asContent(Map<String, Object> dataMap) {
		MessageDeleteEvent source = getSource();
		return source.getAuthor() + " deleted a message: " + DiscordUtil.humanize(source.getMessage());
	}

	@Override
	public EmbedObject asEmbed(Map<String, Object> dataMap) {
		MessageDeleteEvent source = getSource();
		return source.getMessage().getEmbedded().stream()
				.findFirst()
				.map(EmbedObject::new)
				.orElse(null);
	}

	@Override
	public Map<String, Object> asMap(Map<String, Object> dataMap) {
		return new LinkedHashMap<>();
	}
}
