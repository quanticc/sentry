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
    public ResponseEntity<Map<String, List<Series>>> getPlayerCounts()
        throws URISyntaxException {
        log.debug("REST request to get all PlayerCounts as time-grouped Series");
        Map<String, List<Series>> series = playerCountService.getGroupedPointsAfter(ZonedDateTime.now().minusYears(1));
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/player-counts/between")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Map<String, List<Series>>> getPlayerCountsBetween(@RequestParam Long after, @RequestParam Long before)
        throws URISyntaxException {
        ZonedDateTime afterDateTime = Instant.ofEpochMilli(after).atZone(ZoneId.systemDefault());
        ZonedDateTime beforeDateTime = Instant.ofEpochMilli(before).atZone(ZoneId.systemDefault());
        log.debug("REST request to get PlayerCounts between {} and {} as time-grouped Series", afterDateTime, beforeDateTime);
        Map<String, List<Series>> series = playerCountService.getGroupedPointsBetween(afterDateTime, beforeDateTime);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/player-counts/last")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getMostRecentPlayerCount()
        throws URISyntaxException {
        log.debug("REST request to get most recent PlayerCount as Series");
        List<Series> series = playerCountService.getMostRecentPoint();
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

}
