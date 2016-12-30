package org.springframework.social.discord.security;

import org.springframework.social.discord.api.Discord;
import org.springframework.social.discord.connect.DiscordConnectionFactory;
import org.springframework.social.security.provider.OAuth2AuthenticationService;

public class DiscordAuthenticationService extends OAuth2AuthenticationService<Discord> {

    public DiscordAuthenticationService(String apiKey, String appSecret) {
        super(new DiscordConnectionFactory(apiKey, appSecret));
    }
}
