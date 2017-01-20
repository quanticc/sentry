package top.quantic.sentry.service;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ibasco.agql.protocols.valve.source.query.pojos.SourceServer;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.ServerUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.domain.Setting;
import top.quantic.sentry.event.RconRefreshFailedEvent;
import top.quantic.sentry.repository.GameServerRepository;
import top.quantic.sentry.service.dto.GameServerDTO;
import top.quantic.sentry.service.mapper.GameServerMapper;
import top.quantic.sentry.service.util.Detector;
import top.quantic.sentry.service.util.Key;
import top.quantic.sentry.service.util.LoggingDetectorListener;
import top.quantic.sentry.service.util.Result;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static top.quantic.sentry.service.util.DateUtil.formatRelative;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

/**
 * Service Implementation for managing GameServer.
 */
@Service
public class GameServerService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(GameServerService.class);

    private final GameServerRepository gameServerRepository;
    private final GameServerMapper gameServerMapper;
    private final GameAdminService gameAdminService;
    private final GameQueryService gameQueryService;
    private final ApplicationEventPublisher publisher;
    private final MetricRegistry metricRegistry;
    private final SettingService settingService;

    private final Map<GameServer, Detector<Integer>> serverStatusMap = new ConcurrentHashMap<>();
    private final LoggingDetectorListener detectorListener = new LoggingDetectorListener();

    private int rconSayIntervalMinutes = Key.RCON_SAY_INTERVAL_MINUTES.getDefaultValue();
    private int updateAttemptsThreshold = Key.UPDATE_ATTEMPTS_ALERT_THRESHOLD.getDefaultValue();
    private int updateAttemptIntervalMinutes = Key.UPDATE_ATTEMPTS_INTERVAL_MINUTES.getDefaultValue();
    private int pingThreshold = Key.PING_ALERT_THRESHOLD.getDefaultValue();

    @Autowired
    public GameServerService(GameServerRepository gameServerRepository, GameServerMapper gameServerMapper,
                             GameAdminService gameAdminService, GameQueryService gameQueryService,
                             ApplicationEventPublisher publisher, MetricRegistry metricRegistry,
                             SettingService settingService) {
        this.gameServerRepository = gameServerRepository;
        this.gameServerMapper = gameServerMapper;
        this.gameAdminService = gameAdminService;
        this.gameQueryService = gameQueryService;
        this.publisher = publisher;
        this.metricRegistry = metricRegistry;
        this.settingService = settingService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        gameServerRepository.findAll().parallelStream().forEach(this::initServerMetrics);
    }

    //////////
    // CRUD //
    //////////

    /**
     * Save a gameServer.
     *
     * @param gameServerDTO the entity to save
     * @return the persisted entity
     */
    public GameServerDTO save(GameServerDTO gameServerDTO) {
        log.debug("Request to save GameServer : {}", gameServerDTO);
        GameServer gameServer = gameServerMapper.gameServerDTOToGameServer(gameServerDTO);
        gameServer = gameServerRepository.save(gameServer);
        initServerMetrics(gameServer);
        GameServerDTO result = gameServerMapper.gameServerToGameServerDTO(gameServer);
        return result;
    }

    /**
     * Get all the gameServers.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    public Page<GameServerDTO> findAll(Pageable pageable) {
        log.debug("Request to get all GameServers");
        Page<GameServer> result = gameServerRepository.findAll(pageable);
        return result.map(gameServer -> gameServerMapper.gameServerToGameServerDTO(gameServer));
    }

    /**
     * Get one gameServer by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public GameServerDTO findOne(String id) {
        log.debug("Request to get GameServer : {}", id);
        GameServer gameServer = gameServerRepository.findOne(id);
        GameServerDTO gameServerDTO = gameServerMapper.gameServerToGameServerDTO(gameServer);
        return gameServerDTO;
    }

    /**
     * Delete the  gameServer by id.
     *
     * @param id the id of the entity
     */
    public void delete(String id) {
        log.debug("Request to delete GameServer : {}", id);
        gameServerRepository.delete(id);
    }

    /////////////////////////////////
    // Admin panel related methods //
    /////////////////////////////////

    /**
     * Retrieve the latest data from the game server provider's admin panel.
     */
    public void refreshAll() throws IOException {
        gameAdminService.getServerDetails().forEach(this::refreshFromGameAdminData);
    }

    private void refreshFromGameAdminData(String address, Map<String, String> data) {
        GameServer server = gameServerRepository.findByAddress(address).orElseGet(this::newGameServer);
        server.setId(data.get("SUBID"));
        server.setAddress(address);
        server.setName(data.get("name"));
        try {
            initServerMetrics(gameServerRepository.save(server));
        } catch (DataIntegrityViolationException e) {
            log.warn("Unable to update server data", e);
        }
    }

    private GameServer newGameServer() {
        ZonedDateTime epoch = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault());
        GameServer server = new GameServer();
        server.setVersion(0);
        server.setExpirationDate(epoch);
        server.setExpirationCheckDate(epoch);
        server.setLastGameUpdate(epoch);
        server.setLastRconDate(epoch);
        server.setStatusCheckDate(epoch);
        server.setLastValidPing(epoch);
        return server;
    }

    //////////////////////////
    // RCON related methods //
    //////////////////////////

    @Async
    public void refreshRconPasswords() {
        log.debug("==== Refreshing RCON server passwords ====");
        // refreshing passwords of expired servers since they auto restart and change password
        long count = gameServerRepository.findMissingOrExpiredRcon().parallelStream()
            .map(this::refreshRconPassword)
            .filter(Objects::nonNull)
            .count();
        if (count == 0) {
            log.info("All RCON passwords are up-to-date");
        } else {
            log.info("{} updated their RCON passwords", inflect(count, "server"));
        }
    }

    /**
     * Crawl through the remote server panel looking for the rcon_password of the given <code>server</code>.
     *
     * @param server a GameServer
     * @return the updated GameServer, or <code>null</code> if the updated rcon_password could not be retrieved
     */
    public GameServer refreshRconPassword(GameServer server) {
        if (server == null) {
            return null;
        }
        try {
            Map<String, String> result = gameAdminService.getServerConfig(server.getId());
            log.debug("Refreshing RCON data: {}", server);
            server.setRconPassword(result.get("rcon_password")); // can be null if the server is bugged
            server.setSvPassword(result.get("sv_password")); // can be null if the server is bugged
            if (server.getRconPassword() == null || server.getSvPassword() == null) {
                publisher.publishEvent(new RconRefreshFailedEvent(server, "No data - Server could be detached"));
            } else if (result.containsKey("error")) {
                // server might be offline or under maintenance
                publisher.publishEvent(new RconRefreshFailedEvent(server, "Response: " + result.get("error")));
            }
            server.setLastRconDate(ZonedDateTime.now());
            return gameServerRepository.save(server);
        } catch (IOException e) {
            log.warn("Could not refresh RCON data for {}: {}", server.getShortNameAndAddress(), e.toString());
        }
        return null;
    }

    public long refreshExpirationDates(Map<String, Integer> expirationSeconds) {
        ZonedDateTime now = ZonedDateTime.now();
        return gameServerRepository.findByIdIn(expirationSeconds.keySet()).parallelStream()
            .map(server -> {
                int seconds = expirationSeconds.get(server.getId());
                if (seconds != 0) {
                    server.setExpirationDate(now.plusSeconds(seconds));
                }
                server.setExpirationCheckDate(now);
                return server;
            })
            .map(gameServerRepository::save)
            .count();
    }

    public String rcon(GameServer server, String cmd) {
        InetSocketAddress address = getInetSocketAddress(server);
        String password = server.getRconPassword();
        String command = cleanCommand(cmd);

        if (!gameQueryService.isAuthenticated(address)) {
            gameQueryService.authenticate(address, password)
                .handle((status, error) -> {
                    if (error != null) {
                        return gameQueryService.authenticate(address, refreshPasswordAndGet(server));
                    } else {
                        return status;
                    }
                }).join();
        }
        return gameQueryService.execute(address, command)
            // even if we are already authenticated the command can still fail due to a variety of reasons
            // one of them is the RCON password becoming invalid - so we need to retry at least once
            // with a refreshed password
            .handle((response, error) -> {
                if (error != null) {
                    log.warn("Could not execute command", error);
                    return gameQueryService.authenticate(address, refreshPasswordAndGet(server))
                        .thenCompose(status -> {
                            if (status.isAuthenticated()) {
                                return gameQueryService.execute(address, command);
                            } else {
                                return CompletableFuture.completedFuture("Unable to authenticate to server");
                            }
                        }).join();
                } else {
                    return response;
                }
            }).join();
    }

    public String rcon(String address, String command) {
        GameServer server = gameServerRepository.findByAddress(address)
            .orElseThrow(() -> new IllegalArgumentException("Bad address: " + address));
        return rcon(server, command);
    }

    public String rcon(String rawAddress, String password, String cmd) {
        InetSocketAddress address = getInetSocketAddress(rawAddress);
        String command = cleanCommand(cmd);

        if (!gameQueryService.isAuthenticated(address)) {
            gameQueryService.authenticate(address, password).join();
        }
        return gameQueryService.execute(address, command).join();
    }

    public Result<String> tryRcon(GameServer server, String command) {
        try {
            return Result.ok(rcon(server, command));
        } catch (Exception e) {
            log.warn("Could not execute RCON command '{}' on {}", server, command, e);
            return Result.error(e.getMessage(), e);
        }
    }

    public Result<String> tryRcon(String serverAddress, String command) {
        try {
            return Result.ok(rcon(serverAddress, command));
        } catch (Exception e) {
            log.warn("Could not execute RCON command '{}' on {}", serverAddress, command, e);
            return Result.error(e.getMessage(), e);
        }
    }

    public Result<String> tryRcon(String serverAddress, String password, String command) {
        try {
            return Result.ok(rcon(serverAddress, password, command));
        } catch (Exception e) {
            log.warn("Could not execute RCON command '{}' on {}", serverAddress, command, e);
            return Result.error(e.getMessage(), e);
        }
    }

    public String refreshPasswordAndGet(GameServer server) {
        refreshRconPassword(server);
        return server.getRconPassword();
    }

    private String cleanCommand(String command) {
        String prefix = "rcon ";
        if (command.startsWith(prefix)) {
            command = command.substring(prefix.length());
        }
        return command;
    }

    ///////////////////////////////
    // Update related operations //
    ///////////////////////////////

    public int getLatestVersion() {
        return gameQueryService.getServerUpdateStatus()
            .thenApply(ServerUpdateStatus::getRequiredVersion)
            .join();
    }

    public List<GameServer> findOutdatedServers() {
        return gameServerRepository.findByVersionLessThan(getLatestVersion());
    }

    @Async
    public void updateGameServers() {
        log.debug("==== Refreshing server status ====");
        //int latestVersion = getLatestVersion();
        // check for availability of game update from GS side
        long refreshed = gameServerRepository.findAll().parallelStream()
            .map(this::refreshStatus)
            .map(gameServerRepository::save)
            .count();
        List<GameServer> outdated = findOutdatedServers();
        if (!outdated.isEmpty()) {
            refreshSettings();
            long updating = outdated.parallelStream()
                .map(this::executeGameUpdate)
                .map(gameServerRepository::save)
                .count();
            if (updating == 0) {
                log.debug("All servers up-to-date");
                // reset updating flag
                gameServerRepository.findAll().parallelStream()
                    .forEach(server -> {
                        server.setUpdating(false);
                        gameServerRepository.save(server);
                    });
            } else {
                log.info("Update is pending on {}", inflect(updating, "server"));
                // TODO: publish as datadog event instead
                List<GameServer> delaying = gameServerRepository
                    .findByUpdatingIsTrueAndUpdateAttemptsGreaterThan(getUpdateAttemptsThreshold());
                if (!delaying.isEmpty()) {
                    log.debug("-- Servers delaying update --\n", delaying.stream()
                        .map(this::formatDelayed)
                        .collect(Collectors.joining("\n")));
                }
            }
        }

        int unresponsive = findUnresponsiveServers().size();
        log.debug("{} had their status refreshed{}", inflect(refreshed, "server"),
            unresponsive == 0 ? "" : " (" + unresponsive + " currently unreachable)");
    }

    public GameServer refreshStatus(GameServer server) {
        if (server == null) {
            return null;
        }

        Timer timer = getDelayTimer(server);
        Timer.Context context = timer.time();
        int delay;
        try {
            SourceServer source = gameQueryService.getServerInfo(getInetSocketAddress(server)).join();
            delay = Long.valueOf(nanosToMillis(context.stop())).intValue();

            String mapName = source.getMapName();
            int players = source.getNumOfPlayers();
            int max = source.getMaxPlayers();
            String versionString = source.getGameVersion();
            int tvPort = source.getTvPort();

            Histogram histogram = getPlayerHistogram(server);
            histogram.update(players);

//            log.debug("[{}] {} {} {} v{}{} ({})", server.getShortName(),
//                leftPad(server.getAddress(), 25 - Math.min(5, server.getShortName().length())),
//                rightPad(mapName, 20), leftPad(players + " / " + max, 7),
//                versionString, tvPort > 0 ? " with SourceTV @ port " + tvPort : "", delay);

            server.setStatusCheckDate(ZonedDateTime.now());
            server.setMap(mapName);
            server.setPlayers(players);
            server.setMaxPlayers(max);
            server.setVersion(checkedParseInt(versionString));
            server.setTvPort(tvPort);
        } catch (Exception e) {
            log.warn("[{}] Failed to refresh status: {}", server, e.toString());
            delay = Long.valueOf(nanosToMillis(context.stop())).intValue();
        }

        server.setPing(delay);
        if (getStatusTracker(server).check(delay) != Detector.State.BAD) {
            server.setLastValidPing(ZonedDateTime.now());
        }
        return server;
    }

    public InetSocketAddress getInetSocketAddress(GameServer server) {
        return getInetSocketAddress(server.getAddress());
    }

    public InetSocketAddress getInetSocketAddress(String address) {
        int port = 0;
        if (address.indexOf(':') >= 0) {
            String[] tmpAddress = address.split(":", 2);
            port = Integer.parseInt(tmpAddress[1]);
            address = tmpAddress[0];
        }
        if (port == 0) {
            port = 27015;
        }
        return new InetSocketAddress(address, port);
    }

    private GameServer executeGameUpdate(GameServer server) {
        if (!server.isUpdating()) {
            server.setUpdating(true);
            server.setUpdateAttempts(1);
            server.setLastUpdateStart(ZonedDateTime.now());
        }

        // limit max update attempts per server: once per 5 minutes
        if (server.getLastGameUpdate().plusMinutes(getUpdateAttemptIntervalMinutes()).isAfter(ZonedDateTime.now())) {
            log.info("[{}] Server update is on hold. Last attempt {}", server, formatRelative(server.getLastGameUpdate()));
            return server; // don't increase # of attempts
        }

        int players = server.getPlayers();
        if (players > 0) {
            // never upgrade a server with players
            log.info("[{}] Server update is on hold. Players connected: {}", server, server.getPlayers());
            ZonedDateTime lastRconAnnounce = Optional.ofNullable(server.getLastRconAnnounce())
                .orElse(Instant.EPOCH.atZone(ZoneId.systemDefault()));
            // announce only once per interval to avoid spamming
            if (lastRconAnnounce.plusMinutes(getRconSayIntervalMinutes()).isBefore(ZonedDateTime.now())) {
                Result<String> result = tryRcon(server, "Game update on hold until all players leave the server");
                if (result.isSuccessful()) {
                    server.setLastRconAnnounce(ZonedDateTime.now());
                }
            }
        } else if (getStatusTracker(server).getState() != Detector.State.GOOD) {
            // hold servers that are offline - install in progress or a dead server?
            // TODO: consider upgrading anyway after a certain attempt # threshold
            log.info("[{}] Server update is on hold. Current state: {}", server, getStatusTracker(server).getState());
        } else {
            log.debug("[{}] Starting update attempt {}", server.getUpdateAttempts() + 1);
            try {
                if (gameAdminService.upgrade(server.getId()) == GameAdminService.Result.INSTALLING) {
                    server.setLastGameUpdate(ZonedDateTime.now());
                }
            } catch (IOException e) {
                log.warn("Could not perform game update: {}", e.toString());
            }
        }

        server.setUpdateAttempts(server.getUpdateAttempts() + 1);
        return server;
    }

    //////////////////////////////////
    // Health check related methods //
    //////////////////////////////////

    public List<GameServer> findUnresponsiveServers() {
        return gameServerRepository.findByPingGreaterThan(getPingThreshold());
    }

    public List<GameServer> findUnhealthyServers() {
        return gameServerRepository.findAll().parallelStream()
            .filter(server -> getStatusTracker(server).getState() != Detector.State.GOOD)
            .collect(Collectors.toList());
    }

    public List<GameServer> findServersWithoutRcon() {
        return gameServerRepository.findByRconPasswordIsNull();
    }

    public Map<String, String> getSummary() {
        return gameServerRepository.findAll().stream()
            .collect(Collectors.toMap(GameServer::getShortName, GameServer::getSummary));
    }

    /////////////////////////////////
    // Metric collection utilities //
    /////////////////////////////////

    private Timer getDelayTimer(GameServer server) {
        return metricRegistry.timer("UGC.GameServer.delay." + server.getShortName());
    }

    private Histogram getPlayerHistogram(GameServer server) {
        return metricRegistry.histogram("UGC.GameServer.players." + server.getShortName());
    }

    private void initServerMetrics(GameServer server) {
        initStatusGauge(server);
        initPlayerGauge(server);
    }

    private void initStatusGauge(GameServer server) {
        String key = "UGC.GameServer.status." + server.getShortName();
        if (!metricRegistry.getGauges().containsKey(key)) {
            metricRegistry.register(key, (Gauge<Integer>) () -> getStatusTracker(server).getState().ordinal());
        }
    }

    private void initPlayerGauge(GameServer server) {
        String key = "UGC.GameServer.player_count." + server.getShortName();
        if (!metricRegistry.getGauges().containsKey(key)) {
            metricRegistry.register(key, (Gauge<Integer>) server::getPlayers);
        }
    }

    private Detector<Integer> getStatusTracker(GameServer server) {
        return serverStatusMap.computeIfAbsent(server,
            key -> {
                Detector<Integer> detector = new Detector<>(key.getShortNameAndAddress(), getStatusCheckFunction());
                detector.addListener(detectorListener);
                return detector;
            });
    }

    private Function<Integer, Boolean> getStatusCheckFunction() {
        return ping -> ping < getPingThreshold();
    }

    /////////////////////////////////
    // Server search helper method //
    /////////////////////////////////

    public List<GameServer> findServers(String k) {
        String key = k.trim().toLowerCase();
        return gameServerRepository.findAll().stream()
            .filter(s -> isClaimedCase(s, key) || isUnclaimedCase(s, key)
                || containsName(s, key) || isShortName(s, key) || hasAddressLike(s, key))
            .collect(Collectors.toList());
    }

    public List<GameServer> findServersMultiple(List<String> input) {
        List<String> keys = input.stream().map(k -> k.trim().toLowerCase()).collect(Collectors.toList());
        return gameServerRepository.findAll().stream()
            .filter(s -> isClaimedCase(s, keys) || isUnclaimedCase(s, keys)
                || containsName(s, keys) || isShortName(s, keys) || hasAddressLike(s, keys))
            .collect(Collectors.toList());
    }

    private boolean isUnclaimedCase(GameServer s, List<String> keys) {
        ZonedDateTime now = ZonedDateTime.now();
        return keys.stream().anyMatch(k -> k.equals("unclaimed"))
            && now.isAfter(s.getExpirationDate());
    }

    private boolean isClaimedCase(GameServer s, List<String> keys) {
        ZonedDateTime now = ZonedDateTime.now();
        return keys.stream().anyMatch(k -> k.equals("claimed"))
            && now.isBefore(s.getExpirationDate());
    }

    private boolean isUnclaimedCase(GameServer s, String key) {
        ZonedDateTime now = ZonedDateTime.now();
        return key.equals("unclaimed") && now.isAfter(s.getExpirationDate());
    }

    private boolean isClaimedCase(GameServer s, String key) {
        ZonedDateTime now = ZonedDateTime.now();
        return key.equals("claimed") && now.isBefore(s.getExpirationDate());
    }

    private boolean containsName(GameServer server, List<String> keys) {
        return keys.stream().anyMatch(k -> containsName(server, k));
    }

    private boolean isShortName(GameServer server, List<String> keys) {
        return keys.stream().anyMatch(k -> isShortName(server, k));
    }

    private boolean hasAddressLike(GameServer server, List<String> keys) {
        return keys.stream().anyMatch(k -> hasAddressLike(server, k));
    }

    private boolean containsName(GameServer server, String key) {
        return server.getName().trim().toLowerCase().contains(key);
    }

    private boolean isShortName(GameServer server, String key) {
        return key.equals(server.getShortName());
    }

    private boolean hasAddressLike(GameServer server, String key) {
        return server.getAddress().startsWith(key);
    }

    ////////////////////////////////////////
    // Wrappers for game admin operations //
    ////////////////////////////////////////

    public String getServerConsole(GameServer server) throws IOException {
        return gameAdminService.getServerConsole(server.getId());
    }

    public Result<Void> tryRestart(GameServer server) {
        if (!isEmptyAfterRefresh(server)) {
            int count = server.getPlayers();
            log.info("Not restarting server {} due to players present: {}", server, count);
            return Result.error("Not restarting due to players present: " + count);
        } else {
            try {
                GameAdminService.Result response = gameAdminService.restart(server.getId());
                if (response == GameAdminService.Result.RESTARTED) {
                    return Result.empty("Server is restarting...");
                } else {
                    return Result.error("Could not restart: " + response);
                }
            } catch (IOException e) {
                log.warn("Could not restart server: {}", e.toString());
                return Result.error("Could not restart due to an internal error", e);
            }
        }
    }

    public Result<Void> tryUpgrade(GameServer server) {
        if (!isEmptyAfterRefresh(server)) {
            int count = server.getPlayers();
            log.info("Not upgrading server due to players present: {}", count);
            return Result.error("Not upgrading due to players present: " + count);
        } else {
            try {
                GameAdminService.Result response = gameAdminService.upgrade(server.getId());
                if (response == GameAdminService.Result.INSTALLING) {
                    return Result.empty("Server is upgrading...");
                } else {
                    return Result.error("Could not upgrade: " + response);
                }
            } catch (IOException e) {
                log.warn("Could not upgrade server: {}", e.toString());
                return Result.error("Could not upgrade due to an internal error", e);
            }
        }
    }

    public Result<Void> tryModInstall(GameServer server, String modName) {
        Setting setting = settingService.findOneByGuildAndKey("gameServer", modName).orElse(null);
        if (setting == null) {
            log.warn("Invalid mod name: {}", modName);
            return Result.error("Invalid mod name: " + modName);
        }
        if (!isEmptyAfterRefresh(server)) {
            int count = server.getPlayers();
            log.info("Not installing mod due to players present: {}", count);
            return Result.error("Not installing mod due to players present: " + count);
        } else {
            try {
                GameAdminService.Result response = gameAdminService.installMod(server.getId(), setting.getValue());
                if (response == GameAdminService.Result.INSTALLING) {
                    return Result.empty("Server is installing mod...");
                } else {
                    return Result.error("Could not install mod: " + response);
                }
            } catch (IOException e) {
                log.warn("Could not install mod to server: {}", e.toString());
                return Result.error("Could not install mod due to an internal error", e);
            }
        }
    }

    /////////////////////////////////
    // Settings related operations //
    /////////////////////////////////

    public void refreshSettings() {
        rconSayIntervalMinutes = settingService.getValueFromKey(Key.RCON_SAY_INTERVAL_MINUTES);
        updateAttemptsThreshold = settingService.getValueFromKey(Key.UPDATE_ATTEMPTS_ALERT_THRESHOLD);
        updateAttemptIntervalMinutes = settingService.getValueFromKey(Key.UPDATE_ATTEMPTS_INTERVAL_MINUTES);
        int oldPingThreshold = pingThreshold;
        pingThreshold = settingService.getValueFromKey(Key.PING_ALERT_THRESHOLD);
        if (oldPingThreshold != pingThreshold) {
            log.debug("Ping threshold was changed, resetting all flapping detectors");
            serverStatusMap.values().forEach(d -> d.setHealthCheck(getStatusCheckFunction()));
        }
    }

    public int getRconSayIntervalMinutes() {
        return rconSayIntervalMinutes;
    }

    public int getUpdateAttemptsThreshold() {
        return updateAttemptsThreshold;
    }

    public int getUpdateAttemptIntervalMinutes() {
        return updateAttemptIntervalMinutes;
    }

    public int getPingThreshold() {
        return pingThreshold;
    }

    /////////////
    // Utility //
    /////////////

    public GameServer toGameServer(GameServerDTO gameServerDTO) {
        return gameServerMapper.gameServerDTOToGameServer(gameServerDTO);
    }

    public GameServerDTO toDTO(GameServer gameServer) {
        return gameServerMapper.gameServerToGameServerDTO(gameServer);
    }

    private Integer checkedParseInt(Object value) {
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.debug("Invalid format of value: {}", value);
            return null;
        }
    }

    private long nanosToMillis(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }

    private String formatDelayed(GameServer server) {
        return String.format("[%s] %d attempts since %s (%s)", server, server.getUpdateAttempts(),
            server.getLastUpdateStart(), formatRelative(server.getLastUpdateStart()));
    }

    private boolean isEmptyAfterRefresh(GameServer server) {
        server = refreshStatus(server);
        return server.getPlayers() == 0;
    }
}
