package edu.eci.arsw.service;

import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.security.UsersPublicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio para obtener perfiles públicos de usuarios
 */
@Service
@RequiredArgsConstructor
public class PublicProfileService {

  private final UsersPublicClient client;

  /**
   * Versión síncrona (usa el caché del cliente)
   * 
   * @param sub Sub del usuario
   * @param id  ID del usuario
   * @return Perfil público del usuario
   */
  public PublicProfile get(String sub, String id) {
    return client.getPublicProfileCached(sub, id);
  }

  /**
   * Versión reactiva (sin caché adicional; el controller/servicio decide si la
   * usa)
   * 
   * @param sub Sub del usuario
   * @param id  ID del usuario
   * @return Perfil público del usuario
   */
  public Mono<PublicProfile> getAsync(String sub, String id) {
    return client.getPublicProfile(sub, id);
  }
}
