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
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.service.SettingService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;

@Component
public class Twitch implements CommandSupplier {

    private final SettingService settingService;

    @Autowired
    public Twitch(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(twitch());
    }

    private Command twitch() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Twitch IDs separated by space\n\nExamples:\n"
            + "• Add streams: `.twitch add streamerId1 streamerId2`\n"
            + "• Remove stream: `.twitch remove streamerId streamerId2`\n"
            + "• List streams: `.twitch list`\n"
            + "You can also assign a **league** and **division** value when adding streams:\n"
            + "• With league info: `.twitch add league TF2 streamerId`\n"
            + "• With division info: `.twitch add league TF2 division Plat streamerId`\n"
            + "• This format is also allowed: `.twitch add league=TF2 division=Plat streamerId`\n"
            + "Each of the above supports multiple number of streamers defined, separated by spaces\n"
        ).ofType(String.class).describedAs("streamer1 streamer2 streamer3 ...");
        OptionSpec<Void> addSpec = parser.accepts("add", "Add streamers, separated by spaces");
        OptionSpec<Void> removeSpec = parser.accepts("remove", "Remove streamers, separated by spaces");
        OptionSpec<Void> listSpec = parser.accepts("list", "List all registered streamers");
        OptionSpec<String> leagueSpec = parser.accepts("league", "When adding a stream, use to assign league/game info")
            .withRequiredArg().defaultsTo("all");
        OptionSpec<String> divSpec = parser.acceptsAll(asList("div", "division"), "When adding a stream, use to assign division info")
            .requiredIf(leagueSpec).withRequiredArg().defaultsTo("none");
        parser.mutuallyExclusive((OptionSpecBuilder) addSpec, (OptionSpecBuilder) removeSpec);
        return CommandBuilder.of("twitch")
            .describedAs("Manage twitch tracked streamers")
            .in("Integrations")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                OptionSet o = context.getOptionSet();
                List<String> ids = o.valuesOf(nonOptSpec);
                String league = o.valueOf(leagueSpec);
                String div = o.valueOf(divSpec);
                String key = "twitch." + league.replace(".", "_") + "." + div.replace(".", "_");
                if (o.has(addSpec)) {
                    ids.forEach(id -> settingService.createSetting(guild(message), key, id));
                    answer(message, "Added" + (league.equals("all") ? "" : " to **" + league + "** league")
                        + (div.equals("none") ? "" : " and **" + div + "** division ") + ": "
                        + ids.stream().collect(Collectors.joining(", ")));
                } else if (o.has(removeSpec)) {
                    settingService.findByGuildAndKey(guild(message), key)
                        .forEach(setting -> settingService.delete(setting.getId()));
                    answer(message, "Removed *" + ids.stream().collect(Collectors.joining(", ")) + "*");
                } else if (o.has(listSpec)) {
                    answer(message,
                        "Streamers: " + settingService.findByGuildAndKeyStartingWith(guild(message),
                            "twitch.").stream().map(this::streamerInfo).collect(Collectors.joining(", ")));
                }
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private String streamerInfo(Setting setting) {
        String[] parts = setting.getKey().split("\\.");
        String league = null;
        String div = null;
        if (parts.length == 3) {
            league = parts[1];
            div = parts[2];
        } else if (parts.length == 2) {
            league = parts[1];
        }
        String result = "**" + setting.getValue() + "**";
        if (league != null) {
            result += " (" + league;
            if (div != null) {
                result += " " + div;
            }
            result += ")";
        }
        return result;
    }

    private String guild(IMessage message) {
        if (message.getChannel().isPrivate()) {
            return "*";
        } else {
            return message.getGuild().getID();
        }
    }
}
