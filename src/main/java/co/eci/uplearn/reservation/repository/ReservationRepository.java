package co.eci.uplearn.reservation.repository;

import co.eci.uplearn.reservation.domain.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByTutorIdAndDay(String tutorId, LocalDate day);
    List<Reservation> findByStudentIdAndDay(String studentId, LocalDate day);
    List<Reservation> findByStudentId(String studentId);
    List<Reservation> findByTutorId(String tutorId);

    @Query(value = "{ 'tutorId': ?0, 'day': ?1, 'start': { $lt: ?3 }, 'end': { $gt: ?2 } }",
         exists = true)
      Boolean overlaps(String tutorId, LocalDate day, LocalTime start, LocalTime end);
}
