package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.MessageSplitter;
import top.quantic.sentry.service.UgcService;
import top.quantic.sentry.web.rest.vm.UgcSchedule;
import top.quantic.sentry.web.rest.vm.UgcTeam;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Component
public class Ugc implements CommandSupplier, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(Ugc.class);

    private final UgcService ugcService;

    private final Map<String, Color> colorMap = new LinkedHashMap<>();

    @Autowired
    public Ugc(UgcService ugcService) {
        this.ugcService = ugcService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        colorMap.put("DR", new Color(0x777777));
        colorMap.put("NC", new Color(0x777777));
        colorMap.put("I", new Color(0x777777));
        colorMap.put("A", new Color(0x468847));
        colorMap.put("NR", new Color(0xf0ad4e));
        colorMap.put("NRF", new Color(0xf0ad4e));
        colorMap.put("S", new Color(0xb94a48));
        colorMap.put("HP", new Color(0x3a87ad));
        colorMap.put("W", new Color(0x333333));
    }

    @Override
    public List<Command> getCommands() {
        return asList(schedules(), team());
    }

    private Command team() {
        return CommandBuilder.of("team")
            .describedAs("Get a UGC team information")
            .in("Integrations")
            .withExamples("Usage: team <id>\n")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String withCommand = context.getContentAfterPrefix();
                String content = withCommand.contains(" ") ? withCommand.split(" ", 2)[1] : withCommand;
                if (isBlank(content)) {
                    answer(message, context.getCommand().getExamples());
                    return;
                }
                RequestBuffer.RequestFuture<IMessage> header = answer(message, "Getting team data from UGC...");
                try {
                    content = content.trim().toLowerCase().replaceAll("^https?://www\\.ugcleague\\.com/team_page\\.cfm\\?clan_id=([0-9]+)$", "$1");
                    UgcTeam team = ugcService.getTeam(Long.parseLong(content), true);
                    EmbedBuilder builder = authoredSuccessEmbed(message)
                        .withTitle(team.getClanName())
                        .withUrl("http://www.ugcleague.com/team_page.cfm?clan_id=" + team.getClanId())
                        .withDescription(team.getClanTag())
                        .withThumbnail(team.getClanAvatar())
                        .withColor(colorMap.get(team.getStatus()))
                        .appendField("League", team.getLadShort(), true)
                        .appendField("Division", team.getDivName(), true)
                        .appendField("Status", team.getStatus(), true)
                        .appendField("Steam Page", "http://" + team.getClanSteampage(), false);
                    String fieldContent = "";
                    for (UgcTeam.RosteredPlayer player : team.getRoster()) {
                        fieldContent += "• " + player.getMemName() +
                            (player.getMemType().equals("Leader") ? " (Leader) - " : " - ") + player.getSid() + "\n";
                    }
                    builder.appendField("Roster", fieldContent, false)
                        .appendField("Team Page", "http://www.ugcleague.com/team_page.cfm?clan_id=" + team.getClanId(), false);
                    CompletableFuture.runAsync(() -> deleteMessage(header.get()));
                    sendMessage(message.getChannel(), builder.build());
                } catch (NumberFormatException e) {
                    answer(message, "Please enter a valid team ID");
                } catch (IOException e) {
                    log.warn("Could not get team data", e);
                    answer(message, "Could not get team data this time, sorry!");
                }
            }).build();
    }

    private Command schedules() {
        return CommandBuilder.of("schedules", "schedule", "sched")
            .describedAs("Get UGC schedules")
            .in("Integrations")
            .withExamples("Usage: schedules <format> <season> <week> <division>\n" +
                "- **format** must be one of: HL, 6s, 4s\n" +
                "- **division** can be a word like 'NA' for all NA divisions\n")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String withCommand = context.getContentAfterPrefix();
                String content = withCommand.contains(" ") ? withCommand.split(" ", 2)[1] : withCommand;
                if (isBlank(content)) {
                    answer(message, context.getCommand().getExamples());
                    return;
                }
                String[] args = content.split(" ", 4);
                if (args.length < 4) {
                    answer(message, context.getCommand().getExamples());
                    return;
                }
                RequestBuffer.RequestFuture<IMessage> header = answer(message, "Getting match data from UGC...");
                try {
                    boolean regex = args[3].startsWith("~");
                    UgcSchedule schedule = ugcService.getSchedule(args[0], Long.parseLong(args[1]), Long.parseLong(args[2]),
                        (regex ? null : args[3]), false);
                    List<UgcSchedule.Match> matchList = schedule.getSchedule().stream()
                        .filter(match -> !regex || match.getDivName().matches(args[3].substring(1)))
                        .collect(Collectors.toList());
                    EmbedBuilder builder = authoredSuccessEmbed(message)
                        .withTitle("Schedules for " + (args[0].contains("s") ? args[0].toLowerCase() : args[0].toUpperCase()))
                        .withDescription("Season **" + args[1] + "** Week **" + args[2] + "**");
                    Map<String, List<String>> divisionToMatches = new LinkedHashMap<>();
                    int count = 0;
                    for (UgcSchedule.Match match : matchList) {
                        if (count++ % 5 == 0) {
                            int c = count;
                            int progress = (int) (100 * (double) count / matchList.size());
                            CompletableFuture.runAsync(() -> RequestBuffer.request(() -> header.get()
                                .edit("Retrieved " + c + " of " + inflect(matchList.size(), "match") + " from UGC (" + progress + "%)")));
                        }
                        if (match.getClanIdH() != null && match.getClanIdV() != null) {
                            UgcTeam home = ugcService.getTeam(match.getClanIdH(), false);
                            UgcTeam away = ugcService.getTeam(match.getClanIdV(), false);
                            divisionToMatches.computeIfAbsent(match.getDivName(), k -> new ArrayList<>())
                                .add(home.getClanName() + " (" + home.getClanId() + ") vs "
                                    + away.getClanName() + (away.getClanId() == 0 ? "" : " (" + away.getClanId() + ")"));
                        }
                    }
                    divisionToMatches.forEach((div, matches) -> {
                        String fieldContent = matches.stream().collect(Collectors.joining("\n"));
                        if (fieldContent.length() < EmbedBuilder.FIELD_CONTENT_LIMIT) {
                            builder.appendField(div, fieldContent, false);
                        } else {
                            MessageSplitter splitter = new MessageSplitter(fieldContent);
                            int part = 1;
                            List<String> splits = splitter.split(EmbedBuilder.FIELD_CONTENT_LIMIT);
                            for (String split : splits) {
                                builder.appendField(div + " (" + part++ + "/" + splits.size() + ")", split, false);
                            }
                        }
                    });
                    CompletableFuture.runAsync(() -> deleteMessage(header.get()));
                    sendMessage(message.getChannel(), builder.build());
                } catch (NumberFormatException e) {
                    answer(message, "Please enter valid numbers for season and week");
                } catch (IOException e) {
                    log.warn("Could not get schedules", e);
                    answer(message, "Could not get schedules this time, sorry!");
                }
            }).build();
    }
}
