package top.quantic.sentry.service;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.discord.api.Discord;
import org.springframework.social.discord.api.DiscordGuild;
import org.springframework.stereotype.Service;
import sx.blah.discord.handle.obj.Permissions;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.security.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscordSocialService {

    private final UsersConnectionRepository usersConnectionRepository;
    private final PermissionService permissionService;

    @Autowired
    public DiscordSocialService(UsersConnectionRepository usersConnectionRepository, PermissionService permissionService) {
        this.usersConnectionRepository = usersConnectionRepository;
        this.permissionService = permissionService;
    }

    public Optional<Connection<Discord>> getUserConnection(String login) {
        ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(login);
        return connectionRepository.findConnections(Discord.class).stream().findAny();
    }

    public Optional<Connection<Discord>> getCurrentUserConnection() {
        return getUserConnection(SecurityUtils.getCurrentUserLogin());
    }

    public List<DiscordGuild> getCurrentUserGuilds() {
        return getCurrentUserConnection()
            .map(c -> c.getApi().userOperations().getGuilds().stream().collect(Collectors.toList()))
            .orElseGet(ArrayList::new);
    }

    public List<DiscordGuild> getUserManagedGuilds(String login) {
        return getUserConnection(login)
            .map(c -> c.getApi().userOperations().getGuilds().stream()
                .filter(g -> hasDiscordPermission(Permissions.MANAGE_SERVER, g.getPermissions()))
                .collect(Collectors.toList()))
            .orElseGet(ArrayList::new);
    }

    /**
     * Retrieve the list of discord guild the current user has the permissions to access, either by using the Discord
     * MANAGE_SERVER permission or by internal permission system override.
     *
     * @return a list of guilds the current user can manage
     */
    public List<DiscordGuild> getCurrentUserManagedGuilds() {
        Optional<Connection<Discord>> c = getCurrentUserConnection();
        Set<String> roles = RoleBuilder.of(c.orElse(null)).addGuilds().build();
        return c.map(conn -> conn.getApi().userOperations().getGuilds().stream()
            .filter(g -> hasDiscordPermission(Permissions.MANAGE_SERVER, g.getPermissions())
                || permissionService.hasPermission(roles, "viewDashboard", g.getId()))
            .collect(Collectors.toList()))
            .orElseGet(ArrayList::new);
    }

    public boolean canCurrentUserReadDashboard(String dashboardId) {
        Optional<Connection<Discord>> connection = getCurrentUserConnection();
        Set<String> roles = Sets.newHashSet(connection.map(c -> c.getApi().userOperations().getProfileId()).orElse("0"));
        Set<PermissionType> perms = permissionService.check(roles, "viewDashboard", dashboardId);
        boolean isManager = connection.map(c -> c.getApi().userOperations().getGuilds().stream()
            .anyMatch(g -> g.getId().equals(dashboardId) && hasDiscordPermission(Permissions.MANAGE_SERVER, g.getPermissions())))
            .orElse(false);
        return perms.contains(PermissionType.ALLOW) || (isManager && !perms.contains(PermissionType.DENY));
    }

    private boolean hasDiscordPermission(Permissions requestedPermission, long permissions) {
        return requestedPermission.hasPermission((int) permissions);
    }

    private static class RoleBuilder {
        private Connection<Discord> connection;
        private boolean addGuilds = false;
        private Permissions requiredPermission = null;

        static RoleBuilder of(Connection<Discord> connection) {
            return new RoleBuilder(connection);
        }

        private RoleBuilder(Connection<Discord> connection) {
            this.connection = connection;
        }

        RoleBuilder addGuilds() {
            this.addGuilds = true;
            return this;
        }

        RoleBuilder addGuilds(Permissions requiredPermission) {
            this.addGuilds = true;
            this.requiredPermission = requiredPermission;
            return this;
        }

        Set<String> build() {
            Set<String> roles = new HashSet<>();
            if (connection != null) {
                // get the user id
                roles.add(connection.getApi().userOperations().getProfileId());
                if (addGuilds) {
                    roles.addAll(connection.getApi().userOperations().getGuilds().stream()
                        .filter(g -> requiredPermission == null || Permissions.MANAGE_SERVER.hasPermission((int) g.getPermissions()))
                        .map(DiscordGuild::getId).collect(Collectors.toList()));
                }
            }
            return roles;
        }
    }
}
