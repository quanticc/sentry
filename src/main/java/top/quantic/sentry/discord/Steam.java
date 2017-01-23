package top.quantic.sentry.discord;

import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamBanStatus;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamPlayerProfile;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.GameQueryService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.text.WordUtils.capitalizeFully;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;
import static top.quantic.sentry.discord.util.DiscordUtil.appendOrAnswer;
import static top.quantic.sentry.service.util.DateUtil.formatRelative;
import static top.quantic.sentry.service.util.SteamIdConverter.steam2To3;
import static top.quantic.sentry.service.util.SteamIdConverter.steamId64To2;

@Component
public class Steam implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Steam.class);
    private static final String[] PERSONA_STATES = {
        "Offline", "Online", "Busy", "Away", "Snooze", "Looking to trade", "Looking to play"
    };

    private final GameQueryService gameQueryService;

    @Autowired
    public Steam(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(steam());
    }

    private Command steam() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("One or many SteamID32, SteamID64 or Community URL of a Steam user").ofType(String.class);
        return CommandBuilder.of("steam")
            .describedAs("Get information about a Steam user")
            .in("Integrations")
            .parsedBy(parser)
            .onExecute(context -> {
                IMessage message = context.getMessage();
                List<String> queries = context.getOptionSet().valuesOf(nonOptSpec);
                StringBuilder builder = new StringBuilder();
                for (String query : queries) {
                    Long steamId64 = gameQueryService.getSteamId64(query)
                        .exceptionally(t -> {
                            log.warn("Could not resolve {} to a steamId64", query, t);
                            return null;
                        }).join();
                    if (steamId64 == null) {
                        builder.append("• Could not resolve ").append(query).append(" to a valid Steam ID\n");
                        continue;
                    }
                    builder.append(getUserInfo(steamId64));
                    builder = appendOrAnswer(message, builder, "\n");
                }
                answer(message, builder.toString());
            }).build();
    }

    private String getUserInfo(Long steamId64) {
        if (steamId64 == null) {
            return "";
        }

        SteamPlayerProfile profile = gameQueryService.getPlayerProfile(steamId64)
            .exceptionally(t -> {
                log.warn("Could not get profile for {}", steamId64, t);
                return null;
            }).join();

        List<SteamBanStatus> bans = gameQueryService.getPlayerBans(steamId64)
            .exceptionally(t -> {
                log.warn("Could not get ban info for {}", steamId64, t);
                return null;
            }).join();

        String steam2Id = steamId64To2(steamId64);
        String steam3Id = steam2To3(steam2Id);

        int pad = 15;
        String result = "```http\n" + leftPad("Name: ", pad) + profile.getName() + "\n"
            + leftPad("steam3ID: ", pad) + steam3Id + "\n"
            + leftPad("steamID32: ", pad) + steam2Id + "\n"
            + leftPad("steamID64: ", pad) + "http://steamcommunity.com/profiles/" + steamId64 + "\n";
        if (isPublic(profile.getCommunityVisibilityState())) {
            result += leftPad("Status: ", pad) + personaState(profile.getPersonaState()) + "\n"
                + leftPad("Joined: ", pad) + withRelative(Instant.ofEpochSecond(profile.getTimeCreated())) + "\n";
        } else {
            result += leftPad("Status: ", pad) + "Private" + "\n";
        }
        result += leftPad("Last Logoff: ", pad) + withRelative(Instant.ofEpochSecond(profile.getLastLogOff())) + "\n";

        SteamBanStatus status = bans.stream().findAny().orElse(null);
        if (status != null && (!"none".equals(status.getEconomyBan()) || status.isVacBanned() || status.isCommunityBanned())) {
            result += leftPad("Trade Ban: ", pad) + capitalizeFully(status.getEconomyBan()) + "\n"
                + leftPad("VAC Ban: ", pad) + (status.isVacBanned() ? "Banned" : "None") + "\n"
                + leftPad("Community Ban: ", pad) + (status.isCommunityBanned() ? "Banned" : "None") + "\n";
        }

        result += "```\n<http://www.ugcleague.com/players_page.cfm?player_id=" + steamId64 + ">\n";

        return result;
    }

    private String withRelative(Instant time) {
        return time.toString() + " (" + formatRelative(time) + ")";
    }

    private boolean isPublic(int communityVisibilityState) {
        return communityVisibilityState == 3;
    }

    private String personaState(int value) {
        if (value >= 0 && value <= 6) {
            return PERSONA_STATES[value];
        }
        return "?";
    }
}
