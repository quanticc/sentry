package top.quantic.sentry.discord;

import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamBanStatus;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamPlayerProfile;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.GameQueryService;

import java.awt.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.text.WordUtils.capitalizeFully;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;
import static top.quantic.sentry.service.util.DateUtil.withRelative;
import static top.quantic.sentry.service.util.MiscUtil.getDominantColor;
import static top.quantic.sentry.service.util.MiscUtil.inflect;
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
                answer(message, "Retrieving user data of " + inflect(queries.size(), "user") + "...");
                for (String query : queries) {
                    Long steamId64 = gameQueryService.getSteamId64(query)
                        .exceptionally(t -> {
                            log.warn("Could not resolve {} to a steamId64", query, t);
                            return null;
                        }).join();
                    if (steamId64 == null) {
                        sendMessage(message.getChannel(), baseEmbed()
                            .withColor(new Color(0xaa0000))
                            .withTitle("Error")
                            .appendDescription("Could not resolve " + query + " to a valid Steam ID")
                            .build());
                        continue;
                    }
                    sendMessage(message.getChannel(), getUserInfo(steamId64));
                }
            }).build();
    }

    private EmbedBuilder baseEmbed() {
        return new EmbedBuilder()
            .setLenient(true)
            .withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Steam_icon_logo.svg/24px-Steam_icon_logo.svg.png")
            .withFooterText("Steam");
    }

    private EmbedObject getUserInfo(Long steamId64) {
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

        if (profile == null) {
            return baseEmbed()
                .withColor(new Color(0xaa0000))
                .withTitle("Error")
                .appendDescription("Could not get profile information for " + steamId64)
                .build();
        }

        String steam2Id = steamId64To2(steamId64);
        String steam3Id = steam2To3(steam2Id);

        EmbedBuilder builder = baseEmbed()
            .withTitle(profile.getName())
            .withColor(getDominantColor(profile.getAvatarUrl(), new Color(0x0e3496)))
            .withThumbnail(profile.getAvatarFullUrl())
            .withUrl("http://steamcommunity.com/profiles/" + steamId64)
            .appendField("Steam3ID", steam3Id, true)
            .appendField("SteamID32", steam2Id, true)
            .appendField("SteamID64", "http://steamcommunity.com/profiles/" + steamId64, false);
        if (isPublic(profile.getCommunityVisibilityState())) {
            builder.appendField("Status", personaState(profile.getPersonaState()), true)
                .appendField("Last Logoff", withRelative(Instant.ofEpochSecond(profile.getLastLogOff())), true)
                .appendField("Joined", withRelative(Instant.ofEpochSecond(profile.getTimeCreated())), false);
        } else {
            builder.appendField("Status", "Private", true)
                .appendField("Last Logoff", withRelative(Instant.ofEpochSecond(profile.getLastLogOff())), true);
        }

        if (bans != null) {
            SteamBanStatus status = bans.stream().findAny().orElse(null);
            if (status != null && (!"none".equals(status.getEconomyBan()) || status.isVacBanned() || status.isCommunityBanned())) {
                builder.withColor(new Color(0xaa0000))
                    .appendField("Trade Ban", capitalizeFully(status.getEconomyBan()), true)
                    .appendField("VAC Ban", (status.isVacBanned() ? "Banned" : "None"), true)
                    .appendField("Community Ban", (status.isCommunityBanned() ? "Banned" : "None"), true);
            }
        }

        return builder.appendField("UGC", "http://www.ugcleague.com/players_page.cfm?player_id=" + steamId64, false)
            .appendField("Logs.tf", "http://logs.tf/profile/" + steamId64, false)
            .appendField("SizzlingStats", "http://sizzlingstats.com/player/" + steamId64, false)
            .build();
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
