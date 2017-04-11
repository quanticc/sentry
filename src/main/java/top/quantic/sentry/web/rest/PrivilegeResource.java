package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.springframework.security.access.annotation.Secured;
import top.quantic.sentry.domain.Privilege;

import top.quantic.sentry.repository.PrivilegeRepository;
import top.quantic.sentry.security.AuthoritiesConstants;
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
 * REST controller for managing Privilege.
 */
@RestController
@RequestMapping("/api")
public class PrivilegeResource {

    private final Logger log = LoggerFactory.getLogger(PrivilegeResource.class);

    @Inject
    private PrivilegeRepository privilegeRepository;

    /**
     * POST  /privileges : Create a new privilege.
     *
     * @param privilege the privilege to create
     * @return the ResponseEntity with status 201 (Created) and with body the new privilege, or with status 400 (Bad Request) if the privilege has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/privileges")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Privilege> createPrivilege(@Valid @RequestBody Privilege privilege) throws URISyntaxException {
        log.debug("REST request to save Privilege : {}", privilege);
        if (privilege.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("privilege", "idexists", "A new privilege cannot already have an ID")).body(null);
        }
        Privilege result = privilegeRepository.save(privilege);
        return ResponseEntity.created(new URI("/api/privileges/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("privilege", result.getId()))
            .body(result);
    }

    /**
     * PUT  /privileges : Updates an existing privilege.
     *
     * @param privilege the privilege to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated privilege,
     * or with status 400 (Bad Request) if the privilege is not valid,
     * or with status 500 (Internal Server Error) if the privilege couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/privileges")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Privilege> updatePrivilege(@Valid @RequestBody Privilege privilege) throws URISyntaxException {
        log.debug("REST request to update Privilege : {}", privilege);
        if (privilege.getId() == null) {
            return createPrivilege(privilege);
        }
        Privilege result = privilegeRepository.save(privilege);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("privilege", privilege.getId()))
            .body(result);
    }

    /**
     * GET  /privileges : get all the privileges.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of privileges in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/privileges")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Privilege>> getAllPrivileges(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Privileges");
        Page<Privilege> page = privilegeRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/privileges");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /privileges/:id : get the "id" privilege.
     *
     * @param id the id of the privilege to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the privilege, or with status 404 (Not Found)
     */
    @GetMapping("/privileges/{id}")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Privilege> getPrivilege(@PathVariable String id) {
        log.debug("REST request to get Privilege : {}", id);
        Privilege privilege = privilegeRepository.findOne(id);
        return Optional.ofNullable(privilege)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /privileges/:id : delete the "id" privilege.
     *
     * @param id the id of the privilege to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/privileges/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deletePrivilege(@PathVariable String id) {
        log.debug("REST request to delete Privilege : {}", id);
        privilegeRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("privilege", id)).build();
    }

}
