package edu.eci.arsw.controller;

import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
/**
 * Controlador proxy para obtener los roles del usuario autenticado
 */
@RestController
@RequiredArgsConstructor
public class RolesProxyController {

    private final AuthorizationService authz;
    /**
     * Obtener los roles del usuario autenticado
     */
    @GetMapping("/Api-user/my-roles")
    public ResponseEntity<RolesResponse> myRoles(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authz.me(authorization));
    }
}
