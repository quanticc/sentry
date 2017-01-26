package top.quantic.sentry.config;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sentry", ignoreUnknownFields = false)
public class SentryProperties {

    private final Discord discord = new Discord();
    private final Metrics metrics = new Metrics();
    private final Http http = new Http();
    private final GameAdmin gameAdmin = new GameAdmin();
    private final GameQuery gameQuery = new GameQuery();
    private final Twitch twitch = new Twitch();

    public Discord getDiscord() {
        return discord;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Http getHttp() {
        return http;
    }

    public GameAdmin getGameAdmin() {
        return gameAdmin;
    }

    public GameQuery getGameQuery() {
        return gameQuery;
    }

    public Twitch getTwitch() {
        return twitch;
    }

    public static class Discord {

        private List<String> administrators = new ArrayList<>();
        private List<String> managers = new ArrayList<>();
        private List<String> supporters = new ArrayList<>();
        private List<String> defaultPrefixes = Lists.newArrayList("!");

        public List<String> getAdministrators() {
            return administrators;
        }

        public void setAdministrators(List<String> administrators) {
            this.administrators = administrators;
        }

        public List<String> getManagers() {
            return managers;
        }

        public void setManagers(List<String> managers) {
            this.managers = managers;
        }

        public List<String> getSupporters() {
            return supporters;
        }

        public void setSupporters(List<String> supporters) {
            this.supporters = supporters;
        }

        public List<String> getDefaultPrefixes() {
            return defaultPrefixes;
        }

        public void setDefaultPrefixes(List<String> defaultPrefixes) {
            this.defaultPrefixes = defaultPrefixes;
        }
    }

    public static class Metrics {

        private final Datadog datadog = new Datadog();

        public Datadog getDatadog() {
            return datadog;
        }

        public static class Datadog {

            private String apiKey;
            private String host;
            private int period = 10;
            private boolean enabled = false;
            private String prefix;
            private List<String> tags = new ArrayList<>();
            private List<String> expansions = new ArrayList<>();
            private boolean useRegexFilters = false;
            private List<String> includes = new ArrayList<>();
            private List<String> excludes = new ArrayList<>();

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPeriod() {
                return period;
            }

            public void setPeriod(int period) {
                this.period = period;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getPrefix() {
                return prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public List<String> getTags() {
                return tags;
            }

            public void setTags(List<String> tags) {
                this.tags = tags;
            }

            public List<String> getExpansions() {
                return expansions;
            }

            public void setExpansions(List<String> expansions) {
                this.expansions = expansions;
            }

            public boolean isUseRegexFilters() {
                return useRegexFilters;
            }

            public void setUseRegexFilters(boolean useRegexFilters) {
                this.useRegexFilters = useRegexFilters;
            }

            public List<String> getIncludes() {
                return includes;
            }

            public void setIncludes(List<String> includes) {
                this.includes = includes;
            }

            public List<String> getExcludes() {
                return excludes;
            }

            public void setExcludes(List<String> excludes) {
                this.excludes = excludes;
            }
        }
    }

    public static class Http {

        private int maxTotalConnections = 100;
        private int maxConnectionsPerRoute = 5;
        private int readTimeout = 30000;
        private int socketTimeout = 30000;
        private int maxCacheEntries = 1000;
        private long maxObjectSize = 104857600;
        private String cacheDir = "tmp/cache";

        public int getMaxTotalConnections() {
            return maxTotalConnections;
        }

        public void setMaxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
        }

        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getMaxCacheEntries() {
            return maxCacheEntries;
        }

        public void setMaxCacheEntries(int maxCacheEntries) {
            this.maxCacheEntries = maxCacheEntries;
        }

        public long getMaxObjectSize() {
            return maxObjectSize;
        }

        public void setMaxObjectSize(long maxObjectSize) {
            this.maxObjectSize = maxObjectSize;
        }

        public String getCacheDir() {
            return cacheDir;
        }

        public void setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
        }
    }

    public static class GameAdmin {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class GameQuery {

        private String steamApiKey;

        public String getSteamApiKey() {
            return steamApiKey;
        }

        public void setSteamApiKey(String steamApiKey) {
            this.steamApiKey = steamApiKey;
        }
    }

    public static class Twitch {
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

}
