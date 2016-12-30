package org.springframework.social.discord.api.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.social.discord.api.DiscordUser;

import java.util.List;

abstract class DiscordApplicationInfoMixin extends DiscordObjectMixin {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("rpcOrigins")
    private List<String> rpcOrigins;

    @JsonProperty("flags")
    private long flags;

    @JsonProperty("owner")
    private DiscordUser owner;

    DiscordApplicationInfoMixin(@JsonProperty("id") String id) {
    }

}
