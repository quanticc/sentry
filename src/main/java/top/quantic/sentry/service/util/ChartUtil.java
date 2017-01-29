package top.quantic.sentry.service.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.quantic.sentry.web.rest.vm.Series;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Multimaps.flatteningToMultimap;
import static com.google.common.collect.Multimaps.toMultimap;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class ChartUtil {

    private static final Logger log = LoggerFactory.getLogger(ChartUtil.class);

    public static <T> List<Series> getAggregatedSeriesFromData(Stream<T> stream,
                                                               int minutesResolution,
                                                               Function<T, String> seriesMapper,
                                                               Function<T, ZonedDateTime> timeMapper,
                                                               Function<T, Long> valueMapper,
                                                               Function<Map.Entry<Pair<ZonedDateTime, String>, Adder>, T> objectMapper) {
        try (Stream<T> _stream = stream) {
            int resolution = Math.max(1, Math.min(60, minutesResolution));
            return getSeriesFromData(
                aggregate(_stream, resolution, seriesMapper, timeMapper, valueMapper, objectMapper),
                seriesMapper, timeMapper, valueMapper);
        }
    }

//    public static <T> Map<String, List<Series>> getAggregateGroupedSeriesFromData(Stream<T> stream,
//                                                                                  int minutesResolution,
//                                                                                  Function<T, String> seriesMapper,
//                                                                                  Function<T, ZonedDateTime> timeMapper,
//                                                                                  Function<T, Long> valueMapper,
//                                                                                  Function<Map.Entry<Pair<ZonedDateTime, String>, Adder>, T> objectMapper) {
//        try (Stream<T> _stream = stream) {
//            int resolution = Math.max(1, Math.min(60, minutesResolution));
//            return getTimeGroupedSeriesFromData(
//                aggregate(_stream, resolution, seriesMapper, timeMapper, valueMapper, objectMapper),
//                seriesMapper, timeMapper, valueMapper);
//        }
//    }

    private static <T> List<T> aggregate(Stream<T> stream,
                                         int resolution,
                                         Function<T, String> seriesMapper,
                                         Function<T, ZonedDateTime> timeMapper,
                                         Function<T, Long> valueMapper,
                                         Function<Map.Entry<Pair<ZonedDateTime, String>, Adder>, T> objectMapper) {
        return stream
            .map(count -> {
                // map to intermediate aggregation object
                int minute = timeMapper.apply(count).getMinute() / resolution * resolution;
                return Pair.of(
                    Pair.of(timeMapper.apply(count).withMinute(minute), seriesMapper.apply(count)),
                    new Adder(valueMapper.apply(count)));
            })
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue, Adder::sum))
            .entrySet().stream()
            .map(objectMapper)
            .collect(Collectors.toList());
    }

    public static <T> List<Series> getSeriesFromData(T count,
                                                     Function<T, String> seriesMapper,
                                                     Function<T, ZonedDateTime> timeMapper,
                                                     Function<T, Long> valueMapper) {
        return getSeriesFromData(Collections.singletonList(count), seriesMapper, timeMapper, valueMapper);
    }

    public static <T> List<Series> getSeriesFromData(List<T> counts,
                                                     Function<T, String> seriesMapper,
                                                     Function<T, ZonedDateTime> timeMapper,
                                                     Function<T, Long> valueMapper) {
        // create a map of categories to x-y pairs
        Multimap<String, Point> byRegion = counts.stream()
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

    public static int truncateResolution(int resolution, ZonedDateTime afterDateTime, ZonedDateTime beforeDateTime) {
        Duration duration = Duration.between(afterDateTime, beforeDateTime);
        long days = duration.toDays();
        if (days == 0) {
            return Math.max(1, resolution);
        } else if (days < 7) {
            return Math.max(10, resolution);
        } else if (days < 30) {
            return Math.max(30, resolution);
        } else {
            return Math.max(60, resolution);
        }
    }
}
