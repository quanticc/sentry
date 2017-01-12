package top.quantic.sentry.service.mapper;

import top.quantic.sentry.domain.*;
import top.quantic.sentry.service.dto.FlowDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Flow and its DTO FlowDTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(FlowMapperDecorator.class)
public interface FlowMapper {

    FlowDTO flowToFlowDTO(Flow flow);

    List<FlowDTO> flowsToFlowDTOs(List<Flow> flows);

    Flow flowDTOToFlow(FlowDTO flowDTO);

    List<Flow> flowDTOsToFlows(List<FlowDTO> flowDTOs);
}
