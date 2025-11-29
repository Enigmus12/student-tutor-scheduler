package edu.eci.arsw.security;

import edu.eci.arsw.dto.PublicProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersPublicClientTest {

        @Mock
        CacheManager cacheManager;

        @Mock
        Cache cache;

        private UsersPublicClient clientWithExchange(ExchangeFunction fx) throws Exception {
                UsersPublicClient c = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                WebClient webClient = WebClient.builder()
                                .baseUrl("http://localhost")
                                .exchangeFunction(fx)
                                .build();

                Field f = UsersPublicClient.class.getDeclaredField("webClient");
                f.setAccessible(true);
                f.set(c, webClient);
                return c;
        }

        @Test
        void getPublicProfileCachedShouldNormalizeFieldsAndUseCache() {
                when(cacheManager.getCache("userPublicProfiles")).thenReturn(cache);

                UsersPublicClient client = Mockito.spy(
                                new UsersPublicClient(cacheManager, "http://localhost", "/Api-user/public/profile"));

                when(cache.get(anyString(), eq(PublicProfile.class))).thenReturn(null);

                PublicProfile raw = new PublicProfile();
                raw.setId("id1");
                raw.setName("  John Doe  ");
                raw.setEmail("JOHN@EXAMPLE.COM");
                raw.setAvatarUrl("avatar");
                raw.setSub("sub-1");

                when(client.getPublicProfile(any(), any())).thenReturn(Mono.just(raw));

                PublicProfile result = client.getPublicProfileCached(null, "id1");

                assertEquals("John Doe", result.getName());
                assertEquals("john@example.com", result.getEmail());
                assertEquals("avatar", result.getAvatarUrl());
                verify(cache).put(anyString(), same(raw));
        }

        @Test
        void getPublicProfileCachedShouldThrowWhenSubAndIdBlank() {
                UsersPublicClient client = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                assertThrows(IllegalArgumentException.class,
                                () -> client.getPublicProfileCached("  ", "  "));
        }

        @Test
        void getPublicProfileCachedShouldEvictOn401or403() {
                when(cacheManager.getCache("userPublicProfiles")).thenReturn(cache);

                UsersPublicClient client = Mockito.spy(
                                new UsersPublicClient(cacheManager, "http://localhost", "/Api-user/public/profile"));

                when(cache.get(anyString(), eq(PublicProfile.class))).thenReturn(null);

                WebClientResponseException forbidden = new WebClientResponseException("forbidden",
                                HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                new HttpHeaders(),
                                new byte[0],
                                StandardCharsets.UTF_8);

                when(client.getPublicProfile(any(), any())).thenReturn(Mono.error(forbidden));

                assertThrows(WebClientResponseException.class,
                                () -> client.getPublicProfileCached(null, "id1"));
                verify(cache).evictIfPresent(anyString());
        }

        @Test
        void getPublicProfileCachedShouldUseCacheWhenHit() {
                when(cacheManager.getCache("userPublicProfiles")).thenReturn(cache);

                PublicProfile cached = new PublicProfile("id1", "sub1", "  Name  ",
                                "MAIL@EXAMPLE.COM", "avatar");
                when(cache.get(anyString(), eq(PublicProfile.class))).thenReturn(cached);

                UsersPublicClient client = Mockito.spy(
                                new UsersPublicClient(cacheManager, "http://localhost", "/Api-user/public/profile"));

                PublicProfile result = client.getPublicProfileCached("sub1", null);

                assertEquals("Name", result.getName());
                assertEquals("mail@example.com", result.getEmail());
                verify(client, never()).getPublicProfile(any(), any());
                verify(cache, never()).put(anyString(), any());
        }

        @Test
        void getPublicProfileCachedShouldEvictOnGenericException() {
                when(cacheManager.getCache("userPublicProfiles")).thenReturn(cache);

                UsersPublicClient client = Mockito.spy(
                                new UsersPublicClient(cacheManager, "http://localhost", "/Api-user/public/profile"));

                when(cache.get(anyString(), eq(PublicProfile.class))).thenReturn(null);

                RuntimeException boom = new RuntimeException("boom");
                when(client.getPublicProfile(any(), any())).thenReturn(Mono.error(boom));

                RuntimeException thrown = assertThrows(RuntimeException.class,
                                () -> client.getPublicProfileCached(null, "id1"));

                assertSame(boom, thrown);
                verify(cache).evictIfPresent(anyString());
        }

        @Test
        void internalHelpersTrimToNullAndFirstNonBlankShouldWork() throws Exception {
                Method trimToNull = UsersPublicClient.class.getDeclaredMethod("trimToNull", String.class);
                trimToNull.setAccessible(true);

                assertNull(trimToNull.invoke(null, (Object) null));
                assertNull(trimToNull.invoke(null, "   "));
                assertEquals("abc", trimToNull.invoke(null, " abc "));

                Method firstNonBlank = UsersPublicClient.class.getDeclaredMethod("firstNonBlank", String[].class);
                firstNonBlank.setAccessible(true);

                String r1 = (String) firstNonBlank.invoke(null, new Object[] { new String[] { " ", "", "x", "y" } });
                assertEquals("x", r1);

                String r2 = (String) firstNonBlank.invoke(null, new Object[] { new String[] { null, " ", "" } });
                assertNull(r2);
        }

        @Test
        void toProfileAndNormalizeShouldBuildProfileFromMap() throws Exception {
                UsersPublicClient client = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                Map<String, Object> raw = new HashMap<>();
                raw.put("id", "123");
                raw.put("sub", "sub-1");
                raw.put("name", "  ");
                raw.put("fullName", " John Doe ");
                raw.put("displayName", "DISPLAY");
                raw.put("username", "user123");
                raw.put("email", "JOHN@EXAMPLE.COM");
                raw.put("avatarUrl", "avatar");

                Method toProfile = UsersPublicClient.class.getDeclaredMethod("toProfile", Map.class);
                toProfile.setAccessible(true);
                PublicProfile p = (PublicProfile) toProfile.invoke(client, raw);

                assertEquals(" John Doe ", p.getName());
                assertEquals("JOHN@EXAMPLE.COM", p.getEmail());
                assertEquals("avatar", p.getAvatarUrl());

                Method normalize = UsersPublicClient.class.getDeclaredMethod("normalize", PublicProfile.class);
                normalize.setAccessible(true);
                PublicProfile normalized = (PublicProfile) normalize.invoke(client, p);

                assertEquals("John Doe", normalized.getName());
                assertEquals("john@example.com", normalized.getEmail());
        }

        @Test
        void toProfileShouldReturnNullWhenRawIsNull() throws Exception {
                UsersPublicClient client = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                Method toProfile = UsersPublicClient.class.getDeclaredMethod("toProfile", Map.class);
                toProfile.setAccessible(true);

                PublicProfile p = (PublicProfile) toProfile.invoke(client, new Object[] { null });
                assertNull(p);
        }

        @Test
        void normalizeShouldReturnNullWhenInputIsNull() throws Exception {
                UsersPublicClient client = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                Method normalize = UsersPublicClient.class.getDeclaredMethod("normalize", PublicProfile.class);
                normalize.setAccessible(true);

                PublicProfile result = (PublicProfile) normalize.invoke(client, new Object[] { null });
                assertNull(result);
        }

        @Test
        void getPublicProfileShouldFailWhenBothSubAndIdBlank() {
                UsersPublicClient client = new UsersPublicClient(cacheManager, "http://localhost",
                                "/Api-user/public/profile");

                Mono<PublicProfile> mono = client.getPublicProfile("   ", "   ");

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mono::block);
                assertEquals("Debe proporcionar 'sub' o 'id'", ex.getMessage());
        }

        @Test
        void getPublicProfileShouldBuildUriAndMapResponseForSubAndId() throws Exception {
                ExchangeFunction fx = request -> {
                        String json = """
                                        {
                                          "id":"123",
                                          "sub":"sub-1",
                                          "fullName":" John Doe ",
                                          "email":"MAIL@EXAMPLE.COM",
                                          "avatarUrl":"avatar"
                                        }
                                        """;

                        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .body(json)
                                        .build();
                        return Mono.just(response);
                };

                UsersPublicClient client = clientWithExchange(fx);

                PublicProfile p = client.getPublicProfile("sub-1", "123").block();
                assertNotNull(p);
                assertEquals("123", p.getId());
                assertEquals("sub-1", p.getSub());
                assertEquals(" John Doe ", p.getName());
                assertEquals("MAIL@EXAMPLE.COM", p.getEmail());

                assertNotNull(client.getPublicProfile("sub-1", null).block());
                assertNotNull(client.getPublicProfile(null, "123").block());
        }

        @Test
        void getPublicProfileShouldRetryForNonAuthError() throws Exception {
                RuntimeException boom = new RuntimeException("boom");

                ExchangeFunction fx = request -> Mono.error(boom);

                UsersPublicClient client = clientWithExchange(fx);

                RuntimeException thrown = assertThrows(RuntimeException.class,
                                () -> client.getPublicProfile("sub-1", null).block());

                assertSame(boom, thrown.getCause());

                assertTrue(thrown.getClass().getName().contains("RetryExhaustedException"));
        }

        @Test
        void getPublicProfileShouldNotRetryForUnauthorizedOrForbidden() throws Exception {
                WebClientResponseException unauthorized = WebClientResponseException.create(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                HttpHeaders.EMPTY,
                                new byte[0],
                                StandardCharsets.UTF_8);

                ExchangeFunction fx = request -> Mono.error(unauthorized);

                UsersPublicClient client = clientWithExchange(fx);

                WebClientResponseException thrown = assertThrows(WebClientResponseException.class,
                                () -> client.getPublicProfile("sub-1", null).block());

                assertSame(unauthorized, thrown);
                assertEquals(HttpStatus.UNAUTHORIZED, thrown.getStatusCode());
        }
}
