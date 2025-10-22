package edu.eci.arsw.controller;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationCreateRequest;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import edu.eci.arsw.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para manejar las solicitudes de reserva
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;
    private final AuthorizationService authz;

    /**
     * Añadir métodos para manejar las solicitudes de reserva
     */
    @PostMapping
    public ResponseEntity<Reservation> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ReservationCreateRequest req) {
        authz.requireRole(authorization, "STUDENT");
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.create(me.getId(), req));
    }

    /**
     * Obtener las reservas propias
     */
    @GetMapping("/my")
    public ResponseEntity<List<Reservation>> my(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authz.requireRole(authorization, "STUDENT");
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.myReservations(me.getId(), from, to));
    }

    /**
     * Obtener las reservas para el tutor autenticado
     */
    @GetMapping("/for-me")
    public ResponseEntity<List<Reservation>> forMe(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authz.requireRole(authorization, "TUTOR");
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.reservationsForTutor(me.getId(), from, to));
    }

    /**
     * Cancelar una reserva propia (estudiante o tutor)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancel(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) {

        // Método local que decodifica el JWT y devuelve el "sub"
        String actorId = authz.subject(authorization);
        return ResponseEntity.ok(
                service.changeStatusByStudentOrTutor(actorId, id, ReservationStatus.CANCELADO));
    }

    /**
     * Aceptar una reserva propia (tutor)
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<Reservation> accept(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) {
        authz.requireRole(authorization, "TUTOR");
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(
                service.changeStatusByStudentOrTutor(me.getId(), id, ReservationStatus.ACEPTADO));
    }
}
