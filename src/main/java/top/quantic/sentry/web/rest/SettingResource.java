package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import top.quantic.sentry.security.AuthoritiesConstants;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.dto.SettingDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Setting.
 */
@RestController
@RequestMapping("/api")
public class SettingResource {

    private final Logger log = LoggerFactory.getLogger(SettingResource.class);

    @Inject
    private SettingService settingService;

    /**
     * POST  /settings : Create a new setting.
     *
     * @param settingDTO the settingDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new settingDTO, or with status 400 (Bad Request) if the setting has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/settings")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<SettingDTO> createSetting(@Valid @RequestBody SettingDTO settingDTO) throws URISyntaxException {
        log.debug("REST request to save Setting : {}", settingDTO);
        if (settingDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("setting", "idexists", "A new setting cannot already have an ID")).body(null);
        }
        SettingDTO result = settingService.save(settingDTO);
        return ResponseEntity.created(new URI("/api/settings/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("setting", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /settings : Updates an existing setting.
     *
     * @param settingDTO the settingDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated settingDTO,
     * or with status 400 (Bad Request) if the settingDTO is not valid,
     * or with status 500 (Internal Server Error) if the settingDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/settings")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<SettingDTO> updateSetting(@Valid @RequestBody SettingDTO settingDTO) throws URISyntaxException {
        log.debug("REST request to update Setting : {}", settingDTO);
        if (settingDTO.getId() == null) {
            return createSetting(settingDTO);
        }
        SettingDTO result = settingService.save(settingDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("setting", settingDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /settings : get all the settings.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of settings in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/settings")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<SettingDTO>> getAllSettings(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Settings");
        Page<SettingDTO> page = settingService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/settings");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /settings/:id : get the "id" setting.
     *
     * @param id the id of the settingDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the settingDTO, or with status 404 (Not Found)
     */
    @GetMapping("/settings/{id}")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<SettingDTO> getSetting(@PathVariable String id) {
        log.debug("REST request to get Setting : {}", id);
        SettingDTO settingDTO = settingService.findOne(id);
        return Optional.ofNullable(settingDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /settings/:id : delete the "id" setting.
     *
     * @param id the id of the settingDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/settings/{id}")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<Void> deleteSetting(@PathVariable String id) {
        log.debug("REST request to delete Setting : {}", id);
        settingService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("setting", id.toString())).build();
    }

}
