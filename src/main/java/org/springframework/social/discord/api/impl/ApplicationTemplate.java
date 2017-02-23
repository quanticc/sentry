package org.springframework.social.discord.api.impl;

import org.springframework.social.discord.api.ApplicationOperations;
import org.springframework.social.discord.api.DiscordApplicationInfo;
import org.springframework.web.client.RestTemplate;

public class ApplicationTemplate extends AbstractDiscordOperations implements ApplicationOperations {

    public ApplicationTemplate(RestTemplate restTemplate, boolean isAuthorized) {
        super(restTemplate, isAuthorized);
    }

    @Override
    public DiscordApplicationInfo getApplicationInfo() {
        return restTemplate.getForObject(buildUri("/oauth2/applications/@me"), DiscordApplicationInfo.class);
    }
}
