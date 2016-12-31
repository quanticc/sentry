package top.quantic.sentry.security.social;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UserProfile;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Authority;
import top.quantic.sentry.domain.User;
import top.quantic.sentry.repository.AuthorityRepository;
import top.quantic.sentry.repository.UserRepository;
import top.quantic.sentry.security.AuthoritiesConstants;

import java.util.HashSet;
import java.util.Set;

public class ImplicitConnectionSignUp implements ConnectionSignUp {

    private static final Logger log = LoggerFactory.getLogger(ImplicitConnectionSignUp.class);

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final SentryProperties sentryProperties;

    public ImplicitConnectionSignUp(UserRepository userRepository, AuthorityRepository authorityRepository,
                                    PasswordEncoder passwordEncoder, SentryProperties sentryProperties) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.sentryProperties = sentryProperties;
    }

    @Override
    public String execute(Connection<?> connection) {
        UserProfile userProfile = connection.fetchUserProfile();

        String email = userProfile.getEmail();
        String login = userProfile.getId();
        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));

        Set<Authority> authorities;
        if (sentryProperties.getDiscord().getAdministrators().contains(login)) {
            log.info("Giving administrative role to {} ({})", userProfile.getUsername(), userProfile.getId());
            authorities = new HashSet<>(2);
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.ADMIN));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.USER));
        } else {
            authorities = new HashSet<>(1);
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.USER));
        }

        User newUser = new User();
        newUser.setLogin(login);
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userProfile.getFirstName());
        newUser.setLastName(userProfile.getLastName());
        newUser.setEmail(email);
        newUser.setActivated(true);
        newUser.setAuthorities(authorities);
        newUser.setLangKey("en");

        userRepository.save(newUser);

        log.debug("Performing implicit sign up with user : {}", connection.getKey().getProviderUserId());
        return connection.getKey().getProviderUserId();
    }
}
