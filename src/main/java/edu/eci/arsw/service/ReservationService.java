package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationCreateRequest;
import edu.eci.arsw.infra.TimeUtils;
import edu.eci.arsw.repository.AvailabilitySlotRepository;
import edu.eci.arsw.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repo;
    private final AvailabilitySlotRepository avRepo;

    /**
     * Crear una nueva reserva.
     */
    public Reservation create(String studentId, ReservationCreateRequest req) {
        LocalDate date = req.getDate();
        LocalTime start = LocalTime.parse(req.getHour() + ":00");
        LocalTime end = start.plusHours(1);

        if (!TimeUtils.isOnTheHour(start) || !TimeUtils.isOnTheHour(end))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora debe ser exacta (HH:00)");
        if (studentId.equals(req.getTutorId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tutor no puede ser el mismo que el estudiante");
        if (TimeUtils.isPast(date, start, java.time.ZoneId.of("America/Bogota")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede reservar en una hora pasada");

        avRepo.findByTutorIdAndDateAndStart(req.getTutorId(), date, start)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El tutor no tiene disponibilidad en ese horario"));

        if (repo.existsByStudentIdAndDateAndStart(studentId, date, start) || repo.existsByTutorIdAndDateAndStart(req.getTutorId(), date, start)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una reserva en este horario");
        }

        Reservation r = Reservation.builder()
                .studentId(studentId)
                .tutorId(req.getTutorId())
                .date(req.getDate())
                .start(start)
                .end(end)
                .status(ReservationStatus.PENDIENTE) // Inicia como PENDIENTE
                .attended(null)
                .build();
        try {
            return repo.save(r);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La franja de disponibilidad ya fue reservada");
        }
    }

    /**
     * Cambiar el estado de una reserva (lógica de cancelación actualizada).
     */
    public Reservation changeStatusByStudentOrTutor(String actorId, String id, ReservationStatus newStatus) {
        Reservation r = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        boolean isStudent = actorId.equals(r.getStudentId());
        boolean isTutor = actorId.equals(r.getTutorId());

        if (!isStudent && !isTutor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta reserva");
        }

        // impedir cancelación si ya está aceptada 
        if (newStatus == ReservationStatus.CANCELADO && r.getStatus() == ReservationStatus.ACEPTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar una reserva que ya ha sido aceptada.");
        }
        // Solo se puede cancelar si está PENDIENTE
        if (newStatus == ReservationStatus.CANCELADO && r.getStatus() != ReservationStatus.PENDIENTE) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden cancelar reservas con estado PENDIENTE.");
        }
        
        // El tutor solo puede ACEPTAR si está PENDIENTE
        if (isTutor && newStatus == ReservationStatus.ACEPTADO && r.getStatus() != ReservationStatus.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden aceptar reservas con estado PENDIENTE.");
        }

        r.setStatus(newStatus);
        return repo.save(r);
    }
    
    /**
     * Verificar si un tutor tiene una reserva que bloquea su horario.
     * Una reserva PENDIENTE o ACEPTADA se considera que bloquea la disponibilidad.
     */
    public boolean hasActiveReservationForTutorAt(String tutorId, LocalDate date, LocalTime start) {
        return repo.findByTutorIdAndDateAndStart(tutorId, date, start)
                .map(r -> r.getStatus() == ReservationStatus.ACEPTADO || r.getStatus() == ReservationStatus.PENDIENTE)
                .orElse(false);
    }

    /**
     * Marcar asistencia (tutor).
     */
    public Reservation setAttended(String actorId, String id, Boolean attended) {
        Reservation r = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (!actorId.equals(r.getTutorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el tutor puede marcar la asistencia");
        }

        // Solo se puede marcar asistencia en reservas que han sido aceptadas y ya pasaron
        if (r.getStatus() != ReservationStatus.ACEPTADO && r.getStatus() != ReservationStatus.FINALIZADA && r.getStatus() != ReservationStatus.INCUMPLIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede marcar asistencia en clases aceptadas.");
        }

        if (!TimeUtils.isPast(r.getDate(), r.getEnd(), java.time.ZoneId.of("America/Bogota"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede marcar asistencia hasta que la clase haya finalizado.");
        }

        r.setAttended(attended);
        // Si se marca asistencia, la vista lo interpretará como FINALIZADA o INCUMPLIDA.
        return repo.save(r);
    }

    public List<Reservation> myReservations(String studentId, LocalDate from, LocalDate to) {
        return repo.findByStudentIdAndDateBetween(studentId, from, to);
    }

    public List<Reservation> reservationsForTutor(String tutorId, LocalDate from, LocalDate to) {
        return repo.findByTutorIdAndDateGreaterThanEqualAndDateLessThanEqual(tutorId, from, to);
    }
}