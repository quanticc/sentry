package top.quantic.sentry.service;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.domain.Privilege;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.repository.PermissionRepository;
import top.quantic.sentry.repository.PrivilegeRepository;
import top.quantic.sentry.service.util.SentryException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static top.quantic.sentry.discord.util.DiscordUtil.getResourcesFromCommand;
import static top.quantic.sentry.discord.util.DiscordUtil.getRolesFromMessage;

/**
 * Service Implementation for managing Permission.
 */
@Service
public class PermissionService {

    private final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository permissionRepository;
    private final PrivilegeRepository privilegeRepository;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository, PrivilegeRepository privilegeRepository) {
        this.permissionRepository = permissionRepository;
        this.privilegeRepository = privilegeRepository;
    }

    public boolean hasPermission(IMessage message, String operation, Command command) {
        return hasPermission(getRolesFromMessage(message), operation, getResourcesFromCommand(command));
    }

    public boolean hasPermission(IMessage message, String operation, String resource) {
        return hasPermission(getRolesFromMessage(message), operation, resource);
    }

    public boolean hasPermission(Set<String> roles, String operation, String resource) {
        Set<PermissionType> typeSet = checkPermissions(roles, operation, Collections.singleton(resource));
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }

    public boolean hasPermission(Set<String> roles, String operation, Set<String> resources) {
        Set<PermissionType> typeSet = checkPermissions(roles, operation, resources);
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }

    /**
     * Get the types of permissions assigned to the given roles, operation and resources.
     * The roles will be extracted from a Discord message's author and the resources from
     * a Command, taking wildcard resource ("*") and categories into account.
     *
     * @param message   the Discord message to extract roles from
     * @param operation the permissible operation
     * @param command   the Command to extract resources from
     * @return a Set of types of Permission, like ALLOW or DENY. Can also contain both or none.
     */
    public Set<PermissionType> check(IMessage message, String operation, Command command) {
        return checkPermissions(getRolesFromMessage(message), operation, getResourcesFromCommand(command));
    }

    public Set<PermissionType> check(String role, String operation, String resource) {
        return checkPermissions(Sets.newHashSet(role), operation, Collections.singleton(resource));
    }

    public Set<PermissionType> check(Set<String> roles, String operation, String resource) {
        return checkPermissions(roles, operation, Collections.singleton(resource));
    }

    private Set<PermissionType> checkPermissions(Set<String> roles, String operation, Set<String> resources) {
        if (resources.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<String> translated = roles.stream()
                .map(role -> {
                    List<Privilege> privileges = privilegeRepository.findByKey(role);
                    if (privileges.isEmpty()) {
                        return Collections.singleton(role);
                    } else {
                        return privileges.stream().map(Privilege::getRole).collect(Collectors.toSet());
                    }
                })
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            log.trace("Checking '{}' on {} for {}", operation, resources, translated);
            return permissionRepository.findByRoleInAndOperationAndResourceIn(translated, operation, resources)
                .stream()
                .map(Permission::getType)
                .collect(Collectors.toSet());
        }
    }

    /**
     * Save a permission.
     *
     * @param permission the entity to save
     * @return the persisted entity
     */
    public Permission save(Permission permission) throws SentryException {
        log.debug("Request to save Permission : {}", permission);
        // reject permission if a privilege already maps to this
        if (!privilegeRepository.findByKey(permission.getRole()).isEmpty()) {
            throw new SentryException("A privilege already maps to this: " + permission.getRole());
        }
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
    public void delete(String id) {
        log.debug("Request to delete Permission : {}", id);
        permissionRepository.delete(id);
    }

    public static boolean isAllowed(Set<PermissionType> typeSet) {
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }
}
