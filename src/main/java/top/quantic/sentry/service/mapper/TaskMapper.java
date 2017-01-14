package top.quantic.sentry.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Task;
import top.quantic.sentry.service.dto.TaskDTO;
import top.quantic.sentry.service.mapper.util.ObjectMappingUtil;
import top.quantic.sentry.service.mapper.util.StringMappingUtil;

import java.util.List;

/**
 * Mapper for the entity Task and its DTO TaskDTO.
 */
@Mapper(componentModel = "spring",
    uses = {StringMappingUtil.class, ObjectMappingUtil.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    TaskDTO taskToTaskDTO(Task task);

    List<TaskDTO> tasksToTaskDTOs(List<Task> tasks);

    Task taskDTOToTask(TaskDTO taskDTO);

    List<Task> taskDTOsToTasks(List<TaskDTO> taskDTOs);
}
