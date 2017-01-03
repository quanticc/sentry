package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sx.blah.discord.api.IDiscordClient;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscordService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    private final UserService userService;
    private final SentryProperties sentryProperties;

    private final Map<Bot, IDiscordClient> clients = new ConcurrentHashMap<>();

    @Autowired
    public DiscordService(UserService userService, SentryProperties sentryProperties) {
        this.userService = userService;
        this.sentryProperties = sentryProperties;
    }

    @Override
    public void afterPropertiesSet() {
        userService.deleteUser("admin");
        userService.deleteUser("user");
        if (sentryProperties.getDiscord().getAdministrators().isEmpty() && userService.getAdminCount() == 0) {
            log.info("*** No administrators set - Define some under 'sentry.discord.administrators' property");
        }
    }

    public Map<Bot, IDiscordClient> getClients() {
        return clients;
    }
}
