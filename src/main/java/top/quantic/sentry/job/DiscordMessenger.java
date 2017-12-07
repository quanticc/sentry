package top.quantic.sentry.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import top.quantic.sentry.discord.core.ClientRegistry;

import java.util.Map;
import java.util.Optional;

import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;
import static top.quantic.sentry.discord.util.DiscordUtil.snowflake;

public class DiscordMessenger implements Job {

    @Autowired
    private ClientRegistry clientRegistry;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String botId = jobDataMap.getString("bot");
        String channelId = jobDataMap.getString("channel");
        String content = jobDataMap.getString("content");

        if (botId != null && channelId != null && content != null) {
            Optional<IDiscordClient> client = clientRegistry.getClients().entrySet().stream()
                .filter(entry -> botId.equals(entry.getKey().getId())
                    || botId.equals(entry.getKey().getName())
                    || (entry.getValue().isReady() && botId.equals(entry.getValue().getOurUser().getStringID())))
                .map(Map.Entry::getValue)
                .findAny();
            if (client.isPresent()) {
                IChannel channel = client.get().getChannelByID(snowflake(channelId));
                if (channel != null) {
                    sendMessage(channel, content);
                } else {
                    throw new JobExecutionException("Channel " + channelId + " is not known by the bot");
                }
            } else {
                throw new JobExecutionException("No bot exists with identifier: " + botId);
            }
        } else {
            throw new JobExecutionException("Job requires bot, channel and content parameters");
        }
    }
}
