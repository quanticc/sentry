package top.quantic.sentry.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import top.quantic.sentry.service.GameExpiryService;
import top.quantic.sentry.service.GameServerService;

public class RconCheck implements Job {

    private static final Logger log = LoggerFactory.getLogger(RconCheck.class);

    @Autowired
    private GameServerService gameServerService;

    @Autowired
    private GameExpiryService gameExpiryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long refreshed = gameServerService.refreshExpirationDates(gameExpiryService.getExpirationSeconds());
        if (refreshed > 0) {
            log.info("{} expiration date{} refreshed", refreshed, refreshed == 1 ? "" : "s");
            gameServerService.refreshRconPasswords();
        } else {
            log.info("All expiration dates are up-to-date");
        }
    }
}
