package top.quantic.sentry.discord;

import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamPlayerProfile;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.GameQueryService;
import top.quantic.sentry.service.UgcService;
import top.quantic.sentry.service.util.Result;
import top.quantic.sentry.web.rest.vm.UgcPlayer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;
import static top.quantic.sentry.config.Constants.UGC_NEW_DATE_FORMAT;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.withRelative;
import static top.quantic.sentry.service.util.SteamIdConverter.steamId64To3;

@Component
public class Roster implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Roster.class);

    private static final int NAME_MAX_WIDTH = 20;
    private static final int TEAM_MAX_WIDTH = 30;
    private static final ZoneId UGC_ZONE_ID = ZoneId.of("America/New_York");
    private static final int ELIGIBLE_HOURS = 18;

    private static final Pattern STATUS = Pattern.compile("^.+\"(.+)\"\\s+(\\[([a-zA-Z]):([0-5]):([0-9]+)(:[0-9]+)?])\\s+.*$", Pattern.MULTILINE);
    private static final Pattern LOG_LINE = Pattern.compile("^.*\"(.+)<([0-9]+)><(\\[([a-zA-Z]):([0-5]):([0-9]+)(:[0-9]+)?])>.*$", Pattern.MULTILINE);
    private static final Pattern STEAM_3 = Pattern.compile("(\\[U:([0-5]):([0-9]+)(:[0-9]+)?])", Pattern.MULTILINE);
    private static final Pattern STEAM_ID_64 = Pattern.compile("([0-9]{17,19})", Pattern.MULTILINE);

    private final UgcService ugcService;
    private final GameQueryService gameQueryService;

    @Autowired
    public Roster(UgcService ugcService, GameQueryService gameQueryService) {
        this.ugcService = ugcService;
        this.gameQueryService = gameQueryService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(check());
    }

    private Command check() {
        return CommandBuilder.of("check")
            .describedAs("Check status output against UGC roster data")
            .in("Integrations")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String withCommand = context.getContentAfterPrefix();
                String content = withCommand.contains(" ") ? withCommand.split(" ", 2)[1] : null;
                if (isBlank(content)) {
                    answer(message, "Please paste output of a server `status` command");
                    return;
                }
                RequestBuffer.RequestFuture<IMessage> header = answer(message, "Retrieving data from UGC...");
                CompletableFuture.supplyAsync(() -> doCheck(content))
                    .whenCompleteAsync((result, error) -> {
                        if (error == null) {
                            if (result != null && result.isSuccessful()) {
                                answer(message, result.getContent().getLeft()).get();
                                for (Pair<RosterData, UgcPlayer.Membership> join : result.getContent().getRight()) {
                                    sendMessage(message.getChannel(), warningEmbed()
                                        .withTitle("Warning")
                                        .withDescription(join.getLeft().getServerName() + " joined " +
                                            join.getRight().getName() + " less than " + ELIGIBLE_HOURS + " hours ago!")
                                        .appendField("Player", join.getLeft().getServerName() +
                                            "\nhttp://www.ugcleague.com/players_page.cfm?player_id=" + join.getLeft().getCommunityId(), false)
                                        .appendField("Team", join.getRight().getName() +
                                            "\nhttp://www.ugcleague.com/team_page.cfm?clan_id=" + join.getRight().getClanId(), false)
                                        .appendField("Joined", ugcDateToInstant(join.getRight().getJoinedTeam()).toString(), false)
                                        .appendField("Eligible to play", withRelative(getEligibleToPlayDate(join.getRight())), false)
                                        .build()).get();
                                }
                            } else {
                                sendMessage(message.getChannel(), authoredErrorEmbed(message)
                                    .withTitle("Roster Check")
                                    .withDescription("Operation could not be completed")
                                    .build());
                            }
                        } else {
                            log.warn("Failed with error", error);
                            sendMessage(message.getChannel(), authoredErrorEmbed(message)
                                .withTitle("Roster Check")
                                .withDescription("Error while processing your request. Report this!")
                                .build());
                        }
                        deleteMessage(header.get());
                    });
            }).build();
    }

    private Result<Pair<String, List<Pair<RosterData, UgcPlayer.Membership>>>> doCheck(String content) {
        Matcher statusMatcher = STATUS.matcher(content);
        Matcher logMatcher = LOG_LINE.matcher(content);
        Matcher steam3Matcher = STEAM_3.matcher(content);
        Matcher steamId64Matcher = STEAM_ID_64.matcher(content);
        StringBuilder builder = new StringBuilder("```asciidoc\n");
        Set<RosterData> players = new LinkedHashSet<>();
        while (statusMatcher.find()) {
            RosterData player = new RosterData();
            player.setServerName(statusMatcher.group(1));
            player.setModernId(statusMatcher.group(2));
            player.setCommunityId(gameQueryService.getSteamId64(player.getModernId())
                .exceptionally(t -> {
                    log.warn("Could not resolve {} to a steamId64", player.getModernId(), t);
                    return null;
                }).join());
            if (!players.contains(player)) {
                log.debug("Matched by status: {}", player);
                players.add(player);
            }
        }
        while (logMatcher.find()) {
            RosterData player = new RosterData();
            player.setServerName(logMatcher.group(1));
            player.setModernId(logMatcher.group(3));
            player.setCommunityId(gameQueryService.getSteamId64(player.getModernId())
                .exceptionally(t -> {
                    log.warn("Could not resolve {} to a steamId64", player.getModernId(), t);
                    return null;
                }).join());
            if (!players.contains(player)) {
                log.debug("Matched by log: {}", player);
                players.add(player);
            }
        }
        while (steam3Matcher.find()) {
            RosterData player = new RosterData();
            player.setModernId(steam3Matcher.group(1));
            player.setCommunityId(gameQueryService.getSteamId64(player.getModernId())
                .exceptionally(t -> {
                    log.warn("Could not resolve {} to a steamId64", player.getModernId(), t);
                    return null;
                }).join());
            if (player.getCommunityId() != null) {
                player.setServerName(getProfileName(player.getCommunityId()));
            }
            if (!players.contains(player)) {
                log.debug("Matched by Steam3ID: {}", player);
                players.add(player);
            }
        }
        while (steamId64Matcher.find()) {
            RosterData player = new RosterData();
            player.setCommunityId(gameQueryService.getSteamId64(steamId64Matcher.group(1))
                .exceptionally(t -> {
                    log.warn("Could not resolve {} to a steamId64", player.getModernId(), t);
                    return null;
                }).join());
            if (player.getCommunityId() != null) {
                player.setModernId(steamId64To3(player.getCommunityId()));
                player.setServerName(getProfileName(player.getCommunityId()));
            }
            if (!players.contains(player) && player.getModernId() != null) {
                log.debug("Matched by SteamId64: {}", player);
                players.add(player);
            }
        }
        if (players.isEmpty()) {
            return Result.error("No valid player data, Steam3IDs or SteamId64s found");
        }
        String filter = "";
        if (content.startsWith("HL ") || content.startsWith("hl ") || content.startsWith("9 ") || content.startsWith("9v ") || content.startsWith("9v9 ")) {
            filter = "9v9";
        } else if (content.startsWith("6s ") || content.startsWith("6 ") || content.startsWith("6v ") || content.startsWith("6v6 ")) {
            filter = "6v6";
        } else if (content.startsWith("4s ") || content.startsWith("4 ") || content.startsWith("4v ") || content.startsWith("4v4 ")) {
            filter = "4v4";
        }
        List<RosterData> result = players.parallelStream()
            .map(rd -> {
                try {
                    return rd.updateUgcData(ugcService.getPlayer(rd.getCommunityId()));
                } catch (IOException e) {
                    log.warn("Could not update with data from UGC", e);
                    return rd;
                }
            }).collect(Collectors.toList());
        int idWidth = "[U:X:---------]".length() + 1;
        int nameWidth = Math.min(result.stream().map(d -> d.getServerName().length()).reduce(0, Integer::max), NAME_MAX_WIDTH) + 2;
        int teamWidth = Math.min(result.stream().filter(d -> d.getPlayer() != null && d.getPlayer().getTeam() != null)
            .flatMap(d -> d.getPlayer().getTeam().stream())
            .map(t -> (t.getClanId() + " ").length() + t.getName().length()).reduce(0, Integer::max), TEAM_MAX_WIDTH) + 2;
        int divWidth = result.stream().filter(d -> d.getPlayer() != null && d.getPlayer().getTeam() != null)
            .flatMap(d -> d.getPlayer().getTeam().stream())
            .map(t -> t.getDivision().length()).reduce(0, Integer::max) + 2;
        int formatWidth = "9v9".length() + 2;
        builder.append(rightPad("Steam3ID", idWidth)).append(rightPad("Name", nameWidth))
            .append(rightPad("Team", teamWidth)).append(rightPad("Division", divWidth))
            .append(rightPad("Mode", formatWidth)).append("\n")
            .append(repeat('-', idWidth + nameWidth + teamWidth + divWidth + formatWidth)).append("\n");
        String teamSummary = "\nTeams appearing in the check:\n";
        List<Pair<RosterData, UgcPlayer.Membership>> recentJoins = new ArrayList<>();
        Set<String> teamIds = new LinkedHashSet<>();
        for (RosterData player : result) {
            if (Thread.interrupted()) {
                log.warn("Roster check interrupted");
                return Result.error("Check was interrupted");
            }
            if (player.getPlayer() == null || player.getPlayer().getTeam() == null || player.getPlayer().getTeam().isEmpty()) {
                builder.append(rightPad(player.getModernId(), idWidth))
                    .append(rightPad(substring(player.getServerName(), 0, NAME_MAX_WIDTH), nameWidth)).append("\n");
            } else {
                boolean first = true;
                for (UgcPlayer.Membership team : player.getPlayer().getTeam()) {
                    teamIds.add(team.getClanId());
                    if (filter.isEmpty() || team.getFormat().equals(filter)) {
                        builder.append(rightPad(first ? player.getModernId() : "", idWidth))
                            .append(rightPad(first ? substring(player.getServerName(), 0, NAME_MAX_WIDTH) : "", nameWidth))
                            .append(rightPad(substring((team.getClanId() != null ? team.getClanId() + " " : "") +
                                team.getName(), 0, TEAM_MAX_WIDTH), teamWidth))
                            .append(rightPad(team.getDivision(), divWidth))
                            .append(rightPad(team.getFormat().equals("9v9") ? "HL" : team.getFormat(), formatWidth))
                            .append("\n");
                        first = false;
                        if (isRecentJoin(team)) {
                            recentJoins.add(Pair.of(player, team));
                        }
                    }
                }
            }
        }
        if (teamIds.size() > 0) {
            for (String id : teamIds) {
                teamSummary += "<http://www.ugcleague.com/team_page.cfm?clan_id=" + id + ">\n";
            }
        }
        return Result.ok(Pair.of(builder.append("```").append(teamSummary).toString(), recentJoins));
    }

    private String getProfileName(Long steamId64) {
        SteamPlayerProfile profile = gameQueryService.getPlayerProfile(steamId64)
            .exceptionally(t -> {
                log.warn("Could not get profile for {}", steamId64, t);
                return null;
            }).join();
        return profile.getName();
    }

    private boolean isRecentJoin(UgcPlayer.Membership team) {
        if (team.getJoinedTeam() == null || !"true".equals(team.getActive())) {
            return false;
        }
        LocalDateTime date = LocalDateTime.parse(team.getJoinedTeam(), DateTimeFormatter.ofPattern(UGC_NEW_DATE_FORMAT, Locale.ENGLISH));
        ZonedDateTime join = date.atZone(UGC_ZONE_ID);
        return ZonedDateTime.now().minusHours(18).isBefore(join);
    }

    private Instant getEligibleToPlayDate(UgcPlayer.Membership team) {
        LocalDateTime date = LocalDateTime.parse(team.getJoinedTeam(), DateTimeFormatter.ofPattern(UGC_NEW_DATE_FORMAT, Locale.ENGLISH));
        ZonedDateTime join = date.atZone(UGC_ZONE_ID);
        return join.plusHours(18).toInstant();
    }

    private Instant ugcDateToInstant(String input) {
        LocalDateTime date = LocalDateTime.parse(input, DateTimeFormatter.ofPattern(UGC_NEW_DATE_FORMAT, Locale.ENGLISH));
        return date.atZone(UGC_ZONE_ID).toInstant();
    }

    private static class RosterData {

        private String serverName = "<unnamed>";
        private String modernId = "";
        private Long communityId;
        private UgcPlayer player = new UgcPlayer();

        String getServerName() {
            return serverName;
        }

        void setServerName(String serverName) {
            this.serverName = serverName;
        }

        String getModernId() {
            return modernId;
        }

        void setModernId(String modernId) {
            this.modernId = modernId;
        }

        Long getCommunityId() {
            return communityId;
        }

        void setCommunityId(Long communityId) {
            this.communityId = communityId;
        }

        public UgcPlayer getPlayer() {
            return player;
        }

        public void setPlayer(UgcPlayer player) {
            this.player = player;
        }

        RosterData updateUgcData(UgcPlayer player) {
            if (player != null) {
                setPlayer(player);
            }
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RosterData that = (RosterData) o;
            return Objects.equals(modernId, that.modernId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(modernId);
        }

        @Override
        public String toString() {
            return serverName + " " + modernId + " (" + communityId + ")";
        }
    }
}
