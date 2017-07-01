package top.quantic.sentry.discord;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.Monitor;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.toMultimap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.*;
import static top.quantic.sentry.service.util.DateUtil.humanizeShort;
import static top.quantic.sentry.service.util.MiscUtil.getIPAddress;
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
            .describedAs("Perform operations on UGC managed GameServers")
            .in("Integrations")
            .withExamples(serverExamples())
            .nonParsed()
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IChannel channel = getTrustedChannel(settingService, message);
                String[] args = safeSplit(context.getArgs(), 3);
                if (args == null || args.length < 2) {
                    answer(message, "Please specify at least two arguments: action, server");
                    return;
                }
                String action = args[0];
                String serverQuery = args[1];
                List<GameServer> targets = gameServerService.findServersMultiple(Arrays.asList(serverQuery.split("[,;]")));
                String targetList = targets.stream()
                    .map(GameServer::getShortNameAndAddress)
                    .collect(Collectors.joining(", "));
                if ("restart".equals(action)) {
                    answerToChannel(channel, "Restarting " + inflect(targets.size(), "server") + ": " + targetList);
                    int processed = process(targets, gameServerService::tryRestart, channel);
                    if (processed - targets.size() > 0) {
                        answerToChannel(channel, "Completed: " + (processed - targets.size()) + " out of " + inflect(targets.size(), "server") + " processed with errors.");
                    } else {
                        answerToChannel(channel, "Completed: " + inflect(targets.size(), "server") + " successfully processed.");
                    }
                } else if ("stop".equals(action)) {
                    answerToChannel(channel, "Stopping " + inflect(targets.size(), "server") + ": " + targetList);
                    int processed = process(targets, gameServerService::tryStop, channel);
                    if (processed - targets.size() > 0) {
                        answerToChannel(channel, "Completed: " + (processed - targets.size()) + " out of " + inflect(targets.size(), "server") + " processed with errors.");
                    } else {
                        answerToChannel(channel, "Completed: " + inflect(targets.size(), "server") + " successfully processed.");
                    }
                } else if ("update".equals(action)) {
                    answerToChannel(channel, "Updating game version on servers matching " + serverQuery);
                    int processed = process(targets, gameServerService::tryUpdate, channel);
                    if (processed - targets.size() > 0) {
                        answerToChannel(channel, "Completed: " + (processed - targets.size()) + " out of " + inflect(targets.size(), "server") + " processed with errors.");
                    } else {
                        answerToChannel(channel, "Completed: " + inflect(targets.size(), "server") + " successfully processed.");
                    }
                } else if ("install-mod".equals(action)) {
                    if (args.length < 3) {
                        answer(message, "You must add an extra argument with the mod ID or name");
                        return;
                    }
                    String mod = args[2];
                    Optional<Setting> setting = settingService.findMostRecentByGuildAndKey(Constants.ANY, mod);
                    String modName = setting.map(Setting::getValue).orElse(mod);
                    answerToChannel(channel, "Installing mod " + modName + " on servers matching " + serverQuery);
                    int processed = process(targets, target -> gameServerService.tryModInstall(target, modName), channel);
                    if (processed - targets.size() > 0) {
                        answerToChannel(channel, "Completed: " + (processed - targets.size()) + " out of " + inflect(targets.size(), "server") + " processed with errors.");
                    } else {
                        answerToChannel(channel, "Completed: " + inflect(targets.size(), "server") + " successfully processed.");
                    }
                } else if ("status".equals(action)) {
                    answerPrivately(message, "Retrieving status for servers matching " + serverQuery);
                    int latest = gameServerService.getLatestVersion();
                    for (GameServer target : targets) {
                        GameServer server = gameServerService.refreshStatus(target);
                        EmbedBuilder builder = new EmbedBuilder()
                            .setLenient(true)
                            .withAuthorIcon("https://quantic.top/sentry.png")
                            .withAuthorName("Sentry")
                            .withAuthorUrl("https://sentry.quantic.top")
                            .withTitle("Status of " + target.getShortNameAndAddress())
                            .withImage("http://cache.gametracker.com/server_info/" + target.getAddress() +
                                "/b_350_20_692108_381007_FFFFFF_000000.png");
                        if (gameServerService.getState(server) == Monitor.State.BAD) {
                            builder.withColor(new Color(0xaa0000))
                                .withDescription("Server appears to be down since " + formatRelative(server.getLastValidPing()));
                        } else {
                            if (server.isUpdating()) {
                                builder.withColor(new Color(0x0000aa))
                                    .withDescription("Server is being updated")
                                    .appendField("Attempts", "" + target.getUpdateAttempts(), true)
                                    .appendField("Started", formatRelative(target.getLastUpdateStart()), false);
                            } else if (server.getPing() > 1000) {
                                builder.withColor(new Color(0xaaaa00))
                                    .withDescription("Server is experiencing connectivity issues")
                                    .appendField("Last Ping", formatRelativeWithNow(target.getLastValidPing()), false);
                            } else {
                                builder.withColor(new Color(0x00aa00))
                                    .withDescription(targets.size() > 1 ? "Server is online" : "Get more details with `.rcon " + target.getShortName() + " status`")
                                    .appendField("Map", target.getMap(), true)
                                    .appendField("Players", target.getPlayers() + " / " + target.getMaxPlayers(), true)
                                    .appendField("Connect", "steam://connect/" + target.getAddress() + "/" + target.getSvPassword(), false);
                                if (target.getTvPort() != null && target.getTvPort() > 0) {
                                    builder.appendField("SourceTV", "steam://connect/" + getIPAddress(target.getAddress()) + ":" + target.getTvPort() + "/", false);
                                }
                                if (target.getExpirationDate() != null && target.getExpirationDate().isAfter(ZonedDateTime.now())) {
                                    builder.appendField("Expires", withRelative(target.getExpirationDate()), false);
                                }
                            }
                            builder.appendField("Version", "v" + target.getVersion(), true);
                            if (target.getVersion() != latest) {
                                builder.appendField("Status", "Outdated (Latest: " + latest + ")", true);
                            } else {
                                builder.appendField("Status", "Up-to-date", true);
                            }
                        }
                        sendMessage(message.getChannel(), builder.build());
                    }
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
                                .appendField("Response", "Could not connect via RCON" +
                                    (result.getError() != null ? " (" +
                                        getRootCause(result.getError()).getClass().getSimpleName() + ")" : ""), false)
                                .withColor(new Color(0xaa0000))
                                .build());
                        }
                    }
                } else if ("console".equals(action)) {
                    answerToChannel(channel, "Retrieving console for servers matching " + serverQuery);
                    int errors = 0;
                    for (GameServer target : targets) {
                        Result<String> result = gameServerService.tryGetConsole(target);
                        String response;
                        if (result.isSuccessful()) {
                            response = "• [**" + target.getShortName() + "**] (" + target.getAddress() + ")\n" + result.getContent() + "\n";
                        } else {
                            errors++;
                            response = resultLine(target, result);
                        }
                        answerToChannel(channel, response);
                    }
                    if (errors > 0) {
                        answerToChannel(channel, "Completed: " + errors + " out of " + inflect(targets.size(), "server") + " processed with errors.");
                    } else {
                        answerToChannel(channel, "Completed: " + inflect(targets.size(), "server") + " successfully processed.");
                    }
                } else {
                    answer(message, "Invalid action, must be one of: restart, stop, update, install-mod, status or console.");
                }
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();
    }

    private int process(List<GameServer> servers, Function<GameServer, Result<?>> action, IChannel channel) {
        RequestBuffer.RequestFuture<IMessage> status = null;
        int processed = 0;
        String current = "";
        for (GameServer target : servers) {
            Result<?> result = action.apply(target);
            if (result.isSuccessful()) {
                processed++;
            }
            String toAppend = resultLine(target, result);
            if (status == null || current.length() + toAppend.length() > Message.MAX_MESSAGE_LENGTH) {
                status = answerToChannel(channel, toAppend);
            } else {
                IMessage newStatus = status.get();
                String newContent = current + toAppend;
                status = RequestBuffer.request(() -> (IMessage) newStatus.edit(newContent));
            }
            current = status.get().getContent();
        }
        return processed;
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
                List<GameServer> targets = gameServerService.findServersMultiple(Arrays.asList(serverQuery.split("[,;]")));
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
        return "Usage: **rcon** <__servers__> <__command__>\n" +
            "Where **servers** is a list of targeted GameServers (IP address, name, region), separated by commas.\n" +
            "For example to get the status of all Chicago servers: `rcon chi status`\n" +
            "To say hello to three select servers: `rcon chi2,dal7,mia2 say Hey!`\n" +
            "To run a payload config and then change map on chi5 server: `rcon chi5 exec ugc_hl_stopwatch;changelevel pl_upward`\n";
    }

    private String serversExamples() {
        return "Usage: **servers** [__filter__]\nWhere **filter** is an optional query to filter by IP address, name or region.\n";
    }

    private String serverExamples() {
        return "Usage: **server** <__action__> <__server__> [__args__]\n" +
            "Where **action** is one of: restart, stop, update, install-mod, status or console.\n" +
            "And **server** is a list of GameServers (IP address, name or region), separated by commas.\n" +
            "Additionally, some actions like 'install-mod' take an extra argument at the end for the mods to install.\n";
    }
}
