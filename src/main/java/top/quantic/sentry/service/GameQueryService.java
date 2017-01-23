package top.quantic.sentry.service;

import com.ibasco.agql.protocols.valve.source.query.SourceRconAuthStatus;
import com.ibasco.agql.protocols.valve.source.query.client.SourceQueryClient;
import com.ibasco.agql.protocols.valve.source.query.client.SourceRconClient;
import com.ibasco.agql.protocols.valve.source.query.exceptions.RconNotYetAuthException;
import com.ibasco.agql.protocols.valve.source.query.pojos.SourcePlayer;
import com.ibasco.agql.protocols.valve.source.query.pojos.SourceServer;
import com.ibasco.agql.protocols.valve.steam.webapi.SteamWebApiClient;
import com.ibasco.agql.protocols.valve.steam.webapi.enums.VanityUrlType;
import com.ibasco.agql.protocols.valve.steam.webapi.interfaces.SteamApps;
import com.ibasco.agql.protocols.valve.steam.webapi.interfaces.SteamUser;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.ServerUpdateStatus;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamBanStatus;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamPlayerProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.quantic.sentry.service.util.Key;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GameQueryService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GameQueryService.class);
    private static final Pattern COMMUNITY_URL = Pattern.compile("(https?://steamcommunity\\.com/)(id|profiles)/([\\w-]+)/?");
    private static final int APP_ID = 440;
    private static final int DEFAULT_VERSION = 1;

    private final SteamWebApiClient steamWebApiClient;
    private final SettingService settingService;

    private final SteamApps steamApps;
    private final SteamUser steamUser;
    private final SourceQueryClient sourceQueryClient;
    private final SourceRconClient sourceRconClient;

    private ServerUpdateStatus cachedVersion = null;
    private long lastVersionCheck = 0L;
    private long lastSettingsCheck = -1L;
    private int cacheRefreshIntervalMinutes = Key.CACHE_REFRESH_INTERVAL_MINUTES.getDefaultValue();
    private int cacheExpirationIntervalMinutes = Key.CACHE_EXPIRATION_INTERVAL_MINUTES.getDefaultValue();
    private int versionCacheExpirationMinutes = Key.VERSION_CACHE_EXPIRATION_MINUTES.getDefaultValue();

    @Autowired
    public GameQueryService(SteamWebApiClient steamWebApiClient, SettingService settingService) {
        this.steamWebApiClient = steamWebApiClient;
        this.settingService = settingService;
        this.steamApps = new SteamApps(steamWebApiClient);
        this.steamUser = new SteamUser(steamWebApiClient);
        this.sourceQueryClient = new SourceQueryClient();
        this.sourceRconClient = new SourceRconClient();
    }

    public void refreshSettings() {
        if (settingService.isInvalidated(lastSettingsCheck)) {

            int oldCacheRefresh = cacheRefreshIntervalMinutes;
            int oldCacheExpiration = cacheExpirationIntervalMinutes;

            cacheRefreshIntervalMinutes = settingService.getValueFromKey(Key.CACHE_REFRESH_INTERVAL_MINUTES);
            cacheExpirationIntervalMinutes = settingService.getValueFromKey(Key.CACHE_EXPIRATION_INTERVAL_MINUTES);
            versionCacheExpirationMinutes = settingService.getValueFromKey(Key.VERSION_CACHE_EXPIRATION_MINUTES);

            if (oldCacheRefresh != cacheRefreshIntervalMinutes) {
                sourceQueryClient.setCacheRefreshInterval(Duration.ofMinutes(cacheRefreshIntervalMinutes));
            }

            if (oldCacheExpiration != cacheExpirationIntervalMinutes) {
                sourceQueryClient.setCacheExpiration(cacheExpirationIntervalMinutes);
            }

            lastSettingsCheck = settingService.getLastUpdate();
        }
    }

    // Steam Web API - Apps

    public ServerUpdateStatus getCachedVersion() {
        return cachedVersion;
    }

    public synchronized CompletableFuture<ServerUpdateStatus> getServerUpdateStatus() {
        if (cachedVersion == null || hasCachedVersionExpired()) {
            refreshSettings();
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

    // Steam Web API - User

    public CompletableFuture<SteamPlayerProfile> getPlayerProfile(Long steamId64) {
        return steamUser.getPlayerProfile(steamId64);
    }

    public CompletableFuture<Long> getSteamId64(String key) {
        if (key.matches("[0-9]+")) {
            return CompletableFuture.completedFuture(Long.parseLong(key));
        } else if (key.matches("^STEAM_[0-1]:[0-1]:[0-9]+$")) {
            String[] tmpId = key.substring(8).split(":");
            return CompletableFuture.completedFuture(Long.valueOf(tmpId[0]) + Long.valueOf(tmpId[1]) * 2 + 76561197960265728L);
        } else if (key.matches("^U:[0-1]:[0-9]+$")) {
            String[] tmpId = key.substring(2, key.length()).split(":");
            return CompletableFuture.completedFuture(Long.valueOf(tmpId[0]) + Long.valueOf(tmpId[1]) + 76561197960265727L);
        } else if (key.matches("^\\[U:[0-1]:[0-9]+]+$")) {
            String[] tmpId = key.substring(3, key.length() - 1).split(":");
            return CompletableFuture.completedFuture(Long.valueOf(tmpId[0]) + Long.valueOf(tmpId[1]) + 76561197960265727L);
        } else {
            Matcher matcher = COMMUNITY_URL.matcher(key);
            if (matcher.matches()) {
                String type = matcher.group(2);
                String value = matcher.group(3);
                if (type.equalsIgnoreCase("profiles")) {
                    return CompletableFuture.completedFuture(Long.parseLong(value));
                } else {
                    return steamUser.getSteamIdFromVanityUrl(value, VanityUrlType.DEFAULT);
                }
            } else {
                return steamUser.getSteamIdFromVanityUrl(key, VanityUrlType.DEFAULT);
            }
        }
    }

    public CompletableFuture<List<SteamBanStatus>> getPlayerBans(Long steamId64) {
        return steamUser.getPlayerBans(steamId64);
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
