package top.quantic.sentry.web.rest.vm;

import org.springframework.social.discord.api.DiscordConnection;
import org.springframework.social.discord.api.DiscordGuild;
import org.springframework.social.discord.api.DiscordUser;

import java.util.List;

public class DiscordConnectionVM {

    private DiscordUser user;
    private String userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private List<DiscordGuild> guilds;
    private List<DiscordConnection> connections;

    public DiscordConnectionVM() {

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public DiscordUser getUser() {
        return user;
    }

    public void setUser(DiscordUser user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public List<DiscordGuild> getGuilds() {
        return guilds;
    }

    public void setGuilds(List<DiscordGuild> guilds) {
        this.guilds = guilds;
    }

    public List<DiscordConnection> getConnections() {
        return connections;
    }

    public void setConnections(List<DiscordConnection> connections) {
        this.connections = connections;
    }
}
