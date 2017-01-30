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
                                                                   @RequestParam String guild) throws URISyntaxException {
        log.debug("GET UserCounts from {}/{}", bot, guild);
        Map<String, List<Series>> series = userCountService.getGroupedPoints(bot, guild);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/user-counts/between")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getUserCountsBetween(@RequestParam String bot,
                                                             @RequestParam String guild,
                                                             @RequestParam(required = false) Long after,
                                                             @RequestParam(required = false) Long before,
                                                             @RequestParam int resolution) throws URISyntaxException {
        ZonedDateTime afterDateTime = after == null ? Instant.EPOCH.atZone(ZoneId.systemDefault()) : Instant.ofEpochMilli(after).atZone(ZoneId.systemDefault());
        ZonedDateTime beforeDateTime = before == null ? Instant.now().atZone(ZoneId.systemDefault()) : Instant.ofEpochMilli(before).atZone(ZoneId.systemDefault());
        // limit resolution by period duration
        int res = ChartUtil.truncateResolution(resolution, afterDateTime, beforeDateTime);
        log.debug("GET UserCounts from {}/{} between {} and {} with resolution of {} minutes", bot, guild, afterDateTime, beforeDateTime, res);
        List<Series> series = userCountService.getGroupedPointsBetween(bot, guild, afterDateTime, beforeDateTime, res);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

    @GetMapping("/user-counts/last")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getMostRecentUserCount(@RequestParam String bot,
                                                               @RequestParam String guild) throws URISyntaxException {
        log.debug("GET most recent UserCount from {}/{}", bot, guild);
        List<Series> series = userCountService.getMostRecentPoint(bot, guild);
        return new ResponseEntity<>(series, null, HttpStatus.OK);
    }

}
