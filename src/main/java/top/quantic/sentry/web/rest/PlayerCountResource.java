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
import top.quantic.sentry.domain.PlayerCount;
import top.quantic.sentry.security.AuthoritiesConstants;
import top.quantic.sentry.service.PlayerCountService;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;
import top.quantic.sentry.web.rest.vm.Series;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing PlayerCount.
 */
@RestController
@RequestMapping("/api")
public class PlayerCountResource {

    private final Logger log = LoggerFactory.getLogger(PlayerCountResource.class);

    @Inject
    private PlayerCountService playerCountService;

    /**
     * POST  /player-counts : Create a new playerCount.
     *
     * @param playerCount the playerCount to create
     * @return the ResponseEntity with status 201 (Created) and with body the new playerCount, or with status 400 (Bad Request) if the playerCount has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/player-counts")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<PlayerCount> createPlayerCount(@Valid @RequestBody PlayerCount playerCount) throws URISyntaxException {
        log.debug("REST request to save PlayerCount : {}", playerCount);
        if (playerCount.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("playerCount", "idexists", "A new playerCount cannot already have an ID")).body(null);
        }
        PlayerCount result = playerCountService.save(playerCount);
        return ResponseEntity.created(new URI("/api/player-counts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("playerCount", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /player-counts : Updates an existing playerCount.
     *
     * @param playerCount the playerCount to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated playerCount,
     * or with status 400 (Bad Request) if the playerCount is not valid,
     * or with status 500 (Internal Server Error) if the playerCount couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/player-counts")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<PlayerCount> updatePlayerCount(@Valid @RequestBody PlayerCount playerCount) throws URISyntaxException {
        log.debug("REST request to update PlayerCount : {}", playerCount);
        if (playerCount.getId() == null) {
            return createPlayerCount(playerCount);
        }
        PlayerCount result = playerCountService.save(playerCount);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("playerCount", playerCount.getId().toString()))
            .body(result);
    }

    /**
     * GET  /player-counts : get all the playerCounts.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of playerCounts in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/player-counts")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<List<PlayerCount>> getAllPlayerCounts(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of PlayerCounts");
        Page<PlayerCount> page = playerCountService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/player-counts");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/player-counts/day")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getPastDayPlayerCounts()
        throws URISyntaxException {
        log.debug("REST request to get past day PlayerCounts as Series");
        List<Series> series = playerCountService.getPointsFromPastDay();
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    /**
     * GET  /player-counts/:id : get the "id" playerCount.
     *
     * @param id the id of the playerCount to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the playerCount, or with status 404 (Not Found)
     */
    @GetMapping("/player-counts/{id}")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<PlayerCount> getPlayerCount(@PathVariable String id) {
        log.debug("REST request to get PlayerCount : {}", id);
        PlayerCount playerCount = playerCountService.findOne(id);
        return Optional.ofNullable(playerCount)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /player-counts/:id : delete the "id" playerCount.
     *
     * @param id the id of the playerCount to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/player-counts/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deletePlayerCount(@PathVariable String id) {
        log.debug("REST request to delete PlayerCount : {}", id);
        playerCountService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("playerCount", id.toString())).build();
    }

}
