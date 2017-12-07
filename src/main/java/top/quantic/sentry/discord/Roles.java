package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.Result;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordUtil.*;

@Component
public class Roles implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Roles.class);

    private final SettingService settingService;
    private final PermissionService permissionService;

    @Autowired
    public Roles(SettingService settingService, PermissionService permissionService) {
        this.settingService = settingService;
        this.permissionService = permissionService;
    }

    @Override
    public List<Command> getCommands() {
        return asList(set());
    }

    private Command set() {
        return CommandBuilder.of("set")
            .describedAs("Set yourself some role")
            .in("General")
            .withExamples("Usage: **set** __role__")
            .nonParsed()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IChannel channel = message.getChannel();
                String content = context.getContentAfterCommand();
                if (isBlank(content)) {
                    answerToChannel(channel, "Please include a role name or key");
                    return;
                }
                if (channel.isPrivate()) {
                    answerToChannel(channel, "This command does not work in private messages");
                    return;
                }
                // Map role -> role_id
                IUser author = message.getAuthor();
                Optional<Setting> setting = settingService.findMostRecentByGuildAndKey(channel.getGuild().getStringID(), "roleMap:" + content);
                if (setting.isPresent()) {
                    String roleId = setting.get().getValue();
                    if (permissionService.hasPermission(channel.getStringID(), "assignIn", roleId)) {
                        if (permissionService.hasPermission(author.getStringID(), "assignSelf", roleId)) {
                            IRole role = channel.getGuild().getRoleByID(snowflake(roleId));
                            if (role != null) {
                                // Role is ready to be assigned
                                Result<?> result = RequestBuffer.request(() -> {
                                    try {
                                        author.addRole(role);
                                        return Result.empty("Role added successfully");
                                    } catch (DiscordException e) {
                                        log.warn("", e);
                                        return Result.error(e.getErrorMessage(), e);
                                    } catch (MissingPermissionsException e) {
                                        log.warn("", e);
                                        return Result.error("Bot permission error", e);
                                    }
                                }).get();
                                if (result.isSuccessful()) {
                                    log.debug("Set role {} to {} in {}", humanize(role), humanize(author), humanize(channel));
                                    answerToChannel(channel, author.mention() + " :ok_hand:");
                                } else {
                                    log.debug("Role {} could not be assigned to {} in {}", humanize(role), humanize(author), humanize(channel));
                                    answerToChannel(channel, author.mention() + " Role could not be assigned: " + result.getMessage());
                                }
                            } else {
                                log.debug("Role '{}' was not found for {} in {}", content, humanize(author), humanize(channel));
                                answerToChannel(channel, author.mention() + " Role '" + content + "' not found");
                            }
                        } else {
                            log.debug("No user permissions to assign role '{}' to {} in {}", content, humanize(author), humanize(channel));
                            answerToChannel(channel, author.mention() + " You can't assign this role to yourself!");
                        }
                    } else {
                        log.debug("No channel permissions to assign role '{}' to {} in {}", content, humanize(author), humanize(channel));
                        answerToChannel(channel, author.mention() + " Role '" + content + "' not found");
                    }
                } else {
                    log.debug("Role '{}' could not be assigned to {} in {} because it does not exist in {}", content, humanize(author), humanize(channel), humanize(channel.getGuild()));
                    answerToChannel(channel, author.mention() + " Role '" + content + "' not found");
                }
                deleteMessage(message);
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .onBotDenied(context -> log.warn("Bot does not have permissions to assign roles"))
            .build();
    }
}
