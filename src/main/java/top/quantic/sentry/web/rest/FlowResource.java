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
import top.quantic.sentry.service.FlowService;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.util.TaskException;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;
import top.quantic.sentry.web.rest.vm.DatadogPayload;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Flow.
 */
@RestController
@RequestMapping("/api")
public class FlowResource {

    private final Logger log = LoggerFactory.getLogger(FlowResource.class);

    @Inject
    private FlowService flowService;

    @PostMapping("/webhooks/{key}/datadog")
    @Timed
    public ResponseEntity<Void> executeFlow(@PathVariable String key, @RequestBody DatadogPayload event) {
        log.debug("REST request to execute a Datadog flow with key : {}", key);
        flowService.executeDatadogFlowsByKey(key, event);
        return ResponseEntity.ok().build();
    }

    /**
     * POST  /flows : Create a new flow.
     *
     * @param flowDTO the flowDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new flowDTO, or with status 400 (Bad Request) if the flow has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/flows")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<FlowDTO> createFlow(@Valid @RequestBody FlowDTO flowDTO) throws URISyntaxException {
        log.debug("REST request to save Flow : {}", flowDTO);
        if (flowDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("flow",
                "idexists", "A new flow cannot already have an ID"))
                .body(null);
        }
        try {
            FlowDTO result = flowService.save(flowDTO);
            return ResponseEntity.created(new URI("/api/flows/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("flow", result.getId()))
                .body(result);
        } catch (TaskException e) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("flow",
                "badarg", "An event flow must have a valid event type as message"))
                .body(null);
        }
    }

    /**
     * PUT  /flows : Updates an existing flow.
     *
     * @param flowDTO the flowDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated flowDTO,
     * or with status 400 (Bad Request) if the flowDTO is not valid,
     * or with status 500 (Internal Server Error) if the flowDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/flows")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<FlowDTO> updateFlow(@Valid @RequestBody FlowDTO flowDTO) throws URISyntaxException {
        log.debug("REST request to update Flow : {}", flowDTO);
        if (flowDTO.getId() == null) {
            return createFlow(flowDTO);
        }
        try {
            FlowDTO result = flowService.save(flowDTO);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("flow", flowDTO.getId()))
                .body(result);
        } catch (TaskException e) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("flow",
                "badarg", "An event flow must have a valid event type as message"))
                .body(null);
        }
    }

    /**
     * GET  /flows : get all the flows.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of flows in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/flows")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<FlowDTO>> getAllFlows(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Flows");
        Page<FlowDTO> page = flowService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/flows");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /flows/:id : get the "id" flow.
     *
     * @param id the id of the flowDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the flowDTO, or with status 404 (Not Found)
     */
    @GetMapping("/flows/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<FlowDTO> getFlow(@PathVariable String id) {
        log.debug("REST request to get Flow : {}", id);
        FlowDTO flowDTO = flowService.findOne(id);
        return Optional.ofNullable(flowDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /flows/:id : delete the "id" flow.
     *
     * @param id the id of the flowDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/flows/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteFlow(@PathVariable String id) {
        log.debug("REST request to delete Flow : {}", id);
        flowService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("flow", id)).build();
    }

}
