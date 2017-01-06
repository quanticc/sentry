package top.quantic.sentry.config;

import org.apache.commons.lang3.RandomStringUtils;

public class Operations {

    public static final String COORDINATOR_KEY = RandomStringUtils.randomAlphanumeric(7);

    public static final String EXECUTE = "execute";

    private Operations() {

    }
}
