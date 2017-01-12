package top.quantic.sentry.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.service.dto.SubscriberDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class SubscriberMapperDecorator implements SubscriberMapper {

    private static final Logger log = LoggerFactory.getLogger(SubscriberMapperDecorator.class);

    @Autowired
    @Qualifier("delegate")
    private SubscriberMapper delegate;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SubscriberDTO subscriberToSubscriberDTO(Subscriber subscriber) {
        SubscriberDTO dto = delegate.subscriberToSubscriberDTO(subscriber);
        Map<String, Object> variables = subscriber.getVariables();
        if (variables != null) {
            try {
                dto.setTypeParameters(objectMapper.writeValueAsString(variables));
            } catch (JsonProcessingException e) {
                log.warn("Could not map variables field", e);
            }
        }
        return dto;
    }

    @Override
    public Subscriber subscriberDTOToSubscriber(SubscriberDTO subscriberDTO) {
        Subscriber subscriber = delegate.subscriberDTOToSubscriber(subscriberDTO);
        String type = subscriberDTO.getTypeParameters();
        if (isBlank(type)) {
            subscriber.setVariables(new HashMap<>());
        } else {
            try {
                subscriber.setVariables(objectMapper.readValue(type, new TypeReference<Map<String, Object>>() { }));
            } catch (IOException e) {
                log.warn("Could not map subscriber parameters", e);
                subscriber.setVariables(new HashMap<>());
            }
        }
        return subscriber;
    }
}
