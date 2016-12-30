package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import top.quantic.sentry.domain.Permission;
import top.quantic.sentry.service.PermissionService;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Permission.
 */
@RestController
@RequestMapping("/api")
public class PermissionResource {

    private final Logger log = LoggerFactory.getLogger(PermissionResource.class);
        
    @Inject
    private PermissionService permissionService;

    /**
     * POST  /permissions : Create a new permission.
     *
     * @param permission the permission to create
     * @return the ResponseEntity with status 201 (Created) and with body the new permission, or with status 400 (Bad Request) if the permission has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/permissions")
    @Timed
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody Permission permission) throws URISyntaxException {
        log.debug("REST request to save Permission : {}", permission);
        if (permission.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("permission", "idexists", "A new permission cannot already have an ID")).body(null);
        }
        Permission result = permissionService.save(permission);
        return ResponseEntity.created(new URI("/api/permissions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("permission", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /permissions : Updates an existing permission.
     *
     * @param permission the permission to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated permission,
     * or with status 400 (Bad Request) if the permission is not valid,
     * or with status 500 (Internal Server Error) if the permission couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/permissions")
    @Timed
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody Permission permission) throws URISyntaxException {
        log.debug("REST request to update Permission : {}", permission);
        if (permission.getId() == null) {
            return createPermission(permission);
        }
        Permission result = permissionService.save(permission);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("permission", permission.getId().toString()))
            .body(result);
    }

    /**
     * GET  /permissions : get all the permissions.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of permissions in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/permissions")
    @Timed
    public ResponseEntity<List<Permission>> getAllPermissions(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Permissions");
        Page<Permission> page = permissionService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/permissions");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /permissions/:id : get the "id" permission.
     *
     * @param id the id of the permission to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the permission, or with status 404 (Not Found)
     */
    @GetMapping("/permissions/{id}")
    @Timed
    public ResponseEntity<Permission> getPermission(@PathVariable String id) {
        log.debug("REST request to get Permission : {}", id);
        Permission permission = permissionService.findOne(id);
        return Optional.ofNullable(permission)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /permissions/:id : delete the "id" permission.
     *
     * @param id the id of the permission to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/permissions/{id}")
    @Timed
    public ResponseEntity<Void> deletePermission(@PathVariable String id) {
        log.debug("REST request to delete Permission : {}", id);
        permissionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("permission", id.toString())).build();
    }

}
