package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache cache;

    @Test
    void getMyRolesCachedShouldUseCacheWhenPresent() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);

        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        RolesResponse cached = new RolesResponse();
        cached.setRoles(List.of("student"));
        when(cache.get(anyString(), eq(RolesResponse.class))).thenReturn(cached);

        RolesResponse result = client.getMyRolesCached("Bearer token");

        assertNotNull(result);
        assertEquals(List.of("STUDENT"), result.getRoles());
        verify(client, never()).getMyRoles(anyString());
    }

    @Test
    void getMyRolesCachedShouldCallRemoteOnCacheMiss() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);

        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        when(cache.get(anyString(), eq(RolesResponse.class))).thenReturn(null);

        RolesResponse fetched = new RolesResponse();
        fetched.setRoles(List.of("tutor"));
        when(client.getMyRoles(anyString())).thenReturn(Mono.just(fetched));

        RolesResponse result = client.getMyRolesCached("Bearer token");

        assertEquals(List.of("TUTOR"), result.getRoles());
        verify(client).getMyRoles("Bearer token");
        verify(cache).put(anyString(), same(fetched));
    }

    @Test
    void getMyRolesCachedShouldThrowOnBlankHeader() {
        UserServiceClient client = new UserServiceClient(cacheManager, "http://localhost");

        assertThrows(ResponseStatusException.class,
                () -> client.getMyRolesCached(" "));
    }

    @Test
    void getMyRolesCachedShouldEvictCacheOn401or403() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);
        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        when(cache.get(anyString(), eq(RolesResponse.class))).thenReturn(null);

        WebClientResponseException unauthorized = new WebClientResponseException("unauth",
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                null,
                new byte[0],
                StandardCharsets.UTF_8);

        when(client.getMyRoles(anyString())).thenReturn(Mono.error(unauthorized));

        assertThrows(WebClientResponseException.class,
                () -> client.getMyRolesCached("Bearer token"));
        verify(cache).evictIfPresent(anyString());
    }

    @Test
    void getMyRolesCachedShouldNotEvictOnNonAuthWebClientError() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);
        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        when(cache.get(anyString(), eq(RolesResponse.class))).thenReturn(null);

        WebClientResponseException serverError = new WebClientResponseException("error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error",
                null,
                new byte[0],
                StandardCharsets.UTF_8);

        when(client.getMyRoles(anyString())).thenReturn(Mono.error(serverError));

        assertThrows(WebClientResponseException.class,
                () -> client.getMyRolesCached("Bearer token"));
        verify(cache, never()).evictIfPresent(anyString());
    }

    @Test
    void getMyRolesCachedShouldEvictOnGenericException() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);
        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        when(cache.get(anyString(), eq(RolesResponse.class))).thenReturn(null);

        RuntimeException boom = new RuntimeException("boom");
        when(client.getMyRoles(anyString())).thenReturn(Mono.error(boom));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> client.getMyRolesCached("Bearer token"));
        assertSame(boom, thrown);
        verify(cache).evictIfPresent(anyString());
    }

    @Test
    void getMyRolesCachedShouldWorkWhenCacheManagerReturnsNullCache() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(null);

        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        RolesResponse fetched = new RolesResponse();
        fetched.setRoles(List.of("admin"));
        when(client.getMyRoles(anyString())).thenReturn(Mono.just(fetched));

        RolesResponse result = client.getMyRolesCached("Bearer token");

        assertEquals(List.of("ADMIN"), result.getRoles());
        // como no hay caché, nunca se hace put
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void normalizeShouldKeepNullRolesEntries() {
        when(cacheManager.getCache("rolesByBearer")).thenReturn(cache);

        UserServiceClient client = Mockito.spy(new UserServiceClient(cacheManager, "http://localhost"));

        RolesResponse fetched = new RolesResponse();
        // Esta lista SÍ permite null
        fetched.setRoles(Arrays.asList(null, "student"));

        when(client.getMyRoles(anyString())).thenReturn(Mono.just(fetched));

        RolesResponse result = client.getMyRolesCached("Bearer token");

        assertEquals(2, result.getRoles().size());
        assertNull(result.getRoles().get(0));
        assertEquals("STUDENT", result.getRoles().get(1));
    }
}
