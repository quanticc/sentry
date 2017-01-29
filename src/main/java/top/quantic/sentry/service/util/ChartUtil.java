package top.quantic.sentry.service.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.quantic.sentry.web.rest.vm.Series;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.Multimaps.flatteningToMultimap;
import static com.google.common.collect.Multimaps.toMultimap;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class ChartUtil {

    private static final Logger log = LoggerFactory.getLogger(ChartUtil.class);

    public static <T> Map<String, List<Series>> getTimeGroupedSeriesFromData(List<T> counts,
                                                                             Function<T, String> seriesMapper,
                                                                             Function<T, ZonedDateTime> timeMapper,
                                                                             Function<T, Long> valueMapper) {
        Map<String, List<Series>> result = new LinkedHashMap<>();
        result.put("year", getSeriesFromData(counts, seriesMapper, timeMapper, valueMapper,
            ZonedDateTime.now().minusYears(1), null));
        result.put("month", getSeriesFromData(counts, seriesMapper, timeMapper, valueMapper,
            ZonedDateTime.now().minusMonths(1), null));
        result.put("week", getSeriesFromData(counts, seriesMapper, timeMapper, valueMapper,
            ZonedDateTime.now().minusWeeks(1), null));
        result.put("day", getSeriesFromData(counts, seriesMapper, timeMapper, valueMapper,
            ZonedDateTime.now().minusDays(1), null));
        return result;
    }

    public static <T> List<Series> getSeriesFromData(List<T> counts,
                                                     Function<T, String> seriesMapper,
                                                     Function<T, ZonedDateTime> timeMapper,
                                                     Function<T, Long> valueMapper,
                                                     ZonedDateTime after,
                                                     ZonedDateTime before) {
        // create a map of categories to x-y pairs
        Multimap<String, Point> byRegion = counts.stream()
            .filter(count ->
                (after == null || timeMapper.apply(count).isAfter(after))
                    && (before == null || timeMapper.apply(count).isBefore(before)))
            .collect(toMultimap(
                seriesMapper,
                count -> new Point(timeMapper.apply(count).toEpochSecond() * 1000, valueMapper.apply(count)),
                MultimapBuilder.treeKeys().hashSetValues()::build));
        // build a list of default values x-0 for each category
        Set<Long> timestamps = counts.stream()
            .map(count -> timeMapper.apply(count).toEpochSecond())
            .collect(Collectors.toSet());
        Multimap<String, Point> defaults = byRegion.keySet().stream()
            .collect(flatteningToMultimap(
                region -> region,
                region -> timestamps.stream().map(timestamp -> new Point(timestamp * 1000, 0)),
                MultimapBuilder.treeKeys().hashSetValues()::build));
        byRegion.putAll(defaults);
        log.debug("{} across {} found",
            inflect(byRegion.size(), "data point"), inflect(timestamps.size(), "timestamp"));
        // convert to expected structure
        return byRegion.asMap().entrySet().stream()
            .map(entry -> new Series(entry.getKey()).values(
                entry.getValue().stream()
                    .map(pair -> Arrays.asList(pair.getX(), pair.getY()))
                    .sorted(Comparator.comparingInt(o -> o.get(0).intValue()))
                    .collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    private ChartUtil() {

    }
}
