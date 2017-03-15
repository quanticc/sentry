package top.quantic.sentry.service;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import top.quantic.sentry.config.Constants;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.event.ContentSupplier;
import top.quantic.sentry.event.SentryEvent;
import top.quantic.sentry.event.SentryReadyEvent;
import top.quantic.sentry.repository.FlowRepository;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.mapper.FlowMapper;
import top.quantic.sentry.service.util.TaskException;
import top.quantic.sentry.web.rest.vm.DatadogDowntime;
import top.quantic.sentry.web.rest.vm.DatadogEvent;
import top.quantic.sentry.web.rest.vm.DatadogPayload;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
    public SentryReadyEvent onApplicationReady(ApplicationReadyEvent event) {
        return new SentryReadyEvent(event);
    }

    @EventListener
    public void onSentryEvent(SentryEvent event) {
        String className = event.getClass().getSimpleName();
        String content = event.asContent(new LinkedHashMap<>());
        log.debug("[{}] {}", className, content != null ? content : "");
        flowRepository.findByEnabledIsTrueAndInputAndMessage(SENTRY_EVENT, className)
            .forEach(flow -> executeEventFlow(flow, event));
    }

    public void executeDatadogFlowsByKey(String key, DatadogPayload event) {
        flowRepository.findByEnabledIsTrueAndInput(INBOUND_WEBHOOK).stream()
            .filter(flow -> key.equals(flow.getVariables().get("key")))
            .forEach(flow -> executeEventFlow(flow, event));
    }

    private void executeEventFlow(Flow flow, ContentSupplier supplier) {
        log.info("Executing {} flow: {}", supplier.getClass().getSimpleName(), flow);
        String translatorType = flow.getTranslator();
        switch (translatorType) {
            case "DiscordWebhook":
                publish(flow, supplier.getContentId(), asDiscordWebhook(flow, supplier));
                break;
            case "DiscordMessage":
                publish(flow, supplier.getContentId(), supplier.asContent(flow.getVariables()));
                break;
            case "DiscordEmbed":
                publish(flow, supplier.getContentId(), asDiscordEmbed(flow, supplier));
                break;
            case "DiscordMessageEmbed":
                publish(flow, supplier.getContentId(), supplier.asContent(flow.getVariables()), asDiscordEmbed(flow, supplier));
                break;
            case "DatadogEvent":
                publish(flow, supplier.getContentId(), asDatadogEvent(flow, supplier));
                break;
            case "DatadogDowntime":
                publish(flow, supplier.getContentId(), asDatadogDowntime(flow, supplier));
                break;
            default:
                log.warn("Unknown translator type for this flow: {}", translatorType);
                break;
        }
    }

    private EmbedObject asDiscordEmbed(Flow flow, ContentSupplier supplier) {
        return supplier.asEmbed(flow.getVariables());
    }

    private DiscordWebhook asDiscordWebhook(Flow flow, ContentSupplier supplier) {
        Map<String, Object> variables = flow.getVariables();
        String content = supplier.asContent(variables);

        DiscordWebhook webhook = new DiscordWebhook();
        Object username = variables.get("username");
        Object avatarUrl = variables.get("avatarUrl");
        if (username != null) {
            webhook.setUsername((String) username);
        }
        if (avatarUrl != null) {
            webhook.setAvatarUrl((String) avatarUrl);
        }
        webhook.setContent(content);
        // TODO: webhook with embeds
        return webhook;
    }

    @SuppressWarnings("unchecked")
    private DatadogEvent asDatadogEvent(Flow flow, ContentSupplier supplier) {
        Map<String, Object> variables = flow.getVariables();
        Map<String, Object> map = supplier.asMap(variables);

        String title = (String) getFromMap("title", variables, map);
        String text = (String) getFromMap("text", variables, map);
        String alertType = (String) getFromMap("alert_type", variables, map);
        String aggregationKey = (String) getFromMap("aggregation_key", variables, map);
        String priority = (String) getFromMap("priority", variables, map);
        String host = (String) getFromMap("host", variables, map);
        String sourceTypeName = (String) getFromMap("source_type_name", variables, map);
        Long dateHappened = (Long) getFromMap("date_happened", variables, map);

        // decorate with markdown markers if needed
        Object markdown = getFromMap("markdown", variables, map);
        if (markdown != null && (boolean) markdown) {
            text = "%%%\n" + text + "\n%%%";
        }

        // combine tags
        List<String> tags = (List<String>) variables.getOrDefault("tags", new ArrayList<String>());
        tags.addAll((List<String>) map.getOrDefault("tags", new ArrayList<String>()));
        return new DatadogEvent(title, text, dateHappened, priority, host, tags, alertType, aggregationKey, sourceTypeName);
    }

    private DatadogDowntime asDatadogDowntime(Flow flow, ContentSupplier supplier) {
        Map<String, Object> variables = flow.getVariables();
        Map<String, Object> map = supplier.asMap(variables);

        String scope = (String) getFromMap("scope", variables, map);
        String message = (String) getFromMap("message", variables, map);
        Long end = (Long) getFromMap("end", variables, map);

        return new DatadogDowntime(scope, null, end, message, null);
    }

    private Object getFromMap(String key, Map<String, Object> first, Map<String, Object> second) {
        return first.getOrDefault(key, second.get(key));
    }

    private void publish(Flow flow, String id, String content) {
        if (isBlank(content)) {
            log.info("[{}] Not publishing blank content to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, content);
        }
    }

    private void publish(Flow flow, String id, DiscordWebhook webhook) {
        if (isBlank(webhook.getContent())) {
            log.info("[{}] Not publishing blank webhook to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, webhook);
        }
    }

    private void publish(Flow flow, String id, DatadogEvent event) {
        if (isBlank(event.getText()) || isBlank(event.getTitle())) {
            log.info("[{}] Missing title and text - Not publishing event to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, event);
        }
    }

    private void publish(Flow flow, String id, DatadogDowntime event) {
        if (isBlank(event.getScope())) {
            log.info("[{}] Missing scope - Not publishing event to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, event);
        }
    }

    private void publish(Flow flow, String id, EmbedObject embed) {
        if (embed == null) {
            log.info("[{}] Not publishing null embed to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, embed);
        }
    }

    private void publish(Flow flow, String id, String content, EmbedObject embed) {
        if (embed == null && isBlank(content)) {
            log.info("[{}] Not publishing null and/or blank content/embed to {}", flow.getName(), id);
        } else {
            subscriberService.publish(flow.getOutput(), id, content, embed);
        }
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
