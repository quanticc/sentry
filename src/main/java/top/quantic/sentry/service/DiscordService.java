package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.SentryProperties;

@Service
public class DiscordService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    private final UserService userService;
    private final SentryProperties sentryProperties;

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
}
