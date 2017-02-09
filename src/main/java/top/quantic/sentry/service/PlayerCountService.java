package top.quantic.sentry.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.PlayerCount;
import top.quantic.sentry.repository.PlayerCountRepository;
import top.quantic.sentry.service.util.Adder;
import top.quantic.sentry.web.rest.vm.Series;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.time.temporal.ChronoUnit.MINUTES;
import static top.quantic.sentry.service.util.ChartUtil.*;

/**
 * Service Implementation for managing PlayerCount.
 */
@Service
public class PlayerCountService {

    private static final Logger log = LoggerFactory.getLogger(PlayerCountService.class);

    private final PlayerCountRepository playerCountRepository;
    private final MetricRegistry metricRegistry;

    private final Map<String, Long> lastValueMap = new ConcurrentHashMap<>();

    private final Function<PlayerCount, String> seriesMapper = PlayerCount::getRegion;
    private final Function<PlayerCount, ZonedDateTime> timeMapper = PlayerCount::getTimestamp;
    private final Function<PlayerCount, Long> valueMapper = PlayerCount::getValue;
    private final Function<Map.Entry<Pair<ZonedDateTime, String>, Adder>, PlayerCount> objectMapper =
        entry -> new PlayerCount()
            .region(entry.getKey().getRight())
            .timestamp(entry.getKey().getLeft())
            .value(entry.getValue().average());

    @Autowired
    public PlayerCountService(PlayerCountRepository playerCountRepository, MetricRegistry metricRegistry) {
        this.playerCountRepository = playerCountRepository;
        this.metricRegistry = metricRegistry;
    }

    @Scheduled(cron = "10 * * * * ?")
    void storePlayerCountMetrics() {
        ZonedDateTime timestamp = ZonedDateTime.now().truncatedTo(MINUTES);

        Map<String, AtomicLong> aggregations = new ConcurrentHashMap<>();

        metricRegistry.getGauges((name, metric) -> name.startsWith("UGC.GameServer.player_count"))
            .entrySet().stream()
            // all servers -> group into regions
            .collect(toMultimap(
                entry -> extractRegion(entry.getKey()),
                Map.Entry::getValue,
                MultimapBuilder.treeKeys().arrayListValues()::build))
            .entries()
            .forEach(entry -> aggregations.computeIfAbsent(entry.getKey(), k -> new AtomicLong())
                .addAndGet((Integer) entry.getValue().getValue()));

        aggregations.entrySet().stream()
            .filter(entry -> entry.getValue().get() > 0 || lastValueMap.getOrDefault(entry.getKey(), 0L) != 0)
            .forEach(entry -> {
                String region = entry.getKey();
                Long value = entry.getValue().get();
                if (lastValueMap.getOrDefault(entry.getKey(), 0L) == 0) {
                    if (playerCountRepository.countByRegionAndValueAndTimestamp(region, value, timestamp) == 0) {
                        // add a 0 to the previous minute
                        playerCountRepository.save(new PlayerCount()
                            .timestamp(timestamp.minusMinutes(1))
                            .region(region)
                            .value(0L));
                    }
                }
                playerCountRepository.save(new PlayerCount()
                    .timestamp(timestamp)
                    .region(region)
                    .value(value));
                lastValueMap.put(region, value);
            });
    }

    private String extractRegion(String key) {
        return key.replaceAll("^.*\\[region:(\\w+),game:(\\w+)]$", "$1");
    }

    public List<Series> getGroupedPointsBetween(ZonedDateTime from, ZonedDateTime to) {
        return getAggregatedSeriesFromData(playerCountRepository.findByTimestampBetween(from, to),
            getResolution(Duration.between(from, to).toHours()),
            seriesMapper, timeMapper, valueMapper, objectMapper);
    }

    public List<Series> getMostRecentPoint() {
        // simple heuristic to get the most recent point
        List<PlayerCount> count = playerCountRepository.findByTimestampAfter(ZonedDateTime.now().minusSeconds(70))
            .collect(Collectors.toList());
        if (count == null) {
            return Collections.emptyList();
        } else {
            return getSeriesFromData(count, seriesMapper, timeMapper, valueMapper);
        }
    }

    /**
     * Save a playerCount.
     *
     * @param playerCount the entity to save
     * @return the persisted entity
     */
    public PlayerCount save(PlayerCount playerCount) {
        log.debug("Request to save PlayerCount : {}", playerCount);
        PlayerCount result = playerCountRepository.save(playerCount);
        return result;
    }

    /**
     * Get all the playerCounts.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<PlayerCount> findAll(Pageable pageable) {
        log.debug("Request to get all PlayerCounts");
        Page<PlayerCount> result = playerCountRepository.findAll(pageable);
        return result;
    }

    /**
     * Get one playerCount by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public PlayerCount findOne(String id) {
        log.debug("Request to get PlayerCount : {}", id);
        PlayerCount playerCount = playerCountRepository.findOne(id);
        return playerCount;
    }

    /**
     * Delete the  playerCount by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete PlayerCount : {}", id);
        playerCountRepository.delete(id);
    }

    private static class AggregatedPlayerCount {
        private String region;
        private Long value;
        private Long average;
        private Long count;
        private int year;
        private int month;
        private int day;
        private int hour;
        private int minute;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }

        public Long getAverage() {
            return average;
        }

        public void setAverage(Long average) {
            this.average = average;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getHour() {
            return hour;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public int getMinute() {
            return minute;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }
    }
}
