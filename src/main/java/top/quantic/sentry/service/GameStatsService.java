package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.service.util.GameStats;
import top.quantic.sentry.service.util.StatsProvider;

import java.util.List;

@Service
public class GameStatsService {

    private static final Logger log = LoggerFactory.getLogger(GameStatsService.class);

    private final RestTemplate restTemplate;
    private final List<StatsProvider> statsProviderMap;

    @Autowired
    public GameStatsService(RestTemplate restTemplate, List<StatsProvider> statsProviderMap) {
        this.restTemplate = restTemplate;
        this.statsProviderMap = statsProviderMap;
    }

    public String getServerAddress(String url) {
        // discover provider
        StatsProvider provider = statsProviderMap.stream()
            .filter(p -> p.matches(url))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Not a valid provider URL"));
        GameStats stats = provider.getGameStats(url);
        return null;
    }
}
