package top.quantic.sentry.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import top.quantic.sentry.service.StreamerService;

public class StreamPoller implements Job {

    private static final Logger log = LoggerFactory.getLogger(StreamPoller.class);
    private static final double EVENTS_PER_SECOND = 1.0;
    private static final int EXPIRE_MINUTES = 360;
    private static final int GRACE_PERIOD = 0;

    @Autowired
    private StreamerService streamerService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        StreamerService.Config config = new StreamerService.Config();
        config.setEventsPerSecond(Math.max(0.1, doubleOrDefault(dataMap, "events_per_second", EVENTS_PER_SECOND)));
        config.setExpireMinutes(Math.max(1, longOrDefault(dataMap, "expire_minutes", EXPIRE_MINUTES)));
        config.setGracePeriod(Math.max(0, longOrDefault(dataMap, "grace_period", GRACE_PERIOD)));
        streamerService.publishStreams(config);
    }

    private long longOrDefault(JobDataMap map, String key, long defaultValue) {
        try {
            if (map.containsKey(key)) {
                return map.getLongValue(key);
            }
        } catch (Exception e) {
            log.warn("Could not get long from {} -> {}", key, map.get(key));
        }
        return defaultValue;
    }

    private double doubleOrDefault(JobDataMap map, String key, double defaultValue) {
        try {
            if (map.containsKey(key)) {
                return map.getDoubleValue(key);
            }
        } catch (Exception e) {
            log.warn("Could not get double from {} -> {}", key, map.get(key));
        }
        return defaultValue;
    }

}
