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
import top.quantic.sentry.service.TaskService;
import top.quantic.sentry.service.dto.TaskDTO;
import top.quantic.sentry.service.util.TaskException;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Task.
 */
@RestController
@RequestMapping("/api")
public class TaskResource {

    private final Logger log = LoggerFactory.getLogger(TaskResource.class);

    @Inject
    private TaskService taskService;

    /**
     * POST  /tasks : Create a new task.
     *
     * @param taskDTO the taskDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new taskDTO, or with status 400 (Bad Request) if the task has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/tasks")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO taskDTO) throws URISyntaxException {
        log.debug("REST request to save Task : {}", taskDTO);
        if (taskDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("task",
                "idexists", "A new task cannot already have an ID")).body(null);
        }
        try {
            TaskDTO result = taskService.save(taskDTO);
            return ResponseEntity.created(new URI("/api/tasks/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("task", result.getId()))
                .body(result);
        } catch (TaskException e) {
            log.warn("Could not create task: {}", taskDTO, e);
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createErrorAlert("Scheduler could not create this task")).body(null);
        }
    }

    /**
     * PUT  /tasks : Updates an existing task.
     *
     * @param taskDTO the taskDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated taskDTO,
     * or with status 400 (Bad Request) if the taskDTO is not valid,
     * or with status 500 (Internal Server Error) if the taskDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/tasks")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<TaskDTO> updateTask(@Valid @RequestBody TaskDTO taskDTO) throws URISyntaxException {
        log.debug("REST request to update Task : {}", taskDTO);
        if (taskDTO.getId() == null) {
            return createTask(taskDTO);
        }
        try {
            TaskDTO result = taskService.save(taskDTO);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("task", taskDTO.getId()))
                .body(result);
        } catch (TaskException e) {
            log.warn("Could not update task: {}", taskDTO, e);
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createErrorAlert("Scheduler could not update this task")).body(null);
        }
    }

    /**
     * GET  /tasks : get all the tasks.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of tasks in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/tasks")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<TaskDTO>> getAllTasks(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Tasks");
        Page<TaskDTO> page = taskService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/tasks");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /tasks/:id : get the "id" task.
     *
     * @param id the id of the taskDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the taskDTO, or with status 404 (Not Found)
     */
    @GetMapping("/tasks/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<TaskDTO> getTask(@PathVariable String id) {
        log.debug("REST request to get Task : {}", id);
        TaskDTO taskDTO = taskService.findOne(id);
        return Optional.ofNullable(taskDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /tasks/:id : delete the "id" task.
     *
     * @param id the id of the taskDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/tasks/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        log.debug("REST request to delete Task : {}", id);
        try {
            taskService.delete(id);
            return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("task", id)).build();
        } catch (TaskException e) {
            log.warn("Could not delete task with id {}", id, e);
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createErrorAlert("Scheduler could not delete this task")).body(null);
        }
    }

}
