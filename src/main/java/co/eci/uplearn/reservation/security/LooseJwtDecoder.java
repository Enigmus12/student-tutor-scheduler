package co.eci.uplearn.reservation.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class LooseJwtDecoder {

  private static final Base64.Decoder B64 = Base64.getUrlDecoder();
  private final ObjectMapper om = new ObjectMapper();

  public DecodedToken decode(String bearerToken) throws Exception {
    if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing Bearer token");
    }
    String token = bearerToken.substring("Bearer ".length()).trim();
    String[] parts = token.split("\\.");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT format");
    }

    String payloadJson = new String(B64.decode(parts[1]), StandardCharsets.UTF_8);
    @SuppressWarnings("unchecked")
    Map<String,Object> claims = om.readValue(payloadJson, Map.class);

    String sub = str(claims.get("sub"));
    String email = str(claims.get("email"));

    // roles: intentamos varias llaves comunes: cognito:groups, roles, custom:roles
    List<String> roles = extractRoles(claims);

    return new DecodedToken(sub, email, roles, claims);
  }

  private static String str(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static List<String> extractRoles(Map<String,Object> claims) {
    // 1) cognito:groups
    Object v = claims.get("cognito:groups");
    List<String> roles = tryToList(v);
    if (!roles.isEmpty()) return normalize(roles);

    // 2) roles (array o string)
    v = claims.get("roles");
    roles = tryToList(v);
    if (!roles.isEmpty()) return normalize(roles);

    // 3) custom:roles
    v = claims.get("custom:roles");
    roles = tryToList(v);
    if (!roles.isEmpty()) return normalize(roles);

    // 4) scope (espacio-separado)
    v = claims.get("scope");
    if (v instanceof String s && !s.isBlank()) {
      roles = Arrays.asList(s.split("\\s+"));
      return normalize(roles);
    }

    return List.of(); // sin roles
  }

  private static List<String> tryToList(Object v) {
    if (v == null) return List.of();
    if (v instanceof List<?> l) {
      List<String> out = new ArrayList<>();
      for (Object o : l) out.add(String.valueOf(o));
      return out;
    }
    if (v instanceof String s) {
      // separar por coma, espacio o ; si fuera un string múltiple
      String[] parts = s.split("[,;\\s]+");
      List<String> out = new ArrayList<>();
      for (String p : parts) if (!p.isBlank()) out.add(p);
      return out;
    }
    return List.of();
  }

  private static List<String> normalize(List<String> roles) {
    List<String> out = new ArrayList<>();
    for (String r : roles) out.add(r.trim());
    return out;
  }

  public record DecodedToken(
      String sub,
      String email,
      List<String> roles,
      Map<String,Object> claims
  ) {}
}
