package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.StreamerService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;

@Component
public class Twitch implements CommandSupplier {

    private final StreamerService streamerService;

    @Autowired
    public Twitch(StreamerService streamerService) {
        this.streamerService = streamerService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(twitch());
    }

    private Command twitch() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Twitch IDs separated by spaces").ofType(String.class);
        OptionSpec<Void> addSpec = parser.accepts("add", "Add streamers, separated by spaces");
        OptionSpec<Void> removeSpec = parser.accepts("remove", "Remove streamers, separated by spaces");
        OptionSpec<Void> listSpec = parser.accepts("list", "List all registered streamers");
        OptionSpec<String> leagueSpec = parser.accepts("league", "When adding a stream, use to assign league/game info")
            .withRequiredArg().defaultsTo("all").describedAs("game");
        OptionSpec<String> divSpec = parser.acceptsAll(asList("div", "division"), "When adding a stream, use to assign division info")
            .requiredIf(leagueSpec).withRequiredArg().defaultsTo("none").describedAs("name");
        OptionSpec<String> titleSpec = parser.accepts("title", "When adding a stream, use to assign a title filter")
            .withRequiredArg().defaultsTo("").describedAs("name");
        parser.mutuallyExclusive((OptionSpecBuilder) addSpec, (OptionSpecBuilder) removeSpec);
        return CommandBuilder.of("twitch")
            .describedAs("Manage twitch tracked streamers")
            .withExamples(twitchExamples())
            .in("Integrations")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                OptionSet o = context.getOptionSet();
                List<String> ids = o.valuesOf(nonOptSpec);
                String league = o.has(leagueSpec) ? o.valueOf(leagueSpec) : null;
                String div = o.has(divSpec) ? o.valueOf(divSpec) : null;
                String title = o.has(titleSpec) ? o.valueOf(titleSpec) : null;
                if (o.has(addSpec)) {
                    ids.forEach(id -> streamerService.createStreamer("twitch", id, league, div, title));
                    answer(message, "Added" + (league == null ? "" : " to **" + league + "** league")
                        + (div == null ? "" : " and **" + div + "** division ") + ": "
                        + ids.stream().collect(Collectors.joining(", ")));
                } else if (o.has(removeSpec)) {
                    answer(message, "__Removed streamers__\n" + ids.stream()
                        .map(streamerService::deleteStreamer)
                        .flatMap(List::stream)
                        .map(streamer -> "• " + streamer.toShortString())
                        .collect(Collectors.joining("\n")));
                } else if (o.has(listSpec)) {
                    answer(message, "__Current streamers__\n" + streamerService.getStreamers().stream()
                        .map(streamer -> "• " + streamer.toShortString())
                        .collect(Collectors.joining("\n")));
                }
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private String twitchExamples() {
        return "- Add streams: `.twitch add streamerId1 streamerId2`\n"
            + "- Remove stream: `.twitch remove streamerId streamerId2`\n"
            + "- List streams: `.twitch list`\n\n"
            + "You can also set some values and filters when adding streams:\n\n"
            + "- With league info: `.twitch add league TF2 streamerId`\n"
            + "- With division info: `.twitch add league TF2 division Plat streamerId`\n"
            + "- With a title filter: `.twitch add league TF2 title UGC streamerId`\n"
            + "- This format is also allowed: `.twitch add league=TF2 division=Plat streamerId`\n"
            + "Each of the above supports multiple number of streamers defined, separated by spaces.\n"
            + "If you need to use spaces in any field, surround in 'quotes' or \"quotes\" to count as a single argument.\n";
    }
}
