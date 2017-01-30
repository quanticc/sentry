package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.quantic.sentry.security.AuthoritiesConstants;
import top.quantic.sentry.service.PlayerCountService;
import top.quantic.sentry.service.util.ChartUtil;
import top.quantic.sentry.web.rest.vm.Series;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing PlayerCount.
 */
@RestController
@RequestMapping("/api")
public class PlayerCountResource {

    private final Logger log = LoggerFactory.getLogger(PlayerCountResource.class);

    @Inject
    private PlayerCountService playerCountService;

    @GetMapping("/player-counts/all")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Map<String, List<Series>>> getPlayerCounts() throws URISyntaxException {
        log.debug("GET PlayerCounts");
        Map<String, List<Series>> series = playerCountService.getGroupedPoints();
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/player-counts/between")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getPlayerCountsBetween(@RequestParam(required = false) Long after,
                                                               @RequestParam(required = false) Long before,
                                                               @RequestParam Integer resolution) throws URISyntaxException {
        ZonedDateTime afterDateTime = after == null ? Instant.EPOCH.atZone(ZoneId.systemDefault()) : Instant.ofEpochMilli(after).atZone(ZoneId.systemDefault());
        ZonedDateTime beforeDateTime = before == null ? Instant.now().atZone(ZoneId.systemDefault()) : Instant.ofEpochMilli(before).atZone(ZoneId.systemDefault());
        int res = ChartUtil.truncateResolution(resolution, afterDateTime, beforeDateTime);
        log.debug("GET PlayerCounts between {} and {} with resolution of {} minutes", afterDateTime, beforeDateTime, res);
        List<Series> series = playerCountService.getGroupedPointsBetween(afterDateTime, beforeDateTime, res);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/player-counts/last")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getMostRecentPlayerCount() throws URISyntaxException {
        log.debug("GET most recent PlayerCount");
        List<Series> series = playerCountService.getMostRecentPoint();
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

}
