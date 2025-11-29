package edu.eci.arsw.service;

import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.security.UsersPublicClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {

    @Mock
    private UsersPublicClient client;

    @InjectMocks
    private PublicProfileService service;

    @Test
    void getShouldDelegateToClientCached() {
        PublicProfile profile = new PublicProfile();
        when(client.getPublicProfileCached("sub", "id")).thenReturn(profile);

        PublicProfile result = service.get("sub", "id");

        assertSame(profile, result);
    }

    @Test
    void getAsyncShouldDelegateToClient() {
        PublicProfile profile = new PublicProfile();
        when(client.getPublicProfile("sub", "id")).thenReturn(Mono.just(profile));

        PublicProfile result = service.getAsync("sub", "id").block();

        assertSame(profile, result);
    }
}
