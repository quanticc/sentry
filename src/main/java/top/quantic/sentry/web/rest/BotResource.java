package top.quantic.sentry.web.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import sx.blah.discord.util.DiscordException;
import top.quantic.sentry.security.AuthoritiesConstants;
import top.quantic.sentry.service.BotService;
import top.quantic.sentry.service.dto.BotDTO;
import top.quantic.sentry.web.rest.util.HeaderUtil;
import top.quantic.sentry.web.rest.util.PaginationUtil;

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
     * @param botDTO the botDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new botDTO, or with status 400 (Bad Request) if the bot has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/bots")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<BotDTO> createBot(@Valid @RequestBody BotDTO botDTO) throws URISyntaxException {
        log.debug("REST request to save Bot : {}", botDTO);
        if (botDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("bot", "idexists", "A new bot cannot already have an ID")).body(null);
        }
        BotDTO result = botService.save(botDTO);
        return ResponseEntity.created(new URI("/api/bots/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("bot", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /bots : Updates an existing bot.
     *
     * @param botDTO the botDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated botDTO,
     * or with status 400 (Bad Request) if the botDTO is not valid,
     * or with status 500 (Internal Server Error) if the botDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/bots")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<BotDTO> updateBot(@Valid @RequestBody BotDTO botDTO) throws URISyntaxException {
        log.debug("REST request to update Bot : {}", botDTO);
        if (botDTO.getId() == null) {
            return createBot(botDTO);
        }
        BotDTO result = botService.save(botDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("bot", botDTO.getId().toString()))
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
    public ResponseEntity<List<BotDTO>> getAllBots(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Bots");
        Page<BotDTO> page = botService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/bots");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /bots/:id : get the "id" bot.
     *
     * @param id the id of the botDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the botDTO, or with status 404 (Not Found)
     */
    @GetMapping("/bots/{id}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<BotDTO> getBot(@PathVariable String id) {
        log.debug("REST request to get Bot : {}", id);
        BotDTO botDTO = botService.findOne(id);
        return Optional.ofNullable(botDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /bots/:id : delete the "id" bot.
     *
     * @param id the id of the botDTO to delete
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

    private ResponseEntity<Void> loginBot(@PathVariable String id) {
        BotDTO bot = botService.findOne(id);
        if (bot == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            try {
                botService.login(bot);
                return ResponseEntity.ok()
                    .headers(HeaderUtil.createAlert("Request to login bot " + bot.getId() + " sent", bot.getId()))
                    .build();
            } catch (DiscordException e) {
                log.warn("Could not login bot: {}", bot, e);
                return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert("Could not login bot: " + e.getErrorMessage(),
                        bot.getId()))
                    .body(null);
            }
        }
    }

    private ResponseEntity<Void> logoutBot(@PathVariable String id) {
        BotDTO bot = botService.findOne(id);
        if (bot == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            try {
                botService.logout(bot);
                return ResponseEntity.ok()
                    .headers(HeaderUtil.createAlert("Request to logout bot " + bot.getId() + " sent", bot.getId()))
                    .build();
            } catch (DiscordException e) {
                log.warn("Could not logout bot: {}", bot, e);
                return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createErrorAlert("Could not logout bot: " + e.getErrorMessage(),
                        bot.getId()))
                    .body(null);
            }
        }
    }

    private ResponseEntity<Void> resetBot(@PathVariable String id) {
        BotDTO bot = botService.findOne(id);
        if (bot == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            botService.reset(bot);
            return ResponseEntity.ok()
                .headers(HeaderUtil.createAlert("Request to reset bot " + bot.getId() + " sent", bot.getId()))
                .build();
        }
    }

    @PostMapping("/bots/{id}/{action}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> performAction(@PathVariable String id, @PathVariable String action) {
        log.debug("REST request to perform action {} on Bot : {}", action, id);
        switch (action) {
            case "login":
                return loginBot(id);
            case "logout":
                return logoutBot(id);
            case "reset":
                return resetBot(id);
            default:
                return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createAlert("Invalid action: " + action, action))
                    .body(null);
        }
    }

}
