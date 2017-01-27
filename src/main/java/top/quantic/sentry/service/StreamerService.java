package top.quantic.sentry.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.domain.Streamer;
import top.quantic.sentry.event.TwitchStreamEvent;
import top.quantic.sentry.repository.StreamerRepository;
import top.quantic.sentry.service.dto.StreamerDTO;
import top.quantic.sentry.service.mapper.StreamerMapper;
import top.quantic.sentry.web.rest.vm.TwitchStream;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.service.util.DateUtil.withRelative;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

/**
 * Service Implementation for managing Streamer.
 */
@Service
public class StreamerService {

    private static final Logger log = LoggerFactory.getLogger(StreamerService.class);
    private static final int TWITCH_BATCH_SIZE = 100;

    private final StreamerRepository streamerRepository;
    private final StreamerMapper streamerMapper;
    private final SentryProperties sentryProperties;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher publisher;
    private final SettingService settingService;

    private final RateLimiter twitchApiLimiter = RateLimiter.create(1.0);
    private final RateLimiter eventLimiter = RateLimiter.create(1.0);
    private final Handlebars handlebars = new Handlebars();

    @Autowired
    public StreamerService(StreamerRepository streamerRepository, StreamerMapper streamerMapper,
                           SentryProperties sentryProperties, RestTemplate restTemplate,
                           ApplicationEventPublisher publisher, SettingService settingService) {
        this.streamerRepository = streamerRepository;
        this.streamerMapper = streamerMapper;
        this.sentryProperties = sentryProperties;
        this.restTemplate = restTemplate;
        this.publisher = publisher;
        this.settingService = settingService;
    }

    /**
     * Publish streams that have gone past the given amount of minutes, at the specified rate.
     *
     * @param expireMinutes   minutes since last announcement made for a streamer to trigger the event publishing.
     * @param eventsPerSecond rate of events published per second, for throttling purposes.
     */
    public void publishStreams(long expireMinutes, double eventsPerSecond) {
        // Twitch
        List<Streamer> twitchStreamers = findExpiredStreamers("Twitch", expireMinutes).stream()
            .sorted(Comparator.comparing(Streamer::getName))
            .collect(Collectors.toList());

        if (twitchStreamers.isEmpty()) {
            log.debug("All streams were announced less than {} ago", inflect(expireMinutes, "minute"));
            return;
        }

        AtomicInteger streams = new AtomicInteger();
        AtomicInteger published = new AtomicInteger();

        Map<String, String> mappings = settingService.findByGuild("leagueToGame").stream()
            .collect(Collectors.toMap(Setting::getKey, Setting::getValue));

        try {
            for (List<Streamer> streamers : Lists.partition(twitchStreamers, TWITCH_BATCH_SIZE)) {
                String channels = streamers.stream()
                    .map(Streamer::getName)
                    .distinct()
                    .collect(Collectors.joining(","));
                twitchApiLimiter.acquire();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Client-ID", sentryProperties.getTwitch().getClientId());
                ResponseEntity<TwitchStreamResponse> responseEntity = restTemplate.exchange(
                    "https://api.twitch.tv/kraken/streams?channel=" + channels + "&limit=" + TWITCH_BATCH_SIZE,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    TwitchStreamResponse.class);
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    TwitchStreamResponse response = responseEntity.getBody();
                    // send individually, but throttle
                    eventLimiter.setRate(eventsPerSecond);
                    for (TwitchStream stream : response.getStreams()) {
                        streams.incrementAndGet();
                        twitchStreamers.parallelStream()
                            .filter(streamer -> stream.getChannel().getName().equalsIgnoreCase(streamer.getName()))
                            .peek(streamer -> logStreamData(stream, streamer))
                            .filter(streamer -> checkFilter(stream, streamer, mappings))
                            .peek(streamer -> published.incrementAndGet())
                            .forEach(streamer -> publishStream(stream, streamer));
                    }
                } else {
                    log.warn("Could not retrieve streamers: {}", responseEntity);
                }
            }
            log.info("Publishing {} from the received {} live of {}", published, streams,
                inflect(twitchStreamers.size(), "requested stream"));
        } catch (RestClientException e) {
            log.warn("Exception while retrieving streamers", e);
        }

    }

    private List<Streamer> findExpiredStreamers(String provider, long minutesSinceLastAnnouncement) {
        return streamerRepository.findByEnabledIsTrueAndProviderAndLastAnnouncementBefore(provider,
            ZonedDateTime.now().minusMinutes(minutesSinceLastAnnouncement));
    }

    private void logStreamData(TwitchStream stream, Streamer streamer) {
        log.debug("Processing {} with {}", stream, streamer);
    }

    private boolean checkFilter(TwitchStream stream, Streamer streamer, Map<String, String> mappings) {
        // Filter if we have the same stream id than the last one
        // unless the stream started 3 hours ago (!)
        if (stream.getId().equals(streamer.getLastStreamId())
            && stream.getCreatedAt().plusSeconds(60 * 60 * 3).isAfter(Instant.now())) {
            log.info("[{}] Stream was already announced, started {}",
                streamer.getName(), withRelative(stream.getCreatedAt()));
            return false;
        }
        // Filter if game matches the one set in DB
        String game = stream.getGame();
        if (!isBlank(streamer.getLeague())) {
            String gameFilter = mappings.getOrDefault(streamer.getLeague(), streamer.getLeague());
            if (!game.equalsIgnoreCase(gameFilter)) {
                log.info("[{}] Streaming game {} but was registered for {}",
                    streamer.getName(), game, gameFilter);
                return false;
            }
        }
        // Filter if title does not contain the one set in DB
        String status = stream.getChannel().getStatus();
        String statusFilter = streamer.getTitleFilter();
        if (!isBlank(statusFilter)) {
            if (!status.contains(statusFilter)) {
                log.info("[{}] Streaming with title {} which does not contain {}",
                    streamer.getName(), status, statusFilter);
                return false;
            }
        }
        return true;
    }

    private void publishStream(TwitchStream stream, Streamer streamer) {
        eventLimiter.acquire();
        streamer.setLastStreamId(stream.getId());
        streamer.setLastAnnouncement(ZonedDateTime.now());
        streamerRepository.save(streamer);
        TwitchStreamEvent event = new TwitchStreamEvent(stream, streamer);
        String announcementTemplate = streamer.getAnnouncement();
        Map<String, Object> embedFields = streamer.getEmbedFields();
        Context context = Context.newBuilder(streamer)
            .combine("stream", stream)
            .combine("url", stream.getChannel().getUrl())
            .resolver(
                MapValueResolver.INSTANCE,
                JavaBeanValueResolver.INSTANCE
            )
            .build();
        if (announcementTemplate != null) {
            try {
                Template template = handlebars.compileInline(announcementTemplate);
                event.setAnnouncement(template.apply(context));
            } catch (IOException e) {
                log.warn("Could not compile announcement", e);
            }
        }
        if (embedFields != null) {
            Map<String, String> resolvedFields = new LinkedHashMap<>();
            embedFields.entrySet().stream()
                // for now only String fields will be used
                .filter(entry -> entry.getValue() instanceof String)
                .forEach(entry -> {
                    String resolved = (String) entry.getValue();
                    if (resolved.contains("{{") || resolved.contains("}}")) {
                        try {
                            Template template = handlebars.compileInline(resolved);
                            resolved = template.apply(context);
                        } catch (IOException e) {
                            log.warn("Could not compile embed fields", e);
                        }
                    }
                    resolvedFields.put(entry.getKey(), resolved);
                });
            event.setResolvedFields(resolvedFields);
        }
        context.destroy();
        publisher.publishEvent(event);
    }

    /**
     * Save a streamer.
     *
     * @param streamerDTO the entity to save
     * @return the persisted entity
     */
    public StreamerDTO save(StreamerDTO streamerDTO) {
        log.debug("Request to save Streamer : {}", streamerDTO);
        Streamer streamer = streamerMapper.streamerDTOToStreamer(streamerDTO);
        streamer = streamerRepository.save(streamer);
        StreamerDTO result = streamerMapper.streamerToStreamerDTO(streamer);
        return result;
    }

    /**
     * Get all the streamers.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<StreamerDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Streamers");
        Page<Streamer> result = streamerRepository.findAll(pageable);
        return result.map(streamer -> streamerMapper.streamerToStreamerDTO(streamer));
    }

    /**
     * Get one streamer by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public StreamerDTO findOne(String id) {
        log.debug("Request to get Streamer : {}", id);
        Streamer streamer = streamerRepository.findOne(id);
        StreamerDTO streamerDTO = streamerMapper.streamerToStreamerDTO(streamer);
        return streamerDTO;
    }

    /**
     * Delete the  streamer by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Streamer : {}", id);
        streamerRepository.delete(id);
    }

    public static class TwitchStreamResponse {

        @JsonProperty("_total")
        private long total;
        private List<TwitchStream> streams;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public List<TwitchStream> getStreams() {
            return streams;
        }

        public void setStreams(List<TwitchStream> streams) {
            this.streams = streams;
        }

        @Override
        public String toString() {
            return "TwitchStreamResponse{" +
                "total=" + total +
                ", streams=" + streams +
                '}';
        }
    }
}
