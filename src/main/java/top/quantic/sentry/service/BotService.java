package top.quantic.sentry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.DiscordException;
import top.quantic.sentry.discord.core.ClientRegistry;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandRegistry;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.module.DiscordSubscriber;
import top.quantic.sentry.discord.module.ListenerSupplier;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.repository.BotRepository;
import top.quantic.sentry.service.dto.BotDTO;
import top.quantic.sentry.service.mapper.BotMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Bot.
 */
@Service
public class BotService {

    private final Logger log = LoggerFactory.getLogger(BotService.class);

    private final BotMapper botMapper;
    private final BotRepository botRepository;
    private final ClientRegistry clientRegistry;
    private final CommandRegistry commandRegistry;
    private final List<CommandSupplier> commandSuppliers;
    private final List<ListenerSupplier> listenerSuppliers;
    private final List<DiscordSubscriber> discordSubscribers;

    @Autowired
    public BotService(BotMapper botMapper, BotRepository botRepository, ClientRegistry clientRegistry,
                      CommandRegistry commandRegistry, List<CommandSupplier> commandSuppliers,
                      List<ListenerSupplier> listenerSuppliers, List<DiscordSubscriber> discordSubscribers) {
        this.botMapper = botMapper;
        this.botRepository = botRepository;
        this.clientRegistry = clientRegistry;
        this.commandRegistry = commandRegistry;
        this.commandSuppliers = commandSuppliers;
        this.listenerSuppliers = listenerSuppliers;
        this.discordSubscribers = discordSubscribers;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        Discord4J.enableJettyLogging();
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
        if (clientRegistry.getClients().containsKey(bot)) {
            throw new IllegalStateException("Bot is already logged in");
        }
        IDiscordClient client = new ClientBuilder()
            .withToken(bot.getToken())
            .setDaemon(bot.isDaemon())
            .withPingTimeout(bot.getMaxMissedPings())
            .setMaxReconnectAttempts(bot.getMaxReconnectAttempts())
            .login();
        clientRegistry.getClients().put(bot, client);

        for (DiscordSubscriber subscriber : discordSubscribers) {
            log.debug("[{}] Registering subscriber: {}", bot.getName(), subscriber.getClass().getSimpleName());
            client.getDispatcher().registerListener(subscriber);
        }

        for (ListenerSupplier supplier : listenerSuppliers) {
            List<IListener<?>> listeners = supplier.getListeners();
            for (IListener<?> listener : listeners) {
                log.debug("[{}] Registering listener: {}", bot.getName(), listener.getClass().getSimpleName());
                client.getDispatcher().registerListener(listener);
            }
        }

        for (CommandSupplier supplier : commandSuppliers) {
            List<Command> commands = supplier.getCommands().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            log.debug("[{}] Registering commands: {}", bot.getName(), commands.stream()
                .map(Command::getName)
                .collect(Collectors.joining(", ")));
            commandRegistry.addAll(client, commands);
        }

        return client;
    }

    private IDiscordClient logout(Bot bot) throws DiscordException {
        log.debug("Request to logout: {}", bot);
        if (!clientRegistry.getClients().containsKey(bot)) {
            throw new IllegalStateException("Bot is not logged in");
        }

        IDiscordClient client = clientRegistry.getClients().get(bot);
        if (client != null) {
            if (client.isLoggedIn()) {
                client.logout();
            } else {
                log.warn("Bot {} is not logged in", bot.getName());
            }
            clientRegistry.getClients().remove(bot);
            commandRegistry.remove(client);
        }

        return client;
    }

    private void reset(Bot bot) {
        log.debug("Request to reset: {}", bot);
        try {
            logout(bot);
        } catch (DiscordException e) {
            log.warn("Could not logout: {}", bot, e);
        }
        commandRegistry.remove(clientRegistry.getClients().remove(bot));
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
        if (bot.isPrimary() != null && bot.isPrimary()) {
            Optional<Bot> primary = botRepository.findByPrimaryIsTrue();
            if (primary.isPresent() && !primary.get().equals(bot)) {
                Bot primaryBot = primary.get();
                primaryBot.setPrimary(false);
                botRepository.save(primaryBot);
            }
        }
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
}
