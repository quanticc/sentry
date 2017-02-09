package top.quantic.sentry.service;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.UserCount;
import top.quantic.sentry.repository.UserCountRepository;
import top.quantic.sentry.service.util.Adder;
import top.quantic.sentry.web.rest.vm.Series;

import javax.inject.Inject;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;
import static top.quantic.sentry.service.util.ChartUtil.*;

/**
 * Service Implementation for managing UserCount.
 */
@Service
public class UserCountService {

    private static final Logger log = LoggerFactory.getLogger(UserCountService.class);

    private final UserCountRepository userCountRepository;
    private final MetricRegistry metricRegistry;

    private final Function<UserCount, String> seriesMapper = UserCount::getStatus;
    private final Function<UserCount, ZonedDateTime> timeMapper = UserCount::getTimestamp;
    private final Function<UserCount, Long> valueMapper = UserCount::getValue;
    private final Function<Map.Entry<Pair<ZonedDateTime, String>, Adder>, UserCount> objectMapper =
        entry -> new UserCount()
            .status(entry.getKey().getRight())
            .timestamp(entry.getKey().getLeft())
            .value(entry.getValue().average());

    @Inject
    public UserCountService(UserCountRepository userCountRepository, MetricRegistry metricRegistry) {
        this.userCountRepository = userCountRepository;
        this.metricRegistry = metricRegistry;
    }

    @Scheduled(cron = "10 * * * * ?")
    void storeUserCountMetrics() {
        ZonedDateTime timestamp = ZonedDateTime.now().truncatedTo(MINUTES);

        userCountRepository.save(
            metricRegistry.getHistograms((name, metric) -> name.startsWith("discord.ws.users"))
                .entrySet().stream()
                .map(entry -> Pair.of(extractTags(entry.getKey()), (long) entry.getValue().getSnapshot().getMean()))
                .map(pair -> new UserCount()
                    .bot(pair.getKey()[0])
                    .guild(pair.getKey()[1])
                    .status(pair.getKey()[2])
                    .value(pair.getValue())
                    .timestamp(timestamp))
                .collect(Collectors.toList())
        );
    }

    private String[] extractTags(String key) {
        return key.replaceAll("^.*\\[bot:(\\w+),guild:(\\w+),status:(\\w+)]$", "$1;$2;$3").split(";");
    }

    public List<Series> getGroupedPointsBetween(String bot, String guild,
                                                ZonedDateTime from, ZonedDateTime to) {
        List<Series> list = getAggregatedSeriesFromData(userCountRepository.findByBotAndGuildAndTimestampBetween(bot, guild, from, to),
            getResolution(Duration.between(from, to).toHours()), seriesMapper, timeMapper, valueMapper, objectMapper);
        // enrich data
        for (Series series : list) {
            if ("online".equals(series.getKey())) {
                series.setColor("#43b581");
                series.setArea(true);
            } else if ("connected".equals(series.getKey())) {
                series.setColor("#faa61a");
                series.setArea(true);
            } else if ("joined".equals(series.getKey())) {
                series.setColor("#7085d4");
            }
        }
        return list;
    }

    public List<Series> getMostRecentPoint(String bot, String guild) {
        // simple heuristic to get the most recent point
        List<UserCount> count = userCountRepository.findByBotAndGuildAndTimestampAfter(bot, guild,
            ZonedDateTime.now().minusSeconds(70)).collect(Collectors.toList());
        if (count == null) {
            return Collections.emptyList();
        } else {
            return getSeriesFromData(count, seriesMapper, timeMapper, valueMapper);
        }
    }

    /**
     * Save a userCount.
     *
     * @param userCount the entity to save
     * @return the persisted entity
     */
    public UserCount save(UserCount userCount) {
        log.debug("Request to save UserCount : {}", userCount);
        UserCount result = userCountRepository.save(userCount);
        return result;
    }

    /**
     * Get all the userCounts.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<UserCount> findAll(Pageable pageable) {
        log.debug("Request to get all UserCounts");
        Page<UserCount> result = userCountRepository.findAll(pageable);
        return result;
    }

    /**
     * Get one userCount by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public UserCount findOne(String id) {
        log.debug("Request to get UserCount : {}", id);
        UserCount userCount = userCountRepository.findOne(id);
        return userCount;
    }

    /**
     * Delete the  userCount by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete UserCount : {}", id);
        userCountRepository.delete(id);
    }
}
