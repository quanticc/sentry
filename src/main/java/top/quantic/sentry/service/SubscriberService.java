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
import sx.blah.discord.handle.obj.IChannel;
import top.quantic.sentry.domain.Subscriber;
import top.quantic.sentry.repository.SubscriberRepository;
import top.quantic.sentry.service.dto.SubscriberDTO;
import top.quantic.sentry.service.mapper.SubscriberMapper;
import top.quantic.sentry.web.rest.vm.DiscordWebhook;

import java.util.Map;

import static top.quantic.sentry.discord.util.DiscordLimiter.acquireWebhook;
import static top.quantic.sentry.discord.util.DiscordUtil.sendMessage;

/**
 * Service Implementation for managing Subscriber.
 */
@Service
public class SubscriberService {

    private final Logger log = LoggerFactory.getLogger(SubscriberService.class);

    private final SubscriberRepository subscriberRepository;
    private final SubscriberMapper subscriberMapper;
    private final TimeFrameService timeFrameService;
    private final DiscordService discordService;

    @Autowired
    public SubscriberService(SubscriberRepository subscriberRepository, SubscriberMapper subscriberMapper,
                             TimeFrameService timeFrameService, DiscordService discordService) {
        this.subscriberRepository = subscriberRepository;
        this.subscriberMapper = subscriberMapper;
        this.timeFrameService = timeFrameService;
        this.discordService = discordService;
    }

    public void send(String outputChannel, DiscordWebhook message) {
        log.debug("Request to send a webhook message to output channel : {}", outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordWebhook").stream()
            .filter(sub -> timeFrameService.included(sub.getId()))
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
                        execute(message, url);
                    } catch (RestClientException e) {
                        log.warn("Could not execute webhook", e);
                    }
                }
            });
    }

    private ResponseEntity<Map<String, ?>> execute(DiscordWebhook webhook, String webhookUrl) {
        RestTemplate restTemplate = new RestTemplate();
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

    public void send(String outputChannel, String message) {
        log.debug("Request to send a discord message to output channel : {}", outputChannel);
        subscriberRepository.findByChannelAndType(outputChannel, "DiscordMessage").stream()
            .filter(sub -> timeFrameService.included(sub.getId()))
            .forEach(sub -> {
                // Variables required: channel, client (botId)
                String channelId = (String) sub.getVariables().get("channel");
                String clientId = (String) sub.getVariables().get("client");
                if (channelId == null) {
                    log.warn("Subscriber did not define a target URL: {}", sub);
                } else {
                    executeMessage(clientId, channelId, message);
                }
            });
    }

    private void executeMessage(String clientId, String channelId, String message) {
        discordService.getClients().entrySet().stream()
            .filter(entry -> entry.getValue().isReady())
            .filter(entry -> clientId.equals(entry.getKey().getId())
                || clientId.equalsIgnoreCase(entry.getKey().getName())
                || clientId.equalsIgnoreCase(entry.getValue().getOurUser().getName()))
            .forEach(entry -> {
                IChannel channel = entry.getValue().getChannelByID(channelId);
                if (channel != null) {
                    sendMessage(channel, message, false);
                } else {
                    log.warn("Did not found a channel with id {} in bot {}", channelId, clientId);
                }
            });
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
