package top.quantic.sentry.service.util;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static top.quantic.sentry.service.util.DateUtil.humanize;
import static top.quantic.sentry.service.util.DateUtil.instantToDate;

public class LoggingTriggerListener implements TriggerListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingTriggerListener.class);

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        log.info("[{}] Firing job {}.{}", trigger.getKey().getName(),
            context.getJobDetail().getKey().getGroup(), context.getJobDetail().getKey().getName());
    }

    public void triggerMisfired(Trigger trigger) {
        log.info("[{}] Misfiring job {}.{} - Should have fired at {}", trigger.getKey().getName(),
            trigger.getJobKey().getGroup(), trigger.getJobKey().getName(),
            instantToDate(trigger.getNextFireTime().toInstant()));
    }

    public void triggerComplete(Trigger trigger, JobExecutionContext context,
                                Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        log.info("[{}] Completed job {}.{} in {}{}",
            trigger.getKey().getName(), context.getJobDetail().getKey().getGroup(),
            context.getJobDetail().getKey().getName(), humanize(Duration.ofMillis(context.getJobRunTime())),
            (context.getResult() != null ? " with result: " + context.getResult() : ""));
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }
}
