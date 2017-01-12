package top.quantic.sentry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.Flow;
import top.quantic.sentry.repository.FlowRepository;
import top.quantic.sentry.service.dto.FlowDTO;
import top.quantic.sentry.service.mapper.FlowMapper;
import top.quantic.sentry.web.rest.vm.DatadogEvent;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Service Implementation for managing Flow.
 */
@Service
public class FlowService {

    private final Logger log = LoggerFactory.getLogger(FlowService.class);

    private final FlowRepository flowRepository;
    private final FlowMapper flowMapper;
    private final SubscriberService subscriberService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FlowService(FlowRepository flowRepository, FlowMapper flowMapper, SubscriberService subscriberService) {
        this.flowRepository = flowRepository;
        this.flowMapper = flowMapper;
        this.subscriberService = subscriberService;
    }

    public void executeWebhookFlowByKey(String key, String body) {
        flowRepository.findByEnabledIsTrueAndInput("inboundWebhook").stream()
            .filter(flow -> key.equals(flow.getVariables().get("key")))
            .forEach(flow -> {
                try {
                    executeWebhookFlow(flow, body);
                } catch (Exception e) {
                    log.warn("Could not execute flow", e);
                }
            });
    }

    private void executeWebhookFlow(Flow flow, String body) throws IOException {
        log.info("Executing flow {}", isBlank(flow.getName()) ? flow.getId() : flow.getName());
        String messageType = flow.getMessage();
        if (messageType.equals("DatadogEvent")) {
            executeDatadogFlow(flow, toDatadogEvent(body));
        } else {
            log.warn("Unknown message type for this flow: {}", messageType);
        }
    }

    private void executeDatadogFlow(Flow flow, DatadogEvent event) {
        log.debug("Transforming {} for {}", event, flow);
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
            if (translatorType.contains("Simple")) {
                webhook.setContent("**" + event.getTitle() + "**\n" + extract(event.getBody()));
            } else {
                webhook.setContent("**" + event.getTitle() + "**\n" + event.getBody());
            }
            sendWebhook(flow, webhook);
        } else if (translatorType.startsWith("DiscordMessage")) {
            String content;
            if (translatorType.contains("Simple")) {
                content = "**" + event.getTitle() + "**\n" + extract(event.getBody());
            } else {
                content = "**" + event.getTitle() + "**\n" + event.getBody();
            }
            sendMessage(flow, content);
        } else {
            log.warn("Unknown translator type for this flow: {}", translatorType);
        }
    }

    private void sendMessage(Flow flow, String content) {
        subscriberService.send(flow.getOutput(), content);
    }

    private void sendWebhook(Flow flow, DiscordWebhook webhook) {
        subscriberService.send(flow.getOutput(), webhook);
    }

    private String extract(String body) {
        String[] splits = body.split("===", 3);
        if (splits.length >= 2) {
            return splits[1];
        } else if (splits.length == 1) {
            return splits[0];
        } else {
            return body;
        }
    }

    private DatadogEvent toDatadogEvent(String body) throws IOException {
        return objectMapper.readValue(body, DatadogEvent.class);
    }

    /**
     * Save a flow.
     *
     * @param flowDTO the entity to save
     * @return the persisted entity
     */
    public FlowDTO save(FlowDTO flowDTO) {
        log.debug("Request to save Flow : {}", flowDTO);
        Flow flow = flowMapper.flowDTOToFlow(flowDTO);
        flow = flowRepository.save(flow);
        FlowDTO result = flowMapper.flowToFlowDTO(flow);
        return result;
    }

    /**
     *  Get all the flows.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    public Page<FlowDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Flows");
        Page<Flow> result = flowRepository.findAll(pageable);
        return result.map(flow -> flowMapper.flowToFlowDTO(flow));
    }

    /**
     *  Get one flow by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    public FlowDTO findOne(String id) {
        log.debug("Request to get Flow : {}", id);
        Flow flow = flowRepository.findOne(id);
        FlowDTO flowDTO = flowMapper.flowToFlowDTO(flow);
        return flowDTO;
    }

    /**
     *  Delete the  flow by id.
     *
     *  @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Flow : {}", id);
        flowRepository.delete(id);
    }
}
