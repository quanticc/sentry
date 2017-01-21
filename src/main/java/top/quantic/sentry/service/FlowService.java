package top.quantic.sentry.service;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.event.ContentSupplier;
import top.quantic.sentry.event.SentryEvent;
import top.quantic.sentry.repository.FlowRepository;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.mapper.FlowMapper;
import top.quantic.sentry.service.util.TaskException;
import top.quantic.sentry.web.rest.vm.DatadogEvent;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Flow.
 */
@Service
public class FlowService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);
    private static final String SENTRY_EVENT = "sentryEvent";
    private static final String INBOUND_WEBHOOK = "inboundWebhook";

    private final FlowRepository flowRepository;
    private final FlowMapper flowMapper;
    private final SubscriberService subscriberService;
    private final Set<Class<? extends SentryEvent>> eventTypeSet;

    @Autowired
    public FlowService(FlowRepository flowRepository, FlowMapper flowMapper, SubscriberService subscriberService) {
        this.flowRepository = flowRepository;
        this.flowMapper = flowMapper;
        this.subscriberService = subscriberService;
        this.eventTypeSet = new Reflections(Constants.EVENTS_PACKAGE).getSubTypesOf(SentryEvent.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Event types available: {}", eventTypeSet.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", ")));
    }

    @EventListener
    public void onSentryEvent(SentryEvent event) {
        String className = event.getClass().getSimpleName();
        log.debug("[{}] {}", className, event.asContent());
        flowRepository.findByEnabledIsTrueAndInputAndMessage(SENTRY_EVENT, className)
            .forEach(flow -> executeEventFlow(flow, event));
    }

    public void executeDatadogFlowsByKey(String key, DatadogEvent event) {
        flowRepository.findByEnabledIsTrueAndInput(INBOUND_WEBHOOK).stream()
            .filter(flow -> key.equals(flow.getVariables().get("key")))
            .forEach(flow -> executeEventFlow(flow, event));
    }

    private void executeEventFlow(Flow flow, ContentSupplier supplier) {
        log.info("Executing {} flow: {}", supplier.getClass().getSimpleName(), flow);
        String translatorType = flow.getTranslator();
        Map<String, Object> variables = flow.getVariables();
        if (translatorType.startsWith("DiscordWebhook")) {
            DiscordWebhook webhook = new DiscordWebhook();
            Object username = variables.get("username");
            Object avatarUrl = variables.get("avatarUrl");
            if (username != null) {
                webhook.setUsername((String) username);
            }
            if (avatarUrl != null) {
                webhook.setAvatarUrl((String) avatarUrl);
            }
            webhook.setContent(supplier.asContent());
            publish(flow, supplier.getContentId(), webhook);
        } else if (translatorType.startsWith("DiscordMessage")) {
            publish(flow, supplier.getContentId(), supplier.asContent());
        } else {
            log.warn("Unknown translator type for this flow: {}", translatorType);
        }
    }

    private void publish(Flow flow, String id, String content) {
        subscriberService.publish(flow.getOutput(), id, content);
    }

    private void publish(Flow flow, String id, DiscordWebhook webhook) {
        subscriberService.publish(flow.getOutput(), id, webhook);
    }

    /**
     * Save a flow.
     *
     * @param flowDTO the entity to save
     * @return the persisted entity
     */
    public FlowDTO save(FlowDTO flowDTO) throws TaskException {
        log.debug("Request to save Flow : {}", flowDTO);
        Flow flow = flowMapper.flowDTOToFlow(flowDTO);
        if (flowDTO.getInput().equals(SENTRY_EVENT) && eventTypeSet.stream()
            .map(Class::getSimpleName)
            .noneMatch(name -> name.equals(flowDTO.getMessage()))) {
            throw new TaskException("Invalid event type: " + flow.getMessage());
        }
        flow = flowRepository.save(flow);
        FlowDTO result = flowMapper.flowToFlowDTO(flow);
        return result;
    }

    /**
     * Get all the flows.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<FlowDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Flows");
        Page<Flow> result = flowRepository.findAll(pageable);
        return result.map(flow -> flowMapper.flowToFlowDTO(flow));
    }

    /**
     * Get one flow by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public FlowDTO findOne(String id) {
        log.debug("Request to get Flow : {}", id);
        Flow flow = flowRepository.findOne(id);
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);
        return flowDTO;
    }

    /**
     * Delete the  flow by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Flow : {}", id);
        flowRepository.delete(id);
    }
}
