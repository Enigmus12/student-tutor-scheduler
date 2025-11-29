package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    UserServiceClient client;

    @InjectMocks
    AuthorizationService service;

    private static final String JWT_WITH_SUB =
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9.signature";

    private static final String JWT_NO_SUB =
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJmb28iOiJiYXIifQ.signature";

    @Test
    void hasAnyRoleShouldReturnTrueWhenUserHasRole() {
        RolesResponse rr = new RolesResponse();
        rr.setRoles(List.of("student", "tutor"));
        when(client.getMyRolesCached("Bearer token")).thenReturn(rr);

        assertTrue(service.hasAnyRole("Bearer token", "TUTOR"));
    }

    @Test
    void hasAnyRoleShouldReturnFalseWhenNoRolesOrNull() {
        when(client.getMyRolesCached("Bearer token")).thenReturn(null);
        assertFalse(service.hasAnyRole("Bearer token", "STUDENT"));

        RolesResponse rr = new RolesResponse();
        rr.setRoles(null);
        when(client.getMyRolesCached("Bearer token")).thenReturn(rr);
        assertFalse(service.hasAnyRole("Bearer token", "STUDENT"));
    }

    @Test
    void requireRoleShouldThrowWhenUserDoesNotHaveRole() {
        RolesResponse rr = new RolesResponse();
        rr.setRoles(List.of("STUDENT"));
        when(client.getMyRolesCached(anyString())).thenReturn(rr);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requireRole("Bearer token", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void meShouldDelegateToClient() {
        RolesResponse rr = new RolesResponse();
        when(client.getMyRolesCached("Bearer token")).thenReturn(rr);

        assertSame(rr, service.me("Bearer token"));
    }

    @Test
    void subjectShouldExtractSubFromJwtWithOrWithoutBearerPrefix() {
        String bearer = "Bearer " + JWT_WITH_SUB;
        String sub = service.subject(bearer);
        assertEquals("user-123", sub);

        String sub2 = service.subject(JWT_WITH_SUB);
        assertEquals("user-123", sub2);
    }

    @Test
    void subjectShouldThrowWhenAuthorizationMissing() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subject(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Falta Authorization", ex.getReason());
    }

    @Test
    void subjectShouldThrowOnJwtWithoutEnoughParts() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subject("invalidTokenWithoutDots"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("JWT inválido", ex.getReason());
    }

    @Test
    void subjectShouldThrowOnInvalidBase64Payload() {
        String bad = "aaa.bbb.ccc";
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subject("Bearer " + bad));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("No se pudo leer el JWT", ex.getReason());
    }

    @Test
    void subjectShouldThrowWhenJwtHasNoSub() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.subject("Bearer " + JWT_NO_SUB));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("JWT sin 'sub'", ex.getReason());
    }
}
