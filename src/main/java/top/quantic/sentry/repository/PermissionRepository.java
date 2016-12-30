package top.quantic.sentry.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.quantic.sentry.domain.Permission;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Permission entity.
 */
@SuppressWarnings("unused")
public interface PermissionRepository extends MongoRepository<Permission,String> {

    List<Permission> findByRoleAndOperationAndResource(String role, String operation, String resource);
}
