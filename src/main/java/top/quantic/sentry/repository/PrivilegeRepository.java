package top.quantic.sentry.repository;

import top.quantic.sentry.domain.Privilege;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Privilege entity.
 */
@SuppressWarnings("unused")
public interface PrivilegeRepository extends MongoRepository<Privilege,String> {

}
