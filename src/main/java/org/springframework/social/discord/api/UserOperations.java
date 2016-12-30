package org.springframework.social.discord.api;

import java.util.List;

public interface UserOperations {

    String getProfileId();

    String getAvatarUrl();

    String getNickname();

    DiscordUser getUser();

    List<DiscordGuild> getGuilds();

    List<DiscordConnection> getConnections();
}
