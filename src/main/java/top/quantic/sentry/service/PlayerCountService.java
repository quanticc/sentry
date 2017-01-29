package top.quantic.sentry.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.PlayerCount;
import top.quantic.sentry.repository.PlayerCountRepository;
import top.quantic.sentry.web.rest.vm.Series;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.time.temporal.ChronoUnit.MINUTES;
import static top.quantic.sentry.service.util.ChartUtil.getSeriesFromData;
import static top.quantic.sentry.service.util.ChartUtil.getTimeGroupedSeriesFromData;

/**
 * Service Implementation for managing PlayerCount.
 */
@Service
public class PlayerCountService {

    private static final Logger log = LoggerFactory.getLogger(PlayerCountService.class);

    private final PlayerCountRepository playerCountRepository;
    private final MetricRegistry metricRegistry;
    private final MongoTemplate mongoTemplate;

    private final Map<String, Long> lastValueMap = new ConcurrentHashMap<>();

    @Autowired
    public PlayerCountService(PlayerCountRepository playerCountRepository, MetricRegistry metricRegistry,
                              MongoTemplate mongoTemplate) {
        this.playerCountRepository = playerCountRepository;
        this.metricRegistry = metricRegistry;
        this.mongoTemplate = mongoTemplate;
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

    public Map<String, List<Series>> getGroupedPointsAfter(ZonedDateTime dateTime) {
        return getTimeGroupedSeriesFromData(playerCountRepository.findByTimestampAfter(dateTime),
            PlayerCount::getRegion, PlayerCount::getTimestamp, PlayerCount::getValue);
    }

    public Map<String, List<Series>> getGroupedPointsBetween(ZonedDateTime after, ZonedDateTime before) {
        return getTimeGroupedSeriesFromData(playerCountRepository.findByTimestampAfterAndTimestampBefore(after, before),
            PlayerCount::getRegion, PlayerCount::getTimestamp, PlayerCount::getValue);
    }

    public List<Series> getMostRecentPoint() {
        // simple heuristic to get the most recent point
        return getSeriesFromData(playerCountRepository.findByTimestampAfter(ZonedDateTime.now().minusSeconds(70)),
            PlayerCount::getRegion, PlayerCount::getTimestamp, PlayerCount::getValue, null, null);
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
