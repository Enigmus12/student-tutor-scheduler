package co.eci.uplearn.reservation.service;

import co.eci.uplearn.reservation.domain.Availability;
import co.eci.uplearn.reservation.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AvailabilityService {
    private final AvailabilityRepository repo;

    public AvailabilityService(AvailabilityRepository repo) { this.repo = repo; }

    public List<Availability> setDayAvailability(String tutorId, LocalDate day, List<Availability> slots) {
        // Replace all availability for this tutor/day
        repo.deleteByTutorIdAndDay(tutorId, day);
        List<Availability> toSave = new ArrayList<>();
        for (Availability a : slots) {
            if (!tutorId.equals(a.getTutorId())) {
                a.setTutorId(tutorId);
            }
            a.setDay(day);
            if (a.getStart() == null || a.getEnd() == null || !a.getEnd().isAfter(a.getStart())) {
                throw new IllegalArgumentException("Invalid availability interval");
            }
            toSave.add(a);
        }
        toSave.sort(Comparator.comparing(Availability::getStart));
        return repo.saveAll(toSave);
    }

    public List<Availability> getDayAvailability(String tutorId, LocalDate day) {
        return repo.findByTutorIdAndDay(tutorId, day);
    }

    public boolean isCoveredByAvailability(String tutorId, LocalDate day, LocalTime start, LocalTime end) {
        for (Availability a : getDayAvailability(tutorId, day)) {
            if (!a.getStart().isAfter(start) && !a.getEnd().isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
