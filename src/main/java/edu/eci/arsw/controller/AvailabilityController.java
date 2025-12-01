package edu.eci.arsw.controller;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.dto.BulkAvailabilityRequest;
import edu.eci.arsw.dto.DayAvailabilityUpdateRequest;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import edu.eci.arsw.service.AvailabilityService;
import edu.eci.arsw.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador para manejar las solicitudes relacionadas con la disponibilidad
 * de los tutores
 */
@Slf4j
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService service;
    private final AuthorizationService authz;
    private final ReservationService reservationService;
    private static final String TUTOR_ROLE = "TUTOR";

    /**
     * Crear franjas de disponibilidad en bloque
     * 
     * @param authorization Token de autorización
     * @param req           Solicitud con las franjas a crear
     * @return Lista de franjas creadas
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<AvailabilitySlot>> bulk(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody BulkAvailabilityRequest req) {
        authz.requireRole(authorization, TUTOR_ROLE);
        RolesResponse me = authz.me(authorization);
        List<AvailabilitySlot> created = service.bulkCreate(me.getId(), req);
        return ResponseEntity.ok(created);
    }

    /**
     * Obtener las franjas de disponibilidad propias
     * 
     * @param authorization Token de autorización
     * @param from          Fecha de inicio del rango
     * @param to            Fecha de fin del rango
     * @return Lista de franjas de disponibilidad
     */
    @GetMapping("/my")
    public ResponseEntity<List<AvailabilitySlot>> my(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authz.requireRole(authorization, TUTOR_ROLE);
        RolesResponse me = authz.me(authorization);
        return ResponseEntity.ok(service.mySlots(me.getId(), from, to));
    }

    /**
     * Eliminar una franja de disponibilidad propia
     * 
     * @param authorization Token de autorización
     * @param slotId        ID de la franja a eliminar
     */
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("slotId") String slotId) {
        authz.requireRole(authorization, TUTOR_ROLE);
        RolesResponse me = authz.me(authorization);
        AvailabilitySlot s = service.mySlots(me.getId(), LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                .stream()
                .filter(x -> x.getId().equals(slotId)).findFirst().orElse(null);
        boolean has = false;
        if (s != null) {
            has = reservationService.hasActiveReservationForTutorAt(me.getId(), s.getDate(), s.getStart());
        }
        service.deleteOwnSlot(me.getId(), slotId, has);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reemplazar la disponibilidad de un día específico
     * 
     * @param authorization Token de autorización
     * @param date          Fecha del día a reemplazar
     * @param req           Solicitud con las horas nuevas
     */
    @PutMapping("/day/{date}")
    public ResponseEntity<Void> replaceDay(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DayAvailabilityUpdateRequest req) {

        authz.requireRole(authorization, TUTOR_ROLE);
        RolesResponse me = authz.me(authorization);

        // Parsear horas solicitadas 
        List<LocalTime> requestedHours = req.getHours().stream()
                .map(h -> LocalTime.parse(h + ":00"))
                .toList();

        // Cargar TODAS las horas existentes de disponibilidad ese día
        List<AvailabilitySlot> existing = service.slotsForDay(me.getId(), date);

        // Marcar como protegidas TODAS las horas del día que tengan reserva activa
        java.util.Set<LocalTime> hoursWithRes = new java.util.HashSet<>();
        for (AvailabilitySlot s : existing) {
            if (reservationService.hasActiveReservationForTutorAt(me.getId(), date, s.getStart())) {
                hoursWithRes.add(s.getStart());
            }
        }

        service.replaceDay(me.getId(), date, requestedHours, hoursWithRes);
        return ResponseEntity.noContent().build();
    }

    /**
     * Agregar disponibilidad sin eliminar las existentes
     * 
     * @param authorization Token de autorización
     * @param req           Solicitud con la fecha y las horas a agregar
     * @return Mapa con información sobre la operación
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addAvailability(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody Map<String, Object> req) {
        authz.requireRole(authorization, TUTOR_ROLE);
        RolesResponse me = authz.me(authorization);

        String dateStr = (String) req.get("date");
        LocalDate date = LocalDate.parse(dateStr);
        // Validar formato de fecha
        @SuppressWarnings("unchecked")
        List<String> hourStrings = (List<String>) req.get("hours");

        log.info("📥 Recibida petición addAvailability: tutor={}, date={}, hours={}", me.getId(), date, hourStrings);
        // Parsear horas
        List<LocalTime> hours = hourStrings.stream()
                .map(this::parseHour)
                .toList();

        log.info("🕐 Horas parseadas: {}", hours);

        // Agregar sin eliminar existentes
        int added = service.addAvailability(me.getId(), date, hours);

        log.info("✅ Agregadas {} franjas para {}", added, date);

        return ResponseEntity.ok(Map.of(
                "message", "Disponibilidad agregada",
                "date", dateStr,
                "addedCount", added,
                "requestedCount", hours.size()));
    }

    /**
     * Parsear una cadena de hora en formato "HH" o "HH:mm" a LocalTime
     * 
     * @param hourStr Cadena de hora
     * @return LocalTime correspondiente
     */
    private LocalTime parseHour(String hourStr) {
        try {
            // Si ya tiene formato HH:mm o HH:mm:ss
            if (hourStr.contains(":")) {
                return LocalTime.parse(hourStr + (hourStr.length() == 5 ? ":00" : ""));
            }
            // Si es solo número: "4" o "04"
            int hour = Integer.parseInt(hourStr);
            return LocalTime.of(hour, 0);
        } catch (Exception e) {
            log.error("Error parseando hora '{}': {}", hourStr, e.getMessage());
            throw new IllegalArgumentException("Formato de hora inválido: " + hourStr);
        }
    }

}