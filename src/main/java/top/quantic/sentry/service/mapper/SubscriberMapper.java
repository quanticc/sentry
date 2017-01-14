package top.quantic.sentry.service.mapper;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.service.dto.SubscriberDTO;
import top.quantic.sentry.service.mapper.util.ObjectMappingUtil;

import java.util.List;

/**
 * Mapper for the entity Subscriber and its DTO SubscriberDTO.
 */
@Mapper(componentModel = "spring",
    uses = ObjectMappingUtil.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriberMapper {

    @Mapping(source = "variables", target = "typeParameters")
    SubscriberDTO subscriberToSubscriberDTO(Subscriber subscriber);

    List<SubscriberDTO> subscribersToSubscriberDTOs(List<Subscriber> subscribers);

    @InheritInverseConfiguration
    Subscriber subscriberDTOToSubscriber(SubscriberDTO subscriberDTO);

    List<Subscriber> subscriberDTOsToSubscribers(List<SubscriberDTO> subscriberDTOs);
}
