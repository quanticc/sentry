package top.quantic.sentry.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.Operations;
import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.repository.PermissionRepository;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Permission.
 */
@Service
@CacheConfig(cacheNames={"permissions"})
public class PermissionService {

    private final Logger log = LoggerFactory.getLogger(PermissionService.class);

    @Inject
    private PermissionRepository permissionRepository;

    public boolean hasPermission(String role, String operation, String resource) {
        return hasPermission(Collections.singleton(role), operation, resource);
    }

    public boolean hasPermission(Set<String> roles, String operation, String resource) {
        Set<PermissionType> typeSet = checkPermissions(roles, operation, resource);
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }

    public Set<PermissionType> checkPermissions(String role, String operation, String resource) {
        return checkPermissions(Collections.singleton(role), operation, resource);
    }

    public Set<PermissionType> checkPermissions(Set<String> roles, String operation, String resource) {
        String[] steps = operation.split("\\.");
        List<Permission> permissions = new ArrayList<>();
        permissions.addAll(getPermissions(roles, Operations.ALL, resource));
        for (int i = 0; i < steps.length; i++) {
            String op = StringUtils.join(Arrays.copyOfRange(steps, 0, i + 1), '.') + (i + 1 == steps.length ? "" : ".*");
            permissions.addAll(getPermissions(roles, op, resource));
        }
        //permissions.forEach(p -> log.debug("[{}] Attempt to '{}' on '{}' by {}", p.getType(), p.getOperation(), p.getResource(), p.getRole()));
        return permissions.stream().map(Permission::getType).collect(Collectors.toSet());
    }

    @Cacheable
    public List<Permission> getPermissions(Set<String> roles, String operation, String resource) {
        return roles.stream()
            .map(r -> permissionRepository.findByRoleAndOperationAndResource(r, operation, resource))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    /**
     * Save a permission.
     *
     * @param permission the entity to save
     * @return the persisted entity
     */
    @CacheEvict(allEntries = true)
    public Permission save(Permission permission) {
        log.debug("Request to save Permission : {}", permission);
        Permission result = permissionRepository.save(permission);
        return result;
    }

    /**
     * Get all the permissions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<Permission> findAll(Pageable pageable) {
        log.debug("Request to get all Permissions");
        Page<Permission> result = permissionRepository.findAll(pageable);
        return result;
    }

    /**
     * Get one permission by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public Permission findOne(String id) {
        log.debug("Request to get Permission : {}", id);
        Permission permission = permissionRepository.findOne(id);
        return permission;
    }

    /**
     * Delete the  permission by id.
     *
     * @param id the id of the entity
     */
    @CacheEvict(allEntries = true)
    public void delete(String id) {
        log.debug("Request to delete Permission : {}", id);
        permissionRepository.delete(id);
    }
}
