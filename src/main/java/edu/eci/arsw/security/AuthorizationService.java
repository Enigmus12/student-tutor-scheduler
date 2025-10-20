package edu.eci.arsw.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
/**
 * Servicio de autorización basado en roles de usuario
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserServiceClient client;
    /**
     * Verifica si el usuario tiene alguno de los roles necesarios
     */
    public boolean hasAnyRole(String bearer, String... needed) {
        RolesResponse rr = client.getMyRolesCached(bearer);
        if (rr == null || rr.getRoles()==null) return false;
        Set<String> have = new HashSet<>();
        for (String r: rr.getRoles()) {
            if (r!=null) have.add(r.toUpperCase(Locale.ROOT));
        }
        for (String n: needed) {
            if (have.contains(n.toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }
    /**
     * Requiere que el usuario tenga alguno de los roles necesarios, o lanza 403
     */
    public void requireRole(String bearer, String... needed) {
        if (!hasAnyRole(bearer, needed)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role requerido");
        }
    }
    /**
     * Obtener los roles del usuario autenticado
     */
    public RolesResponse me(String bearer) {
        return client.getMyRolesCached(bearer);
    }
}
