package top.quantic.sentry.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.mapper.util.ObjectMappingUtil;

import java.util.List;

/**
 * Mapper for the entity Flow and its DTO FlowDTO.
 */
@Mapper(componentModel = "spring",
    uses = ObjectMappingUtil.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlowMapper {

    @Mapping(source = "variables", target = "inputParameters")
    FlowDTO flowToFlowDTO(Flow flow);

    List<FlowDTO> flowsToFlowDTOs(List<Flow> flows);

    @InheritInverseConfiguration
    Flow flowDTOToFlow(FlowDTO flowDTO);

    List<Flow> flowDTOsToFlows(List<FlowDTO> flowDTOs);
}
