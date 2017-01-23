package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import top.quantic.sentry.discord.core.ClientRegistry;
import top.quantic.sentry.domain.AbstractAuditingEntity;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.repository.SubscriberRepository;
import top.quantic.sentry.service.dto.SubscriberDTO;
import top.quantic.sentry.service.mapper.SubscriberMapper;
import top.quantic.sentry.web.rest.vm.DatadogEvent;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static top.quantic.sentry.discord.util.DiscordLimiter.acquireWebhook;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;

/**
 * Service Implementation for managing Subscriber.
 */
@Service
public class SubscriberService {

    private static final Logger log = LoggerFactory.getLogger(SubscriberService.class);
    private static final String LAST_PUBLISHED = "lastPublished";

    private final SubscriberRepository subscriberRepository;
    private final SubscriberMapper subscriberMapper;
    private final TimeFrameService timeFrameService;
    private final ClientRegistry clientRegistry;
    private final SettingService settingService;
    private final RestTemplate restTemplate;

    @Autowired
    public SubscriberService(SubscriberRepository subscriberRepository, SubscriberMapper subscriberMapper,
                             TimeFrameService timeFrameService, ClientRegistry clientRegistry,
                             SettingService settingService, RestTemplate restTemplate) {
        this.subscriberRepository = subscriberRepository;
        this.subscriberMapper = subscriberMapper;
        this.timeFrameService = timeFrameService;
        this.clientRegistry = clientRegistry;
        this.settingService = settingService;
        this.restTemplate = restTemplate;
    }

    public void publish(String outputChannel, String id, DiscordWebhook message) {
        log.debug("Publishing a webhook message ({}) to output channel : {}", id, outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordWebhook").stream()
            .filter(sub -> timeFrameService.included(sub.getId()))
            .filter(sub -> checkDuplicate(sub, id))
            .forEach(sub -> {
                String url = (String) sub.getVariables().get("url");
                if (url == null) {
                    log.warn("Subscriber did not define a target URL: {}", sub);
                } else {
                    String username = (String) sub.getVariables().get("username");
                    String avatarUrl = (String) sub.getVariables().get("avatarUrl");
                    if (username != null) {
                        message.setUsername(username);
                    }
                    if (avatarUrl != null) {
                        message.setAvatarUrl(avatarUrl);
                    }
                    try {
                        ResponseEntity<Map<String, ?>> responseEntity = execute(message, url);
                        log.debug("[{}] Response: {}", outputChannel, responseEntity);
                    } catch (RestClientException e) {
                        log.warn("Could not execute webhook", e);
                    }
                }
            });
    }

    private ResponseEntity<Map<String, ?>> execute(DiscordWebhook webhook, String webhookUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("User-Agent", "curl"); // cloudflare!
        acquireWebhook();
        return restTemplate.exchange(webhookUrl,
            HttpMethod.POST,
            new HttpEntity<>(webhook, headers),
            new ParameterizedTypeReference<Map<String, ?>>() {
            });
    }

    public void publish(String outputChannel, String id, String content, EmbedObject embedObject) {
        log.debug("Publishing a discord message and embed ({}) to output channel : {}", id, outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordMessageEmbed").stream()
            // only include if the subscriber has allowed this time
            .filter(sub -> timeFrameService.included(sub.getId()))
            // abort if one of the target clients is not ready
            .filter(sub -> isClientReady((String) sub.getVariables().get("client")))
            // check for duplicated messages to avoid spam
            .filter(sub -> checkDuplicate(sub, id))
            .forEach(sub -> {
                String channelId = (String) sub.getVariables().get("channel");
                String clientId = (String) sub.getVariables().get("client");
                if (channelId == null) {
                    log.warn("Subscriber did not define a target channel: {}", sub);
                } else {
                    executeMessage(clientId, channelId, content, embedObject);
                }
            });
    }

    public void publish(String outputChannel, String id, EmbedObject embedObject) {
        log.debug("Publishing a discord embed ({}) to output channel : {}", id, outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordEmbed").stream()
            // only include if the subscriber has allowed this time
            .filter(sub -> timeFrameService.included(sub.getId()))
            // abort if one of the target clients is not ready
            .filter(sub -> isClientReady((String) sub.getVariables().get("client")))
            // check for duplicated messages to avoid spam
            .filter(sub -> checkDuplicate(sub, id))
            .forEach(sub -> {
                String channelId = (String) sub.getVariables().get("channel");
                String clientId = (String) sub.getVariables().get("client");
                if (channelId == null) {
                    log.warn("Subscriber did not define a target channel: {}", sub);
                } else {
                    executeMessage(clientId, channelId, null, embedObject);
                }
            });
    }

    public void publish(String outputChannel, String id, String message) {
        log.debug("Publishing a discord message ({}) to output channel : {}", id, outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordMessage").stream()
            // only include if the subscriber has allowed this time
            .filter(sub -> timeFrameService.included(sub.getId()))
            // abort if one of the target clients is not ready
            .filter(sub -> isClientReady((String) sub.getVariables().get("client")))
            // check for duplicated messages to avoid spam
            .filter(sub -> checkDuplicate(sub, id))
            .forEach(sub -> {
                String channelId = (String) sub.getVariables().get("channel");
                String clientId = (String) sub.getVariables().get("client");
                if (channelId == null) {
                    log.warn("Subscriber did not define a target channel: {}", sub);
                } else {
                    executeMessage(clientId, channelId, message, null);
                }
            });
    }

    private boolean isClientReady(String clientId) {
        boolean ready = clientRegistry.getClients().entrySet().stream()
            .filter(entry -> matchesClient(clientId, entry))
            .allMatch(entry -> entry.getValue().isReady());
        if (!ready) {
            log.warn("At least one of the target clients matching {} is not ready yet", clientId);
        }
        return ready;
    }

    private void executeMessage(String clientId, String channelId, String content, EmbedObject embedObject) {
        clientRegistry.getClients().entrySet().stream()
            .filter(entry -> matchesClient(clientId, entry))
            .forEach(entry -> {
                IChannel channel = entry.getValue().getChannelByID(channelId);
                if (channel != null) {
                    sendMessage(channel, content, embedObject);
                } else {
                    log.warn("Did not found a channel with id {} in bot {}", channelId, clientId);
                }
            });
    }

    private boolean matchesClient(String clientId, Map.Entry<Bot, IDiscordClient> entry) {
        return clientId.equals(entry.getKey().getId())
            || clientId.equalsIgnoreCase(entry.getKey().getName())
            || clientId.equalsIgnoreCase(entry.getValue().getOurUser().getName());
    }

    public void publish(String outputChannel, String id, DatadogEvent message) {
        log.debug("Publishing an event message ({}) to output channel : {}", id, outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DatadogEvent").stream()
            .filter(sub -> timeFrameService.included(sub.getId()))
            .filter(sub -> checkDuplicate(sub, id))
            .forEach(sub -> {
                String apiKey = (String) sub.getVariables().get("api_key");
                if (apiKey == null) {
                    log.warn("Subscriber did not define a Datadog API Key: {}", sub);
                } else {
                    try {
                        ResponseEntity<Map<String, ?>> responseEntity = publishEvent(message, apiKey);
                        log.debug("[{}] Response: {}", outputChannel, responseEntity);
                    } catch (RestClientException e) {
                        log.warn("Could not publish event", e);
                    }
                }
            });
    }

    private ResponseEntity<Map<String, ?>> publishEvent(DatadogEvent event, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("User-Agent", "curl");
        return restTemplate.exchange("https://app.datadoghq.com/api/v1/events",
            HttpMethod.POST,
            new HttpEntity<>(event, headers),
            new ParameterizedTypeReference<Map<String, ?>>() {
            }, "api_key", apiKey);
    }

    private boolean checkDuplicate(Subscriber subscriber, String id) {
        // check if the last message published was the same as this one
        List<Setting> settingList = settingService.findByGuildAndKey(subscriber.getChannel(), LAST_PUBLISHED).stream()
            .sorted(nullsLast(comparing(AbstractAuditingEntity::getLastModifiedDate)))
            .collect(Collectors.toList());
        if (settingList.isEmpty()) {
            // no messages published here, record this one
            settingService.createSetting(subscriber.getChannel(), LAST_PUBLISHED, id);
            return true;
        } else {
            Setting mostRecent = settingList.get(0);
            // delete old settings for this same (group, key) pair -if apply-
            settingList.subList(1, settingList.size()).stream()
                .map(Setting::getId)
                .forEach(settingService::delete);
            // check ids
            if (id.equals(mostRecent.getValue())) {
                log.info("Duplicate message not being published to {}: {}", subscriber, id);
                return false; // is duplicate, should skip if Subscriber disallows duplicates
            } else {
                // new message, store most recent id
                settingService.updateValue(mostRecent, id);
                return true;
            }
        }
    }

    /**
     * Save a subscriber.
     *
     * @param subscriberDTO the entity to save
     * @return the persisted entity
     */
    public SubscriberDTO save(SubscriberDTO subscriberDTO) {
        log.debug("Request to save Subscriber : {}", subscriberDTO);
        Subscriber subscriber = subscriberMapper.subscriberDTOToSubscriber(subscriberDTO);
        subscriber = subscriberRepository.save(subscriber);
        SubscriberDTO result = subscriberMapper.subscriberToSubscriberDTO(subscriber);
        return result;
    }

    /**
     * Get all the subscribers.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<SubscriberDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Subscribers");
        Page<Subscriber> result = subscriberRepository.findAll(pageable);
        return result.map(subscriber -> subscriberMapper.subscriberToSubscriberDTO(subscriber));
    }

    /**
     * Get one subscriber by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public SubscriberDTO findOne(String id) {
        log.debug("Request to get Subscriber : {}", id);
        Subscriber subscriber = subscriberRepository.findOne(id);
        SubscriberDTO subscriberDTO = subscriberMapper.subscriberToSubscriberDTO(subscriber);
        return subscriberDTO;
    }

    /**
     * Delete the  subscriber by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Subscriber : {}", id);
        subscriberRepository.delete(id);
    }
}
