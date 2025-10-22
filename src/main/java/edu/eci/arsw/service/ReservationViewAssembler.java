package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.security.UsersPublicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
/**
 * Ensamblador para convertir entidades Reservation a vistas ReservationView
 */
@Service
@RequiredArgsConstructor
public class ReservationViewAssembler {

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
                .status(r.getStatus() == null ? null : r.getStatus().name())
                .studentName(student != null && student.getName() != null ? student.getName() : "Estudiante")
                .studentAvatar(student != null ? student.getAvatarUrl() : null)
                .tutorName(tutor != null && tutor.getName() != null ? tutor.getName() : "Tutor")
                .tutorAvatar(tutor != null ? tutor.getAvatarUrl() : null)
                .build();
    }
}
