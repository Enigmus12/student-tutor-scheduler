package edu.eci.arsw.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Locale;

/**
 * Cliente para comunicarse con el servicio de usuarios y obtener roles
 */
@Slf4j
@Component
public class UserServiceClient {

    private final CacheManager cacheManager;
    private final WebClient webClient;

    /**
     * Constructor del cliente de servicio de usuarios
     */
    @Autowired
    public UserServiceClient(CacheManager cacheManager,
            @Value("${user.service.base-url}") String baseUrl) {
        this.cacheManager = cacheManager;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Obtener los roles del usuario autenticado
     */
    public Mono<RolesResponse> getMyRoles(String bearerHeader) {
        return webClient.get()
                .uri("/Api-user/my-roles")
                .header(HttpHeaders.AUTHORIZATION, bearerHeader)
                .retrieve()
                .bodyToMono(RolesResponse.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .filter(ex -> !(ex instanceof WebClientResponseException.Unauthorized
                                        || ex instanceof WebClientResponseException.Forbidden)));
    }

    /**
     * Obtener los roles del usuario autenticado con caché
     */
    public RolesResponse getMyRolesCached(String bearerHeader) {
        if (bearerHeader == null || bearerHeader.isBlank()) {
            log.error("Authorization header is null or blank");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autorización requerida");
        }
        log.debug("Fetching roles for bearer token (hash): {}", Sha256.hash(bearerHeader));

        String key = Sha256.hash(bearerHeader);
        Cache cache = cacheManager.getCache("rolesByBearer");

        RolesResponse cached = getCachedRoles(cache, key);
        if (cached != null) {
            return normalize(cached);
        }

        try {
            RolesResponse fetched = getMyRoles(bearerHeader).block();
            if (cache != null && fetched != null)
                cache.put(key, fetched);
            log.debug("roles cache MISS");
            return normalize(fetched);
        } catch (WebClientResponseException e) {
            evictOnAuthErrors(cache, key, e);
            throw e;
        } catch (Exception e) {
            evictCacheIfPresent(cache, key);
            throw e;
        }
    }

    /**
     * Obtener roles desde la caché
     */
    private RolesResponse getCachedRoles(Cache cache, String key) {
        if (cache == null)
            return null;
        RolesResponse cached = cache.get(key, RolesResponse.class);
        if (cached != null) {
            log.debug("roles cache HIT");
        }
        return cached;
    }

    /**
     * Evictar caché en caso de errores de autenticación
     */
    private void evictOnAuthErrors(Cache cache, String key, WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            evictCacheIfPresent(cache, key);
        }
    }

    /**
     * Evictar caché si está presente
     */
    private void evictCacheIfPresent(Cache cache, String key) {
        if (cache != null)
            cache.evictIfPresent(key);
    }

    /**
     * Normalizar los roles a mayúsculas
     */
    private RolesResponse normalize(RolesResponse in) {
        if (in == null)
            return null;
        if (in.getRoles() != null) {
            in.setRoles(in.getRoles().stream()
                    .map(r -> r == null ? null : r.toUpperCase(Locale.ROOT))
                    .toList());
        }
        return in;
    }
}
