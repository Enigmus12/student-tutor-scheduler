package co.eci.uplearn.reservation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final LooseJwtDecoder decoder;

  public JwtAuthFilter(LooseJwtDecoder decoder) {
    this.decoder = decoder;
  }

 @Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
    throws ServletException, IOException {
  String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
  if (auth != null && auth.startsWith("Bearer ")) {
    try {
      var decoded = decoder.decode(auth);

      // roles que vengan en el token (si los hay)
      List<SimpleGrantedAuthority> tokenAuth = decoded.roles().stream()
          .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
          .map(SimpleGrantedAuthority::new)
          .toList();

      //  rol que manda el front (dashboard actual)
      String appRole = request.getHeader("X-App-Role"); // p.ej. "tutor"
      var allAuth = new java.util.ArrayList<>(tokenAuth);
      var rolesForPrincipal = new java.util.ArrayList<>(decoded.roles());

      if (appRole != null && !appRole.isBlank()) {
        String norm = appRole.trim();
        allAuth.add(new SimpleGrantedAuthority("ROLE_" + norm.toUpperCase()));
        rolesForPrincipal.add(norm); // “raw” para whoami
      }

      var principal = new UserPrincipal(decoded.sub(), decoded.email(), rolesForPrincipal);
      var authentication = new UsernamePasswordAuthenticationToken(principal, null, allAuth);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception e) {
      SecurityContextHolder.clearContext();
    }
  }
  filterChain.doFilter(request, response);
}
   public static class UserPrincipal {
    private final String sub;
    private final String email;
    private final List<String> roles;

    public UserPrincipal(String sub, String email, List<String> roles) {
      this.sub = sub;
      this.email = email;
      this.roles = roles;
    }

    public String getSub() { return sub; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
  }
}