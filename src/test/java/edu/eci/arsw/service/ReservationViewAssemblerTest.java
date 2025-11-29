package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.PublicProfile;
import edu.eci.arsw.dto.ReservationView;
import edu.eci.arsw.security.UsersPublicClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationViewAssemblerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Mock
    private UsersPublicClient usersPublicClient;

    @InjectMocks
    private ReservationViewAssembler assembler;

    @Test
    void shouldReturnActivaWhenAcceptedAndWithinTimeWindow() {
        when(usersPublicClient.getPublicProfileCached(null, "student"))
                .thenReturn(PublicProfile.builder().id("student").name("Alumno").avatarUrl("s.png").build());
        when(usersPublicClient.getPublicProfileCached(null, "tutor"))
                .thenReturn(PublicProfile.builder().id("tutor").name("Profesor").avatarUrl("t.png").build());

        LocalDate today = LocalDate.now(ZONE);

        Reservation r = Reservation.builder()
                .id("r1")
                .studentId("student")
                .tutorId("tutor")
                .date(today)
                .start(LocalTime.MIN)      
                .end(LocalTime.MAX)        
                .status(ReservationStatus.ACEPTADO)
                .attended(null)
                .build();

        ReservationView view = assembler.toView(r);

        assertEquals("ACTIVA", view.getStatus());
        assertEquals("Alumno", view.getStudentName());
        assertEquals("Profesor", view.getTutorName());
        assertEquals("s.png", view.getStudentAvatar());
        assertEquals("t.png", view.getTutorAvatar());
    }

    @Test
    void shouldReturnFinalizadaOrIncumplidaAfterEndDependingOnAttendance() {
        when(usersPublicClient.getPublicProfileCached(null, "student"))
                .thenReturn(PublicProfile.builder().id("student").build());
        when(usersPublicClient.getPublicProfileCached(null, "tutor"))
                .thenReturn(PublicProfile.builder().id("tutor").build());

        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);

        Reservation attended = Reservation.builder()
                .id("r1")
                .studentId("student")
                .tutorId("tutor")
                .date(yesterday)
                .start(LocalTime.NOON.minusHours(1))
                .end(LocalTime.NOON)
                .status(ReservationStatus.ACEPTADO)
                .attended(true)
                .build();

        Reservation missed = Reservation.builder()
                .id("r2")
                .studentId("student")
                .tutorId("tutor")
                .date(yesterday)
                .start(LocalTime.NOON.minusHours(1))
                .end(LocalTime.NOON)
                .status(ReservationStatus.ACEPTADO)
                .attended(false)
                .build();

        assertEquals("FINALIZADA", assembler.toView(attended).getStatus());
        assertEquals("INCUMPLIDA", assembler.toView(missed).getStatus());
    }

    @Test
    void shouldUseFallbackNamesAndHandleSimpleAndNullStatuses() {
        // estudiante sin perfil y tutor con nombre nulo -> usa defaults
        when(usersPublicClient.getPublicProfileCached(null, "s2")).thenReturn(null);
        when(usersPublicClient.getPublicProfileCached(null, "t2"))
                .thenReturn(PublicProfile.builder().id("t2").name(null).build());

        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);

        Reservation pending = Reservation.builder()
                .id("p1").studentId("s2").tutorId("t2")
                .date(tomorrow)
                .start(LocalTime.NOON)
                .end(LocalTime.NOON.plusHours(1))
                .status(ReservationStatus.PENDIENTE)
                .build();

        Reservation cancelled = Reservation.builder()
                .id("c1").studentId("s2").tutorId("t2")
                .date(tomorrow)
                .start(LocalTime.NOON)
                .end(LocalTime.NOON.plusHours(1))
                .status(ReservationStatus.CANCELADO)
                .build();

        Reservation nullStatus = Reservation.builder()
                .id("n1").studentId("s2").tutorId("t2")
                .date(tomorrow)
                .start(LocalTime.NOON)
                .end(LocalTime.NOON.plusHours(1))
                .status(null)
                .build();

        ReservationView vPending = assembler.toView(pending);
        ReservationView vCancelled = assembler.toView(cancelled);
        ReservationView vNull = assembler.toView(nullStatus);

        assertEquals("PENDIENTE", vPending.getStatus());
        assertEquals("CANCELADO", vCancelled.getStatus());
        assertNull(vNull.getStatus());

        assertEquals("Estudiante", vPending.getStudentName());
        assertEquals("Tutor", vPending.getTutorName());
    }

    @Test
    void acceptedReservationBeforeStartKeepsAcceptedStatus() {
        when(usersPublicClient.getPublicProfileCached(null, "student"))
                .thenReturn(PublicProfile.builder().id("student").build());
        when(usersPublicClient.getPublicProfileCached(null, "tutor"))
                .thenReturn(PublicProfile.builder().id("tutor").build());

        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);

        Reservation r = Reservation.builder()
                .id("f1")
                .studentId("student")
                .tutorId("tutor")
                .date(tomorrow)
                .start(LocalTime.NOON)
                .end(LocalTime.NOON.plusHours(1))
                .status(ReservationStatus.ACEPTADO)
                .build();

        assertEquals("ACEPTADO", assembler.toView(r).getStatus());
    }
}
