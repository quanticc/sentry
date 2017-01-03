package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.DiscordException;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandRegistry;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.module.ListenerSupplier;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.repository.BotRepository;
import top.quantic.sentry.service.dto.BotDTO;
import top.quantic.sentry.service.mapper.BotMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Bot.
 */
@Service
public class BotService implements InitializingBean, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(BotService.class);

    private final BotMapper botMapper;
    private final BotRepository botRepository;
    private final DiscordService discordService;
    private final CommandRegistry commandRegistry;
    private final List<CommandSupplier> commandSuppliers;
    private final List<ListenerSupplier> listenerSuppliers;

    @Autowired
    public BotService(BotMapper botMapper, BotRepository botRepository, DiscordService discordService,
                      CommandRegistry commandRegistry, List<CommandSupplier> commandSuppliers,
                      List<ListenerSupplier> listenerSuppliers) {
        this.botMapper = botMapper;
        this.botRepository = botRepository;
        this.discordService = discordService;
        this.commandRegistry = commandRegistry;
        this.commandSuppliers = commandSuppliers;
        this.listenerSuppliers = listenerSuppliers;
    }

    public void login(BotDTO dto) throws DiscordException {
        login(botMapper.botDTOToBot(dto));
    }

    public void logout(BotDTO dto) throws DiscordException {
        logout(botMapper.botDTOToBot(dto));
    }

    public void reset(BotDTO dto) {
        reset(botMapper.botDTOToBot(dto));
    }

    private IDiscordClient login(Bot bot) throws DiscordException {
        log.debug("Request to login : {}", bot);
        if (discordService.getClients().containsKey(bot)) {
            throw new IllegalStateException("Bot is already logged in");
        }
        IDiscordClient client = new ClientBuilder()
            .withToken(bot.getToken())
            .setDaemon(bot.isDaemon())
            .withPingTimeout(bot.getMaxMissedPings())
            .setMaxReconnectAttempts(bot.getMaxReconnectAttempts())
            .login();
        discordService.getClients().put(bot, client);

        for (ListenerSupplier supplier : listenerSuppliers) {
            List<IListener<?>> listeners = supplier.getListeners();
            for (IListener<?> listener : listeners) {
                log.debug("[{}] Registering listener: {}", bot.getName(), listener.getClass().getSimpleName());
                client.getDispatcher().registerListener(listener);
            }
        }

        for (CommandSupplier supplier : commandSuppliers) {
            List<Command> commands = supplier.getCommands();
            log.debug("[{}] Registering commands: {}", bot.getName(), commands.stream()
                .map(Command::getName).collect(Collectors.joining(", ")));
            commandRegistry.registerAll(client, commands);
        }

        return client;
    }

    private IDiscordClient logout(Bot bot) throws DiscordException {
        log.debug("Request to logout: {}", bot);
        if (!discordService.getClients().containsKey(bot)) {
            throw new IllegalStateException("Bot is not logged in");
        }

        IDiscordClient client = discordService.getClients().get(bot);
        if (client.isLoggedIn()) {
            client.logout();
        } else {
            log.warn("Bot {} is not logged in", bot.getName());
        }
        discordService.getClients().remove(bot);

        return client;
    }

    private void reset(Bot bot) {
        log.debug("Request to reset: {}", bot);
        try {
            logout(bot);
        } catch (DiscordException e) {
            log.warn("Could not logout: {}", bot, e);
        }
        discordService.getClients().remove(bot);
    }

    //////////////////
    // CRUD Methods //
    //////////////////

    /**
     * Save a bot.
     *
     * @param botDTO the entity to save
     * @return the persisted entity
     */
    public BotDTO save(BotDTO botDTO) {
        log.debug("Request to save Bot : {}", botDTO);
        Bot bot = botMapper.botDTOToBot(botDTO);
        bot = botRepository.save(bot);
        BotDTO result = botMapper.botToBotDTO(bot);
        return result;
    }

    /**
     * Get all the bots.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<BotDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Bots");
        Page<Bot> result = botRepository.findAll(pageable);
        return result.map(bot -> botMapper.botToBotDTO(bot));
    }

    /**
     * Get one bot by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public BotDTO findOne(String id) {
        log.debug("Request to get Bot : {}", id);
        Bot bot = botRepository.findOne(id);
        BotDTO botDTO = botMapper.botToBotDTO(bot);
        return botDTO;
    }

    /**
     * Delete the  bot by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete Bot : {}", id);
        botRepository.delete(id);
    }

    ///////////////
    // Lifecycle //
    ///////////////

    @Override
    public void afterPropertiesSet() throws Exception {
        autoLoginBots();
    }

    private void autoLoginBots() {
        if (botRepository.count() == 0) {
            log.warn("No bots in store - Please add at least one bot definition");
        } else {
            botRepository.findAll().stream()
                .filter(Bot::isAutoLogin)
                .forEach(this::checkedLogin);
        }
    }

    private void checkedLogin(Bot bot) {
        try {
            login(bot);
        } catch (DiscordException e) {
            log.warn("Could not auto-login : " + bot.toString(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        discordService.getClients().entrySet().forEach(entry -> {
            if (entry.getValue().isLoggedIn()) {
                try {
                    logout(entry.getKey());
                } catch (DiscordException e) {
                    log.warn("Could not logout: {}", entry.getKey(), e);
                }
            }
        });
    }
}
