package top.quantic.sentry.job;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class SchedulerStatus implements Job {

    private static final Logger log = LoggerFactory.getLogger(SchedulerStatus.class);

    @Autowired
    private SchedulerFactoryBean schedulerFactory;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        try {
            log.info("{}", scheduler.getMetaData().toString());
        } catch (SchedulerException e) {
            log.warn("Could not get scheduler meta-data", e);
        }
    }
}
