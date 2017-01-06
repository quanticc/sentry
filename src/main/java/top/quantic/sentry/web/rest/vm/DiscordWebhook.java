package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordWebhook {

    private String content;

    private String username;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private boolean tts;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isTts() {
        return tts;
    }

    public void setTts(boolean tts) {
        this.tts = tts;
    }
}
