package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Subscriber;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Subscriber entity.
 */
@SuppressWarnings("unused")
public interface SubscriberRepository extends MongoRepository<Subscriber, String> {

    List<Subscriber> findByChannel(String channel);

    List<Subscriber> findByChannelAndType(String channel, String type);
}
