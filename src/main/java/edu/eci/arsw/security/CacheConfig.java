package edu.eci.arsw.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de cachés de la aplicación.
 */
@Configuration
@EnableCaching
public class CacheConfig {

        /**
         * Configuración del caché rolesByBearer
         * 
         * @return Gestor de cachés
         */
        @Bean
        public CacheManager cacheManager(Environment env) {
                // Configuración del caché rolesByBearer
                int rolesTtl = Integer.parseInt(env.getProperty("roles.cache.ttl-seconds", "240"));
                int rolesMaxSize = Integer.parseInt(env.getProperty("roles.cache.max-size", "10000"));

                Caffeine<Object, Object> rolesCaffeine = Caffeine.newBuilder()
                                .maximumSize(rolesMaxSize)
                                .expireAfterWrite(rolesTtl, TimeUnit.SECONDS)
                                .recordStats();

                CaffeineCache rolesCache = new CaffeineCache("rolesByBearer", rolesCaffeine.build());

                // userPublicProfiles (para /public/profile)
                int profilesTtl = Integer.parseInt(
                                env.getProperty("profiles.cache.ttl-seconds",
                                                env.getProperty("userPublicProfiles.cache.ttl-seconds",
                                                                String.valueOf(rolesTtl))));
                int profilesMaxSize = Integer.parseInt(
                                env.getProperty("profiles.cache.max-size",
                                                env.getProperty("userPublicProfiles.cache.max-size",
                                                                String.valueOf(rolesMaxSize))));

                Caffeine<Object, Object> profilesCaffeine = Caffeine.newBuilder()
                                .maximumSize(profilesMaxSize)
                                .expireAfterWrite(profilesTtl, TimeUnit.SECONDS)
                                .recordStats();

                CaffeineCache profilesCache = new CaffeineCache("userPublicProfiles", profilesCaffeine.build());

                // Registrar ambos cachés
                SimpleCacheManager manager = new SimpleCacheManager();
                manager.setCaches(Arrays.asList(
                                rolesCache,
                                profilesCache));
                return manager;
        }
}
