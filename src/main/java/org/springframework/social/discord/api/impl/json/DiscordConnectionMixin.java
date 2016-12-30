package org.springframework.social.discord.api.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract class DiscordConnectionMixin extends DiscordObjectMixin {

    @JsonProperty("visibility")
    private long visibility;

    @JsonProperty("friend_sync")
    private boolean friendSync;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    DiscordConnectionMixin(@JsonProperty("id") String id) {
    }

}
