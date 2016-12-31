package top.quantic.sentry.config;

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

        public List<String> getAdministrators() {
            return administrators;
        }
    }

}
