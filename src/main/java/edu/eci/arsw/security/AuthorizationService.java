package edu.eci.arsw.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Verifica si el usuario tiene alguno de los roles necesarios
     * 
     * @param bearer Token de autorización
     * @param needed Roles necesarios
     */
    public boolean hasAnyRole(String bearer, String... needed) {
        RolesResponse rr = client.getMyRolesCached(bearer);
        if (rr == null || rr.getRoles() == null)
            return false;
        Set<String> have = new HashSet<>();
        for (String r : rr.getRoles()) {
            if (r != null)
                have.add(r.toUpperCase(Locale.ROOT));
        }
        for (String n : needed) {
            if (have.contains(n.toUpperCase(Locale.ROOT)))
                return true;
        }
        return false;
    }

    /**
     * Requiere que el usuario tenga alguno de los roles necesarios, o lanza 403
     * 
     * @param bearer Token de autorización
     * @param needed Roles necesarios
     * @throws ResponseStatusException si no tiene los roles necesarios
     */
    public void requireRole(String bearer, String... needed) {
        if (!hasAnyRole(bearer, needed)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role requerido");
        }
    }

    /**
     * Obtener los roles del usuario autenticado
     * 
     * @param bearer Token de autorización
     * @return Roles del usuario
     */
    public RolesResponse me(String bearer) {
        return client.getMyRolesCached(bearer);
    }

    /**
     * Extraer el "sub" del JWT en el header Authorization
     * 
     * @param bearer Token de autorización
     * @return El "sub" del JWT
     */
    public String subject(String bearer) {
        String token = extractToken(bearer);
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT inválido");
            // Decodificar el payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(payloadJson);
            String sub = node.path("sub").asText(null);
            // Verificar que el "sub" no esté vacío
            if (sub == null || sub.isBlank())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT sin 'sub'");

            return sub;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo leer el JWT", e);
        }
    }

    /**
     * Extraer el token del header Authorization
     * 
     * @param bearer Token de autorización
     * @return El token sin el prefijo "Bearer "
     */
    private static String extractToken(String bearer) {
        if (bearer == null || bearer.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta Authorization");
        String b = bearer.trim();
        if (b.toLowerCase(Locale.ROOT).startsWith("bearer "))
            b = b.substring(7).trim();
        return b;
    }
}
