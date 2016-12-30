package org.springframework.social.discord.connect;

import org.springframework.social.discord.api.Discord;
import org.springframework.social.discord.api.impl.DiscordTemplate;
import org.springframework.social.oauth2.AbstractOAuth2ServiceProvider;
import org.springframework.social.oauth2.OAuth2Template;

public class DiscordServiceProvider extends AbstractOAuth2ServiceProvider<Discord> {

    public DiscordServiceProvider(String clientId, String clientSecret) {
        super(createOAuth2Template(clientId, clientSecret));
    }

    private static OAuth2Template createOAuth2Template(String clientId, String clientSecret) {
        OAuth2Template oAuth2Template = new OAuth2Template(clientId, clientSecret, "https://discordapp.com/api/oauth2/authorize", "https://discordapp.com/api/oauth2/token");
        oAuth2Template.setUseParametersForClientAuthentication(true);
        return oAuth2Template;
    }

    @Override
    public Discord getApi(String accessToken) {
        return new DiscordTemplate(accessToken);
    }
}
