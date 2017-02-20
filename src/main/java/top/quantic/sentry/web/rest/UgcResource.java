package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import top.quantic.sentry.service.UgcService;
import top.quantic.sentry.web.rest.util.RateLimited;
import top.quantic.sentry.web.rest.vm.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static top.quantic.sentry.service.util.MiscUtil.inflect;

/**
 * REST controller to wrap UGC API functions.
 */
@RestController
@RequestMapping("/api/v1/ugc")
public class UgcResource {

    private final Logger log = LoggerFactory.getLogger(UgcResource.class);

    private final UgcService ugcService;

    @Autowired
    public UgcResource(UgcService ugcService) {
        this.ugcService = ugcService;
    }

    @GetMapping("/schedule/{ladder}/{season}/{week}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<UgcSchedule> getSchedule(@ApiIgnore HttpServletRequest request,
                                                   @PathVariable String ladder,
                                                   @PathVariable Long season,
                                                   @PathVariable Long week) throws IOException {
        log.debug("REST request to get schedule for {} s{}w{}", ladder, season, week);
        return ResponseEntity.ok(ugcService.getSchedule(ladder, season, week));
    }

    @GetMapping("/team/{id}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<UgcTeam> getTeam(@ApiIgnore HttpServletRequest request,
                                           @PathVariable Long id) throws IOException {
        log.debug("REST request to get team with id {}", id);
        return ResponseEntity.ok(ugcService.getTeam(id, true));
    }

    @GetMapping("/results/{season}/{week}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<UgcResults> getResults(@ApiIgnore HttpServletRequest request,
                                                 @PathVariable Long season,
                                                 @PathVariable Long week) throws IOException {
        log.debug("REST request to get results from s{}w{}", season, week);
        return ResponseEntity.ok(ugcService.getResults(season, week));
    }

    @GetMapping("/player-legacy/{steamId64}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<UgcLegacyPlayer> getLegacyPlayer(@ApiIgnore HttpServletRequest request,
                                                           @PathVariable Long steamId64) throws IOException {
        log.debug("REST request to get UGC API v1 player data for {}", steamId64);
        return ResponseEntity.ok(ugcService.getLegacyPlayer(steamId64));
    }

    @GetMapping("/player/{steamId64}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<UgcPlayer> getPlayer(@ApiIgnore HttpServletRequest request,
                                               @PathVariable Long steamId64) throws IOException {
        log.debug("REST request to get player data for {}", steamId64);
        return ResponseEntity.ok(ugcService.getPlayer(steamId64));
    }

    @GetMapping("/bans")
    @Timed
    @RateLimited(1)
    public ResponseEntity<List<UgcBan>> getBanList(@ApiIgnore HttpServletRequest request) throws IOException {
        log.debug("REST request to get ban list");
        return ResponseEntity.ok(ugcService.getBanList());
    }

    @GetMapping("/transactions/{ladder}/{days}")
    @Timed
    @RateLimited(1)
    public ResponseEntity<List<UgcTransaction>> getTransactions(@ApiIgnore HttpServletRequest request,
                                                                @PathVariable String ladder,
                                                                @PathVariable Long days) throws IOException {
        Long cappedDays = Math.max(1, Math.min(days, 7)); // valid spans: 1-7 days
        log.debug("REST request to get roster transactions for ladder {} in the past {}", ladder, inflect(cappedDays, "day"));
        return ResponseEntity.ok(ugcService.getTransactions(ladder, cappedDays));
    }

}
