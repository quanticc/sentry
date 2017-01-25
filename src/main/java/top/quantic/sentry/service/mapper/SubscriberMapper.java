package top.quantic.sentry.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.service.dto.SubscriberDTO;

import java.util.List;

/**
 * Mapper for the entity Subscriber and its DTO SubscriberDTO.
 */
@Mapper(componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubscriberMapper {

    SubscriberDTO subscriberToSubscriberDTO(Subscriber subscriber);

    List<SubscriberDTO> subscribersToSubscriberDTOs(List<Subscriber> subscribers);

    Subscriber subscriberDTOToSubscriber(SubscriberDTO subscriberDTO);

    List<Subscriber> subscriberDTOsToSubscribers(List<SubscriberDTO> subscriberDTOs);
}
