package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.springframework.security.access.annotation.Secured;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.security.AuthoritiesConstants;
import top.quantic.sentry.service.BotService;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Bot.
 */
@RestController
@RequestMapping("/api")
public class BotResource {

    private final Logger log = LoggerFactory.getLogger(BotResource.class);

    @Inject
    private BotService botService;

    /**
     * POST  /bots : Create a new bot.
     *
     * @param bot the bot to create
     * @return the ResponseEntity with status 201 (Created) and with body the new bot, or with status 400 (Bad Request) if the bot has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/bots")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Bot> createBot(@Valid @RequestBody Bot bot) throws URISyntaxException {
        log.debug("REST request to save Bot : {}", bot);
        if (bot.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("bot", "idexists", "A new bot cannot already have an ID")).body(null);
        }
        Bot result = botService.save(bot);
        return ResponseEntity.created(new URI("/api/bots/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("bot", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /bots : Updates an existing bot.
     *
     * @param bot the bot to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated bot,
     * or with status 400 (Bad Request) if the bot is not valid,
     * or with status 500 (Internal Server Error) if the bot couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/bots")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Bot> updateBot(@Valid @RequestBody Bot bot) throws URISyntaxException {
        log.debug("REST request to update Bot : {}", bot);
        if (bot.getId() == null) {
            return createBot(bot);
        }
        Bot result = botService.save(bot);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("bot", bot.getId().toString()))
            .body(result);
    }

    /**
     * GET  /bots : get all the bots.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of bots in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/bots")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<Bot>> getAllBots(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Bots");
        Page<Bot> page = botService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/bots");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /bots/:id : get the "id" bot.
     *
     * @param id the id of the bot to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the bot, or with status 404 (Not Found)
     */
    @GetMapping("/bots/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Bot> getBot(@PathVariable String id) {
        log.debug("REST request to get Bot : {}", id);
        Bot bot = botService.findOne(id);
        return Optional.ofNullable(bot)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /bots/:id : delete the "id" bot.
     *
     * @param id the id of the bot to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/bots/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteBot(@PathVariable String id) {
        log.debug("REST request to delete Bot : {}", id);
        botService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("bot", id.toString())).build();
    }

}