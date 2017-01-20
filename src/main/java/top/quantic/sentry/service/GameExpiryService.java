package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

import static org.springframework.http.HttpMethod.GET;
import static top.quantic.sentry.config.Constants.USER_AGENT;

@Service
public class GameExpiryService {

    private static final Logger log = LoggerFactory.getLogger(GameExpiryService.class);
    private static final String SERVICE_URL = "https://www.gameservers.com/ugcleague/free/index.php?action=get_status";

    private final RestTemplate restTemplate;
    private final HttpEntity<String> entity;
    private final ParameterizedTypeReference<Map<String, Integer>> type = new ParameterizedTypeReference<Map<String, Integer>>() {
    };

    @Autowired
    public GameExpiryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("User-Agent", USER_AGENT);
        this.entity = new HttpEntity<>(null, headers);
    }

    public Map<String, Integer> getExpirationSeconds() {
        try {
            ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(SERVICE_URL, GET, entity, type);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("GameServer expiration status refreshed");
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.warn("Could not refresh expiration status: {}", e.toString());
        } catch (HttpMessageNotReadableException e) {
            log.warn("Could not refresh expiration status", e);
        }
        return Collections.emptyMap();
    }
}
