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
import top.quantic.sentry.service.PlayerCountService;
import top.quantic.sentry.web.rest.vm.Series;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST controller for managing PlayerCount.
 */
@RestController
@RequestMapping("/api")
public class PlayerCountResource {

    private static final Logger log = LoggerFactory.getLogger(PlayerCountResource.class);

    private final PlayerCountService playerCountService;

    @Autowired
    public PlayerCountResource(PlayerCountService playerCountService) {
        this.playerCountService = playerCountService;
    }

    @GetMapping("/player-counts/points")
    @Timed
    @Secured(AuthoritiesConstants.SUPPORT)
    public ResponseEntity<List<Series>> getPlayerCountsBetween(@RequestParam Long from,
                                                               @RequestParam Long to) throws URISyntaxException {

        ZonedDateTime fromTime = Instant.ofEpochMilli(from).atZone(ZoneId.systemDefault());
        ZonedDateTime toTime = Instant.ofEpochMilli(to).atZone(ZoneId.systemDefault());
        log.debug("GET PlayerCounts between {} and {}", fromTime, toTime);
        List<Series> series = playerCountService.getGroupedPointsBetween(fromTime, toTime);
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
