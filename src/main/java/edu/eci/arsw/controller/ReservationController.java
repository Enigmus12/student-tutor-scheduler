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
import java.util.Map;

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

    // Constantes para evitar duplicación de literales
    private static final String FIELD_STUDENT_ID = "studentId";
    private static final String FIELD_TUTOR_ID = "tutorId";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_START = "start";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_TUTOR = "TUTOR";

    /** Crear una nueva reserva */
    @PostMapping
    public ResponseEntity<Reservation> create(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ReservationCreateRequest req) {
        authz.requireRole(authorization, ROLE_STUDENT);
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.create(me.getId(), req));
    }

    /** Obtener mis reservas como estudiante */
    @GetMapping("/my")
    public List<Reservation> my(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        RolesResponse me = authz.me(authorization);
        final String studentId = me.getId();
        Query q = new Query().addCriteria(Criteria.where(FIELD_STUDENT_ID).is(studentId));

        Criteria c = Criteria.where(FIELD_DATE);
        if (StringUtils.hasText(from))
            c = c.gte(from);
        if (StringUtils.hasText(to))
            c = c.lte(to);
        q.addCriteria(c);

        q.with(Sort.by(Sort.Direction.ASC, FIELD_DATE).and(Sort.by(FIELD_START)));
        return mongo.find(q, Reservation.class);
    }

    /** Obtener mis reservas como tutor */
    @GetMapping("/for-me")
    public List<Reservation> forMe(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        RolesResponse me = authz.me(authorization);
        final String tutorId = me.getId();
        Query q = new Query().addCriteria(Criteria.where(FIELD_TUTOR_ID).is(tutorId));

        Criteria c = Criteria.where(FIELD_DATE);
        if (StringUtils.hasText(from))
            c = c.gte(from);
        if (StringUtils.hasText(to))
            c = c.lte(to);
        q.addCriteria(c);

        q.with(Sort.by(Sort.Direction.ASC, FIELD_DATE).and(Sort.by(FIELD_START)));
        return mongo.find(q, Reservation.class);
    }

    /** Cancelar una reserva (estudiante o tutor) */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancel(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) {
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.changeStatusByStudentOrTutor(me.getId(), id, ReservationStatus.CANCELADO));
    }

    /** Aceptar una reserva (tutor) */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<Reservation> accept(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) {
        authz.requireRole(authorization, ROLE_TUTOR);
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.changeStatusByStudentOrTutor(me.getId(), id, ReservationStatus.ACEPTADO));
    }

    /** Marcar asistencia a una reserva (tutor) */
    @PatchMapping("/{id}/attended")
    public ResponseEntity<Reservation> attended(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id,
            @RequestParam("value") Boolean value) {
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.setAttended(me.getId(), id, value));
    }

    /**
     * Verificar si el usuario autenticado puede chatear con otro usuario
     * 
     * El usuario debe estar autenticado (token válido)
     * Debe existir al menos una reserva ACEPTADA o INCUMPLIDA entre ambos
     * El usuario autenticado debe ser parte de esa reserva (como estudiante o
     * tutor)
     * 
     * @param authorization Token de autenticación (Bearer token)
     * @param withUserId    ID del otro usuario con quien se quiere chatear
     * @return JSON con canChat (boolean) y información adicional
     */
    @GetMapping("/can-chat")
    public ResponseEntity<Map<String, Object>> canChat(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("withUserId") String withUserId) {

        // Validar autenticación y obtener ID del usuario actual
        RolesResponse me = authz.me(authorization);
        String myId = me.getId();

        // Validar que no esté intentando chatear consigo mismo
        if (myId.equals(withUserId)) {
            return ResponseEntity.ok(Map.of(
                    "canChat", false,
                    "reason", "No puedes chatear contigo mismo",
                    "myId", myId,
                    "withUserId", withUserId));
        }

        // Buscar reservas donde ambos usuarios estén relacionados
        Query q = new Query();
        q.addCriteria(new Criteria().orOperator(
                // Yo soy estudiante, el otro es tutor
                new Criteria().andOperator(
                        Criteria.where(FIELD_STUDENT_ID).is(myId),
                        Criteria.where(FIELD_TUTOR_ID).is(withUserId)),
                // Yo soy tutor, el otro es estudiante
                new Criteria().andOperator(
                        Criteria.where(FIELD_TUTOR_ID).is(myId),
                        Criteria.where(FIELD_STUDENT_ID).is(withUserId))));

        // Solo permitir chat si hay reservas ACEPTADAS o INCUMPLIDAS
        q.addCriteria(Criteria.where("status").in(
                ReservationStatus.ACEPTADO,
                ReservationStatus.INCUMPLIDA));

        List<Reservation> reservations = mongo.find(q, Reservation.class);
        boolean canChat = !reservations.isEmpty();

        // Construir respuesta detallada
        return ResponseEntity.ok(Map.of(
                "canChat", canChat,
                "myId", myId,
                "withUserId", withUserId,
                "reservationCount", reservations.size(),
                "reason", canChat
                        ? "Existe " + reservations.size() + " reserva(s) activa(s) o finalizada(s)"
                        : "No existe una reserva activa entre estos usuarios",
                "reservationIds", reservations.stream()
                        .map(Reservation::getId)
                        .limit(5)
                        .toList()));
    }

    /** Obtener una reserva por ID */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("id") String id) {

        // Autenticado
        RolesResponse me = authz.me(authorization);

        // Recupera la reserva
        Reservation r = service.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Reservation not found"));

        // Solo participante puede verla
        boolean participant = me.getId().equals(r.getStudentId()) || me.getId().equals(r.getTutorId());
        if (!participant) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not a participant");
        }

        return ResponseEntity.ok(Map.of(
                "id", r.getId(),
                "status", r.getStatus().name(), 
                FIELD_STUDENT_ID, r.getStudentId(),
                FIELD_TUTOR_ID, r.getTutorId(),
                "date", r.getDate(), 
                FIELD_START, r.getStart(), 
                "end", r.getEnd() 
        ));

    }
}