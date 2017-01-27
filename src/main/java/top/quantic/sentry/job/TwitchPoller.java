package top.quantic.sentry.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class TwitchPoller implements Job {

    private static final Logger log = LoggerFactory.getLogger(TwitchPoller.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("Use StreamPoller job instead");
    }

}
