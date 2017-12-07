package top.quantic.sentry.discord;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;
import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.discord.module.DiscordSubscriber;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;

@Component
public class Chatter implements DiscordSubscriber, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(Chatter.class);
    private static final Pattern UNICODE = Pattern.compile("\\|([\\da-fA-F]+)");

    private final SentryProperties sentryProperties;
    private final Executor taskExecutor;

    private final Map<String, ChatterBotSession> sessionMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private ChatterBot clever;

    @Autowired
    public Chatter(SentryProperties sentryProperties, Executor taskExecutor) {
        this.sentryProperties = sentryProperties;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        clever = createClever();
    }

    private ChatterBot createClever() {
        try {
            return new ChatterBotFactory()
                .create(ChatterBotType.CLEVERBOT, sentryProperties.getDiscord().getCleverBotApiKey());
        } catch (Exception e) {
            log.warn("Could not create CleverBot session", e);
        }
        return null;
    }

    private ChatterBotSession getSession(String key) {
        log.debug("Creating new session for key {}", key);
        return sessionMap.computeIfAbsent(key, k -> clever.createSession());
    }

    @EventSubscriber
    public void onMention(MentionEvent event) {
        if (clever == null) {
            clever = createClever();
            if (clever == null) {
                return;
            }
        }
        IMessage message = event.getMessage();
        IChannel channel = event.getMessage().getChannel();
        IUser author = event.getMessage().getAuthor();
        boolean everyone = message.mentionsEveryone() || message.mentionsHere();
        boolean dm = channel.isPrivate();
        boolean self = event.getClient().getOurUser().equals(author);
        if (!everyone && !dm && !self) {
            CompletableFuture.runAsync(() -> {
                synchronized (lock) {
                    channel.toggleTypingStatus();
                    long start = System.currentTimeMillis();
                    String content = EmojiParser.parseToAliases(message.getContent()
                            .replace(event.getClient().getOurUser().mention(true), "")
                            .replace(event.getClient().getOurUser().mention(false), ""),
                        EmojiParser.FitzpatrickAction.REMOVE);
                    try {
                        log.debug("[{}ms] >>> {}", System.currentTimeMillis() - start, content);
                        String response = getSession(author.getStringID()).think(content);
                        response = StringEscapeUtils.unescapeHtml4(response);
                        Matcher matcher = UNICODE.matcher(response);
                        while (matcher.find()) {
                            String hex = matcher.group(1);
                            response = matcher.replaceFirst(new String(Character.toChars(Integer.parseInt(hex, 16))));
                        }
                        long delay = System.currentTimeMillis() - start;
                        log.debug("[{}ms] <<< ", delay, response);
                        if (delay < 3000L) {
                            long sleep = Math.min(3000L, (response.length() + 1) * 100L) - delay;
                            log.debug("Sleeping for {} ms", sleep);
                            Thread.sleep(sleep);
                        }
                        sendMessage(channel, author.mention() + " " + response);
                    } catch (Exception e) {
                        log.warn("Could not process chatter input", e);
                        channel.toggleTypingStatus();
                    }
                }
            }, taskExecutor);
        }
    }
}
