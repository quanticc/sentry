package top.quantic.sentry.discord;

import com.google.common.base.CaseFormat;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.core.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.service.PermissionService;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static top.quantic.sentry.config.Operations.QUERY_ALL_GUILDS;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.humanize;
import static top.quantic.sentry.service.util.DateUtil.systemToInstant;

@Component
public class Info implements CommandSupplier {

    private final PermissionService permissionService;
    private final BuildProperties buildProperties;

    @Autowired
    public Info(PermissionService permissionService, BuildProperties buildProperties) {
        this.permissionService = permissionService;
        this.buildProperties = buildProperties;
    }

    @Override
    public List<Command> getCommands() {
        return asList(info(), user(), role(), channel());
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
                    "**Uptime:** " + humanize(Duration.ofMillis(uptime), false, true) + '\n';
                answer(message, content);
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
                IDiscordClient client = message.getClient();
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*");
                StringBuilder builder = new StringBuilder();
                Set<IUser> matched = new HashSet<>();
                for (String query : queries) {
                    String id = query.replaceAll("<@!?(\\d+)>", "$1");
                    List<IUser> users;
                    if (aware) {
                        users = client.getUsers();
                    } else if (!channel.isPrivate()) {
                        users = channel.getGuild().getUsers();
                    } else {
                        users = asList(message.getAuthor(), client.getOurUser());
                    }
                    List<IUser> matching = users.stream()
                        .filter(u -> !matched.contains(u))
                        .filter(u -> u.getID().equals(id) || equalsAnyName(u, query, channel.getGuild()))
                        .distinct()
                        .peek(matched::add)
                        .collect(Collectors.toList());
                    if (matching.size() == 1) {
                        IUser user = matching.get(0);
                        builder.append(getUserInfo(user, context));
                    } else if (matching.size() > 1) {
                        builder.append("Multiple matches for ").append(query).append("\n")
                            .append(matching.stream()
                                .map(this::getShortUserInfo)
                                .collect(Collectors.joining("\n")));
                    } else {
                        builder.append("No users matching ").append(id).append("\n");
                    }
                    builder = appendOrAnswer(message, builder, "\n");
                }
                answer(message, builder.toString());
            }).build();
    }

    private String getUserInfo(IUser user, CommandContext context) {
        if (user == null) {
            return "";
        }
        IGuild guild = context.getMessage().getChannel().getGuild();
        int pad = 10;
        String result = "```http\n" + leftPad("User: ", pad) + user.getName() + '#' + user.getDiscriminator() + (user.isBot() ? " [BOT]\n" : '\n');
        if (guild != null && !user.getName().equals(user.getNicknameForGuild(guild).orElse(user.getName()))) {
            result += leftPad("Nickname: ", pad) + user.getDisplayName(guild) + '\n';
        }
        result += leftPad("ID: ", pad) + '<' + user.getID() + ">\n"
            + leftPad("Joined: ", pad) + systemToInstant(user.getCreationDate()).toString() + '\n'
            + leftPad("Status: ", pad) + user.getPresence().getStatus().name().toLowerCase() + '\n';
        if (guild != null) {
            result += leftPad("Roles: ", pad) + formatRoles(user.getRolesForGuild(guild)) + '\n';
        }
        result += "\n```" + user.getAvatarURL() + '\n';
        return result;
    }

    private String formatRoles(List<IRole> roles) {
        String names = roles.stream()
            .map(IRole::getName)
            .filter(s -> !s.equals("@everyone"))
            .collect(Collectors.joining(", "));
        if (names.isEmpty()) {
            return "<none>";
        } else {
            return names;
        }
    }

    private String getShortUserInfo(IUser user) {
        if (user == null) {
            return "";
        }
        return "• " + user.getName() + " <" + user.getID() + ">\n";
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
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*");
                StringBuilder builder = new StringBuilder();
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
                        boolean withGuild = aware &&
                            (channel.isPrivate() || !channel.getGuild().equals(role.getGuild()));
                        builder.append(getRoleInfo(role, withGuild));
                    } else if (matching.size() > 1) {
                        builder.append("Multiple matches for ").append(query).append("\n")
                            .append(matching.stream()
                                .map(this::getShortRoleInfo)
                                .collect(Collectors.joining("\n")));
                    } else {
                        builder.append("No roles matching ").append(id).append("\n");
                    }
                    builder = appendOrAnswer(message, builder, "\n");
                }
                answer(message, builder.toString());
            }).build();
    }

    private String getRoleInfo(IRole role, boolean withGuild) {
        if (role == null) {
            return "";
        }
        int pad = 13;
        String created = systemToInstant(role.getCreationDate()).toString();
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
        String result = "```http\n" + leftPad("Role: ", pad) + mentionBuster(role.getName()) + '\n'
            + leftPad("ID: ", pad) + '<' + role.getID() + ">\n"
            + leftPad("Color: ", pad) + hex + '\n'
            + leftPad("Position: ", pad) + role.getPosition() + '\n'
            + leftPad("Created: ", pad) + created + '\n';
        if (!isEveryone && (hoisted || mentionable || managed)) {
            result += leftPad("Tags: ", pad) +
                Stream.of((hoisted ? "hoisted" : ""), (mentionable ? "mentionable" : ""), (managed ? "managed" : ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(", "));
        }
        result += leftPad("Permissions: ", pad) + perms + "\n```";
        if (isEveryone || withGuild) {
            result += "\n" + getGuildInfo(role.getGuild());
        }
        return result;
    }

    private String getGuildInfo(IGuild guild) {
        if (guild == null) {
            return "";
        }
        int pad = 9;
        return "```http\n" + leftPad("Guild: ", pad) + guild.getName() + " <" + guild.getID() + ">\n"
            + leftPad("Owner: ", pad) + guild.getOwner().getName() + " <" + guild.getOwnerID() + ">\n"
            + leftPad("Members: ", pad) + guild.getTotalMemberCount() + '\n'
            + "\n```" + guild.getIconURL() + "\n";
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
                boolean aware = permissionService.hasPermission(message, QUERY_ALL_GUILDS, "*");
                StringBuilder builder = new StringBuilder();
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
                        builder.append(getChannelInfo(matching.get(0)));
                    } else if (matching.size() > 1) {
                        builder.append("Multiple matches for ").append(query).append("\n")
                            .append(matching.stream()
                                .map(this::getShortChannelInfo)
                                .collect(Collectors.joining("\n")));
                    } else {
                        builder.append("No channels matching ").append(id).append("\n");
                    }
                    builder = appendOrAnswer(message, builder, "\n");
                }
                answer(message, builder.toString());
            }).build();
    }

    private String getChannelInfo(IChannel channel) {
        if (channel == null) {
            return "";
        }
        int pad = 10;
        String created = systemToInstant(channel.getCreationDate()).toString();
        String result = "```http\n" + leftPad("Channel: ", pad) + channel.getName() + '\n'
            + leftPad("ID: ", pad) + '<' + channel.getID() + ">\n"
            + leftPad("Position: ", pad) + channel.getPosition() + '\n'
            + leftPad("Created: ", pad) + created + '\n';
        if (!isBlank(channel.getTopic())) {
            result += leftPad("Topic: ", pad) + channel.getTopic() + '\n';
        }
        if (channel instanceof IVoiceChannel) {
            IVoiceChannel voice = (IVoiceChannel) channel;
            int connected = voice.getConnectedUsers().size();
            int capacity = voice.getUserLimit();
            result += leftPad("Bitrate: ", pad) + voice.getBitrate() + '\n';
            if (connected > 0) {
                result += leftPad("Users: ", pad) + connected + (capacity > 0 ? "/" + capacity : "") + '\n';
            }
        }
        result += "```\n";
        if (!channel.isPrivate() && channel.getGuild().getID().equals(channel.getID())) {
            result += getGuildInfo(channel.getGuild());
        }
        return result;
    }

    private String getShortChannelInfo(IChannel channel) {
        if (channel == null) {
            return "";
        }
        return "• " + channel.getName() + " <" + channel.getID() + ">";
    }
}
