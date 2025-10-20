package edu.eci.arsw.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;
/** Configuración del gestor de caché para roles de usuario */
@Configuration
@EnableCaching
public class CacheConfig {
    /** Configuración del gestor de caché para roles de usuario */
    @Bean
    public CacheManager cacheManager(org.springframework.core.env.Environment env) {
        int ttl = Integer.parseInt(env.getProperty("roles.cache.ttl-seconds", "240"));
        int maxSize = Integer.parseInt(env.getProperty("roles.cache.max-size", "10000"));
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl, TimeUnit.SECONDS)
                .recordStats();
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rolesByBearer");
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
