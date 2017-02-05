package top.quantic.sentry.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String ACTUATOR = "ROLE_ACTUATOR";

    public static final String MANAGER = "ROLE_MANAGER";

    public static final String SUPPORT = "ROLE_SUPPORT";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {
    }
}
