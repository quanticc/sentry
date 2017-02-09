package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import top.quantic.sentry.domain.PlayerCount;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

/**
 * Spring Data MongoDB repository for the PlayerCount entity.
 */
@SuppressWarnings("unused")
public interface PlayerCountRepository extends MongoRepository<PlayerCount, String> {

    Stream<PlayerCount> findByTimestampAfter(ZonedDateTime dateTime);

    @Query("{ 'timestamp' : {'$gte': ?0, '$lte': ?1 } }")
    Stream<PlayerCount> findByTimestampBetween(ZonedDateTime from, ZonedDateTime to);

    Long countByRegionAndValueAndTimestamp(String region, Long value, ZonedDateTime timestamp);
}
