package top.quantic.sentry.repository;

import top.quantic.sentry.domain.Bot;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Bot entity.
 */
@SuppressWarnings("unused")
public interface BotRepository extends MongoRepository<Bot,String> {

}
