package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.UserCount;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for the UserCount entity.
 */
@SuppressWarnings("unused")
public interface UserCountRepository extends MongoRepository<UserCount, String> {

    List<UserCount> findByTimestampAfter(ZonedDateTime dateTime);

    List<UserCount> findByBotAndGuildAndTimestampAfter(String bot, String guild, ZonedDateTime dateTime);

    List<UserCount> findByBotAndGuildAndTimestampAfterAndTimestampBefore(String bot, String guild, ZonedDateTime after, ZonedDateTime before);
}
