package top.quantic.sentry.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMonitorListener implements Monitor.Listener {

    private static final Logger log = LoggerFactory.getLogger(LoggingMonitorListener.class);

    @Override
    public void onStateChange(Monitor.Snapshot snapshot) {
        log.debug("[{}] Status change: {} -> {} ({}%)",
            snapshot.getName(), snapshot.getLastState(), snapshot.getCurrentState(),
            (int) (snapshot.getLastRatio() * 100));
    }
}
