package top.quantic.sentry.discord;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import sx.blah.discord.Discord4J;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static top.quantic.sentry.discord.util.DiscordUtil.answer;
import static top.quantic.sentry.service.util.DateUtil.humanize;

@Component
public class Info implements CommandSupplier {

    private final BuildProperties buildProperties;

    @Autowired
    public Info(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public List<Command> getCommands() {
        return Lists.newArrayList(info());
    }

    private Command info() {
        return CommandBuilder.of("info")
            .describedAs("Get Discord information about the bot")
            .in("General")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String version = buildProperties.getVersion();
                version = (version == null ? "snapshot" : version);
                RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
                long uptime = rb.getUptime();
                String content = "Hey! I'm here to help with **UGC support**.\n\n" +
                    "**Version:** " + version + '\n' +
                    "**Discord4J:** " + Discord4J.VERSION + '\n' +
                    "**Uptime:** " + humanize(Duration.ofMillis(uptime)) +'\n';
                answer(message, content);
            }).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) value;
    }
}
