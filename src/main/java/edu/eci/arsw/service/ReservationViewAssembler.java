package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.security.UsersPublicClient;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
/**
 * Ensamblador para convertir entidades Reservation a vistas ReservationView
 */
@Service
@RequiredArgsConstructor
public class ReservationViewAssembler {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private final UsersPublicClient usersPublicClient;
    /** Convertir una entidad Reservation a una vista ReservationView */
    public ReservationView toView(Reservation r) {
        // Tenemos IDs en la entidad => consultamos por id (sub = null)
        PublicProfile student = usersPublicClient.getPublicProfileCached(null, r.getStudentId());
        PublicProfile tutor   = usersPublicClient.getPublicProfileCached(null, r.getTutorId());

        return ReservationView.builder()
                .id(r.getId())
                .tutorId(r.getTutorId())
                .studentId(r.getStudentId())
                .date(r.getDate())
                .start(r.getStart())
                .end(r.getEnd())
                .status(materializeStatus(r))
                .attended(r.getAttended())
                .studentName(student != null && student.getName() != null ? student.getName() : "Estudiante")
                .studentAvatar(student != null ? student.getAvatarUrl() : null)
                .tutorName(tutor != null && tutor.getName() != null ? tutor.getName() : "Tutor")
                .tutorAvatar(tutor != null ? tutor.getAvatarUrl() : null)
                .build();
    }

    private String materializeStatus(Reservation r) {
        ReservationStatus st = r.getStatus();
        if (st == null) return null;

        // Si ya pasó la hora y no se reportó asistencia => INCUMPLIDA
        if ((st == ReservationStatus.ACEPTADO )
                && isPast(r.getDate(), r.getEnd(), ZONE)
                && (r.getAttended() == null || !r.getAttended())) {
            return ReservationStatus.INCUMPLIDA.name();
        }
        return st.name();
    }

    private static boolean isPast(LocalDate date, LocalTime time, ZoneId zone) {
        return ZonedDateTime.of(date, time, zone).isBefore(ZonedDateTime.now(zone));
    }
}
