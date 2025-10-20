package edu.eci.arsw.service;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.dto.BulkAvailabilityRequest;
import edu.eci.arsw.infra.TimeUtils;
import edu.eci.arsw.repository.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para manejar las franjas de disponibilidad de los tutores
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository repo;

    /**
     * Crear franjas de disponibilidad en bloque
     */
    public List<AvailabilitySlot> bulkCreate(String tutorId, BulkAvailabilityRequest req) {
        if (req.getFromDate().isAfter(req.getToDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha de inicio debe ser antes de fecha de fin");
        }
        LocalTime from = LocalTime.parse(req.getFromHour()+":00");
        LocalTime to = LocalTime.parse(req.getToHour()+":00");
        if (!TimeUtils.isOnTheHour(from) || !TimeUtils.isOnTheHour(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las horas deben estar en la hora (HH:00)");
        }
        if (!to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser mayor que la hora de inicio");
        }
        Set<DayOfWeek> dows = req.getDaysOfWeek()==null? EnumSet.allOf(DayOfWeek.class) : EnumSet.copyOf(req.getDaysOfWeek());
        List<AvailabilitySlot> created = new ArrayList<>();
        for (LocalDate d = req.getFromDate(); !d.isAfter(req.getToDate()); d = d.plusDays(1)) {
            if (!dows.contains(d.getDayOfWeek())) continue;
            for (LocalTime h = from; h.isBefore(to); h = h.plusHours(1)) {
                LocalTime end = h.plusHours(1);
                AvailabilitySlot slot = AvailabilitySlot.builder()
                        .tutorId(tutorId)
                        .date(d)
                        .start(h)
                        .end(end)
                        .build();
                try {
                    created.add(repo.save(slot));
                } catch (DuplicateKeyException e) {
                    // idempotente, ya existe
                }
            }
        }
        return created;
    }

    /**
     * Obtener las franjas de disponibilidad propias
     */
    public List<AvailabilitySlot> mySlots(String tutorId, LocalDate from, LocalDate to) {
        return repo.findByTutorIdAndDateBetween(tutorId, from, to);
    }

    /**
     * Eliminar una franja de disponibilidad propia
     */
    public void deleteOwnSlot(String tutorId, String slotId, boolean hasActiveReservation) {
        AvailabilitySlot slot = repo.findById(slotId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!slot.getTutorId().equals(tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es tu franja de disponibilidad (hora)");
        }
        if (hasActiveReservation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "franja de disponibilidad (hora) tiene reserva activa");
        }
        repo.deleteById(slotId);
    }

    /**
     * Reemplazar las franjas de disponibilidad de un día específico
     */
    public void replaceDay(String tutorId, LocalDate date, List<LocalTime> hours, Set<LocalTime> hoursWithActiveRes) {
        for (LocalTime h : hours) {
            if (!TimeUtils.isOnTheHour(h)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horas deben ser HH:00");
        }
        List<AvailabilitySlot> existing = repo.findByTutorIdAndDate(tutorId, date);
        Set<LocalTime> existingHours = existing.stream().map(AvailabilitySlot::getStart).collect(Collectors.toSet());
        for (AvailabilitySlot s : existing) {
            if (!hours.contains(s.getStart()) && !hoursWithActiveRes.contains(s.getStart())) {
                repo.deleteById(s.getId());
            }
        }
        for (LocalTime h : hours) {
            if (!existingHours.contains(h)) {
                try {
                    repo.save(AvailabilitySlot.builder()
                            .tutorId(tutorId).date(date).start(h).end(h.plusHours(1)).build());
                } catch (DuplicateKeyException ignore) {
                    // idempotent: another concurrent request may have created the same slot; safe to ignore
                }
            }
        }
    }

    /**
     * Agregar disponibilidad sin eliminar las franjas existentes
     * (Solo agrega las nuevas horas que no existan)
     */
    public void addAvailability(String tutorId, LocalDate date, List<LocalTime> hours) {
        // Validar que todas las horas sean HH:00
        for (LocalTime h : hours) {
            if (!TimeUtils.isOnTheHour(h)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Horas deben ser HH:00");
            }
        }

        // Obtener las franjas existentes para ese día
        List<AvailabilitySlot> existing = repo.findByTutorIdAndDate(tutorId, date);
        Set<LocalTime> existingHours = existing.stream()
                .map(AvailabilitySlot::getStart)
                .collect(Collectors.toSet());

        // Solo agregar las horas que NO existen
        for (LocalTime h : hours) {
            if (!existingHours.contains(h)) {
                try {
                    AvailabilitySlot newSlot = AvailabilitySlot.builder()
                            .tutorId(tutorId)
                            .date(date)
                            .start(h)
                            .end(h.plusHours(1))
                            .build();
                    repo.save(newSlot);
                    log.info("Añadido franja de disponibilidad: tutor={}, date={}, hour={}", tutorId, date, h);
                } catch (DuplicateKeyException e) {
                    // Idempotente: si ya existe por una petición concurrente, ignorar
                    log.debug("Franja de disponibilidad ya existe (creación concurrente): tutor={}, date={}, hour={}", tutorId, date, h);
                }
            } else {
                log.debug("Franja de disponibilidad ya existe, omitiendo: tutor={}, date={}, hour={}", tutorId, date, h);
            }
        }
    }
}