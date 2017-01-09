package top.quantic.sentry.config;

import org.apache.commons.lang3.RandomStringUtils;

public class Operations {

    public static final String COORDINATOR_KEY = RandomStringUtils.randomAlphanumeric(7);

    /**
     * Run a bot command.
     */
    public static final String EXECUTE = "execute";

    /**
     * Receive information about guilds only the bot is part of.
     */
    public static final String QUERY_ALL_GUILDS = "queryAllGuilds";

    private Operations() {

    }
}
