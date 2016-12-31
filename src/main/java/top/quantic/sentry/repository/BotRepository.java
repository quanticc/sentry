package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Bot;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the Bot entity.
 */
@SuppressWarnings("unused")
public interface BotRepository extends MongoRepository<Bot,String> {

    Optional<Bot> findByPrimaryIsTrue();

}
