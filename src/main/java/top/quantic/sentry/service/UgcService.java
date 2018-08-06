package top.quantic.sentry.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static top.quantic.sentry.service.util.MiscUtil.inflect;

@Service
public class UgcService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(UgcService.class);

    private final SentryProperties sentryProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private Map<String, String> endpoints;
    private Map<Long, UgcLadder> ladders = new ConcurrentHashMap<>();
    private Map<Long, UgcDivision> divisions = new ConcurrentHashMap<>();
    private Map<String, UgcLadder> ladderAliases = new ConcurrentHashMap<>();
    private Map<Long, Map<String, UgcDivision>> divByLadderAliases = new ConcurrentHashMap<>();

    @Autowired
    public UgcService(SentryProperties sentryProperties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.sentryProperties = sentryProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SentryProperties.Ugc ugc = sentryProperties.getUgc();
        endpoints = ugc.getEndpoints();
        log.debug("Loaded endpoints: {}", endpoints.keySet().toString());
        ladders = ugc.getLadders().entrySet().stream()
            .map(entry -> UgcLadder.parse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toMap(UgcLadder::getLadderId, Function.identity()));
        log.debug("Loaded {}", inflect(ladders.keySet().size(), "ladder"));
        divisions = ugc.getDivisions().entrySet().stream()
            .map(entry -> UgcDivision.parse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toMap(UgcDivision::getDivId, Function.identity()));
        log.debug("Loaded {}", inflect(divisions.keySet().size(), "division"));
        ladders.forEach((id, ladder) -> ladder.getAliases().forEach(alias -> ladderAliases.put(alias, ladder)));
        divisions.forEach((id, division) ->
            division.getAliases().forEach(alias -> divByLadderAliases
                .computeIfAbsent(division.getLadderId(), k -> new ConcurrentHashMap<>())
                .put(alias, division)));
        divisions.forEach((id, division) -> divByLadderAliases
            .computeIfAbsent(division.getLadderId(), k -> new ConcurrentHashMap<>())
            .put(division.getDivName(), division));
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("schedule")
    public UgcSchedule getSchedule(String ladder, Long season, Long week, String division, Boolean withTeams) throws IOException {
        Objects.requireNonNull(ladder, "Ladder must not be null");
        Objects.requireNonNull(season, "Season must not be null");
        Objects.requireNonNull(week, "Week must not be null");
        Objects.requireNonNull(withTeams, "withTeams must not be null");
        UgcLadder ladderSpec = ladderAliases.get(ladder.toLowerCase());
        if (ladderSpec == null) {
            throw new CustomParameterizedException("Invalid ladder name: " + ladder + ". Use one of: " +
                String.join(", ", ladderAliases.keySet()));
        }
        Long ladderId = ladderSpec.getLadderId();
        Map<String, Object> vars = getVariablesMap();
        vars.put("ladder", ladderId);
        vars.put("season", season);
        vars.put("week", week);
        ResponseEntity<String> responseEntity;
        UgcDivision divisionSpec = null;
        if (division == null) {
            responseEntity = restTemplate.getForEntity(endpoints.get("schedule"), String.class, vars);
        } else {
            divisionSpec = findDivisionByAlias(ladderId, division);
            if (divisionSpec == null) {
                throw new CustomParameterizedException("Invalid division name: " + division + ". Use one of: " +
                    String.join(", ", divByLadderAliases.get(ladderId).keySet()));
            }
            vars.put("div", divisionSpec.getDivId());
            responseEntity = restTemplate.getForEntity(endpoints.get("scheduleByDiv"), String.class, vars);
        }
        log.trace("[Schedule] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        UgcSchedule schedule = new UgcSchedule();
        schedule.setLadder(ladderSpec.getShortName());
        schedule.setSeason(season);
        schedule.setWeek(week);
        schedule.setDivision(division);
        List<UgcSchedule.Match> matches = objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcSchedule.Match>>() {
        });
        if (withTeams) {
            matches.forEach(match -> {
                if (match.getClanIdH() != null) {
                    try {
                        match.setHomeTeam(getTeam(match.getClanIdH(), false).getClanName());
                    } catch (IOException e) {
                        log.warn("Could not get team data", e);
                    }
                }
                if (match.getClanIdV() != null) {
                    try {
                        match.setAwayTeam(getTeam(match.getClanIdV(), false).getClanName());
                    } catch (IOException e) {
                        log.warn("Could not get team data", e);
                    }
                }
            });
        }
        schedule.setSchedule(matches.stream()
            .sorted(Comparator.comparingLong(match -> divisions.get(match.getDivId()).getDivOrder()))
            .collect(Collectors.toList()));
        log.debug("Schedule of {} s{}w{}{} retrieved: {}", ladderSpec.getShortName(), season, week,
            divisionSpec == null ? "" : " and " + divisionSpec.getDivName() + " division", inflect(schedule.getSchedule().size(), "match"));
        return schedule;
    }

    private UgcDivision findDivisionByAlias(Long ladderId, String key) {
        return Optional.ofNullable(divByLadderAliases.get(ladderId))
            .map(aliases -> aliases.get(key)).orElse(null);
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("team")
    public UgcTeam getTeam(Long id, Boolean withRoster) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id", id);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("teamPage"), String.class, vars);
        log.trace("[TeamPage] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(clean(responseEntity.getBody()), JsonUgcResponse.class);
        List<Map<String, Object>> raw = convertTabularData(response);
        if (!raw.isEmpty()) {
            UgcTeam team = objectMapper.convertValue(raw.get(0), UgcTeam.class);
            log.debug("Team {} ({}) retrieved", team.getClanName(), team.getClanId());
            if (withRoster) {
                team.setRoster(getRoster(id));
            }
            return team;
        } else {
            throw new CustomParameterizedException("Could not find a team with id: " + id);
        }
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("roster")
    public List<UgcTeam.RosteredPlayer> getRoster(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id", id);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("teamRoster"), String.class, vars);
        log.trace("[TeamRoster] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        List<UgcTeam.RosteredPlayer> result = objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcTeam.RosteredPlayer>>() {
        });
        log.debug("Roster for team {} retrieved: {}", id, inflect(result.size(), "member"));
        return result;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("results")
    public UgcResults getResults(String ladder, Long season, Long week) throws IOException {
        Objects.requireNonNull(ladder, "Ladder must not be null");
        Objects.requireNonNull(season, "Season must not be null");
        Objects.requireNonNull(week, "Week must not be null");
        UgcLadder ladderSpec = ladderAliases.get(ladder.toLowerCase());
        if (ladderSpec == null) {
            throw new CustomParameterizedException("Invalid ladder name: " + ladder + ". Use one of: " +
                String.join(", ", ladderAliases.keySet()));
        }
        Map<String, Object> vars = getVariablesMap();
        String formatSimple;
        if ("HL".equals(ladderSpec.getShortName())) {
            formatSimple = "h";
        } else if ("6s".equals(ladderSpec.getShortName())) {
            formatSimple = "6";
        } else {
            throw new CustomParameterizedException("Invalid ladder name: only HL and 6s supported");
        }
        vars.put("format", formatSimple);
        vars.put("season", season);
        vars.put("week", week);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(endpoints.get("matchResults"), String.class, vars);
        log.trace("[Results] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        JsonUgcResponse response = objectMapper.readValue(responseEntity.getBody(), JsonUgcResponse.class);
        UgcResults results = new UgcResults();
        results.setLadder(ladderSpec.getShortName());
        results.setSeason(season);
        results.setWeek(week);
        results.setMatches(objectMapper.convertValue(convertTabularData(response), new TypeReference<List<UgcResults.Match>>() {
        }));
        log.debug("Results of HL s{}w{} retrieved: {}", season, week, inflect(results.getMatches().size(), "game"));
        return results;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("legacyPlayer")
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
    @Cacheable("player")
    public UgcPlayer getPlayer(Long id) throws IOException {
        Objects.requireNonNull(id, "ID must not be null");
        Map<String, Object> vars = getVariablesMap();
        vars.put("id64", id);
        ResponseEntity<UgcPlayer> responseEntity = restTemplate.getForEntity(endpoints.get("playerTeamCurrent"), UgcPlayer.class, vars);
        log.trace("[Player] {}", responseEntity);
        if (responseEntity.getStatusCode().is4xxClientError() || responseEntity.getStatusCode().is5xxServerError()) {
            throw new CustomParameterizedException("UGC API returned status " + responseEntity.getStatusCode());
        }
        UgcPlayer player = responseEntity.getBody();
        if (player != null && (player.getTeam() == null || player.getTeam().isEmpty())) {
            player.setUgcPage("http://www.ugcleague.com/players_page.cfm?player_id=" + id);
        }
        return player;
    }

    @Retryable(maxAttempts = 10, backoff = @Backoff(2000L))
    @Cacheable("banList")
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
    @Cacheable("transactions")
    public List<UgcTransaction> getTransactions(String ladder, Long days) throws IOException {
        Objects.requireNonNull(ladder, "Ladder must not be null");
        Objects.requireNonNull(days, "Days span must not be null");
        UgcLadder ladderSpec = ladderAliases.get(ladder.toLowerCase());
        if (ladderSpec == null) {
            throw new CustomParameterizedException("Invalid ladder name: " + ladder + ". Use one of: " +
                ladderAliases.keySet().stream().collect(Collectors.joining(", ")));
        }
        Long ladderId = ladderSpec.getLadderId();
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

    // Admin level

	@Retryable(maxAttempts = 3, backoff = @Backoff(2000L))
	public void getActiveTeamTickets() {
    	ResponseEntity<List<UgcTeamTicket>> responseEntity = restTemplate.exchange(endpoints.get("currentTickets"),
			    HttpMethod.GET, null,
			    new ParameterizedTypeReference<List<UgcTeamTicket>>(){});
	}

    // Utilities

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
