package top.quantic.sentry.discord;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.toMultimap;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.humanizeShort;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class GameServers implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(GameServers.class);

    private final GameServerService gameServerService;
    private final SettingService settingService;

    @Autowired
    public GameServers(GameServerService gameServerService, SettingService settingService) {
        this.gameServerService = gameServerService;
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(rcon(), servers(), server());
    }

    private Command server() {
        return CommandBuilder.of("server")
            .describedAs("Get all UGC managed GameServers")
            .in("Integrations")
            .withExamples(serverExamples())
            .nonParsed()
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String[] args = safeSplit(context.getArgs(), 3);
                if (args == null || args.length < 2) {
                    answer(message, "Please specify at least two arguments: action, server");
                    return;
                }
                String action = args[0];
                String serverQuery = args[1];
                List<GameServer> targets = gameServerService.findServersMultiple(Arrays.asList(serverQuery.split(",|;")));
                if (action.equals("restart")) {
                    String response = "Restarting servers matching " + serverQuery + "\n";
                    for (GameServer target : targets) {
                        response = resultLine(target, gameServerService.tryRestart(target));
                    }
                    answer(message, response);
                } else if (action.equals("stop")) {
                    String response = "Stopping servers matching " + serverQuery + "\n";
                    for (GameServer target : targets) {
                        response = resultLine(target, gameServerService.tryStop(target));
                    }
                    answer(message, response);
                } else if (action.equals("update")) {
                    String response = "Updating game version on servers matching " + serverQuery + "\n";
                    for (GameServer target : targets) {
                        response = resultLine(target, gameServerService.tryUpdate(target));
                    }
                    answer(message, response);
                } else if (action.equals("install-mod")) {
                    if (args.length < 3) {
                        answer(message, "You must add an extra argument with the mod ID or name");
                        return;
                    }
                    String mod = args[2];
                    Optional<Setting> setting = settingService.findMostRecentByGuildAndKey(Constants.ANY, mod);
                    String modName = setting.isPresent() ? setting.get().getValue() : mod;
                    String response = "Installing mod " + modName + " on servers matching " + serverQuery + "\n";
                    for (GameServer target : targets) {
                        response = resultLine(target, gameServerService.tryModInstall(target, modName));
                    }
                    answerPrivately(message, response);
                } else if (action.equals("status")) {
                    String response = "Retrieving status for servers matching " + serverQuery + "\n```\n";
                    for (GameServer target : targets) {
                        GameServer server = gameServerService.refreshStatus(target);
                        response += server.getSummary() + "\n";
                    }
                    response += "\n```";
                    answerPrivately(message, response);
                    if (targets.size() == 1) {
                        GameServer target = targets.get(0);
                        Result<String> result = gameServerService.tryRcon(target, "status");
                        if (result.isSuccessful()) {
                            answer(message, "**" + target.getShortNameAndAddress() + "**\n```\n" +
                                result.getContent() + "\n```");
                        } else {
                            log.debug("Unsuccessful response from {} for command status: {}", target, result.getContent());
                            sendMessage(message.getChannel(), new EmbedBuilder()
                                .setLenient(true)
                                .withTitle("Status of " + target.getShortNameAndAddress())
                                .appendField("Response", result.getMessage() +
                                    (result.getError() != null ? "(" + result.getError().getMessage() + ")" : ""), false)
                                .withColor(new Color(0xaa0000))
                                .build());
                        }
                    }
                } else if (action.equals("console")) {
                    String response = "Retrieving console for servers matching " + serverQuery + "\n";
                    for (GameServer target : targets) {
                        Result<String> result = gameServerService.tryGetConsole(target);
                        if (result.isSuccessful()) {
                            response = "• [**" + target.getShortName() + "**] (" + target.getAddress() + ")\n" + result.getContent() + "\n";
                        } else {
                            response = resultLine(target, result);
                        }
                    }
                    answerPrivately(message, response);
                } else {
                    answer(message, "Invalid action, must be one of: restart, stop, update, install-mod, status or console.");
                }
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private String resultLine(GameServer server, Result<?> result) {
        return "• [**" + server.getShortName() + "**] (" + server.getAddress() + ") " + result.getMessage() + "\n";
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
                String[] args = safeSplit(context.getArgs(), 2);
                if (args == null || args.length < 2) {
                    answer(message, "Requires at least 2 arguments: servers and command");
                    return;
                }
                String serverQuery = args[0];
                String command = args[1];
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
            "(IP address, name, region), separated by commas.";
    }

    private String serversExamples() {
        return "Usage: servers [filter]\nWhere **[filter]** is an optional query to filter by IP address, name or region.";
    }

    private String serverExamples() {
        return "Usage: server <action> <server> [args]\n" +
            "Where **<action>** is one of: restart, stop, update, install-mod, status or console.\n" +
            "And **<server>** is a list of GameServers (IP address, name or region), separated by commas.\n" +
            "Additionally, some actions like 'install-mod' take an extra argument at the end for the mods to install.\n";
    }
}
