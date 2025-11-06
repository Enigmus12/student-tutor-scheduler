package edu.eci.arsw.controller;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationCreateRequest;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import edu.eci.arsw.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;

import jakarta.validation.Valid;

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
    private final MongoTemplate mongo;

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
    public List<Reservation> my(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        RolesResponse me = authz.me(authorization);
        final String studentId = me.getId();
        Query q = new Query().addCriteria(Criteria.where("studentId").is(studentId));

        Criteria c = Criteria.where("date");
        if (StringUtils.hasText(from))
            c = c.gte(from);
        if (StringUtils.hasText(to))
            c = c.lte(to);
        q.addCriteria(c);

        q.with(Sort.by(Sort.Direction.ASC, "date").and(Sort.by("start")));
        return mongo.find(q, Reservation.class);
    }

    /**
     * Obtener las reservas para el tutor autenticado
     */
    @GetMapping("/for-me")
    public List<Reservation> forMe(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        RolesResponse me = authz.me(authorization);
        final String tutorId = me.getId();
        Query q = new Query().addCriteria(Criteria.where("tutorId").is(tutorId));

        Criteria c = Criteria.where("date");
        if (StringUtils.hasText(from))
            c = c.gte(from);
        if (StringUtils.hasText(to))
            c = c.lte(to);
        q.addCriteria(c);

        q.with(Sort.by(Sort.Direction.ASC, "date").and(Sort.by("start")));
        return mongo.find(q, Reservation.class);
    }

    /**
     * Cancelar una reserva propia (estudiante o tutor)
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancel(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) { 
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.changeStatusByStudentOrTutor(me.getId(), id, ReservationStatus.CANCELADO));
    }

    /**
     * Aceptar una reserva propia 
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<Reservation> accept(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) { 
        authz.requireRole(authorization, "TUTOR");
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.changeStatusByStudentOrTutor(me.getId(), id, ReservationStatus.ACEPTADO));
    }

    /** Marcar asistencia (tutor o estudiante) */
    @PatchMapping("/{id}/attended")
    public ResponseEntity<Reservation> attended(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id, 
            @RequestParam("value") Boolean value) {
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.setAttended(me.getId(), id, value));
    }
}