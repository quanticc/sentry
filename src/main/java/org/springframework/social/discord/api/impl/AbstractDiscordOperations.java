package org.springframework.social.discord.api.impl;

import org.springframework.social.MissingAuthorizationException;

class AbstractDiscordOperations {

    private final boolean isAuthorized;

    public AbstractDiscordOperations(boolean isAuthorized) {
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

    private static final String API_BASE_URL = "https://discordapp.com/api";
}
