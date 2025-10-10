package co.eci.uplearn.reservation.service;

import co.eci.uplearn.reservation.domain.Reservation;
import co.eci.uplearn.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class ReservationService {
    private final ReservationRepository repo;
    private final AvailabilityService availabilityService;

    public ReservationService(ReservationRepository repo, AvailabilityService availabilityService) {
        this.repo = repo;
        this.availabilityService = availabilityService;
    }

    public Reservation create(String studentId, String tutorId, LocalDate day, LocalTime start, LocalTime end) {
        validate(day, start, end);

        if (!availabilityService.isCoveredByAvailability(tutorId, day, start, end)) {
            throw new IllegalArgumentException("Requested time is not in tutor availability");
        }
        // no overlap with other reservations for the same tutor
        if (Boolean.TRUE.equals(repo.overlaps(tutorId, day, start, end))) {
            throw new IllegalStateException("Overlapping reservation");
     }


        Reservation r = Reservation.builder()
                .studentId(studentId)
                .tutorId(tutorId)
                .day(day)
                .start(start)
                .end(end)
                .status(Reservation.Status.CONFIRMED)
                .build();
        return repo.save(r);
    }

    public List<Reservation> byTutorAndDay(String tutorId, LocalDate day) {
        return repo.findByTutorIdAndDay(tutorId, day);
    }

    public List<Reservation> byStudentAndDay(String studentId, LocalDate day) {
        return repo.findByStudentIdAndDay(studentId, day);
    }

    public List<Reservation> byStudent(String studentId) {
        return repo.findByStudentId(studentId);
    }

    public List<Reservation> byTutor(String tutorId) {
        return repo.findByTutorId(tutorId);
    }

    private void validate(LocalDate day, LocalTime start, LocalTime end) {
        if (day == null || start == null || end == null) {
            throw new IllegalArgumentException("Missing required fields");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        ZonedDateTime startZ = ZonedDateTime.of(day, start, now.getZone());
        ZonedDateTime endZ = ZonedDateTime.of(day, end, now.getZone());

        if (startZ.isBefore(now) || endZ.isBefore(now)) {
            throw new IllegalArgumentException("Reservations cannot start or end in the past");
        }
        if (startZ.isAfter(now.plusMonths(3))) {
            throw new IllegalArgumentException("Reservations can be created at most 3 months in advance");
        }
    }
}
