package edu.eci.arsw.service;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.dto.ScheduleCell;
import edu.eci.arsw.repository.AvailabilitySlotRepository;
import edu.eci.arsw.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
/**
 * Servicio para manejar los horarios de los tutores
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final AvailabilitySlotRepository avRepo;
    private final ReservationRepository resRepo;
    /**
     * Obtener el horario semanal de un tutor específico
     */
    public List<ScheduleCell> weekForTutor(String tutorId, LocalDate weekStart) {
    if (tutorId == null || weekStart == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tutorId and weekStart are required");
    }
    LocalDate weekEnd = weekStart.plusDays(6);

    List<AvailabilitySlot> slots =
            java.util.Optional.ofNullable(avRepo.findByTutorIdAndDateBetween(tutorId, weekStart, weekEnd))
                    .orElse(java.util.Collections.emptyList());
    List<Reservation> res =
            java.util.Optional.ofNullable(resRepo.findByTutorIdAndDateBetween(tutorId, weekStart, weekEnd))
                    .orElse(java.util.Collections.emptyList());

    Map<String, ScheduleCell> map = new java.util.HashMap<>(slots.size() + res.size() + 7 * 24);

    for (AvailabilitySlot s : slots) {
        if (s.getDate() == null || s.getStart() == null) continue;
        var hour = s.getStart().withSecond(0).withNano(0);
        String key = s.getDate() + "_" + hour;
        map.put(key, new ScheduleCell(s.getDate().toString(), hour.toString(), "DISPONIBLE", null, null));
    }

    for (Reservation r : res) {
        if (r.getDate() == null || r.getStart() == null) continue;
        var hour = r.getStart().withSecond(0).withNano(0);
        String key = r.getDate() + "_" + hour;
        ScheduleCell cell = map.getOrDefault(key,
                new ScheduleCell(r.getDate().toString(), hour.toString(), null, null, null));
        cell.setStatus(r.getStatus() != null ? r.getStatus().name() : "ACTIVA");
        cell.setReservationId(r.getId());
        cell.setStudentId(r.getStudentId());
        map.put(key, cell);
    }

    List<ScheduleCell> result = new java.util.ArrayList<>(7 * 24);
    for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
        for (int i = 0; i < 24; i++) {
            var hour = java.time.LocalTime.of(i, 0);
            String key = d + "_" + hour;
            ScheduleCell c = map.get(key);
            if (c == null) c = new ScheduleCell(d.toString(), hour.toString(), null, null, null);
            result.add(c);
        }
    }
    return result;
}

}
