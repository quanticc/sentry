package top.quantic.sentry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.web.rest.errors.CustomParameterizedException;
import top.quantic.sentry.web.rest.vm.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UgcService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(UgcService.class);

    private final SentryProperties sentryProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private Map<String, String> endpoints;

    @Autowired
    public UgcService(SentryProperties sentryProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.sentryProperties = sentryProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        endpoints = sentryProperties.getUgc().getEndpoints();
        log.debug("Loaded endpoints: {}", endpoints.keySet().toString());
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public UgcSchedule getSchedule(String ladder, Long season, Long week) throws IOException {
        Objects.requireNonNull(ladder, "Ladder must not be null");
        Objects.requireNonNull(season, "Season must not be null");
        Objects.requireNonNull(week, "Week must not be null");
        String ladderId = sentryProperties.getUgc().getLadders().get(ladder);
        if (ladderId == null) {
            throw new CustomParameterizedException("Invalid ladder name: " + ladder + ". Use one of: " +
                sentryProperties.getUgc().getLadders().keySet().stream().collect(Collectors.joining(", ")));
        }
        Map<String, Object> vars = getVariablesMap();
        vars.put("ladder", ladderId);
        vars.put("season", season);
        vars.put("week", week);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("schedule"), String.class, vars);
        log.trace("[Schedule] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        UgcSchedule schedule = new UgcSchedule();
        schedule.setLadder(ladder);
        schedule.setSeason(season);
        schedule.setWeek(week);
        schedule.setSchedule(objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcSchedule.Match>>() {
        }));
        return schedule;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public UgcTeam getTeam(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id", id);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("teamPage"), String.class, vars);
        log.trace("[TeamPage] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(clean(responseEntity.getBody()), JsonUgcResponse.class);
        UgcTeam team = objectMapper.convertValue(convertTabularData(response).get(0), UgcTeam.class);
        team.setRoster(getRoster(id));
        return team;
    }

    private List<UgcTeam.RosteredPlayer> getRoster(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id", id);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("teamRoster"), String.class, vars);
        log.trace("[TeamRoster] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        return objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcTeam.RosteredPlayer>>() {
        });
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public UgcResults getResults(Long season, Long week) throws IOException {
        Objects.requireNonNull(season, "Season must not be null");
        Objects.requireNonNull(week, "Week must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("season", season);
        vars.put("week", week);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("matchResults"), String.class, vars);
        log.trace("[Results] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        UgcResults results = new UgcResults();
        results.setLadder("hl"); // only available for HL
        results.setSeason(season);
        results.setWeek(week);
        results.setMatches(objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcResults.Match>>() {
        }));
        return results;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public UgcLegacyPlayer getLegacyPlayer(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id64", id);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("teamPlayer"), String.class, vars);
        log.trace("[Player] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        UgcLegacyPlayer player = new UgcLegacyPlayer();
        player.setId(id);
        player.setTeams(objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcLegacyPlayer.Membership>>() {
        }));
        return player;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public UgcPlayer getPlayer(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id64", id);
        ResponseEntity<UgcPlayer> responseEntity = restTemplate.getForEntity(endpoints.get("playerTeamCurrent"), UgcPlayer.class, vars);
        log.trace("[Player] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        return responseEntity.getBody();
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public List<UgcBan> getBanList() throws IOException {
        Map<String, Object> vars = getVariablesMap();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("banList"), String.class, vars);
        log.trace("[BanList] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        return objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcBan>>() {
        });
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    public List<UgcTransaction> getTransactions(String ladder, Long days) throws IOException {
        Objects.requireNonNull(ladder, "Ladder must not be null");
        Objects.requireNonNull(days, "Days span must not be null");
        String ladderId = sentryProperties.getUgc().getLadders().get(ladder);
        if (ladderId == null) {
            throw new CustomParameterizedException("Invalid ladder name: " + ladder + ". Use one of: " +
                sentryProperties.getUgc().getLadders().keySet().stream().collect(Collectors.joining(", ")));
        }
        Map<String, Object> vars = getVariablesMap();
        vars.put("ladder", ladderId);
        vars.put("span", days);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("transactions"), String.class, vars);
        log.trace("[Transactions] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("Returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        return objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcTransaction>>() {
        });
    }

    private Map<String, Object> getVariablesMap() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("key", sentryProperties.getUgc().getApiKey());
        return vars;
    }

    private List<Map<String, Object>> convertTabularData(JsonUgcResponse response) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = response.getColumns();
        for (List<Object> row : response.getData()) {
            Map<String, Object> resultRow = new LinkedHashMap<>();
            for (int i = 0; i < row.size(); i++) {
                resultRow.put(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnNames.get(i)), row.get(i));
            }
            result.add(resultRow);
        }
        return result;
    }

    private String clean(String input) {
        return input.replaceAll("(^onLoad\\()|(\\)$)", "");
    }
}
