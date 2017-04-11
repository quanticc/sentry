package top.quantic.sentry.config;


import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@AutoConfigureAfter(value = {MetricsConfiguration.class})
@AutoConfigureBefore(value = {WebConfigurer.class, DatabaseConfiguration.class})
public class CacheConfiguration {

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public CacheManager cacheManager() {
        log.debug("Creating Guava cache manager");
        CacheBuilder<Object, Object> thirtyMinuteBuilder = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).recordStats();
        CacheBuilder<Object, Object> oneHourBuilder = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).recordStats();
        CacheBuilder<Object, Object> threeHoursBuilder = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).recordStats();
        CacheBuilder<Object, Object> oneMonthBuilder = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.DAYS).recordStats();

        Map<String, CacheBuilder<Object, Object>> builderMap = new LinkedHashMap<>();
        builderMap.put("schedule", oneHourBuilder);
        builderMap.put("team", threeHoursBuilder);
        builderMap.put("roster", threeHoursBuilder);
        builderMap.put("results", thirtyMinuteBuilder);
        builderMap.put("legacyPlayer", threeHoursBuilder);
        builderMap.put("player", threeHoursBuilder);
        builderMap.put("banList", threeHoursBuilder);
        builderMap.put("transactions", thirtyMinuteBuilder);
        builderMap.put("overwatch", threeHoursBuilder);
        builderMap.put("permissions", oneMonthBuilder);

        GuavaCacheManager guavaCacheManager = new GuavaCacheManager() {
            @Override
            protected com.google.common.cache.Cache<Object, Object> createNativeGuavaCache(String name) {
                CacheBuilder<Object, Object> builder = builderMap.get(name);
                if (builder != null) {
                    log.debug("Creating cache for {}: {}", name, builder.toString());
                    return builder.build();
                }
                log.debug("Creating fallback cache for {}", name);
                return super.createNativeGuavaCache(name);
            }
        };
        guavaCacheManager.setCacheBuilder(thirtyMinuteBuilder);
        return guavaCacheManager;
    }
}
