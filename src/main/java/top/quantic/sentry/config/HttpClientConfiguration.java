package top.quantic.sentry.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.client.cache.FileResourceFactory;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
public class HttpClientConfiguration implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfiguration.class);

    @Autowired
    private SentryProperties sentryProperties;

    private CloseableHttpClient client;
    private ManagedHttpCacheStorage cacheStorage;

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

        File cacheDir = new File(properties().getCacheDir());
        try {
            Files.createDirectories(cacheDir.toPath());
        } catch (IOException e) {
            log.warn("Could not create cache directory - using temp folder", e);
            try {
                cacheDir = Files.createTempDirectory("cache").toFile();
            } catch (IOException ee) {
                log.warn("Could not create temp cache directory", ee);
            }
        }

        FileResourceFactory resourceFactory = new FileResourceFactory(cacheDir);
        cacheStorage = new ManagedHttpCacheStorage(cacheConfig);

        client = CachingHttpClients.custom()
            .setCacheConfig(cacheConfig)
            .setResourceFactory(resourceFactory)
            .setHttpCacheStorage(cacheStorage)
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .setUserAgent(Constants.USER_AGENT)
            .build();

        return client;
    }

    private SentryProperties.Http properties() {
        return sentryProperties.getHttp();
    }

    @Override
    public void destroy() throws Exception {
        log.debug("Closing HttpClient and CacheStorage");
        client.close();
        cacheStorage.cleanResources();
    }
}
