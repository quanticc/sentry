package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import top.quantic.sentry.domain.UserCount;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

/**
 * Spring Data MongoDB repository for the UserCount entity.
 */
@SuppressWarnings("unused")
public interface UserCountRepository extends MongoRepository<UserCount, String> {

    Stream<UserCount> findByBotAndGuildAndTimestampAfter(String bot, String guild, ZonedDateTime dateTime);

    @Query("{ 'bot' : ?0, 'guild' : ?1, 'timestamp' : {'$gte': ?2, '$lte': ?3 } }")
    Stream<UserCount> findByBotAndGuildAndTimestampBetween(String bot, String guild, ZonedDateTime from, ZonedDateTime to);
}
