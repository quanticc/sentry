package top.quantic.sentry.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.RateLimiter;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.event.TwitchStreamEvent;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.web.rest.vm.TwitchStream;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static top.quantic.sentry.service.util.DateUtil.humanize;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class TwitchPoller implements Job {

    private static final Logger log = LoggerFactory.getLogger(TwitchPoller.class);
    private static final int BATCH_SIZE = 100;
    private static final double EVENTS_PER_SECOND = 1.0;
    private static final int EXPIRE_MINUTES = 360;

    @Autowired
    private SentryProperties sentryProperties;

    @Autowired
    private SettingService settingService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationEventPublisher publisher;

    private final RateLimiter limiter = RateLimiter.create(EVENTS_PER_SECOND);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        int batchSize = Math.max(1, Math.min(100, getIntOrDefault(dataMap, "batch_size", BATCH_SIZE)));
        double eventsPerSecond = Math.max(0.1, getDoubleOrDefault(dataMap, "events_per_second", EVENTS_PER_SECOND));
        int expireMinutes = Math.max(1, getIntOrDefault(dataMap, "expire_minutes", EXPIRE_MINUTES));

        List<Setting> streamers = settingService.findByKeyStartingWith("twitch.");

        try {
            int streams = 0;
            int recent = 0;
            for (int x = 0; x < (streamers.size() / batchSize) + 1; x++) {
                List<Setting> subList = streamers.subList(x * batchSize, Math.min(streamers.size(), (x + 1) * batchSize));
                String channels = subList.stream()
                    .map(Setting::getValue)
                    .collect(Collectors.joining(","));

                limiter.acquire();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Client-ID", sentryProperties.getTwitch().getClientId());
                ResponseEntity<TwitchStreamResponse> responseEntity = restTemplate.exchange(
                    "https://api.twitch.tv/kraken/streams?channel=" + channels + "&limit=" + batchSize,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    TwitchStreamResponse.class);

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    limiter.setRate(eventsPerSecond);
                    TwitchStreamResponse response = responseEntity.getBody();
                    streams += response.getStreams().size();
                    // send individually, but throttle
                    for (TwitchStream stream : response.getStreams()) {
                        if (!wasRecentlyPublished(stream, expireMinutes)) {
                            CompletableFuture.runAsync(() -> {
                                limiter.acquire();
                                publisher.publishEvent(new TwitchStreamEvent(stream));
                            });
                        } else {
                            recent++;
                        }
                    }

                } else {
                    log.warn("Could not retrieve streamers: {}", responseEntity);
                }
            }
            log.info("Received {} of {} ({} recently published)", streams,
                inflect(streamers.size(), "requested stream"), recent);
        } catch (RestClientException e) {
            log.warn("Exception while retrieving streamers", e);
        }
    }

    private boolean wasRecentlyPublished(TwitchStream stream, int expireMinutes) {
        String group = "twitch:" + stream.getChannel().getName();
        String key = "lastStream";
        String value = stream.getId() + "@" + stream.getCreatedAt().getEpochSecond();
        Optional<Setting> setting = settingService.findMostRecentByGuildAndKey(group, key);
        if (setting.isPresent()) {
            ZonedDateTime lastModified = setting.get().getLastModifiedDate();
            if (value.equals(setting.get().getValue())) {
                if (lastModified.plusMinutes(expireMinutes).isBefore(ZonedDateTime.now())) {
                    log.debug("[{}] Last announcement is older than {}: {}",
                        stream.getChannel().getName(), humanize(Duration.ofMinutes(expireMinutes)), lastModified);
                    settingService.updateValue(setting.get(), value);
                    return false;
                }
                return true;
            }
        }
        settingService.createSetting(group, key, value);
        return false;
    }

    private int getIntOrDefault(JobDataMap map, String key, int defaultValue) {
        try {
            if (map.containsKey(key)) {
                return map.getIntValue(key);
            }
        } catch (Exception e) {
            log.warn("Could not get integer from {} -> {}", key, map.get(key));
        }
        return defaultValue;
    }

    private double getDoubleOrDefault(JobDataMap map, String key, double defaultValue) {
        try {
            if (map.containsKey(key)) {
                return map.getDoubleValue(key);
            }
        } catch (Exception e) {
            log.warn("Could not get double from {} -> {}", key, map.get(key));
        }
        return defaultValue;
    }

    public static class TwitchStreamResponse {

        @JsonProperty("_total")
        private long total;
        private List<TwitchStream> streams;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public List<TwitchStream> getStreams() {
            return streams;
        }

        public void setStreams(List<TwitchStream> streams) {
            this.streams = streams;
        }

        @Override
        public String toString() {
            return "TwitchStreamResponse{" +
                "total=" + total +
                ", streams=" + streams +
                '}';
        }
    }

}
