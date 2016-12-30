package top.quantic.sentry.config.social;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.ProviderSignInUtils;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.discord.connect.DiscordConnectionFactory;
import org.springframework.social.security.AuthenticationNameUserIdSource;
import top.quantic.sentry.repository.AuthorityRepository;
import top.quantic.sentry.repository.CustomSocialUsersConnectionRepository;
import top.quantic.sentry.repository.SocialUserConnectionRepository;
import top.quantic.sentry.repository.UserRepository;
import top.quantic.sentry.security.social.CustomSignInAdapter;
import top.quantic.sentry.security.social.ImplicitConnectionSignUp;

import javax.inject.Inject;

// jhipster-needle-add-social-connection-factory-import-package

/**
 * Basic Spring Social configuration.
 *
 * <p>Creates the beans necessary to manage Connections to social services and
 * link accounts from those services to internal Users.</p>
 */
@Configuration
@EnableSocial
public class SocialConfiguration implements SocialConfigurer {
    private final Logger log = LoggerFactory.getLogger(SocialConfiguration.class);

    @Inject
    private SocialUserConnectionRepository socialUserConnectionRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    Environment environment;

    @Bean
    public ConnectController connectController(ConnectionFactoryLocator connectionFactoryLocator,
            ConnectionRepository connectionRepository) {

        ConnectController controller = new ConnectController(connectionFactoryLocator, connectionRepository);
        controller.setApplicationUrl(environment.getProperty("spring.application.url"));
        return controller;
    }

    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
        String discordClientId = environment.getProperty("spring.social.discord.clientId");
        String discordClientSecret = environment.getProperty("spring.social.discord.clientSecret");
        if (discordClientId != null && discordClientSecret != null) {
            log.debug("Configuring DiscordConnectionFactory");
            connectionFactoryConfigurer.addConnectionFactory(
                new DiscordConnectionFactory(
                    discordClientId,
                    discordClientSecret
                )
            );
        } else {
            log.error("Cannot configure DiscordConnectionFactory id or secret null");
        }

        // jhipster-needle-add-social-connection-factory
    }

    @Override
    public UserIdSource getUserIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        CustomSocialUsersConnectionRepository usersConnectionRepository = new CustomSocialUsersConnectionRepository(
            socialUserConnectionRepository, connectionFactoryLocator);
        usersConnectionRepository.setConnectionSignUp(new ImplicitConnectionSignUp(userRepository, authorityRepository, passwordEncoder));
        return usersConnectionRepository;
    }

    @Bean
    public SignInAdapter signInAdapter() {
        return new CustomSignInAdapter();
    }

    @Bean
    public ProviderSignInController providerSignInController(ConnectionFactoryLocator connectionFactoryLocator, UsersConnectionRepository usersConnectionRepository, SignInAdapter signInAdapter) throws Exception {
        ProviderSignInController providerSignInController = new ProviderSignInController(connectionFactoryLocator, usersConnectionRepository, signInAdapter);
        providerSignInController.setSignUpUrl("/social/signup");
        providerSignInController.setApplicationUrl(environment.getProperty("spring.application.url"));
        return providerSignInController;
    }

    @Bean
    public ProviderSignInUtils getProviderSignInUtils(ConnectionFactoryLocator connectionFactoryLocator, UsersConnectionRepository usersConnectionRepository) {
        return new ProviderSignInUtils(connectionFactoryLocator, usersConnectionRepository);
    }
}
