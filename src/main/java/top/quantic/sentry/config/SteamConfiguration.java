package top.quantic.sentry.config;

import com.ibasco.agql.protocols.valve.steam.webapi.SteamWebApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SteamConfiguration {

    @Autowired
    private SentryProperties sentryProperties;

    @Bean
    public SteamWebApiClient steamWebApiClient() {
        String apiKey = sentryProperties.getGameQuery().getSteamApiKey();
        if (apiKey == null) {
            throw new IllegalArgumentException("Steam API key not set!");
        }
        return new SteamWebApiClient(apiKey);
    }
}
