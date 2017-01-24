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
import top.quantic.sentry.service.TimeFrameService;
import top.quantic.sentry.service.dto.TimeFrameDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing TimeFrame.
 */
@RestController
@RequestMapping("/api")
public class TimeFrameResource {

    private final Logger log = LoggerFactory.getLogger(TimeFrameResource.class);

    @Inject
    private TimeFrameService timeFrameService;

    /**
     * POST  /time-frames : Create a new timeFrame.
     *
     * @param timeFrameDTO the timeFrameDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new timeFrameDTO, or with status 400 (Bad Request) if the timeFrame has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/time-frames")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<TimeFrameDTO> createTimeFrame(@Valid @RequestBody TimeFrameDTO timeFrameDTO) throws URISyntaxException {
        log.debug("REST request to save TimeFrame : {}", timeFrameDTO);
        if (timeFrameDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("timeFrame", "idexists", "A new timeFrame cannot already have an ID")).body(null);
        }
        TimeFrameDTO result = timeFrameService.save(timeFrameDTO);
        return ResponseEntity.created(new URI("/api/time-frames/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("timeFrame", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /time-frames : Updates an existing timeFrame.
     *
     * @param timeFrameDTO the timeFrameDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated timeFrameDTO,
     * or with status 400 (Bad Request) if the timeFrameDTO is not valid,
     * or with status 500 (Internal Server Error) if the timeFrameDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/time-frames")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<TimeFrameDTO> updateTimeFrame(@Valid @RequestBody TimeFrameDTO timeFrameDTO) throws URISyntaxException {
        log.debug("REST request to update TimeFrame : {}", timeFrameDTO);
        if (timeFrameDTO.getId() == null) {
            return createTimeFrame(timeFrameDTO);
        }
        TimeFrameDTO result = timeFrameService.save(timeFrameDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("timeFrame", timeFrameDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /time-frames : get all the timeFrames.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of timeFrames in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/time-frames")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<TimeFrameDTO>> getAllTimeFrames(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of TimeFrames");
        Page<TimeFrameDTO> page = timeFrameService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/time-frames");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /time-frames/:id : get the "id" timeFrame.
     *
     * @param id the id of the timeFrameDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the timeFrameDTO, or with status 404 (Not Found)
     */
    @GetMapping("/time-frames/{id}")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<TimeFrameDTO> getTimeFrame(@PathVariable String id) {
        log.debug("REST request to get TimeFrame : {}", id);
        TimeFrameDTO timeFrameDTO = timeFrameService.findOne(id);
        return Optional.ofNullable(timeFrameDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /time-frames/:id : delete the "id" timeFrame.
     *
     * @param id the id of the timeFrameDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/time-frames/{id}")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Void> deleteTimeFrame(@PathVariable String id) {
        log.debug("REST request to delete TimeFrame : {}", id);
        timeFrameService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("timeFrame", id.toString())).build();
    }

}
