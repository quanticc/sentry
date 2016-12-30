package org.springframework.social.discord.api.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract class DiscordUserProfileMixin extends DiscordObjectMixin {

    @JsonProperty("username")
    String username;

    @JsonProperty("verified")
    boolean verified;

    @JsonProperty("mfa_enabled")
    boolean mfaEnabled;

    @JsonProperty("avatar")
    String avatar;

    @JsonProperty("discriminator")
    String discriminator;

    @JsonProperty("email")
    String email;

    DiscordUserProfileMixin(@JsonProperty("id") String id) {
    }
}
