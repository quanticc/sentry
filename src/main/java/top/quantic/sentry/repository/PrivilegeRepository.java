package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Privilege;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Privilege entity.
 */
@SuppressWarnings("unused")
public interface PrivilegeRepository extends MongoRepository<Privilege, String> {

    List<Privilege> findByKey(String key);

}
