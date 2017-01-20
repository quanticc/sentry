package top.quantic.sentry.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Configuration
public class HttpClientConfiguration {

    @Autowired
    private SentryProperties sentryProperties;

    @Bean
    public ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }

    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(properties().getReadTimeout())
            .setSocketTimeout(properties().getSocketTimeout())
            .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties().getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(properties().getMaxConnectionsPerRoute());

        CacheConfig cacheConfig = CacheConfig.custom()
            .setMaxCacheEntries(properties().getMaxCacheEntries())
            .setMaxObjectSize(properties().getMaxObjectSize())
            .setHeuristicCachingEnabled(true) // important!
            .build();

        return CachingHttpClients.custom()
            .setCacheConfig(cacheConfig)
            .setCacheDir(new File(properties().getCacheDir()))
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();
    }

    private SentryProperties.Http properties() {
        return sentryProperties.getHttp();
    }
}
