package top.quantic.sentry.service.mapper;

import top.quantic.sentry.domain.*;
import top.quantic.sentry.service.dto.SubscriberDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Subscriber and its DTO SubscriberDTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriberMapperDecorator.class)
public interface SubscriberMapper {

    SubscriberDTO subscriberToSubscriberDTO(Subscriber subscriber);

    List<SubscriberDTO> subscribersToSubscriberDTOs(List<Subscriber> subscribers);

    Subscriber subscriberDTOToSubscriber(SubscriberDTO subscriberDTO);

    List<Subscriber> subscriberDTOsToSubscribers(List<SubscriberDTO> subscriberDTOs);
}
