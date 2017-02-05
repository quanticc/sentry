package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TwitchStream {

    @JsonProperty("_id")
    private Long id;
    private String game;
    private long viewers;
    @JsonProperty("video_height")
    private long videoHeight;
    @JsonProperty("average_fps")
    private long averageFps;
    private long delay;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("is_playlist")
    private boolean playlist;
    private Map<String, String> preview;
    private Channel channel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public long getViewers() {
        return viewers;
    }

    public void setViewers(long viewers) {
        this.viewers = viewers;
    }

    public long getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(long videoHeight) {
        this.videoHeight = videoHeight;
    }

    public long getAverageFps() {
        return averageFps;
    }

    public void setAverageFps(long averageFps) {
        this.averageFps = averageFps;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPlaylist() {
        return playlist;
    }

    public void setPlaylist(boolean playlist) {
        this.playlist = playlist;
    }

    public Map<String, String> getPreview() {
        return preview;
    }

    public void setPreview(Map<String, String> preview) {
        this.preview = preview;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String toShortString() {
        return "TwitchStreamResponse{" +
            "id=" + id +
            ", game='" + game + '\'' +
            ", createdAt=" + createdAt +
            ", channel=" + channel.toShortString() +
            '}';
    }

    @Override
    public String toString() {
        return "TwitchStreamResponse{" +
            "id=" + id +
            ", game='" + game + '\'' +
            ", viewers=" + viewers +
            ", videoHeight=" + videoHeight +
            ", averageFps=" + averageFps +
            ", delay=" + delay +
            ", createdAt=" + createdAt +
            ", playlist=" + playlist +
            ", preview=" + preview +
            ", channel=" + channel +
            '}';
    }

    public static class Channel {
        @JsonProperty("_id")
        private long id;
        private boolean mature;
        private String status;
        @JsonProperty("broadcaster_language")
        private String broadcasterLanguage;
        @JsonProperty("display_name")
        private String displayName;
        private String game;
        private String language;
        private String name;
        @JsonProperty("created_at")
        private Instant createdAt;
        @JsonProperty("updated_at")
        private Instant updatedAt;
        private boolean partner;
        private String logo;
        @JsonProperty("video_banner")
        private String videoBanner;
        @JsonProperty("profile_banner")
        private String profileBanner;
        @JsonProperty("profile_banner_background_color")
        private String profileBannerBackgroundColor;
        private String url;
        private long views;
        private long followers;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public boolean isMature() {
            return mature;
        }

        public void setMature(boolean mature) {
            this.mature = mature;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBroadcasterLanguage() {
            return broadcasterLanguage;
        }

        public void setBroadcasterLanguage(String broadcasterLanguage) {
            this.broadcasterLanguage = broadcasterLanguage;
        }

        public String getDisplayName() {
            return displayName.replace("_", "\\_");
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getGame() {
            return game;
        }

        public void setGame(String game) {
            this.game = game;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public boolean isPartner() {
            return partner;
        }

        public void setPartner(boolean partner) {
            this.partner = partner;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public String getVideoBanner() {
            return videoBanner;
        }

        public void setVideoBanner(String videoBanner) {
            this.videoBanner = videoBanner;
        }

        public String getProfileBanner() {
            return profileBanner;
        }

        public void setProfileBanner(String profileBanner) {
            this.profileBanner = profileBanner;
        }

        public String getProfileBannerBackgroundColor() {
            return profileBannerBackgroundColor;
        }

        public void setProfileBannerBackgroundColor(String profileBannerBackgroundColor) {
            this.profileBannerBackgroundColor = profileBannerBackgroundColor;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }

        public long getFollowers() {
            return followers;
        }

        public void setFollowers(long followers) {
            this.followers = followers;
        }

        public String toShortString() {
            return "Channel{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", displayName='" + displayName + '\'' +
                ", game='" + game + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
        }

        @Override
        public String toString() {
            return "Channel{" +
                "id=" + id +
                ", mature=" + mature +
                ", status='" + status + '\'' +
                ", broadcasterLanguage='" + broadcasterLanguage + '\'' +
                ", displayName='" + displayName + '\'' +
                ", game='" + game + '\'' +
                ", language='" + language + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", partner=" + partner +
                ", logo='" + logo + '\'' +
                ", videoBanner='" + videoBanner + '\'' +
                ", profileBanner='" + profileBanner + '\'' +
                ", profileBannerBackgroundColor='" + profileBannerBackgroundColor + '\'' +
                ", url='" + url + '\'' +
                ", views=" + views +
                ", followers=" + followers +
                '}';
        }
    }
}
