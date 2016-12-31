package org.springframework.social.discord.connect;

import org.springframework.social.connect.ApiAdapter;
import org.springframework.social.connect.ConnectionValues;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UserProfileBuilder;
import org.springframework.social.discord.api.Discord;
import org.springframework.social.discord.api.DiscordUser;
import org.springframework.web.client.HttpClientErrorException;

public class DiscordAdapter implements ApiAdapter<Discord> {
    @Override
    public boolean test(Discord api) {
        try {
            api.applicationOperations().getApplicationInfo();
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    @Override
    public void setConnectionValues(Discord api, ConnectionValues values) {
        DiscordUser profile = api.userOperations().getUser();
        values.setProviderUserId(String.valueOf(profile.getId()));
        values.setDisplayName(api.userOperations().getNickname());
        values.setProfileUrl("https://discordapp.com/channels/@me");
        values.setImageUrl(api.userOperations().getAvatarUrl());
    }

    @Override
    public UserProfile fetchUserProfile(Discord api) {
        DiscordUser profile = api.userOperations().getUser();
        return new UserProfileBuilder()
            .setId(profile.getId())
            .setFirstName(profile.getUsername())
            .setLastName("#" + profile.getDiscriminator())
            .setUsername(api.userOperations().getNickname())
            .setEmail(profile.getEmail())
            .build();
    }

    @Override
    public void updateStatus(Discord api, String message) {
        // not supported
    }
}
