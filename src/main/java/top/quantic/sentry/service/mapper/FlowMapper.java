package top.quantic.sentry.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.service.dto.FlowDTO;

import java.util.List;

/**
 * Mapper for the entity Flow and its DTO FlowDTO.
 */
@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlowMapper {

    FlowDTO flowToFlowDTO(Flow flow);

    List<FlowDTO> flowsToFlowDTOs(List<Flow> flows);

    Flow flowDTOToFlow(FlowDTO flowDTO);

    List<Flow> flowDTOsToFlows(List<FlowDTO> flowDTOs);
}
