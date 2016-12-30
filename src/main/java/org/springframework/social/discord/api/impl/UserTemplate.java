package org.springframework.social.discord.api.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.discord.api.DiscordConnection;
import org.springframework.social.discord.api.DiscordGuild;
import org.springframework.social.discord.api.DiscordUser;
import org.springframework.social.discord.api.UserOperations;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class UserTemplate extends AbstractDiscordOperations implements UserOperations {

    private final RestTemplate restTemplate;

    public UserTemplate(RestTemplate restTemplate, boolean isAuthorizedForUser) {
        super(isAuthorizedForUser);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProfileId() {
        return getUser().getId();
    }

    @Override
    public String getAvatarUrl() {
        DiscordUser profile = getUser();
        return profile.getAvatar() == null ? null : String.format("https://cdn.discordapp.com/avatars/%s/%s.jpg",
            profile.getId(), profile.getAvatar());
    }

    @Override
    public String getNickname() {
        DiscordUser profile = getUser();
        return String.format("%s#%s", profile.getUsername(), profile.getDiscriminator());
    }

    @Override
    public DiscordUser getUser() {
        return restTemplate.getForObject(buildUri("/users/@me"), DiscordUser.class);
    }

    @Override
    public List<DiscordGuild> getGuilds() {
        ResponseEntity<List<DiscordGuild>> responseEntity = restTemplate.exchange(buildUri("/users/@me/guilds"),
            HttpMethod.GET, null, new ParameterizedTypeReference<List<DiscordGuild>>() {
            });
        return responseEntity.getBody();
    }

    @Override
    public List<DiscordConnection> getConnections() {
        ResponseEntity<List<DiscordConnection>> responseEntity = restTemplate.exchange(buildUri("/users/@me/connections"),
            HttpMethod.GET, null, new ParameterizedTypeReference<List<DiscordConnection>>() {
            });
        return responseEntity.getBody();
    }
}
