package top.quantic.sentry.health;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    @Autowired
    public CacheHealthIndicator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        Map<String, Map<String, Object>> caches = cacheManager.getCacheNames().stream()
            .map(name -> (GuavaCache) cacheManager.getCache(name))
            .map(cache -> buildStats(cache.getName(), cache.getNativeCache()))
            .collect(Collectors.toMap(stats -> (String) stats.get("name"), stats -> stats));
        Health.Builder builder = Health.up()
            .withDetail("count", caches.size());
        caches.forEach(builder::withDetail);
        return builder.build();
    }

    private Map<String, Object> buildStats(String name, Cache<Object, Object> cache) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("status", "UP");
        map.put("size", cache.size());
        CacheStats stats = cache.stats();
        map.put("hitRate", stats.hitRate());
        map.put("hitCount", stats.hitCount());
        map.put("missCount", stats.missRate());
        map.put("loadSuccessCount", stats.loadSuccessCount());
        map.put("loadExceptionCount", stats.loadExceptionCount());
        map.put("totalLoadTime", stats.totalLoadTime());
        map.put("evictionCount", stats.evictionCount());
        return map;
    }
}
