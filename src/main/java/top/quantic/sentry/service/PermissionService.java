package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.domain.Privilege;
import top.quantic.sentry.domain.enumeration.PermissionType;
import top.quantic.sentry.repository.PermissionRepository;
import top.quantic.sentry.repository.PrivilegeRepository;

import java.util.ArrayList;
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

    public boolean hasPermission(String role, String operation, String resource) {
        return hasPermission(Collections.singleton(role), operation, resource);
    }

    public boolean hasPermission(IMessage message, String operation, Command command) {
        return hasPermission(getRolesFromMessage(message), operation, getResourcesFromCommand(command));
    }

    public boolean hasPermission(IMessage message, String operation, String resource) {
        return hasPermission(getRolesFromMessage(message), operation, resource);
    }

    public boolean hasPermission(Set<String> roles, String operation, String resource) {
        Set<PermissionType> typeSet = check(roles, operation, resource);
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }

    public boolean hasPermission(Set<String> roles, String operation, List<String> resources) {
        Set<PermissionType> typeSet = check(roles, operation, resources);
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }

    public Set<PermissionType> check(Set<String> roles, String operation, List<String> resources) {
        Set<PermissionType> typeSet;
        if (resources.isEmpty()) {
            typeSet = Collections.emptySet();
        } else if (resources.size() == 1) {
            typeSet = check(roles, operation, resources.get(0));
        } else {
            typeSet = resources.stream()
                .map(resource -> check(roles, operation, resource))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        }
        return typeSet;
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
        return check(getRolesFromMessage(message), operation, getResourcesFromCommand(command));
    }

    public Set<PermissionType> check(IMessage message, String operation, String resource) {
        return check(getRolesFromMessage(message), operation, resource);
    }

    public Set<PermissionType> check(String role, String operation, String resource) {
        return check(Collections.singleton(role), operation, resource);
    }

    public Set<PermissionType> check(Set<String> roles, String operation, String resource) {
        // incoming roles will be discord object ids like user, role, channel, guild
        Set<String> translated = roles.stream()
            .map(privilegeRepository::findByKey)
            .flatMap(List::stream)
            .map(Privilege::getRole)
            .collect(Collectors.toSet());
        List<Permission> permissions = new ArrayList<>();
        permissions.addAll(getPermissions(translated, operation, resource));
        if (permissions.stream().map(Permission::getType).distinct().count() < 2) {
            permissions.addAll(getPermissions(roles, operation, resource));
        }
        return permissions.stream().map(Permission::getType).collect(Collectors.toSet());
    }

    private List<Permission> getPermissions(Set<String> roles, String operation, String resource) {
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
    public void delete(String id) {
        log.debug("Request to delete Permission : {}", id);
        permissionRepository.delete(id);
    }

    public static boolean isAllowed(Set<PermissionType> typeSet) {
        return typeSet.contains(PermissionType.ALLOW) && !typeSet.contains(PermissionType.DENY);
    }
}
