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
import top.quantic.sentry.service.SubscriberService;
import top.quantic.sentry.service.dto.SubscriberDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Subscriber.
 */
@RestController
@RequestMapping("/api")
public class SubscriberResource {

    private final Logger log = LoggerFactory.getLogger(SubscriberResource.class);

    @Inject
    private SubscriberService subscriberService;

    /**
     * POST  /subscribers : Create a new subscriber.
     *
     * @param subscriberDTO the subscriberDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new subscriberDTO, or with status 400 (Bad Request) if the subscriber has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/subscribers")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<SubscriberDTO> createSubscriber(@Valid @RequestBody SubscriberDTO subscriberDTO) throws URISyntaxException {
        log.debug("REST request to save Subscriber : {}", subscriberDTO);
        if (subscriberDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("subscriber", "idexists", "A new subscriber cannot already have an ID")).body(null);
        }
        SubscriberDTO result = subscriberService.save(subscriberDTO);
        return ResponseEntity.created(new URI("/api/subscribers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("subscriber", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /subscribers : Updates an existing subscriber.
     *
     * @param subscriberDTO the subscriberDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated subscriberDTO,
     * or with status 400 (Bad Request) if the subscriberDTO is not valid,
     * or with status 500 (Internal Server Error) if the subscriberDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/subscribers")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<SubscriberDTO> updateSubscriber(@Valid @RequestBody SubscriberDTO subscriberDTO) throws URISyntaxException {
        log.debug("REST request to update Subscriber : {}", subscriberDTO);
        if (subscriberDTO.getId() == null) {
            return createSubscriber(subscriberDTO);
        }
        SubscriberDTO result = subscriberService.save(subscriberDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("subscriber", subscriberDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /subscribers : get all the subscribers.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of subscribers in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/subscribers")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<SubscriberDTO>> getAllSubscribers(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Subscribers");
        Page<SubscriberDTO> page = subscriberService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/subscribers");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /subscribers/:id : get the "id" subscriber.
     *
     * @param id the id of the subscriberDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the subscriberDTO, or with status 404 (Not Found)
     */
    @GetMapping("/subscribers/{id}")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<SubscriberDTO> getSubscriber(@PathVariable String id) {
        log.debug("REST request to get Subscriber : {}", id);
        SubscriberDTO subscriberDTO = subscriberService.findOne(id);
        return Optional.ofNullable(subscriberDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /subscribers/:id : delete the "id" subscriber.
     *
     * @param id the id of the subscriberDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/subscribers/{id}")
    @Timed
    @Secured(AuthoritiesConstants.MANAGER)
    public ResponseEntity<Void> deleteSubscriber(@PathVariable String id) {
        log.debug("REST request to delete Subscriber : {}", id);
        subscriberService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("subscriber", id.toString())).build();
    }

}
