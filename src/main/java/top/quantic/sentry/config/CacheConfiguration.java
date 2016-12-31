package top.quantic.sentry.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Arrays;

@Configuration
@EnableCaching
@AutoConfigureAfter(value = {MetricsConfiguration.class})
@AutoConfigureBefore(value = {WebConfigurer.class, DatabaseConfiguration.class})
public class CacheConfiguration {

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Inject
    private CacheManager cacheManager;

    @PreDestroy
    public void destroy() {
        log.info("Closing Cache Manager");
    }

//    @Bean
//    public CacheManager cacheManager() {
//        log.debug("No cache");
//        cacheManager = new NoOpCacheManager();
//        return cacheManager;
//    }

    @Bean
    public CacheManager cacheManager() {
        log.debug("Simple cache enabled");
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
            new ConcurrentMapCache("permissions"),
            new ConcurrentMapCache("prefixes")));
        cacheManager = manager;
        return cacheManager;
    }
}
