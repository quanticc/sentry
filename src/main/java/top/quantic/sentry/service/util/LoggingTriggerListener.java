package top.quantic.sentry.service.util;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static top.quantic.sentry.service.util.DateUtil.*;

public class LoggingTriggerListener implements TriggerListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingTriggerListener.class);

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void triggerFired(Trigger trigger, JobExecutionContext context) {
//        log.info("[{}] Trigger '{}' is firing job", context.getJobDetail().getKey(), trigger.getKey().getName());
    }

    public void triggerMisfired(Trigger trigger) {
        log.info("[{}] Trigger '{}' misfired job - Should have fired at {} ({})",
            trigger.getJobKey(), trigger.getKey().getName(),
            instantToSystem(trigger.getNextFireTime().toInstant()),
            formatRelative(trigger.getNextFireTime().toInstant()));
    }

    public void triggerComplete(Trigger trigger, JobExecutionContext context,
                                Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        if (context.getJobRunTime() > 1000) {
            log.info("[{}] Trigger '{}' completed job in {}{}",
                context.getJobDetail().getKey(), trigger.getKey().getName(),
                humanize(Duration.ofMillis(context.getJobRunTime())),
                (context.getResult() != null ? " with result: " + context.getResult() : ""));
        }
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }
}
