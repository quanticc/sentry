package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.PlayerCount;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

/**
 * Spring Data MongoDB repository for the PlayerCount entity.
 */
@SuppressWarnings("unused")
public interface PlayerCountRepository extends MongoRepository<PlayerCount, String> {

    PlayerCount findFirstByTimestampAfter(ZonedDateTime dateTime);

    Stream<PlayerCount> findByTimestampAfter(ZonedDateTime dateTime);

    Stream<PlayerCount> findByTimestampAfterAndTimestampBefore(ZonedDateTime after, ZonedDateTime before);

    Long countByRegionAndValueAndTimestamp(String region, Long value, ZonedDateTime timestamp);
}
