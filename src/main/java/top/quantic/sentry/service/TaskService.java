package top.quantic.sentry.service;

import org.quartz.*;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.domain.Task;
import top.quantic.sentry.repository.TaskRepository;
import top.quantic.sentry.service.dto.TaskDTO;
import top.quantic.sentry.service.mapper.TaskMapper;
import top.quantic.sentry.service.util.LoggingTriggerListener;
import top.quantic.sentry.service.util.TaskException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static top.quantic.sentry.service.util.DateUtil.*;

/**
 * Service Implementation for managing Task.
 */
@Service
public class TaskService implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final Scheduler scheduler;
    private final Set<Class<? extends Job>> jobTypeSet;

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper, Scheduler scheduler) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.scheduler = scheduler;
        this.jobTypeSet = new Reflections(Constants.JOBS_PACKAGE).getSubTypesOf(Job.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Job types available: {}", jobTypeSet.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", ")));

        // synchronize Tasks with current Jobs
        taskRepository.findAll().stream()
            .filter(task -> {
                try {
                    return !scheduler.checkExists(getJobKey(task));
                } catch (SchedulerException e) {
                    log.warn("Could not check job from task: {}", task, e);
                }
                return false;
            })
            .forEach(task -> {
                try {
                    tryRegister(task);
                } catch (TaskException e) {
                    log.warn("Could not register task: {}", task, e);
                }
            });

        scheduler.getListenerManager().addTriggerListener(new LoggingTriggerListener());
    }

    private void tryRegister(Task task) throws TaskException {
        try {
            register(task);
        } catch (SchedulerException e) {
            try {
                unregister(task);
            } catch (SchedulerException ex) {
                log.warn("Could not cleanup task registration", ex);
            }
            throw new TaskException(e);
        }
    }

    private void tryUnregister(Task task) throws TaskException {
        try {
            unregister(task);
        } catch (SchedulerException e) {
            throw new TaskException(e);
        }
    }

    private void register(Task task) throws SchedulerException, TaskException {
        log.info("Register task with the scheduler : {}", task);
        JobKey jobKey = getJobKey(task);
        JobDetail jobDetail = newJob(getJobClass(task.getJobClass()))
            .withIdentity(jobKey)
            .withDescription(task.getDescription())
            .usingJobData(getJobDataMap(task.getDataMap()))
            .storeDurably(true)
            .build();
        // MongoDB store for Quartz does not support updating Triggers
        // so we remove all and re-add them each time
        scheduler.unscheduleJobs(scheduler.getTriggersOfJob(jobKey).stream()
            .map(Trigger::getKey)
            .collect(Collectors.toList()));
        scheduler.addJob(jobDetail, true);
        Set<CronTrigger> triggerSet = task.getTriggers().stream()
            .map(cron -> newTrigger()
                .withIdentity(cron, task.getName())
                .forJob(jobKey)
                .withSchedule(cronSchedule(cron)
                    .withMisfireHandlingInstructionDoNothing())
                .build())
            .collect(Collectors.toSet());
        for (CronTrigger trigger : triggerSet) {
            log.debug("[{}] Scheduling trigger: {}", jobKey,
                humanizeCronPattern(trigger.getCronExpression()));
            scheduler.scheduleJob(trigger);
        }
        if (task.getId() != null) {
            if (task.isEnabled()) {
                log.debug("[{}] Job enabled - Next attempt at {} ({})", jobKey,
                    instantToSystem(nextValidTimeFromCron(task.getTriggers())),
                    relativeNextTriggerFromCron(task.getTriggers()));
                scheduler.resumeJob(jobKey);
            } else {
                log.debug("[{}] Job paused", jobKey);
                scheduler.pauseJob(jobKey);
            }
        } else {
            log.debug("[{}] Next trigger attempt at {} ({})", jobKey,
                instantToSystem(nextValidTimeFromCron(task.getTriggers())),
                relativeNextTriggerFromCron(task.getTriggers()));
        }
    }

    private JobKey getJobKey(Task task) {
        return jobKey(task.getName(), task.getGroup());
    }

    private void unregister(Task task) throws SchedulerException {
        log.info("Unregister task with the scheduler : {}", task);
        scheduler.deleteJob(getJobKey(task));
    }

    private JobDataMap getJobDataMap(Map<String, Object> map) {
        if (map == null) {
            return new JobDataMap();
        }
        return new JobDataMap(map);
    }

    private Class<? extends Job> getJobClass(String name) throws TaskException {
        return jobTypeSet.stream()
            .filter(clz -> clz.getSimpleName().equals(name))
            .findAny()
            .orElseThrow(() -> new TaskException("Invalid job class name"));
    }

    /**
     * Save a task.
     *
     * @param taskDTO the entity to save
     * @return the persisted entity
     */
    public TaskDTO save(TaskDTO taskDTO) throws TaskException {
        log.debug("Request to save Task : {}", taskDTO);
        Task task = taskMapper.taskDTOToTask(taskDTO);
        // is this a rename? then unregister previous one
        if (task.getId() != null) {
            Task oldTask = taskRepository.findOne(task.getId());
            if (oldTask != null && !getJobKey(oldTask).equals(getJobKey(task))) {
                tryUnregister(task);
            }
        }
        tryRegister(task);
        task = taskRepository.save(task);
        TaskDTO result = taskMapper.taskToTaskDTO(task);
        return result;
    }

    /**
     * Get all the tasks.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<TaskDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Tasks");
        Page<Task> result = taskRepository.findAll(pageable);
        return result.map(task -> taskMapper.taskToTaskDTO(task));
    }

    /**
     * Get one task by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public TaskDTO findOne(String id) {
        log.debug("Request to get Task : {}", id);
        Task task = taskRepository.findOne(id);
        TaskDTO taskDTO = taskMapper.taskToTaskDTO(task);
        return taskDTO;
    }

    /**
     * Delete the  task by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) throws TaskException {
        log.debug("Request to delete Task : {}", id);
        Task task = taskRepository.findOne(id);
        if (task != null) {
            tryUnregister(task);
        }
        taskRepository.delete(id);
    }
}
