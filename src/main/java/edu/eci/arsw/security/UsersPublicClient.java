package edu.eci.arsw.security;

import edu.eci.arsw.dto.PublicProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Cliente para comunicarse con el servicio de usuarios y obtener perfiles
 * públicos
 */
@Slf4j
@Component
public class UsersPublicClient {

    private final CacheManager cacheManager;
    private final WebClient webClient;
    private final String profilePath;

    /**
     * Constructor del cliente de servicio de usuarios
     */
    @Autowired
    public UsersPublicClient(
            CacheManager cacheManager,
            @Value("${user.service.base-url}") String baseUrl,
            @Value("${user.service.profile-path:/Api-user/public/profile}") String profilePath) {
        this.cacheManager = cacheManager;
        this.profilePath = profilePath;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Llama al endpoint público sin caché */
    public Mono<PublicProfile> getPublicProfile(String sub, String id) {
        String subTrim = trimToNull(sub);
        String idTrim = trimToNull(id);
        if (subTrim == null && idTrim == null) {
            return Mono.error(new IllegalArgumentException("Debe proporcionar 'sub' o 'id'"));
        }

        return webClient.get()
                .uri(b -> {
                    var ub = b.path(profilePath);
                    if (subTrim != null)
                        ub.queryParam("sub", subTrim);
                    if (idTrim != null)
                        ub.queryParam("id", idTrim);
                    return ub.build();
                })
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::toProfile)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .filter(ex -> !(ex instanceof WebClientResponseException.Unauthorized
                                        || ex instanceof WebClientResponseException.Forbidden)));
    }

    /** Con caché (igual patrón que getMyRolesCached) */
    public PublicProfile getPublicProfileCached(String sub, String id) {
        String subTrim = trimToNull(sub);
        String idTrim = trimToNull(id);
        if (subTrim == null && idTrim == null) {
            throw new IllegalArgumentException("Debe proporcionar 'sub' o 'id'");
        }

        // Clave simétrica a roles: se hashea
        String rawKey = subTrim != null ? "sub:" + subTrim : "id:" + idTrim;
        String key = Sha256.hash(rawKey);
        Cache cache = cacheManager.getCache("userPublicProfiles");

        PublicProfile cached = cache != null ? cache.get(key, PublicProfile.class) : null;
        if (cached != null) {
            log.debug("profiles cache HIT");
            return normalize(cached);
        }

        try {
            PublicProfile fetched = getPublicProfile(subTrim, idTrim).block();
            if (cache != null && fetched != null)
                cache.put(key, fetched);
            log.debug("profiles cache MISS");
            return normalize(fetched);
        } catch (WebClientResponseException e) {
            int st = e.getStatusCode().value();
            if ((st == 401 || st == 403) && cache != null) {
                cache.evictIfPresent(key);
            }
            throw e;
        } catch (Exception e) {
            if (cache != null)
                cache.evictIfPresent(key);
            throw e;
        }
    }

    /** Trim a null si es vacío */
    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Convertir mapa bruto a PublicProfile */
    private PublicProfile toProfile(Map<String, Object> raw) {
        if (raw == null)
            return null;
        PublicProfile p = new PublicProfile();
        p.setId(asString(raw.get("id")));
        p.setSub(asString(raw.get("sub")));
        p.setEmail(asString(raw.get("email")));
        p.setAvatarUrl(asString(raw.get("avatarUrl")));
        p.setName(firstNonBlank(
                asString(raw.get("name")),
                asString(raw.get("fullName")),
                asString(raw.get("displayName")),
                asString(raw.get("username")),
                p.getEmail()));
        return p;
    }

    /** Convertir objeto a cadena */
    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** Primera cadena no vacía */
    private static String firstNonBlank(String... xs) {
        for (String s : xs)
            if (s != null && !s.isBlank())
                return s;
        return null;
    }

    /** Normalizar campos del perfil */
    private PublicProfile normalize(PublicProfile in) {
        if (in == null)
            return null;
        if (in.getName() != null)
            in.setName(in.getName().trim());
        if (in.getEmail() != null)
            in.setEmail(in.getEmail().trim().toLowerCase(Locale.ROOT));
        return in;
    }
}
