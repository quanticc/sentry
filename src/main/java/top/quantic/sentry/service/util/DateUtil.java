package top.quantic.sentry.service.util;

import net.redhogs.cronparser.CronExpressionDescriptor;
import org.apache.commons.lang3.tuple.Pair;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.units.JustNow;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class DateUtil {

    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    public static String humanize(Duration duration) {
        return humanize(duration, false, false);
    }

    public static String humanize(Duration duration, boolean compact, boolean elide) {
        Duration abs = duration.abs();
        long totalSeconds = abs.getSeconds();
        if (totalSeconds == 0) {
            if (compact) {
                return abs.toMillis() + "ms";
            } else {
                return inflect(abs.toMillis(), "millisecond");
            }
        }
        long d = totalSeconds / (3600 * 24);
        long h = (totalSeconds % (3600 * 24)) / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        String days = compact ? d + "d" : inflect(d, "day");
        String hours = compact ? h + "h" : inflect(h, "hour");
        String minutes = compact ? m + "m" : inflect(m, "minute");
        String seconds = compact ? s + "s" : inflect(s, "second");
        return Stream.of(Pair.of(d, days), Pair.of(h, hours), Pair.of(m, minutes), Pair.of(s, seconds))
            .filter(pair -> !pair.getValue().isEmpty() && (!elide || pair.getKey() != 0))
            .map(Pair::getValue)
            .collect(Collectors.joining(compact ? "" : ", "));
    }

    public static Instant systemToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static LocalDateTime instantToSystem(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static String formatElapsed(double seconds) {
        long totalSeconds = (long) seconds;
        return String.format(
            "%d:%02d:%02d",
            totalSeconds / 3600,
            (totalSeconds % 3600) / 60,
            totalSeconds % 60);
    }

    public static String now(String format) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    public static String formatMillis(final long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);

        return (hours == 0 ? "00" : hours < 10 ? String.valueOf("0" + hours) : String.valueOf(hours)) +
            ":" +
            (minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) : String.valueOf(minutes)) +
            ":" +
            (seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) : String.valueOf(seconds));
    }

    public static String formatDuration(final Duration duration) {
        long absSeconds = Math.abs(duration.getSeconds());
        long seconds = absSeconds % 60;
        long minutes = (absSeconds % 3600) / 60;
        long hours = absSeconds / 3600;

        return (hours == 0 ? "" : hours + ":") +
            (minutes == 0 ? "00" : minutes < 10 ? String.valueOf("0" + minutes) : String.valueOf(minutes)) +
            ":" +
            (seconds == 0 ? "00" : seconds < 10 ? String.valueOf("0" + seconds) : String.valueOf(seconds));
    }

    public static String humanizeCronPatterns(String patterns) {
        String[] array = patterns.split("\\||;");
        if (array.length == 1) {
            return humanizeCronPattern(patterns);
        } else {
            return Arrays.stream(array)
                .map(DateUtil::humanizeCronPattern)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(".\n"));
        }
    }

    public static String humanizeCronPattern(String pattern) {
        try {
            return CronExpressionDescriptor.getDescription(pattern, Locale.ENGLISH);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Instant nextValidTimeFromCron(List<String> patterns) {
        Instant next = Instant.MAX;
        for (String pattern : patterns) {
            try {
                CronExpression cronExpression = new CronExpression(pattern);
                Instant nextPart = cronExpression.getNextValidTimeAfter(Date.from(Instant.now())).toInstant();
                next = nextPart.isBefore(next) ? nextPart : next;
            } catch (ParseException e) {
                log.warn("Could not parse cron expression: {}", e.toString());
            }
        }
        return next;
    }

    public static String relativeNextTriggerFromCron(List<String> patterns) {
        return formatRelative(nextValidTimeFromCron(patterns));
    }

    /**
     * Returns a corrected Date which originally has an incorrect time while keeping the same time-zone.
     *
     * @param incorrect the incorrect Date
     * @return a corrected ZonedDateTime, with the same time-zone as the given date but with the hour fixed.
     */
    public static ZonedDateTime correctOffsetSameZone(Date incorrect) {
        Instant instant = incorrect.toInstant();
        ZonedDateTime zoned = instant.atZone(ZoneId.systemDefault());
        return zoned.plusSeconds(zoned.getOffset().getTotalSeconds());
    }

    public static ZonedDateTime parseTimeDate(String s) {
        List<Date> parsed = new PrettyTimeParser().parse(s); // never null, can be empty
        if (!parsed.isEmpty()) {
            Date first = parsed.get(0);
            return ZonedDateTime.ofInstant(first.toInstant(), ZoneId.systemDefault());
        }
        log.warn("Could not parse a valid date from input: {}", s);
        return null;
    }

    public static String formatRelative(LocalDateTime then) {
        return formatRelative(then.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String formatRelative(ZonedDateTime then) {
        return formatRelative(then.toInstant());
    }

    public static String formatRelative(Instant then) {
        PrettyTime prettyTime = new PrettyTime(Locale.ENGLISH);
        prettyTime.removeUnit(JustNow.class);
        return then == Instant.MAX ? "never" : prettyTime.format(Date.from(then));
    }

    public static String formatAbsolute(Instant then) {
        PrettyTime prettyTime = new PrettyTime(Locale.ENGLISH);
        prettyTime.removeUnit(JustNow.class);
        return prettyTime.formatDuration(Date.from(then));
    }

    private DateUtil() {

    }
}
