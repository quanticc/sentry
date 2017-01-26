package top.quantic.sentry.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Authority;
import top.quantic.sentry.domain.User;
import top.quantic.sentry.repository.AuthorityRepository;
import top.quantic.sentry.repository.UserRepository;
import top.quantic.sentry.security.AuthoritiesConstants;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SocialService {

    private final Logger log = LoggerFactory.getLogger(SocialService.class);

    private final UsersConnectionRepository usersConnectionRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SentryProperties sentryProperties;

    @Autowired
    public SocialService(UsersConnectionRepository usersConnectionRepository, AuthorityRepository authorityRepository,
                         PasswordEncoder passwordEncoder, UserRepository userRepository, MailService mailService,
                         SentryProperties sentryProperties) {
        this.usersConnectionRepository = usersConnectionRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.sentryProperties = sentryProperties;
    }

    public void deleteUserSocialConnection(String login) {
        ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(login);
        connectionRepository.findAllConnections().keySet().stream()
            .forEach(providerId -> {
                connectionRepository.removeConnections(providerId);
                log.debug("Delete user social connection providerId: {}", providerId);
            });
    }

    public void createSocialUser(Connection<?> connection, String langKey) {
        if (connection == null) {
            log.error("Cannot create social user because connection is null");
            throw new IllegalArgumentException("Connection cannot be null");
        }
        UserProfile userProfile = connection.fetchUserProfile();
        String providerId = connection.getKey().getProviderId();
        User user = createUserIfNotExist(userProfile, langKey, providerId);
        createSocialConnection(user.getLogin(), connection);
        mailService.sendSocialRegistrationValidationEmail(user, providerId);
    }

    private User createUserIfNotExist(UserProfile userProfile, String langKey, String providerId) {
        String email = userProfile.getEmail();
        String userName = userProfile.getUsername();
        if (!StringUtils.isBlank(userName)) {
            userName = userName.toLowerCase(Locale.ENGLISH);
        }
        if (StringUtils.isBlank(email) && StringUtils.isBlank(userName)) {
            log.error("Cannot create social user because email and login are null");
            throw new IllegalArgumentException("Email and login cannot be null");
        }
        if (StringUtils.isBlank(email) && userRepository.findOneByLogin(userName).isPresent()) {
            log.error("Cannot create social user because email is null and login already exist, login -> {}", userName);
            throw new IllegalArgumentException("Email cannot be null with an existing login");
        }
        if (!StringUtils.isBlank(email)) {
            Optional<User> user = userRepository.findOneByEmail(email);
            if (user.isPresent()) {
                log.info("User already exist associate the connection to this account");
                return user.get();
            }
        }

        String login = getLoginDependingOnProviderId(userProfile, providerId);
        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));

        Set<Authority> authorities;
        if (sentryProperties.getDiscord().getAdministrators().contains(login)) {
            log.info("Giving administrative role to {} ({})", userProfile.getUsername(), userProfile.getId());
            authorities = new HashSet<>(4);
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.ADMIN));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.MANAGER));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.SUPPORT));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.USER));
        } else if (sentryProperties.getDiscord().getAdministrators().contains(login)) {
            log.info("Giving manager role to {} ({})", userProfile.getUsername(), userProfile.getId());
            authorities = new HashSet<>(3);
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.MANAGER));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.SUPPORT));
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.USER));
        } else if (sentryProperties.getDiscord().getAdministrators().contains(login)) {
            log.info("Giving support role to {} ({})", userProfile.getUsername(), userProfile.getId());
            authorities = new HashSet<>(2);
            authorities.add(authorityRepository.findOne(AuthoritiesConstants.SUPPORT));
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
        newUser.setLangKey(langKey);

        return userRepository.save(newUser);
    }

    /**
     * @return login if provider manage a login like Twitter or Github otherwise email address.
     * Because provider like Google or Facebook didn't provide login or login like "12099388847393"
     */
    private String getLoginDependingOnProviderId(UserProfile userProfile, String providerId) {
        switch (providerId) {
            case "discord":
            case "twitter":
                // username is actually the user's snowflake id
                return userProfile.getUsername();
            default:
                return userProfile.getEmail();
        }
    }

    private void createSocialConnection(String login, Connection<?> connection) {
        ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(login);
        connectionRepository.addConnection(connection);
    }
}
