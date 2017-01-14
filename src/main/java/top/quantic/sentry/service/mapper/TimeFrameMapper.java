package top.quantic.sentry.service.mapper;

import org.mapstruct.*;
import top.quantic.sentry.domain.TimeFrame;
import top.quantic.sentry.service.dto.TimeFrameDTO;
import top.quantic.sentry.service.mapper.util.TimeMappingUtil;

import java.util.List;

/**
 * Mapper for the entity TimeFrame and its DTO TimeFrameDTO.
 */
@Mapper(componentModel = "spring",
    uses = TimeMappingUtil.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimeFrameMapper {

    @Mapping(source = "recurrence", target = "recurrenceValue")
    TimeFrameDTO timeFrameToTimeFrameDTO(TimeFrame timeFrame);

    List<TimeFrameDTO> timeFramesToTimeFrameDTOs(List<TimeFrame> timeFrames);

    @InheritInverseConfiguration
    TimeFrame timeFrameDTOToTimeFrame(TimeFrameDTO timeFrameDTO);

    List<TimeFrame> timeFrameDTOsToTimeFrames(List<TimeFrameDTO> timeFrameDTOs);
}
