package top.quantic.sentry.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingDetectorListener implements Detector.FlappingListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingDetectorListener.class);

    @Override
    public void onStateChange(Detector.Snapshot snapshot) {
        log.debug("[{}] Status change: {} -> {} (last={}, avg={})",
            snapshot.getName(), snapshot.getPreviousState(), snapshot.getState(), snapshot.getLastRatio(), snapshot.getAverageRatio());
    }
}
