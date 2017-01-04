package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.discord.api.Discord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.quantic.sentry.service.DiscordSocialService;
import top.quantic.sentry.web.rest.vm.DiscordConnectionVM;

import javax.inject.Inject;

@RestController
@RequestMapping("/api")
public class DiscordResource {

    @Inject
    private DiscordSocialService discordSocialService;

    @GetMapping("/discord")
    @Timed
    public DiscordConnectionVM getConnectionDetails() {
        return discordSocialService.getCurrentUserConnection()
            .map(this::cachedViewModel)
            .orElse(new DiscordConnectionVM());
    }

    @GetMapping("/discord/user")
    @Timed
    public DiscordConnectionVM getUser() {
        return discordSocialService.getCurrentUserConnection()
            .map(this::viewModelWithUser)
            .orElse(new DiscordConnectionVM());
    }

    @GetMapping("/discord/guilds")
    @Timed
    public DiscordConnectionVM getUserGuilds() {
        return discordSocialService.getCurrentUserConnection()
            .map(this::viewModelWithGuilds)
            .orElse(new DiscordConnectionVM());

    }

    @GetMapping("/discord/connections")
    @Timed
    public DiscordConnectionVM getUserConnections() {
        return discordSocialService.getCurrentUserConnection()
            .map(this::viewModelWithConnections)
            .orElse(new DiscordConnectionVM());
    }

    @GetMapping("/discord/full")
    @Timed
    public DiscordConnectionVM getFullUserConnection() {
        return discordSocialService.getCurrentUserConnection()
            .map(this::fullViewModel)
            .orElse(new DiscordConnectionVM());
    }

    private DiscordConnectionVM cachedViewModel(Connection<Discord> connection) {
        return viewModel(connection, false, false, false);
    }

    private DiscordConnectionVM fullViewModel(Connection<Discord> connection) {
        return viewModel(connection, true, true, true);
    }

    private DiscordConnectionVM viewModelWithUser(Connection<Discord> connection) {
        return viewModel(connection, true, false, false);
    }

    private DiscordConnectionVM viewModelWithGuilds(Connection<Discord> connection) {
        return viewModel(connection, false, true, false);
    }

    private DiscordConnectionVM viewModelWithConnections(Connection<Discord> connection) {
        return viewModel(connection, false, false, true);
    }

    private DiscordConnectionVM viewModel(Connection<Discord> connection, boolean user, boolean guilds, boolean connections) {
        DiscordConnectionVM vm = new DiscordConnectionVM();
        vm.setUserId(connection.getKey().getProviderUserId());
        vm.setUsername(connection.getDisplayName());
        vm.setAvatarUrl(connection.getImageUrl());
        if (user) {
            UserProfile userProfile = connection.fetchUserProfile();
            vm.setNickname(userProfile.getFirstName() + "#" + userProfile.getLastName());
        }
        if (guilds) {
            vm.setGuilds(connection.getApi().userOperations().getGuilds());
        }
        if (connections) {
            vm.setConnections(connection.getApi().userOperations().getConnections());
        }
        return vm;
    }
}
