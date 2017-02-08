package org.springframework.social.discord.config.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.xml.ApiHelper;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.discord.api.Discord;

public class DiscordApiHelper implements ApiHelper<Discord> {

    private static final Logger log = LoggerFactory.getLogger(DiscordApiHelper.class);

    private final UsersConnectionRepository usersConnectionRepository;
    private final UserIdSource userIdSource;

    private DiscordApiHelper(UsersConnectionRepository usersConnectionRepository, UserIdSource userIdSource) {
        this.usersConnectionRepository = usersConnectionRepository;
        this.userIdSource = userIdSource;
    }

    @Override
    public Discord getApi() {
        if (log.isDebugEnabled()) {
            log.debug("Getting API binding instance for Discord");
        }

        Connection<Discord> connection = usersConnectionRepository.createConnectionRepository(userIdSource.getUserId()).findPrimaryConnection(Discord.class);
        if (log.isDebugEnabled() && connection == null) {
            log.debug("No current connection; Returning default DiscordTemplate instance.");
        }
        return connection != null ? connection.getApi() : null;
    }

}
