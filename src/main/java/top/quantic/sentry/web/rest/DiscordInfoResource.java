package top.quantic.sentry.web.rest;

import org.springframework.http.MediaType;
import org.springframework.social.connect.Connection;
import org.springframework.social.discord.api.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.quantic.sentry.service.DiscordSocialService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class DiscordInfoResource {

    @Inject
    private DiscordSocialService discordSocialService;

    @RequestMapping(value = "/discord-info",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public DiscordInfoResponse getDiscordInfo() {
        Optional<Connection<Discord>> optional = discordSocialService.getCurrentUserConnection();
        if (!optional.isPresent()) {
            return new DiscordInfoResponse();
        } else {
            Connection<Discord> connection = optional.get();
            UserOperations userOperations = connection.getApi().userOperations();
            return new DiscordInfoResponse(
                userOperations.getUser(), connection.getImageUrl(),
                userOperations.getGuilds(), userOperations.getConnections()
            );
        }
    }

    class DiscordInfoResponse {

        public DiscordUser user;
        public String avatarUrl;
        public List<DiscordGuild> guilds;
        public List<DiscordConnection> connections;

        public DiscordInfoResponse(DiscordUser user, String avatarUrl,
                                   List<DiscordGuild> guilds, List<DiscordConnection> connections) {
            this.user = user;
            this.avatarUrl = avatarUrl;
            this.guilds = guilds;
            this.connections = connections;
        }

        public DiscordInfoResponse() {

        }
    }
}
