package top.quantic.sentry.service;

import com.ibasco.agql.protocols.valve.source.query.SourceRconAuthStatus;
import com.ibasco.agql.protocols.valve.source.query.client.SourceQueryClient;
import com.ibasco.agql.protocols.valve.source.query.client.SourceRconClient;
import com.ibasco.agql.protocols.valve.source.query.exceptions.RconNotYetAuthException;
import com.ibasco.agql.protocols.valve.source.query.pojos.SourcePlayer;
import com.ibasco.agql.protocols.valve.source.query.pojos.SourceServer;
import com.ibasco.agql.protocols.valve.steam.webapi.SteamWebApiClient;
import com.ibasco.agql.protocols.valve.steam.webapi.interfaces.SteamApps;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.ServerUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.quantic.sentry.service.util.Key;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GameQueryService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GameQueryService.class);

    private static final int APP_ID = 440;
    private static final int DEFAULT_VERSION = 1;

    private final SteamWebApiClient steamWebApiClient;
    private final SourceQueryClient sourceQueryClient;
    private final SteamApps steamApps;
    private final SettingService settingService;
    private final SourceRconClient sourceRconClient;

    private volatile ServerUpdateStatus cachedVersion = null;
    private volatile int versionCacheExpirationMinutes = 1;
    private volatile long lastVersionCheck = 0L;

    @Autowired
    public GameQueryService(SteamWebApiClient steamWebApiClient, SettingService settingService) {
        this.steamWebApiClient = steamWebApiClient;
        this.steamApps = new SteamApps(steamWebApiClient);
        this.settingService = settingService;
        this.sourceQueryClient = new SourceQueryClient();
        this.sourceRconClient = new SourceRconClient();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        sourceQueryClient.setCacheRefreshInterval(Duration.ofMinutes(
            settingService.getValueFromKey(Key.CACHE_REFRESH_INTERVAL_MINUTES)
        ));
        sourceQueryClient.setCacheExpiration(Duration.ofMinutes(
            settingService.getValueFromKey(Key.CACHE_EXPIRATION_INTERVAL_MINUTES)
        ));
        versionCacheExpirationMinutes = settingService.getValueFromKey(Key.VERSION_CACHE_EXPIRATION_MINUTES);
    }

    // Steam Web API

    public ServerUpdateStatus getCachedVersion() {
        return cachedVersion;
    }

    public synchronized CompletableFuture<ServerUpdateStatus> getServerUpdateStatus() {
        if (cachedVersion == null || hasCachedVersionExpired()) {
            return steamApps.getServerUpdateStatus(DEFAULT_VERSION, APP_ID)
                .whenComplete((status, error) -> {
                    if (status != null && status.isSuccess()) {
                        lastVersionCheck = System.currentTimeMillis();
                        if (cachedVersion == null || cachedVersion.getRequiredVersion() != status.getRequiredVersion()) {
                            log.info("Latest version: {}", status.getRequiredVersion());
                        }
                        cachedVersion = status;
                    }
                });
        } else {
            return CompletableFuture.completedFuture(cachedVersion);
        }
    }

    private boolean hasCachedVersionExpired() {
        return lastVersionCheck + 60 * versionCacheExpirationMinutes < System.currentTimeMillis();
    }

    // Source Query

    public CompletableFuture<SourceServer> getServerInfo(InetSocketAddress address) {
        return sourceQueryClient.getServerInfo(address);
    }

    public CompletableFuture<List<SourcePlayer>> getCachedPlayersInfo(InetSocketAddress address) {
        return sourceQueryClient.getPlayersCached(address);
    }

    public CompletableFuture<List<SourcePlayer>> getPlayersInfo(InetSocketAddress address) {
        return sourceQueryClient.getPlayers(address);
    }

    public CompletableFuture<Map<String, String>> getCachedServerRules(InetSocketAddress address) {
        return sourceQueryClient.getServerRulesCached(address);
    }

    public CompletableFuture<Map<String, String>> getServerRules(InetSocketAddress address) {
        return sourceQueryClient.getServerRules(address);
    }

    // Source RCON

    public boolean isAuthenticated(InetSocketAddress address) {
        return sourceRconClient.isAuthenticated(address);
    }

    public CompletableFuture<SourceRconAuthStatus> authenticate(InetSocketAddress address, String password) {
        return sourceRconClient.authenticate(address, password);
    }

    public CompletableFuture<String> execute(InetSocketAddress address, String command) {
        try {
            return sourceRconClient.execute(address, command);
        } catch (RconNotYetAuthException e) {
            return CompletableFuture.completedFuture(e.getMessage());
        }
    }

    @Override
    public void destroy() throws Exception {
        steamWebApiClient.close();
        sourceQueryClient.close();
        sourceRconClient.close();
    }

    private Long checkedParseLong(Object value) {
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.debug("Invalid format of value: {}", value);
            return null;
        }
    }
}
