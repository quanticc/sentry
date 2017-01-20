package top.quantic.sentry.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import top.quantic.sentry.service.GameServerService;

public class StatusCheck implements Job {

    @Autowired
    private GameServerService gameServerService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        gameServerService.updateGameServers();
    }
}
