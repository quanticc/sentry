package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.GameQueryService;
import top.quantic.sentry.web.rest.vm.OverwatchStats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Overwatch implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Overwatch.class);

    private final GameQueryService gameQueryService;

    @Autowired
    public Overwatch(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(overwatch());
    }

    private Command overwatch() {
        return CommandBuilder.of("overwatch", "ow")
            .describedAs("Get information about an Overwatch player")
            .withExamples("Usage: **overwatch** __tag__")
            .in("Integrations")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                String content = context.getContentAfterCommand();
                if (isBlank(content)) {
                    answer(message, "Please include a Battle.net tag");
                    return;
                }
                String[] tags = content.split(" ");
                RequestBuffer.RequestFuture<IMessage> header =
                    sendMessage(channel, "Retrieving stats for " + inflect(tags.length, "player") + "...");
                for (String tag : tags) {
                    if (!tag.matches("^[A-Za-z][A-Za-z0-9]{2,11}[#-][0-9]{4,5}$")) {
                        answer(message, "Invalid Battle.net tag: " + tag);
                    }
                    OverwatchStats stats = gameQueryService.getOverwatchStats(tag);
                    if (stats == null) {
                        sendMessage(channel, authoredErrorEmbed(message)
                            .withTitle(tag)
                            .withDescription("Unable to find Overwatch player")
                            .build());
                    } else {
                        OverwatchStats.ModeStats quickplay = stats.getRegionStats().getStats().get("quickplay");
                        Map<String, Object> quickStats = quickplay.getOverallStats();
                        OverwatchStats.ModeStats competitive = stats.getRegionStats().getStats().get("competitive");
                        Map<String, Object> compStats = competitive.getOverallStats();
                        sendMessage(channel, authoredSuccessEmbed(message)
                            .withTitle(tag)
                            .withDescription("Player Stats")
                            .withThumbnail(getString(quickStats, "avatar"))
                            .appendField("Region", stats.getRegion(), true)
                            .appendField("Level", "" + (getInt(quickStats, "prestige") * 100 + getInt(quickStats, "level")), true)
                            .appendField("Quickplay Wins", getString(quickStats, "wins"), true)
                            .appendField("Competitive Win/Loss/Tie", getString(compStats, "wins") + "-" + getString(compStats, "losses") + "-" + getString(compStats, "ties"), true)
                            .appendField("Skill Rating", getString(compStats, "comprank"), true)
                            .build());
                    }
                }
                CompletableFuture.runAsync(() -> deleteMessage(header.get()));
            }).build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : (int) getDouble(map, key);
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : Double.valueOf((String) value);
    }
}
