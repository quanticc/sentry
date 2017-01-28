package top.quantic.sentry.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.PlayerCount;
import top.quantic.sentry.repository.PlayerCountRepository;
import top.quantic.sentry.web.rest.vm.Series;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.flatteningToMultimap;
import static com.google.common.collect.Multimaps.toMultimap;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Service Implementation for managing PlayerCount.
 */
@Service
public class PlayerCountService {

    private static final Logger log = LoggerFactory.getLogger(PlayerCountService.class);

    private final PlayerCountRepository playerCountRepository;
    private final MetricRegistry metricRegistry;

    private final Map<String, Long> lastValueMap = new ConcurrentHashMap<>();

    @Autowired
    public PlayerCountService(PlayerCountRepository playerCountRepository, MetricRegistry metricRegistry) {
        this.playerCountRepository = playerCountRepository;
        this.metricRegistry = metricRegistry;
    }

    @Scheduled(cron = "10 * * * * ?")
    void reportMetrics() {
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

    public List<PlayerCount> findAllFromPastDay() {
        return playerCountRepository.findByTimestampAfter(ZonedDateTime.now().minusDays(1));
    }

    public List<Series> getPointsFromPastDay() {
        List<PlayerCount> counts = findAllFromPastDay();
        // x-axis is normalized between entities but some values could be missing
        // in those cases, use y = 0
        // create a map of categories to x-y pairs
        Multimap<String, Pair> byRegion = counts.stream()
            .collect(toMultimap(
                PlayerCount::getRegion,
                count -> new Pair(count.getTimestamp().toEpochSecond(), count.getValue()),
                MultimapBuilder.treeKeys().hashSetValues()::build));
        // build a list of default values x-0 for each category
        Set<Long> timestamps = counts.stream()
            .map(count -> count.getTimestamp().toEpochSecond())
            .collect(Collectors.toSet());
        Multimap<String, Pair> defaults = byRegion.keySet().stream()
            .collect(flatteningToMultimap(
                region -> region,
                region -> timestamps.stream().map(timestamp -> new Pair(timestamp, 0)),
                MultimapBuilder.treeKeys().hashSetValues()::build));
        byRegion.putAll(defaults);
        // convert to expected structure
        return byRegion.asMap().entrySet().stream()
            .map(entry -> new Series(entry.getKey()).values(
                entry.getValue().stream()
                    .map(pair -> Arrays.asList(pair.x, pair.y))
                    .sorted(Comparator.comparingInt(o -> o.get(0).intValue()))
                    .collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    private static class Pair {
        private final Number x;
        private final Number y;

        Pair(Number x, Number y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return Objects.equals(x, pair.x);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x);
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
}
