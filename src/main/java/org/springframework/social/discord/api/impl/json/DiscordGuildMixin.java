package org.springframework.social.discord.api.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract class DiscordGuildMixin extends DiscordObjectMixin {

    @JsonProperty("owner")
    private boolean owner;

    @JsonProperty("permissions")
    private long permissions;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("name")
    private String name;

    DiscordGuildMixin(@JsonProperty("id") String id) {
    }
}
