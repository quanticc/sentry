package top.quantic.sentry.service;

import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Permission.
 */
@Service
public class PermissionService {

    private final Logger log = LoggerFactory.getLogger(PermissionService.class);
    
    @Inject
    private PermissionRepository permissionRepository;

    /**
     * Save a permission.
     *
     * @param permission the entity to save
     * @return the persisted entity
     */
    public Permission save(Permission permission) {
        log.debug("Request to save Permission : {}", permission);
        Permission result = permissionRepository.save(permission);
        return result;
    }

    /**
     *  Get all the permissions.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    public Page<Permission> findAll(Pageable pageable) {
        log.debug("Request to get all Permissions");
        Page<Permission> result = permissionRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one permission by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    public Permission findOne(String id) {
        log.debug("Request to get Permission : {}", id);
        Permission permission = permissionRepository.findOne(id);
        return permission;
    }

    /**
     *  Delete the  permission by id.
     *
     *  @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Permission : {}", id);
        permissionRepository.delete(id);
    }
}
