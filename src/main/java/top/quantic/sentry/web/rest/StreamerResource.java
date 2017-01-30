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
import org.springframework.web.bind.annotation.*;
import top.quantic.sentry.service.StreamerService;
import top.quantic.sentry.service.dto.StreamerDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Streamer.
 */
@RestController
@RequestMapping("/api")
public class StreamerResource {

    private final Logger log = LoggerFactory.getLogger(StreamerResource.class);

    @Inject
    private StreamerService streamerService;

    /**
     * POST  /streamers : Create a new streamer.
     *
     * @param streamerDTO the streamerDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new streamerDTO, or with status 400 (Bad Request) if the streamer has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/streamers")
    @Timed
    public ResponseEntity<StreamerDTO> createStreamer(@Valid @RequestBody StreamerDTO streamerDTO) throws URISyntaxException {
        log.debug("REST request to save Streamer : {}", streamerDTO);
        if (streamerDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("streamer", "idexists", "A new streamer cannot already have an ID")).body(null);
        }
        StreamerDTO result = streamerService.save(streamerDTO);
        return ResponseEntity.created(new URI("/api/streamers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("streamer", result.getId()))
            .body(result);
    }

    /**
     * PUT  /streamers : Updates an existing streamer.
     *
     * @param streamerDTO the streamerDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated streamerDTO,
     * or with status 400 (Bad Request) if the streamerDTO is not valid,
     * or with status 500 (Internal Server Error) if the streamerDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/streamers")
    @Timed
    public ResponseEntity<StreamerDTO> updateStreamer(@Valid @RequestBody StreamerDTO streamerDTO) throws URISyntaxException {
        log.debug("REST request to update Streamer : {}", streamerDTO);
        if (streamerDTO.getId() == null) {
            return createStreamer(streamerDTO);
        }
        StreamerDTO result = streamerService.save(streamerDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("streamer", streamerDTO.getId()))
            .body(result);
    }

    /**
     * GET  /streamers : get all the streamers.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of streamers in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/streamers")
    @Timed
    public ResponseEntity<List<StreamerDTO>> getAllStreamers(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Streamers");
        Page<StreamerDTO> page = streamerService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/streamers");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /streamers/:id : get the "id" streamer.
     *
     * @param id the id of the streamerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the streamerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/streamers/{id}")
    @Timed
    public ResponseEntity<StreamerDTO> getStreamer(@PathVariable String id) {
        log.debug("REST request to get Streamer : {}", id);
        StreamerDTO streamerDTO = streamerService.findOne(id);
        return Optional.ofNullable(streamerDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /streamers/:id : delete the "id" streamer.
     *
     * @param id the id of the streamerDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/streamers/{id}")
    @Timed
    public ResponseEntity<Void> deleteStreamer(@PathVariable String id) {
        log.debug("REST request to delete Streamer : {}", id);
        streamerService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("streamer", id)).build();
    }

}
