package top.quantic.sentry.service;

import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.repository.BotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Bot.
 */
@Service
public class BotService {

    private final Logger log = LoggerFactory.getLogger(BotService.class);
    
    @Inject
    private BotRepository botRepository;

    /**
     * Save a bot.
     *
     * @param bot the entity to save
     * @return the persisted entity
     */
    public Bot save(Bot bot) {
        log.debug("Request to save Bot : {}", bot);
        Bot result = botRepository.save(bot);
        return result;
    }

    /**
     *  Get all the bots.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    public Page<Bot> findAll(Pageable pageable) {
        log.debug("Request to get all Bots");
        Page<Bot> result = botRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one bot by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    public Bot findOne(String id) {
        log.debug("Request to get Bot : {}", id);
        Bot bot = botRepository.findOne(id);
        return bot;
    }

    /**
     *  Delete the  bot by id.
     *
     *  @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Bot : {}", id);
        botRepository.delete(id);
    }
}
