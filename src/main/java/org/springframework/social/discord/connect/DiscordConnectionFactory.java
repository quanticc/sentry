package org.springframework.social.discord.connect;

import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.discord.api.Discord;

public class DiscordConnectionFactory extends OAuth2ConnectionFactory<Discord> {

    /**
     * Creates a factory for Discord connections.
     *
     * @param clientId client ID
     * @param clientSecret client secret
     */
    public DiscordConnectionFactory(String clientId, String clientSecret) {
        super("discord", new DiscordServiceProvider(clientId, clientSecret), new DiscordAdapter());
    }
}
