package top.quantic.sentry.repository;

import top.quantic.sentry.domain.Setting;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Setting entity.
 */
@SuppressWarnings("unused")
public interface SettingRepository extends MongoRepository<Setting,String> {

}
