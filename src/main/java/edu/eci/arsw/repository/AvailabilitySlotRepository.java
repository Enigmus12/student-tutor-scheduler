package edu.eci.arsw.repository;

import edu.eci.arsw.domain.AvailabilitySlot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
/**
 * Repositorio para gestionar los intervalos de disponibilidad de tutores en la base de datos MongoDB
 */
public interface AvailabilitySlotRepository extends MongoRepository<AvailabilitySlot, String> {
    List<AvailabilitySlot> findByTutorIdAndDateBetween(String tutorId, LocalDate from, LocalDate to);
    Optional<AvailabilitySlot> findByTutorIdAndDateAndStart(String tutorId, LocalDate date, LocalTime start);
    List<AvailabilitySlot> findByTutorIdAndDate(String tutorId, LocalDate date);
    void deleteByTutorIdAndDate(String tutorId, LocalDate date);
}
