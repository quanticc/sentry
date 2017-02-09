package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST controller for managing UserCount.
 */
@RestController
@RequestMapping("/api")
public class UserCountResource {

    private final Logger log = LoggerFactory.getLogger(UserCountResource.class);

    private final UserCountService userCountService;

    @Autowired
    public UserCountResource(UserCountService userCountService) {
        this.userCountService = userCountService;
    }

    @GetMapping("/user-counts/points")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getUserCountsBetween(@RequestParam String bot,
                                                             @RequestParam String guild,
                                                             @RequestParam Long from,
                                                             @RequestParam Long to) throws URISyntaxException {

        ZonedDateTime fromTime = Instant.ofEpochMilli(from).atZone(ZoneId.systemDefault());
        ZonedDateTime toTime = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault());
        log.debug("GET UserCounts from {}/{} between {} and {}", bot, guild, fromTime, toTime);
        List<Series> series = userCountService.getGroupedPointsBetween(bot, guild, fromTime, toTime);
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
