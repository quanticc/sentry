package top.quantic.sentry.discord;

import com.google.common.base.CaseFormat;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.DiscordUtil;
import top.quantic.sentry.service.PermissionService;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.config.Operations.QUERY_ALL_GUILDS;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.humanize;
import static top.quantic.sentry.service.util.DateUtil.*;
import static top.quantic.sentry.service.util.MiscUtil.getDominantColor;

@Component
public class Info implements CommandSupplier, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(Info.class);

    private final PermissionService permissionService;
    private final BuildProperties buildProperties;
    private final RestTemplate restTemplate;

    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final Properties properties = new Properties();

    @Autowired
    public Info(PermissionService permissionService, BuildProperties buildProperties, RestTemplate restTemplate) {
        this.permissionService = permissionService;
        this.buildProperties = buildProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (InputStream stream = Discord4J.class.getClassLoader().getResourceAsStream("app.properties")) {
            properties.load(stream);
        } catch (IOException e) {
            log.warn("", e);
        }
    }

    @Override
    public List<Command> getCommands() {
        return asList(info(), user(), role(), channel());
    }

    private Command info() {
        return CommandBuilder.of("info", "about")
            .describedAs("Get Discord information about the bot")
            .in("General")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                String version = buildProperties.getVersion();
                version = (version == null ? "snapshot" : version);
                long uptime = runtimeMXBean.getUptime();
                IUser me = message.getClient().getOurUser();
                String appVersion = properties.getProperty("application.version");
                String appGitCommit = properties.getProperty("application.git.commit");
                String discordVersion = appVersion == null || appGitCommit == null ? "" : (appVersion + " (" + appGitCommit + ")");
                sendMessage(message.getChannel(), authoredEmbed(message)
                    .withColor(getDominantColor(asInputStream(me.getAvatarURL()), new Color(0xd5bb59)))
                    .withThumbnail("http://i.imgur.com/SFF4jLF.png")
                    .withTitle(me.getDisplayName(message.getChannel().getGuild()))
                    .withDescription("Hey! I'm here to help with **UGC Support** and **League Operations**.\n" +
                        "Check out the commands using `.help` or `.help more`")
                    .appendField("Version", version, true)
                    .appendField("Discord4J", discordVersion, true)
                    .appendField("Uptime", humanize(Duration.ofMillis(uptime), false, true), false)
                    .appendField("Author", "<@134127815531560960>", true)
                    .appendField("Website", "https://sentry.quantic.top/", true)
                    .build());
            }).build();
    }

    private Command user() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("User IDs, names or mentions").ofType(String.class);
        return CommandBuilder.of("user")
            .describedAs("Get Discord information about the given users")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                List<String> queries = context.getOptionSet().valuesOf(nonOptSpec);
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*", true);
                Set<IUser> matched = new HashSet<>();
                for (String query : queries) {
                    String id = query.replaceAll("<@!?(\\d+)>", "$1");
                    List<IUser> users = awareUserList(aware, message);
                    List<IUser> matching = users.stream()
                        .filter(u -> !matched.contains(u))
                        .filter(u -> u.getID().equals(id) || equalsAnyName(u, query, channel.getGuild()))
                        .distinct()
                        .peek(matched::add)
                        .collect(Collectors.toList());
                    if (matching.size() == 1) {
                        IUser user = matching.get(0);
                        sendMessage(message.getChannel(), getUserInfo(user, context)).get();
                    } else if (matching.size() > 1) {
                        sendMessage(message.getChannel(), new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaaaa00))
                            .withTitle("User Search")
                            .withDescription("Multiple matches for " + query)
                            .appendField("Users", matching.stream()
                                .map(DiscordUtil::humanizeShort)
                                .collect(Collectors.joining("\n")), false)
                            .build()).get();
                    } else {
                        sendMessage(message.getChannel(), new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaa0000))
                            .withTitle("User Search")
                            .withDescription("No users matching " + id)
                            .build()).get();
                    }
                }
            }).build();
    }

    private EmbedObject getUserInfo(IUser user, CommandContext context) {
        if (user == null) {
            return null;
        }
        IGuild guild = context.getMessage().getGuild();
        IPresence presence = user.getPresence();
        EmbedBuilder builder = authoredEmbed(context.getMessage())
            .withThumbnail(user.getAvatarURL())
            .withColor(getDominantColor(asInputStream(user.getAvatarURL()), new Color(0x00aa00)))
            .appendField((user.isBot() ? "Bot" : "User"), user.getName() + '#' + user.getDiscriminator(), false);
        if (guild != null && !user.getName().equals(Optional.ofNullable(user.getNicknameForGuild(guild)).orElse(user.getName()))) {
            builder.appendField("Nickname", user.getDisplayName(guild), false);
        }
        builder.appendField("ID", user.getID(), true)
            .appendField("Mention", user.mention(), true)
            .appendField("Joined", withRelative(systemToInstant(user.getCreationDate())), false)
            .appendField("Status", formatStatus(presence.getStatus()), true);
        if (presence.getPlayingText().isPresent()) {
            builder.appendField("Playing", presence.getPlayingText().get(), true);
        }
        if (presence.getStreamingUrl().isPresent()) {
            builder.appendField("Streaming", presence.getStreamingUrl().get(), true);
        }
        if (!context.getMessage().getChannel().isPrivate()) {
            builder.appendField("Roles", formatRoles(user.getRolesForGuild(guild)), false);
        }
        return builder.build();
    }

    private InputStream asInputStream(String url) {
        String targetUrl = url.replace(".webp", ".png");
        HttpHeaders headers = new HttpHeaders();
        // workaround to go through CloudFlare :^)
        headers.add("User-Agent", Constants.USER_AGENT);
        try {
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(targetUrl,
                HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
            return responseEntity.getBody().getInputStream();
        } catch (IOException | RestClientException e) {
            log.warn("Could not get {} as InputStream", e);
            return null;
        }
    }

    private String formatStatus(StatusType status) {
        if (status == StatusType.DND) {
            return "DND";
        } else {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, status.name());
        }
    }

    private String formatRoles(List<IRole> roles) {
        String names = roles.stream()
            .map(IRole::getName)
            .filter(s -> !"@everyone".equals(s))
            .collect(Collectors.joining(", "));
        if (names.isEmpty()) {
            return "*none*";
        } else {
            return names;
        }
    }

    private Command role() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Role IDs, names or mentions").ofType(String.class);
        return CommandBuilder.of("role")
            .describedAs("Get Discord information about a role")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                List<String> queries = context.getOptionSet().valuesOf(nonOptSpec);
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                IDiscordClient client = message.getClient();
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*", true);
                Set<IRole> matched = new HashSet<>();
                for (String query : queries) {
                    String id = query.replaceAll("<@&(\\d+)>", "$1");
                    List<IRole> roles;
                    if (aware) {
                        roles = client.getRoles();
                    } else if (!channel.isPrivate()) {
                        roles = channel.getGuild().getRoles();
                    } else {
                        roles = Collections.emptyList();
                    }
                    List<IRole> matching = roles.stream()
                        .filter(r -> !matched.contains(r))
                        .filter(r -> r.getID().equals(id)
                            || r.getName().equalsIgnoreCase(query))
                        .distinct()
                        .peek(matched::add)
                        .collect(Collectors.toList());
                    if (matching.size() == 1) {
                        IRole role = matching.get(0);
                        boolean withGuild = aware && (channel.isPrivate() || !channel.getGuild().equals(role.getGuild()));
                        for (EmbedObject embed : getRoleInfo(role, withGuild, context)) {
                            sendMessage(channel, embed).get();
                        }
                    } else if (matching.size() > 1) {
                        sendMessage(channel, new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaaaa00))
                            .withTitle("Role Search")
                            .withDescription("Multiple matches for " + query)
                            .appendField("Roles", matching.stream()
                                .map(this::getShortRoleInfo)
                                .collect(Collectors.joining("\n")), false)
                            .build()).get();
                    } else {
                        sendMessage(channel, new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaa0000))
                            .withTitle("Role Search")
                            .withDescription("No roles matching " + id)
                            .build()).get();
                    }
                }
            }).build();
    }

    private List<EmbedObject> getRoleInfo(IRole role, boolean withGuild, CommandContext context) {
        List<EmbedObject> embeds = new ArrayList<>();
        if (role == null) {
            return embeds;
        }
        Color color = role.getColor();
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        String perms = role.getPermissions().stream()
            .map(Enum::toString)
            .map(p -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, p))
            .collect(Collectors.joining(", "));
        boolean hoisted = role.isHoisted();
        boolean mentionable = role.isMentionable();
        boolean managed = role.isManaged();
        boolean isEveryone = role.isEveryoneRole();

        EmbedBuilder builder = authoredEmbed(context.getMessage())
            .withColor(role.getColor() != null ? role.getColor() : new Color(0))
            .appendField("Role", mentionBuster(role.getName()), false)
            .appendField("ID", "<" + role.getID() + ">", false)
            .appendField("Color", hex, true)
            .appendField("Position", "" + role.getPosition(), true)
            .appendField("Created", withRelative(systemToInstant(role.getCreationDate())), false);
        if (!isEveryone && (hoisted || mentionable || managed)) {
            builder.appendField("Tags", Stream.of((hoisted ? "hoisted" : ""), (mentionable ? "mentionable" : ""), (managed ? "managed" : ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", ")), false);
        }
        builder.appendField("Permissions", perms, true);
        embeds.add(builder.build());
        if (isEveryone || withGuild) {
            embeds.add(getGuildInfo(role.getGuild()));
        }
        return embeds;
    }

    private EmbedObject getGuildInfo(IGuild guild) {
        if (guild == null) {
            return null;
        }
        return new EmbedBuilder()
            .setLenient(true)
            .withThumbnail(guild.getIconURL())
            .withColor(getDominantColor(asInputStream(guild.getIconURL()), new Color(0x00aa00)))
            .appendField("Guild", guild.getName() + " <" + guild.getID() + ">", false)
            .appendField("Owner", guild.getOwner().getName() + " <" + guild.getOwnerID() + ">", false)
            .appendField("Members", "" + guild.getTotalMemberCount(), false)
            .build();
    }

    private String getShortRoleInfo(IRole role) {
        if (role == null) {
            return "";
        }
        return "• " + mentionBuster(role.getName()) + " <" + role.getID() + ">";
    }

    private String mentionBuster(String name) {
        return name.replace("@", "@\u200B");
    }

    private Command channel() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> nonOptSpec = parser.nonOptions("Channel IDs, names or mentions").ofType(String.class);
        return CommandBuilder.of("channel")
            .describedAs("Get Discord information about a channel")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                List<String> queries = context.getOptionSet().valuesOf(nonOptSpec);
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                IDiscordClient client = message.getClient();
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*", true);
                Set<IChannel> matched = new HashSet<>();
                for (String query : queries) {
                    String id = query.replaceAll("<#(\\d+)>", "$1");
                    List<IChannel> channels = new ArrayList<>();
                    if (aware) {
                        channels.addAll(client.getChannels());
                        channels.addAll(client.getVoiceChannels());
                    } else if (!channel.isPrivate()) {
                        channels.addAll(channel.getGuild().getChannels());
                        channels.addAll(channel.getGuild().getVoiceChannels());
                    }
                    List<IChannel> matching = channels.stream()
                        .filter(r -> !matched.contains(r))
                        .filter(r -> r.getID().equals(id) || r.getName().equalsIgnoreCase(query))
                        .distinct()
                        .peek(matched::add)
                        .collect(Collectors.toList());
                    if (matching.size() == 1) {
                        for (EmbedObject embed : getChannelInfo(matching.get(0), context)) {
                            sendMessage(channel, embed).get();
                        }
                    } else if (matching.size() > 1) {
                        sendMessage(channel, new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaaaa00))
                            .withTitle("Channel Search")
                            .withDescription("Multiple matches for " + query)
                            .appendField("Channels", matching.stream()
                                .map(this::getShortChannelInfo)
                                .collect(Collectors.joining("\n")), false)
                            .build()).get();
                    } else {
                        sendMessage(channel, new EmbedBuilder()
                            .setLenient(true)
                            .withColor(new Color(0xaa0000))
                            .withTitle("Channel Search")
                            .withDescription("No channel matching " + id)
                            .build()).get();
                    }
                }
            }).build();
    }

    private List<EmbedObject> getChannelInfo(IChannel channel, CommandContext context) {
        List<EmbedObject> embeds = new ArrayList<>();
        if (channel == null) {
            return embeds;
        }
        String created = systemToInstant(channel.getCreationDate()).toString();
        EmbedBuilder builder = authoredEmbed(context.getMessage())
            .appendField("Channel", channel.getName(), false)
            .appendField("ID", "<" + channel.getID() + ">", false)
            .appendField("Created", created, false);
        if (!isBlank(channel.getTopic())) {
            builder.appendField("Topic", channel.getTopic(), false);
        }
        builder.appendField("Position", "" + channel.getPosition(), true);
        if (channel instanceof IVoiceChannel) {
            IVoiceChannel voice = (IVoiceChannel) channel;
            int connected = voice.getConnectedUsers().size();
            int capacity = voice.getUserLimit();
            builder.appendField("Bitrate", "" + voice.getBitrate(), true);
            if (connected > 0) {
                builder.appendField("Users", "" + connected + (capacity > 0 ? "/" + capacity : ""), true);
            }
        }
        embeds.add(builder.build());
        if (!channel.isPrivate() && channel.getGuild().getID().equals(channel.getID())) {
            embeds.add(getGuildInfo(channel.getGuild()));
        }
        return embeds;
    }

    private String getShortChannelInfo(IChannel channel) {
        if (channel == null) {
            return "";
        }
        return "• " + channel.getName() + " <" + channel.getID() + ">";
    }
}
