package top.quantic.sentry.discord;

import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.*;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandBuilder;
import top.quantic.sentry.discord.command.CommandContext;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.MessageSplitter;
import top.quantic.sentry.service.PermissionService;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static top.quantic.sentry.config.Operations.GUILD_AWARE;
import static top.quantic.sentry.discord.util.DiscordUtil.answer;
import static top.quantic.sentry.service.util.DateUtil.humanize;

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
        return Lists.newArrayList(info());
    }

    private Command info() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> infoNonOptSpec = parser.nonOptions("Objects like users, roles, channels, guilds. " +
            "Leave empty to get bot information").ofType(String.class);
        return CommandBuilder.of("info")
            .describedAs("Get information about a Discord object")
            .in("General")
            .parsedBy(parser)
            .onExecute(context -> {
                IMessage message = context.getMessage();
                List<String> args = new ArrayList<>(context.getOptionSet().valuesOf(infoNonOptSpec));
                if (args.isEmpty()) {
                    answerWithBotInfo(context);
                } else {
                    StringBuilder builder = new StringBuilder();
                    Set<String> retrievedIds = new HashSet<>();
                    for (IUser user : message.getMentions()) {
                        args.removeIf(arg -> user.mention().equals(arg) ||
                            user.mention(false).equals(arg));
                        builder = appendOrAnswer(message, builder, getUserInfo(user));
                        retrievedIds.add(user.getID());
                    }
                    for (IRole role : message.getRoleMentions()) {
                        args.removeIf(arg -> role.mention().equals(arg));
                        builder = appendOrAnswer(message, builder, getRoleInfo(role));
                        retrievedIds.add(role.getID());
                    }
                    for (IChannel channel : message.getChannelMentions()) {
                        args.removeIf(arg -> channel.mention().equals(arg));
                        builder = appendOrAnswer(message, builder, getChannelInfo(channel));
                        retrievedIds.add(channel.getID());
                    }
                    for (String arg : args) {
                        // check by id
                        if (arg.matches("[0-9]+")) {
                            String content = getInfoById(context, arg, retrievedIds);
                            if (content != null) {
                                builder = appendOrAnswer(message, builder, content);
                                continue;
                            }
                        }
                        // then by name
                        builder = appendOrAnswer(message, builder, getInfoByName(context, arg, retrievedIds));
                    }
                    answer(message, builder.toString());
                }
            }).build();
    }

    private String getInfoByName(CommandContext context, String name, Set<String> retrievedIds) {
        IMessage message = context.getMessage();
        IChannel channel = message.getChannel();
        IGuild guild = channel.getGuild();
        IDiscordClient client = message.getClient();
        // information obtained should be scoped within the source guild
        // unless the user has permissions to see all guilds
        boolean isGuildAware = permissionService.hasPermission(message, GUILD_AWARE, "*");
        // matching by name: 0..*
        ////////////////////////////////////
        // if its a user
        List<IUser> candidateUsers = Lists.newArrayList(message.getAuthor());
        if (isGuildAware) {
            candidateUsers.addAll(client.getUsers());
        } else if (!channel.isPrivate()) {
            candidateUsers.addAll(guild.getUsers());
        }
        List<IUser> usersByName = candidateUsers.stream()
            .filter(u -> matchesName(name, u, guild))
            .collect(Collectors.toList());
        if (usersByName.size() == 1) {
            return getUserInfo(nullIfDuplicate(usersByName.get(0), retrievedIds));
        } else if (usersByName.size() > 1) {
            String result = "**Multiple users matching '" + name +
                "'**\n*Refine your search using IDs or mentions to get more details*\n\n";
            for (IUser user : usersByName) {
                if (!isDuplicate(user, retrievedIds)) {
                    result += "• " + user.getDisplayName(guild) + " <" + user.getID() + ">\n";
                }
            }
            return result;
        }
        ////////////////////////////////////
        // if its a channel
        List<IChannel> candidateChannels = Lists.newArrayList(channel);
        if (isGuildAware) {
            candidateChannels.addAll(client.getChannels());
        } else if (!channel.isPrivate()) {
            candidateChannels.addAll(guild.getChannels());
        }
        List<IChannel> channelsByName = candidateChannels.stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .collect(Collectors.toList());
        if (channelsByName.size() == 1) {
            return getChannelInfo(channelsByName.get(0));
        } else if (channelsByName.size() > 1) {
            String result = "**Multiple channels matching '" + name +
                "'**\n*Refine your search using IDs or mentions to get more details*\n\n";
            for (IChannel ch : channelsByName) {
                if (!isDuplicate(ch, retrievedIds)) {
                    result += "• " + ch.getName() + " <" + ch.getID() + ">\n";
                }
            }
            return result;
        }
        ////////////////////////////////////
        // if its a role
        List<IRole> candidateRoles = null;
        if (isGuildAware) {
            candidateRoles = client.getRoles();
        } else if (!channel.isPrivate()) {
            candidateRoles = guild.getRoles();
        }
        if (candidateRoles != null) {
            List<IRole> rolesByName = candidateRoles.stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
            if (rolesByName.size() == 1) {
                return getRoleInfo(rolesByName.get(0));
            } else if (rolesByName.size() > 1) {
                String result = "**Multiple roles matching '" + name +
                    "'**\n*Refine your search using IDs or mentions to get more details*\n\n";
                for (IRole role : rolesByName) {
                    if (!isDuplicate(role, retrievedIds)) {
                        result += "• " + role.getName() + " <" + role.getID() + ">\n";
                    }
                }
                return result;
            }
        }
        ////////////////////////////////////
        // if its a voice channel
        List<IVoiceChannel> candidateVoiceChannels = null;
        if (isGuildAware) {
            candidateVoiceChannels = client.getVoiceChannels();
        } else if (!channel.isPrivate()) {
            candidateVoiceChannels = guild.getVoiceChannels();
        }
        if (candidateVoiceChannels != null) {
            List<IVoiceChannel> voiceChannelsByName = candidateVoiceChannels.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
            if (voiceChannelsByName.size() == 1) {
                return getChannelInfo(voiceChannelsByName.get(0));
            } else if (voiceChannelsByName.size() > 1) {
                String result = "**Multiple voice channels matching '" + name +
                    "'**\n*Refine your search using IDs or mentions to get more details*\n\n";
                for (IVoiceChannel voiceChannel : voiceChannelsByName) {
                    if (!isDuplicate(voiceChannel, retrievedIds)) {
                        result += "• " + voiceChannel.getName() + " <" + voiceChannel.getID() + ">\n";
                    }
                }
                return result;
            }
        }
        return null;
    }

    private <T extends IDiscordObject> T nullIfDuplicate(T item, Set<String> retrievedIds) {
        if (retrievedIds.contains(item.getID())) {
            return null;
        } else {
            retrievedIds.add(item.getID());
            return item;
        }
    }

    private <T extends IDiscordObject> boolean isDuplicate(T item, Set<String> retrievedIds) {
        if (retrievedIds.contains(item.getID())) {
            return true;
        } else {
            retrievedIds.add(item.getID());
            return false;
        }
    }

    private boolean matchesName(String name, IUser user, IGuild guild) {
        Optional<String> nickname = user.getNicknameForGuild(guild);
        if (nickname.isPresent()) {
            return nickname.get().equalsIgnoreCase(name) || user.getName().equalsIgnoreCase(name);
        } else {
            return user.getName().equalsIgnoreCase(name);
        }
    }

    private String getInfoById(CommandContext context, String id, Set<String> retrievedIds) {
        IMessage message = context.getMessage();
        IChannel channel = message.getChannel();
        IDiscordClient client = message.getClient();
        // information obtained should be scoped within the source guild
        // unless the user has permissions to see all guilds
        boolean isGuildAware = permissionService.hasPermission(message, GUILD_AWARE, "*");
        // matching by id: 0..1
        ////////////////////////////////////
        // if its a user
        IUser userById = null;
        if (isGuildAware) {
            userById = client.getUserByID(id);
        } else if (!channel.isPrivate()) {
            userById = channel.getGuild().getUserByID(id);
        } else if (message.getAuthor().getID().equals(id)) {
            userById = message.getAuthor();
        }
        if (userById != null) {
            return getUserInfo(nullIfDuplicate(userById, retrievedIds));
        }
        ////////////////////////////////////
        // if its a channel
        IChannel channelById = null;
        if (isGuildAware) {
            channelById = client.getChannelByID(id);
        } else if (!channel.isPrivate()) {
            channelById = channel.getGuild().getChannelByID(id);
        } else if (channel.getID().equals(id)) {
            channelById = channel;
        }
        if (channelById != null) {
            return getChannelInfo(nullIfDuplicate(channelById, retrievedIds));
        }
        ////////////////////////////////////
        // if its a role
        IRole roleById = null;
        if (isGuildAware) {
            roleById = client.getRoleByID(id);
        } else if (!channel.isPrivate()) {
            roleById = channel.getGuild().getRoleByID(id);
        }
        if (roleById != null) {
            return getRoleInfo(nullIfDuplicate(roleById, retrievedIds));
        }
        ////////////////////////////////////
        // if its a voice channel
        IVoiceChannel voiceChannelById = null;
        if (isGuildAware) {
            voiceChannelById = client.getVoiceChannelByID(id);
        } else if (!channel.isPrivate()) {
            voiceChannelById = channel.getGuild().getVoiceChannelByID(id);
        }
        if (voiceChannelById != null) {
            return getVoiceChannelInfo(nullIfDuplicate(voiceChannelById, retrievedIds));
        }
        // TODO: info for emoji, guild
        return null;
    }

    private String getUserInfo(IUser user) {
        if (user == null) {
            return "";
        }
        return "• " + user.getName() + " <" + user.getID() + ">\n";
    }

    private String getRoleInfo(IRole role) {
        if (role == null) {
            return "";
        }
        return "• " + role.getName() + " <" + role.getID() + ">\n";
    }

    private String getChannelInfo(IChannel channel) {
        if (channel == null) {
            return "";
        }
        return "• " + channel.getName() + " <" + channel.getID() + ">\n";
    }

    private String getVoiceChannelInfo(IVoiceChannel voiceChannel) {
        if (voiceChannel == null) {
            return "";
        }
        return "• " + voiceChannel.getName() + " <" + voiceChannel.getID() + ">\n";
    }

    private StringBuilder appendOrAnswer(IMessage message, StringBuilder builder, String content) {
        if (content != null) {
            if (shouldSplit(builder, content)) {
                answer(message, builder.toString());
                builder = new StringBuilder();
            }
            builder.append(content);
        }
        return builder;
    }

    private boolean shouldSplit(StringBuilder builder, String content) {
        return builder.length() + content.length() > MessageSplitter.LENGTH_LIMIT;
    }

    private void answerWithBotInfo(CommandContext context) {
        IMessage message = context.getMessage();
        String version = buildProperties.getVersion();
        version = (version == null ? "snapshot" : version);
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptime = rb.getUptime();
        String content = "Hey! I'm here to help with **UGC support**.\n\n" +
            "**Version:** " + version + '\n' +
            "**Discord4J:** " + Discord4J.VERSION + '\n' +
            "**Uptime:** " + humanize(Duration.ofMillis(uptime)) + '\n';
        answer(message, content);
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
