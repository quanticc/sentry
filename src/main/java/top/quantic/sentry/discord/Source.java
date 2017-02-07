package top.quantic.sentry.discord;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.toMultimap;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;
import static top.quantic.sentry.service.util.DateUtil.humanizeShort;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Source implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Source.class);

    private final GameServerService gameServerService;

    @Autowired
    public Source(GameServerService gameServerService) {
        this.gameServerService = gameServerService;
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(rcon(), servers());
    }

    private Command servers() {
        return CommandBuilder.of("servers")
            .describedAs("Get all UGC managed GameServers")
            .in("Integrations")
            .withExamples(serversExamples())
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String response;
                List<GameServer> servers;
                if (context.getArgs() != null) {
                    String query = context.getArgs()[0].split(" ", 1)[0];
                    response = "GameServers matching " + query + "\n";
                    servers = gameServerService.findServers(query);
                } else {
                    response = "GameServers\n";
                    servers = gameServerService.findServers();
                }
                Multimap<String, GameServer> serverByRegion = servers.stream()
                    .collect(toMultimap(
                        GameServer::getRegion,
                        server -> server,
                        MultimapBuilder.treeKeys().arrayListValues()::build
                    ));
                for (Map.Entry<String, Collection<GameServer>> region : serverByRegion.asMap().entrySet()) {
                    response += "*" + region.getKey() + "*\n"
                        + region.getValue().stream()
                        .sorted(Comparator.comparingInt(GameServer::getRegionNumber))
                        .map(server -> "• [**" + server.getShortName() + "**] " + server.getAddress())
                        .collect(Collectors.joining("\n")) + "\n";
                }
                answer(message, response);
            }).build();
    }

    private Command rcon() {
        return CommandBuilder.of("rcon")
            .describedAs("Interact with a GameServer via RCON")
            .in("Integrations")
            .withExamples(rconExamples())
            .nonParsed()
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String[] queries = context.getArgs()[0].split(" ", 2);
                if (queries.length < 2) {
                    answer(message, "Requires at least 2 arguments: servers and command");
                    return;
                }
                String serverQuery = queries[0];
                String command = queries[1];
                boolean quiet = command.contains("exec");
                List<GameServer> targets = gameServerService.findServersMultiple(Arrays.asList(serverQuery.split(",|;")));
                if (targets.isEmpty()) {
                    answer(message, "Could not find any server given your query");
                    return;
                }
                answer(message, "Connecting to " + inflect(targets.size(), "server") + ": "
                    + targets.stream().map(GameServer::getShortNameAndAddress).collect(Collectors.joining(", ")));
                CompletableFuture.runAsync(() -> {
                    for (GameServer target : targets) {
                        long start = System.currentTimeMillis();
                        Result<String> response = gameServerService.tryRcon(target, command);
                        if (response.isSuccessful() && response.getContent().length() > EmbedBuilder.FIELD_CONTENT_LIMIT) {
                            answer(message, "**" + target.getShortNameAndAddress() + "**\n```\n" +
                                response.getContent() + "\n```");
                        } else {
                            EmbedBuilder builder = new EmbedBuilder()
                                .setLenient(true)
                                .withTitle("RCON to " + target.getShortNameAndAddress())
                                .appendField("Command", "`" + command + "`", false);
                            if (response.isSuccessful()) {
                                log.debug("RCON response from {} for command {} is: {}", target, command, response.getContent());
                                Duration millis = Duration.ofMillis(System.currentTimeMillis() - start);
                                String result = quiet ? "Executed in " + humanizeShort(millis) : "```\n" + response.getContent() + "\n```";
                                builder.appendField("Response", result, false)
                                    .withColor(new Color(0x00aa00));
                            } else {
                                log.debug("Unsuccessful response from {} for command {}: {}", target, command, response.getContent());
                                builder.appendField("Response", response.getMessage() +
                                    (response.getError() != null ? "(" + response.getError().getMessage() + ")" : ""), false)
                                    .withColor(new Color(0xaa0000));
                            }
                            sendMessage(message.getChannel(), builder.build());
                        }
                    }
                });
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private String rconExamples() {
        return "Usage: rcon <servers> <command>\nWhere **<servers>** is a list of targeted GameServers " +
            "(IP address, short name, region group), separated by commas.";
    }

    private String serversExamples() {
        return "Usage: servers [filter]\nWhere **[filter]** is an optional query to filter by IP address, name or region.";
    }
}
