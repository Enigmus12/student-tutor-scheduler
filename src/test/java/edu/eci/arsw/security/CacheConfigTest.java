package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    private final CacheConfig config = new CacheConfig();

    @Test
    void cacheManagerShouldExposeExpectedCaches() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("roles.cache.ttl-seconds", "60")
                .withProperty("roles.cache.max-size", "100")
                .withProperty("profiles.cache.ttl-seconds", "120")
                .withProperty("profiles.cache.max-size", "200");

        CacheManager cm = config.cacheManager(env);

        assertTrue(cm instanceof SimpleCacheManager);
        SimpleCacheManager manager = (SimpleCacheManager) cm;
        manager.afterPropertiesSet();

        Cache roles = manager.getCache("rolesByBearer");
        Cache profiles = manager.getCache("userPublicProfiles");

        assertNotNull(roles, "rolesByBearer cache should exist");
        assertNotNull(profiles, "userPublicProfiles cache should exist");
        assertNotSame(roles, profiles);
    }
}
