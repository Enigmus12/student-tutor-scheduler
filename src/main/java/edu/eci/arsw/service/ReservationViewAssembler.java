package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.security.UsersPublicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class ReservationViewAssembler {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final UsersPublicClient usersPublicClient;

    /**
     * Convertir una entidad Reservation a una vista ReservationView con estados dinámicos.
     */
    public ReservationView toView(Reservation r) {
        PublicProfile student = usersPublicClient.getPublicProfileCached(null, r.getStudentId());
        PublicProfile tutor = usersPublicClient.getPublicProfileCached(null, r.getTutorId());

        return ReservationView.builder()
                .id(r.getId())
                .tutorId(r.getTutorId())
                .studentId(r.getStudentId())
                .date(r.getDate())
                .start(r.getStart())
                .end(r.getEnd())
                // Se usa un método para calcular el estado dinámicamente
                .status(calculateDynamicStatus(r))
                .attended(r.getAttended())
                .studentName(student != null && student.getName() != null ? student.getName() : "Estudiante")
                .studentAvatar(student != null ? student.getAvatarUrl() : null)
                .tutorName(tutor != null && tutor.getName() != null ? tutor.getName() : "Tutor")
                .tutorAvatar(tutor != null ? tutor.getAvatarUrl() : null)
                .build();
    }

    /**
     * Calcula el estado final de la reserva para mostrar en el frontend.
     */
    private String calculateDynamicStatus(Reservation r) {
        ReservationStatus storedStatus = r.getStatus();
        if (storedStatus == null) return null;

        // Si el estado es PENDIENTE o CANCELADO, no hay nada más que calcular.
        if (storedStatus == ReservationStatus.PENDIENTE || storedStatus == ReservationStatus.CANCELADO) {
            return storedStatus.name();
        }

        // estados dinámicos
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime startTime = ZonedDateTime.of(r.getDate(), r.getStart(), ZONE);
        ZonedDateTime endTime = ZonedDateTime.of(r.getDate(), r.getEnd(), ZONE);

        // Si la reserva está aceptada, verificamos si está activa, finalizada o incumplida.
        if (storedStatus == ReservationStatus.ACEPTADO) {
            //  para ACTIVA
            if (now.isAfter(startTime) && now.isBefore(endTime)) {
                return ReservationStatus.ACTIVA.name();
            }
            
            // para FINALIZADA o INCUMPLIDA (si la clase ya terminó)
            if (now.isAfter(endTime)) {
                if (Boolean.TRUE.equals(r.getAttended())) {
                    return ReservationStatus.FINALIZADA.name();
                } else {
                    return ReservationStatus.INCUMPLIDA.name();
                }
            }
        }
        
        // Si ninguna de las condiciones dinámicas se cumple, devuelve el estado guardado.
        return storedStatus.name();
    }
}