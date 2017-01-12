package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.TimeFrame;

import java.util.List;

/**
 * Spring Data MongoDB repository for the TimeFrame entity.
 */
@SuppressWarnings("unused")
public interface TimeFrameRepository extends MongoRepository<TimeFrame, String> {

    List<TimeFrame> findBySubscriber(String subscriber);
}
