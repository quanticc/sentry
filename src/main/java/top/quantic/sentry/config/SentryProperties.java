package top.quantic.sentry.config;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sentry", ignoreUnknownFields = false)
public class SentryProperties {

    private final Discord discord = new Discord();

    public Discord getDiscord() {
        return discord;
    }

    public static class Discord {

        private List<String> administrators = new ArrayList<>();
        private List<String> defaultPrefixes = Lists.newArrayList("!");
        private Coordinator coordinator = new Coordinator();

        public List<String> getAdministrators() {
            return administrators;
        }

        public void setAdministrators(List<String> administrators) {
            this.administrators = administrators;
        }

        public List<String> getDefaultPrefixes() {
            return defaultPrefixes;
        }

        public void setDefaultPrefixes(List<String> defaultPrefixes) {
            this.defaultPrefixes = defaultPrefixes;
        }

        public Coordinator getCoordinator() {
            return coordinator;
        }

        public void setCoordinator(Coordinator coordinator) {
            this.coordinator = coordinator;
        }

        public static class Coordinator {

            private String webhookUrl;
            private String username;
            private String avatarUrl;

            public String getWebhookUrl() {
                return webhookUrl;
            }

            public void setWebhookUrl(String webhookUrl) {
                this.webhookUrl = webhookUrl;
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
        }
    }

}
