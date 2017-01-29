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
import top.quantic.sentry.service.UserCountService;
import top.quantic.sentry.web.rest.vm.Series;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing UserCount.
 */
@RestController
@RequestMapping("/api")
public class UserCountResource {

    private final Logger log = LoggerFactory.getLogger(UserCountResource.class);

    @Inject
    private UserCountService userCountService;


    @GetMapping("/user-counts/all")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Map<String, List<Series>>> getUserCounts(@RequestParam String bot,
                                                                   @RequestParam String guild)
        throws URISyntaxException {
        log.debug("REST request to get all UserCounts from {}/{} as time-grouped Series", bot, guild);
        Map<String, List<Series>> series = userCountService.getGroupedPointsAfter(bot, guild, ZonedDateTime.now().minusYears(1));
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/user-counts/between")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<Map<String, List<Series>>> getUserCountsBetween(@RequestParam String bot,
                                                                            @RequestParam String guild,
                                                                            @RequestParam Long after,
                                                                            @RequestParam Long before)
        throws URISyntaxException {
        ZonedDateTime afterDateTime = Instant.ofEpochMilli(after).atZone(ZoneId.systemDefault());
        ZonedDateTime beforeDateTime = Instant.ofEpochMilli(before).atZone(ZoneId.systemDefault());
        log.debug("REST request to get UserCounts from {}/{} between {} and {} as time-grouped Series",
            bot, guild, afterDateTime, beforeDateTime);
        Map<String, List<Series>> series = userCountService.getGroupedPointsBetween(bot, guild, afterDateTime, beforeDateTime);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/user-counts/last")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getMostRecentUserCount(@RequestParam String bot,
                                                               @RequestParam String guild)
        throws URISyntaxException {
        log.debug("REST request to get most recent UserCount from {}/{} as Series", bot, guild);
        List<Series> series = userCountService.getMostRecentPoint();
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

}
