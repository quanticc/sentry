package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.PlayerCount;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for the PlayerCount entity.
 */
@SuppressWarnings("unused")
public interface PlayerCountRepository extends MongoRepository<PlayerCount, String> {

    List<PlayerCount> findByTimestampAfter(ZonedDateTime dateTime);

    List<PlayerCount> findByTimestampAfterAndTimestampBefore(ZonedDateTime after, ZonedDateTime before);

    Long countByRegionAndValueAndTimestamp(String region, Long value, ZonedDateTime timestamp);
}
