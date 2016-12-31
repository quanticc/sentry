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
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.command.Command;
import top.quantic.sentry.discord.command.CommandRegistry;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.module.ListenerSupplier;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.repository.BotRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing Bot.
 */
@Service
public class BotService implements InitializingBean, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(BotService.class);

    private final BotRepository botRepository;
    private final CommandRegistry commandRegistry;
    private final List<CommandSupplier> commandSuppliers;
    private final List<ListenerSupplier> listenerSuppliers;

    private final Map<Bot, IDiscordClient> discordClientMap = new ConcurrentHashMap<>();

    @Autowired
    public BotService(BotRepository botRepository, CommandRegistry commandRegistry, List<CommandSupplier> commandSuppliers,
                      List<ListenerSupplier> listenerSuppliers) {
        this.botRepository = botRepository;
        this.commandRegistry = commandRegistry;
        this.commandSuppliers = commandSuppliers;
        this.listenerSuppliers = listenerSuppliers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("*** Initializing BotService ***");

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

    public IDiscordClient login(Bot bot) throws DiscordException {
        log.debug("Request to login : {}", bot);
        if (discordClientMap.containsKey(bot)) {
            throw new IllegalStateException("Bot is already logged in");
        }
        IDiscordClient client = new ClientBuilder()
            .withToken(bot.getToken())
            .setDaemon(bot.isDaemon())
            .withPingTimeout(bot.getMaxMissedPings())
            .setMaxReconnectAttempts(bot.getMaxReconnectAttempts())
            .login();
        discordClientMap.put(bot, client);

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

    public void logout(Bot bot) {
        log.debug("Request to logout : {}", bot);
        if (!discordClientMap.containsKey(bot)) {
            throw new IllegalStateException("Bot is not logged in");
        }

        IDiscordClient client = discordClientMap.get(bot);
        RequestBuffer.request(() -> {
            if (client.isLoggedIn()) {
                try {
                    client.logout();
                } catch (DiscordException e) {
                    log.warn("Could not logout bot", e);
                }
            } else {
                log.warn("Bot {} is not logged in", bot.getName());
            }
            discordClientMap.remove(bot);
        });
    }

    public void reset(Bot bot) {
        log.debug("Request to reset : {}", bot);
        logout(bot);
        discordClientMap.remove(bot);
    }

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
     * Get all the bots.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<Bot> findAll(Pageable pageable) {
        log.debug("Request to get all Bots");
        Page<Bot> result = botRepository.findAll(pageable);
        return result;
    }

    /**
     * Get one bot by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public Bot findOne(String id) {
        log.debug("Request to get Bot : {}", id);
        Bot bot = botRepository.findOne(id);
        return bot;
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

    @Override
    public void destroy() throws Exception {
        log.debug("Logging out all bots");
        discordClientMap.keySet().forEach(this::logout);
    }
}
