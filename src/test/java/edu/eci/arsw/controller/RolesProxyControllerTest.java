package edu.eci.arsw.controller;

import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesProxyControllerTest {

    @Mock
    private AuthorizationService authz;

    @InjectMocks
    private RolesProxyController controller;

    private static final String TOKEN = "Bearer token";

    @Test
    void myRoles_shouldReturnRolesFromAuthService() {
        RolesResponse me = new RolesResponse();
        me.setId("user-1");

        when(authz.me(TOKEN)).thenReturn(me);

        ResponseEntity<RolesResponse> response = controller.myRoles(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("user-1", response.getBody().getId());
    }

    @Test
    void myRoles_shouldReturnOkEvenWithoutRoles() {
        RolesResponse me = new RolesResponse();
        when(authz.me(TOKEN)).thenReturn(me);

        ResponseEntity<RolesResponse> response = controller.myRoles(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void myRoles_shouldFailWhenAuthServiceThrows() {
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.myRoles(TOKEN));
    }

    @Test
    void myRoles_shouldCallAuthServiceWithSameToken() {
        RolesResponse me = new RolesResponse();
        when(authz.me(TOKEN)).thenReturn(me);

        controller.myRoles(TOKEN);

        verify(authz).me(TOKEN);
    }
}
