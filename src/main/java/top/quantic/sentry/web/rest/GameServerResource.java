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
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.dto.GameServerDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing GameServer.
 */
@RestController
@RequestMapping("/api")
public class GameServerResource {

    private final Logger log = LoggerFactory.getLogger(GameServerResource.class);

    @Inject
    private GameServerService gameServerService;

    /**
     * POST  /game-servers : Create a new gameServer.
     *
     * @param gameServerDTO the gameServerDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new gameServerDTO, or with status 400 (Bad Request) if the gameServer has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/game-servers")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<GameServerDTO> createGameServer(@Valid @RequestBody GameServerDTO gameServerDTO) throws URISyntaxException {
        log.debug("REST request to save GameServer : {}", gameServerDTO);
        if (gameServerDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("gameServer", "idexists", "A new gameServer cannot already have an ID")).body(null);
        }
        GameServerDTO result = gameServerService.save(gameServerDTO);
        return ResponseEntity.created(new URI("/api/game-servers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("gameServer", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /game-servers : Updates an existing gameServer.
     *
     * @param gameServerDTO the gameServerDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated gameServerDTO,
     * or with status 400 (Bad Request) if the gameServerDTO is not valid,
     * or with status 500 (Internal Server Error) if the gameServerDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/game-servers")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<GameServerDTO> updateGameServer(@Valid @RequestBody GameServerDTO gameServerDTO) throws URISyntaxException {
        log.debug("REST request to update GameServer : {}", gameServerDTO);
        if (gameServerDTO.getId() == null) {
            return createGameServer(gameServerDTO);
        }
        GameServerDTO result = gameServerService.save(gameServerDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("gameServer", gameServerDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /game-servers : get all the gameServers.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of gameServers in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/game-servers")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<GameServerDTO>> getAllGameServers(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of GameServers");
        Page<GameServerDTO> page = gameServerService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/game-servers");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /game-servers/:id : get the "id" gameServer.
     *
     * @param id the id of the gameServerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the gameServerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/game-servers/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<GameServerDTO> getGameServer(@PathVariable String id) {
        log.debug("REST request to get GameServer : {}", id);
        GameServerDTO gameServerDTO = gameServerService.findOne(id);
        return Optional.ofNullable(gameServerDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /game-servers/:id : delete the "id" gameServer.
     *
     * @param id the id of the gameServerDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/game-servers/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteGameServer(@PathVariable String id) {
        log.debug("REST request to delete GameServer : {}", id);
        gameServerService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("gameServer", id.toString())).build();
    }

    @PostMapping("/game-servers/refresh")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<GameServerDTO>> refreshAllGameServers(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to refresh all GameServers");
        try {
            gameServerService.refreshAll();
            return getAllGameServers(pageable);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createErrorAlert("Could not fetch data from remote panel"))
                .body(null);
        }
    }

}
