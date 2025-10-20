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
/**
 * Servicio para manejar las reservas de tutorías
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repo;
    private final AvailabilitySlotRepository avRepo;
    /**
     * Crear una nueva reserva
     */
    public Reservation create(String studentId, ReservationCreateRequest req) {
        LocalDate date = req.getDate();
        LocalTime start = LocalTime.parse(req.getHour()+":00");
        if (!TimeUtils.isOnTheHour(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hora debe ser HH:00");
        if (studentId.equals(req.getTutorId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tutor no puede ser el mismo que el estudiante");
        if (TimeUtils.isPast(date, start, java.time.ZoneId.of("America/Bogota"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede reservar horas pasadas");
        avRepo.findByTutorIdAndDateAndStart(req.getTutorId(), date, start)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El tutor no está disponible en ese momento"));
        if (repo.existsByStudentIdAndDateAndStart(studentId, date, start)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya tiene una reserva a esa hora");
        }
        if (repo.existsByTutorIdAndDateAndStart(req.getTutorId(), date, start)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El tutor ya tiene una reserva a esa hora");
        }

        // Si el usuario también es TUTOR y ya tiene una clase ACTIVA/ACEPTADO a esa hora, bloquear.
        if (hasActiveReservationForTutorAt(studentId, date, start)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes una sesión de tutoría a esa hora (como TUTOR)");
        }
        Reservation r = Reservation.builder()
                .studentId(studentId).tutorId(req.getTutorId())
                .date(date).start(start).end(start.plusHours(1))
                .status(ReservationStatus.ACTIVA).build();
        try {
            return repo.save(r);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "franja de disponibilidad ya reservada");
        }
    }
    /**
     * Obtener las reservas propias de un estudiante
     */
    public List<Reservation> myReservations(String studentId, LocalDate from, LocalDate to) {
        return repo.findByStudentIdAndDateBetween(studentId, from, to);
    }
    /**
     * Obtener las reservas para un tutor
     */
    public List<Reservation> reservationsForTutor(String tutorId, LocalDate from, LocalDate to) {
        return repo.findByTutorIdAndDateBetween(tutorId, from, to);
    }
    /**
     * Cambiar el estado de una reserva propia (estudiante o tutor)
     */
    public Reservation changeStatusByStudentOrTutor(String actorId, String id, ReservationStatus newStatus) {
        Reservation r = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        boolean isStudent = actorId.equals(r.getStudentId());
        boolean isTutor = actorId.equals(r.getTutorId());
        if (newStatus==ReservationStatus.ACEPTADO && !isTutor) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Solo el tutor puede aceptar");
        }
        if (!(isStudent || isTutor)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No permitido");
        }
        if (r.getStatus()==ReservationStatus.CANCELADO) return r;
        r.setStatus(newStatus);
        return repo.save(r);
    }
    /**
     * Verificar si un tutor tiene una reserva activa en una fecha y hora específicas
     */
    public boolean hasActiveReservationForTutorAt(String tutorId, LocalDate date, LocalTime start) {
        Reservation existing = repo.findByTutorIdAndDateAndStart(tutorId, date, start).orElse(null);
        if (existing==null) return false;
        return existing.getStatus()==ReservationStatus.ACTIVA || existing.getStatus()==ReservationStatus.ACEPTADO;
    }
}
