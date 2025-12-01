package edu.eci.arsw.repository;

import edu.eci.arsw.domain.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar reservas 
 */
public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByStudentIdAndDateBetween(String studentId, LocalDate from, LocalDate to);

    List<Reservation> findByTutorIdAndDateBetween(String tutorId, LocalDate from, LocalDate to);

    Optional<Reservation> findByTutorIdAndDateAndStart(String tutorId, LocalDate date, LocalTime start);

    boolean existsByStudentIdAndDateAndStart(String studentId, LocalDate date, LocalTime start);

    boolean existsByTutorIdAndDateAndStart(String tutorId, LocalDate date, LocalTime start);

    List<Reservation> findByTutorIdAndDateGreaterThanEqualAndDateLessThanEqual(
            String tutorId, LocalDate from, LocalDate to);

    List<Reservation> findByTutorId(String tutorId);

    List<Reservation> findByStudentId(String studentId);

    List<Reservation> findByTutorIdOrderByDateAscStartAsc(String tutorId);

    List<Reservation> findByStudentIdOrderByDateAscStartAsc(String studentId);
}
