package org.springframework.social.discord.api.impl;

import org.springframework.social.MissingAuthorizationException;
import org.springframework.web.client.RestTemplate;

class AbstractDiscordOperations {

    private static final String API_BASE_URL = "https://discordapp.com/api";

    protected final RestTemplate restTemplate;
    protected final boolean isAuthorized;

    public AbstractDiscordOperations(RestTemplate restTemplate, boolean isAuthorized) {
        this.restTemplate = restTemplate;
        this.isAuthorized = isAuthorized;
    }

    protected void requireAuthorization() {
        if (!isAuthorized) {
            throw new MissingAuthorizationException("discord");
        }
    }

    protected String buildUri(String path) {
        return API_BASE_URL + path;
    }
}
