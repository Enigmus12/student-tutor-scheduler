package co.eci.uplearn.reservation.repository;

import co.eci.uplearn.reservation.domain.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AvailabilityRepository extends MongoRepository<Availability, String> {
    List<Availability> findByTutorIdAndDay(String tutorId, LocalDate day);
    void deleteByTutorIdAndDay(String tutorId, LocalDate day);
    boolean existsByTutorIdAndDayAndStartLessThanEqualAndEndGreaterThanEqual(
    String tutorId, LocalDate day, LocalTime start, LocalTime end);
}
