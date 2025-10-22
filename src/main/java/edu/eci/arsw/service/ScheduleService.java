package edu.eci.arsw.service;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.dto.ScheduleCell;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Servicio para manejar los horarios de los tutores
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MongoTemplate mongo;

    /**
     * Obtener el horario semanal de un tutor específico
     */
    public List<ScheduleCell> weekForTutor(String tutorId, LocalDate weekStart) {
        if (tutorId == null || tutorId.isBlank() || weekStart == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tutorId and weekStart are required");
        }

        LocalDate weekEnd = weekStart.plusDays(6);

        List<AvailabilitySlot> slots = findAvailabilitySlots(tutorId, weekStart, weekEnd);
        List<Reservation> reservations = findReservations(tutorId, weekStart, weekEnd);

        // tabla hash para combinar disponibilidad y reservas
        Map<String, ScheduleCell> map = new HashMap<>(7 * 24);

        applyAvailability(map, slots);
        applyReservations(map, reservations);

        return buildResult(map, weekStart, weekEnd);
    }
    /** Buscar franjas de disponibilidad en MongoDB */
    private List<AvailabilitySlot> findAvailabilitySlots(String tutorId, LocalDate weekStart, LocalDate weekEnd) {
        Query qAvail = Query.query(
                Criteria.where("tutorId").is(tutorId)
                        .and("date").gte(weekStart).lte(weekEnd));
        return Optional.ofNullable(mongo.find(qAvail, AvailabilitySlot.class)).orElseGet(Collections::emptyList);
    }
    /** Buscar reservas en MongoDB */
    private List<Reservation> findReservations(String tutorId, LocalDate weekStart, LocalDate weekEnd) {
        Query qRes = Query.query(
                Criteria.where("tutorId").is(tutorId)
                        .and("date").gte(weekStart).lte(weekEnd));
        return Optional.ofNullable(mongo.find(qRes, Reservation.class)).orElseGet(Collections::emptyList);
    }
    /** Aplicar franjas de disponibilidad al mapa */
    private void applyAvailability(Map<String, ScheduleCell> map, List<AvailabilitySlot> slots) {
        for (AvailabilitySlot s : slots) {
            if (s == null || s.getDate() == null || s.getStart() == null) {
                continue;
            }
            LocalTime hour = s.getStart().withSecond(0).withNano(0);
            String key = s.getDate() + "_" + hour;
            map.put(key, new ScheduleCell(s.getDate().toString(), hour.toString(), "DISPONIBLE", null, null));
        }
    }
    /** Aplicar reservas al mapa */
    private void applyReservations(Map<String, ScheduleCell> map, List<Reservation> reservations) {
        for (Reservation r : reservations) {
            if (r == null || r.getDate() == null || r.getStart() == null) {
                continue;
            }
            LocalTime hour = r.getStart().withSecond(0).withNano(0);
            String key = r.getDate() + "_" + hour;

            ScheduleCell cell = map.getOrDefault(
                    key,
                    new ScheduleCell(r.getDate().toString(), hour.toString(), null, null, null));
            // Si la reserva trae status nulo, asumimos ACTIVA
            cell.setStatus(r.getStatus() != null ? r.getStatus().name() : "ACTIVA");
            cell.setReservationId(r.getId());
            cell.setStudentId(r.getStudentId());
            map.put(key, cell);
        }
    }
    /** Construir la lista final de celdas del horario */
    private List<ScheduleCell> buildResult(Map<String, ScheduleCell> map, LocalDate weekStart, LocalDate weekEnd) {
        List<ScheduleCell> result = new ArrayList<>(7 * 24);
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            for (int h = 0; h < 24; h++) {
                LocalTime hour = LocalTime.of(h, 0);
                String key = d + "_" + hour;
                ScheduleCell c = map.get(key);
                if (c == null) {
                    c = new ScheduleCell(d.toString(), hour.toString(), null, null, null);
                }
                result.add(c);
            }
        }
        return result;
    }
}
