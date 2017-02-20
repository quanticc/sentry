package top.quantic.sentry.config;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Application constants.
 */
public final class Constants {

    //Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";
    // Spring profiles for development, test and production, see http://jhipster.github.io/profiles/
    public static final String SPRING_PROFILE_DEVELOPMENT = "dev";
    public static final String SPRING_PROFILE_TEST = "test";
    public static final String SPRING_PROFILE_PRODUCTION = "prod";
    // Spring profile used when deploying with Spring Cloud (used when deploying to CloudFoundry)
    public static final String SPRING_PROFILE_CLOUD = "cloud";
    // Spring profile used when deploying to Heroku
    public static final String SPRING_PROFILE_HEROKU = "heroku";
    // Spring profile used to disable swagger
    public static final String SPRING_PROFILE_SWAGGER = "swagger";
    // Spring profile used to disable running liquibase
    public static final String SPRING_PROFILE_NO_LIQUIBASE = "no-liquibase";

    public static final String SYSTEM_ACCOUNT = "system";

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97 Safari/537.36";

    public static final String JOBS_PACKAGE = "top.quantic.sentry.job";
    public static final String EVENTS_PACKAGE = "top.quantic.sentry.event";

    public static final String INSTANCE_KEY = RandomStringUtils.randomAlphanumeric(7);

    public static final String UGC_DATE_FORMAT = "MMMM, dd yyyy HH:mm:ss";

    // Settings
    public static final String ANY = "*";
    public static final String KEY_PREFIX = "prefix";

    private Constants() {
    }
}
