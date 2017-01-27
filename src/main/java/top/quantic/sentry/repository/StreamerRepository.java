package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Streamer;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for the Streamer entity.
 */
@SuppressWarnings("unused")
public interface StreamerRepository extends MongoRepository<Streamer, String> {

    List<Streamer> findByEnabledIsTrueAndProvider(String provider);

    List<Streamer> findByEnabledIsTrueAndProviderAndLastAnnouncementBefore(String provider, ZonedDateTime date);
}
