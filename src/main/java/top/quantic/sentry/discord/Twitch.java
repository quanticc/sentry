package top.quantic.sentry.discord;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static top.quantic.sentry.discord.util.DiscordUtil.answer;

@Component
public class Twitch implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Twitch.class);

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
        OptionSpec<String> nonOptSpec = parser.nonOptions("Twitch IDs separated by space").ofType(String.class);
        OptionSpec<Void> addSpec = parser.accepts("add", "Add streamers");
        OptionSpec<Void> removeSpec = parser.accepts("remove", "Remove streamers");
        OptionSpec<Void> listSpec = parser.accepts("list", "List streamers");
        OptionSpec<String> gameSpec = parser.accepts("group", "Set group to store")
            .withRequiredArg().defaultsTo("all");
        return CommandBuilder.of("twitch")
            .describedAs("Manage twitch tracked streamers")
            .in("Integrations")
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                OptionSet o = context.getOptionSet();
                List<String> ids = o.valuesOf(nonOptSpec);
                String game = o.valueOf(gameSpec);
                if (o.has(addSpec)) {
                    ids.forEach(id -> settingService.createSetting(guild(message), "twitch." + game, id));
                    answer(message, "Added: " + ids.stream().collect(Collectors.joining(", ")));
                } else if (o.has(removeSpec)) {
                    settingService.findByGuildAndKey(guild(message), "twitch." + game)
                        .forEach(setting -> settingService.delete(setting.getId()));
                    answer(message, "Removed: " + ids.stream().collect(Collectors.joining(", ")));
                } else if (o.has(listSpec)) {
                    answer(message, "Streamers: " + settingService.findByGuildAndKey(guild(message), "twitch." + game).stream()
                        .map(Setting::getValue)
                        .collect(Collectors.joining(", ")));
                }
            }).build();
    }

    private String guild(IMessage message) {
        if (message.getChannel().isPrivate()) {
            return "*";
        } else {
            return message.getGuild().getID();
        }
    }

    private Setting newSetting(IMessage message, String game, String name) {
        Setting setting = new Setting();

        setting.setKey("twitch." + game);
        setting.setValue(name);
        return setting;
    }
}
