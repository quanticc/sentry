package top.quantic.sentry.service.mapper;

import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.TimeFrame;
import top.quantic.sentry.service.dto.TimeFrameDTO;

import java.util.List;

/**
 * Mapper for the entity TimeFrame and its DTO TimeFrameDTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(TimeFrameMapperDecorator.class)
public interface TimeFrameMapper {

    TimeFrameDTO timeFrameToTimeFrameDTO(TimeFrame timeFrame);

    List<TimeFrameDTO> timeFramesToTimeFrameDTOs(List<TimeFrame> timeFrames);

    TimeFrame timeFrameDTOToTimeFrame(TimeFrameDTO timeFrameDTO);

    List<TimeFrame> timeFrameDTOsToTimeFrames(List<TimeFrameDTO> timeFrameDTOs);
}
