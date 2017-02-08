package top.quantic.sentry.web.rest;

import top.quantic.sentry.SentryApp;

import top.quantic.sentry.domain.Task;
import top.quantic.sentry.repository.TaskRepository;
import top.quantic.sentry.service.TaskService;
import top.quantic.sentry.service.dto.TaskDTO;
import top.quantic.sentry.service.mapper.TaskMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the TaskResource REST controller.
 *
 * @see TaskResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SentryApp.class)
public class TaskResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_GROUP = "AAAAAAAAAA";
    private static final String UPDATED_GROUP = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_JOB_CLASS = "AAAAAAAAAA";
    private static final String UPDATED_JOB_CLASS = "BBBBBBBBBB";

    private static final String DEFAULT_DATA_MAP = "AAAAAAAAAA";
    private static final String UPDATED_DATA_MAP = "BBBBBBBBBB";

    private static final String DEFAULT_TRIGGERS = "AAAAAAAAAA";
    private static final String UPDATED_TRIGGERS = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    @Inject
    private TaskRepository taskRepository;

    @Inject
    private TaskMapper taskMapper;

    @Inject
    private TaskService taskService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restTaskMockMvc;

    private Task task;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TaskResource taskResource = new TaskResource();
        ReflectionTestUtils.setField(taskResource, "taskService", taskService);
        this.restTaskMockMvc = MockMvcBuilders.standaloneSetup(taskResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Task createEntity() {
        Task task = new Task()
                .name(DEFAULT_NAME)
                .group(DEFAULT_GROUP)
                .description(DEFAULT_DESCRIPTION)
                .jobClass(DEFAULT_JOB_CLASS)
//                .dataMap(DEFAULT_DATA_MAP)
//                .triggers(DEFAULT_TRIGGERS)
                .enabled(DEFAULT_ENABLED);
        return task;
    }

    @Before
    public void initTest() {
        taskRepository.deleteAll();
        task = createEntity();
    }

    @Test
    public void createTask() throws Exception {
        int databaseSizeBeforeCreate = taskRepository.findAll().size();

        // Create the Task
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isCreated());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeCreate + 1);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testTask.getGroup()).isEqualTo(DEFAULT_GROUP);
        assertThat(testTask.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testTask.getJobClass()).isEqualTo(DEFAULT_JOB_CLASS);
        assertThat(testTask.getDataMap()).isEqualTo(DEFAULT_DATA_MAP);
        assertThat(testTask.getTriggers()).isEqualTo(DEFAULT_TRIGGERS);
        assertThat(testTask.isEnabled()).isEqualTo(DEFAULT_ENABLED);
    }

    @Test
    public void createTaskWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = taskRepository.findAll().size();

        // Create the Task with an existing ID
        Task existingTask = new Task();
        existingTask.setId("existing_id");
        TaskDTO existingTaskDTO = taskMapper.taskToTaskDTO(existingTask);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingTaskDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setName(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkGroupIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setGroup(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkJobClassIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setJobClass(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void checkEnabledIsRequired() throws Exception {
        int databaseSizeBeforeTest = taskRepository.findAll().size();
        // set the field null
        task.setEnabled(null);

        // Create the Task, which fails.
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        restTaskMockMvc.perform(post("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isBadRequest());

        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    public void getAllTasks() throws Exception {
        // Initialize the database
        taskRepository.save(task);

        // Get all the taskList
        restTaskMockMvc.perform(get("/api/tasks?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(task.getId())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].group").value(hasItem(DEFAULT_GROUP)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].jobClass").value(hasItem(DEFAULT_JOB_CLASS)))
            .andExpect(jsonPath("$.[*].dataMap").value(hasItem(DEFAULT_DATA_MAP)))
            .andExpect(jsonPath("$.[*].triggers").value(hasItem(DEFAULT_TRIGGERS)))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED)));
    }

    @Test
    public void getTask() throws Exception {
        // Initialize the database
        taskRepository.save(task);

        // Get the task
        restTaskMockMvc.perform(get("/api/tasks/{id}", task.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(task.getId()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.group").value(DEFAULT_GROUP))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.jobClass").value(DEFAULT_JOB_CLASS))
            .andExpect(jsonPath("$.dataMap").value(DEFAULT_DATA_MAP))
            .andExpect(jsonPath("$.triggers").value(DEFAULT_TRIGGERS))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED));
    }

    @Test
    public void getNonExistingTask() throws Exception {
        // Get the task
        restTaskMockMvc.perform(get("/api/tasks/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateTask() throws Exception {
        // Initialize the database
        taskRepository.save(task);
        int databaseSizeBeforeUpdate = taskRepository.findAll().size();

        // Update the task
        Task updatedTask = taskRepository.findOne(task.getId());
        updatedTask
                .name(UPDATED_NAME)
                .group(UPDATED_GROUP)
                .description(UPDATED_DESCRIPTION)
                .jobClass(UPDATED_JOB_CLASS)
//                .dataMap(UPDATED_DATA_MAP)
//                .triggers(UPDATED_TRIGGERS)
                .enabled(UPDATED_ENABLED);
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(updatedTask);

        restTaskMockMvc.perform(put("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isOk());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeUpdate);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testTask.getGroup()).isEqualTo(UPDATED_GROUP);
        assertThat(testTask.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testTask.getJobClass()).isEqualTo(UPDATED_JOB_CLASS);
        assertThat(testTask.getDataMap()).isEqualTo(UPDATED_DATA_MAP);
        assertThat(testTask.getTriggers()).isEqualTo(UPDATED_TRIGGERS);
        assertThat(testTask.isEnabled()).isEqualTo(UPDATED_ENABLED);
    }

    @Test
    public void updateNonExistingTask() throws Exception {
        int databaseSizeBeforeUpdate = taskRepository.findAll().size();

        // Create the Task
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restTaskMockMvc.perform(put("/api/tasks")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(taskDTO)))
            .andExpect(status().isCreated());

        // Validate the Task in the database
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    public void deleteTask() throws Exception {
        // Initialize the database
        taskRepository.save(task);
        int databaseSizeBeforeDelete = taskRepository.findAll().size();

        // Get the task
        restTaskMockMvc.perform(delete("/api/tasks/{id}", task.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
