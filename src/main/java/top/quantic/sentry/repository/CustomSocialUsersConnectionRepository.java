package top.quantic.sentry.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.social.connect.*;
import top.quantic.sentry.domain.SocialUserConnection;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomSocialUsersConnectionRepository implements UsersConnectionRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomSocialUsersConnectionRepository.class);

    private SocialUserConnectionRepository socialUserConnectionRepository;

    private ConnectionFactoryLocator connectionFactoryLocator;

    private ConnectionSignUp connectionSignUp;

    public CustomSocialUsersConnectionRepository(SocialUserConnectionRepository socialUserConnectionRepository, ConnectionFactoryLocator connectionFactoryLocator) {
        this.socialUserConnectionRepository = socialUserConnectionRepository;
        this.connectionFactoryLocator = connectionFactoryLocator;
    }

    /**
     * The command to execute to create a new local user profile in the event no user id could be mapped to a connection.
     * Allows for implicitly creating a user profile from connection data during a provider sign-in attempt.
     * Defaults to null, indicating explicit sign-up will be required to complete the provider sign-in attempt.
     *
     * @param connectionSignUp a {@link ConnectionSignUp} object
     * @see #findUserIdsWithConnection(Connection)
     */
    public void setConnectionSignUp(ConnectionSignUp connectionSignUp) {
        this.connectionSignUp = connectionSignUp;
    }

    @Override
    public List<String> findUserIdsWithConnection(Connection<?> connection) {
        ConnectionKey key = connection.getKey();
        List<SocialUserConnection> socialUserConnections =
            socialUserConnectionRepository.findAllByProviderIdAndProviderUserId(key.getProviderId(), key.getProviderUserId());
        if (socialUserConnections.size() == 0 && connectionSignUp != null) {
            log.debug("Executing implicit sign-up for user id : {}", key.getProviderUserId());
            String newUserId = connectionSignUp.execute(connection);
            createConnectionRepository(newUserId).addConnection(connection);
            return Collections.singletonList(newUserId);
        }
        return socialUserConnections.stream()
            .map(SocialUserConnection::getUserId)
            .collect(Collectors.toList());
    }

    @Override
    public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
        List<SocialUserConnection> socialUserConnections =
            socialUserConnectionRepository.findAllByProviderIdAndProviderUserIdIn(providerId, providerUserIds);
        return socialUserConnections.stream()
            .map(SocialUserConnection::getUserId)
            .collect(Collectors.toSet());
    }

    @Override
    public ConnectionRepository createConnectionRepository(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return new CustomSocialConnectionRepository(userId, socialUserConnectionRepository, connectionFactoryLocator);
    }
}
