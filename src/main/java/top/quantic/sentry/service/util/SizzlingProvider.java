package top.quantic.sentry.service.util;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.web.rest.vm.SizzStatsResponse;

import java.util.regex.Pattern;

@Component
public class SizzlingProvider implements StatsProvider {

    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://)?sizzlingstats\\.com/([0-9]+)");

    private final SentryProperties sentryProperties;
    private final RestTemplate restTemplate;

    private final RateLimiter apiLimiter = RateLimiter.create(4);

    @Autowired
    public SizzlingProvider(SentryProperties sentryProperties, RestTemplate restTemplate) {
        this.sentryProperties = sentryProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean matches(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    @Override
    public String getId(String url) {
        return URL_PATTERN.matcher(url).replaceAll("$1");
    }

    @Override
    public GameStats getGameStats(String url) {
        apiLimiter.acquire();
        String statsUrl = sentryProperties.getStats().getEndpoints().get("ssStats");
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", Constants.USER_AGENT);
        ResponseEntity<SizzStatsResponse> response = restTemplate.exchange(statsUrl, HttpMethod.GET,
            new HttpEntity<>(headers), SizzStatsResponse.class, getId(url));
        return response.getBody().getStats();
    }
}
