package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Flow;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Flow entity.
 */
@SuppressWarnings("unused")
public interface FlowRepository extends MongoRepository<Flow, String> {

    List<Flow> findByEnabledIsTrueAndInput(String input);

    List<Flow> findByEnabledIsTrueAndInputAndMessage(String input, String message);
}
