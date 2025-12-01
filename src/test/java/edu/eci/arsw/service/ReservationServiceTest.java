package edu.eci.arsw.service;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationCreateRequest;
import edu.eci.arsw.repository.AvailabilitySlotRepository;
import edu.eci.arsw.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository repo;

    @Mock
    private AvailabilitySlotRepository avRepo;

    @InjectMocks
    private ReservationService service;

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Test
    void createShouldCreateReservationWhenValid() {
        String studentId = "s1";
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("t1");
        req.setDate(LocalDate.now(ZONE).plusDays(2));
        req.setHour("10:00");

        when(avRepo.findByTutorIdAndDateAndStart(eq("t1"), any(), any()))
                .thenReturn(Optional.of(new edu.eci.arsw.domain.AvailabilitySlot()));

        when(repo.existsByStudentIdAndDateAndStart(any(), any(), any())).thenReturn(false);
        when(repo.existsByTutorIdAndDateAndStart(any(), any(), any())).thenReturn(false);

        when(repo.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId("res-1");
                    return r;
                });

        Reservation result = service.create(studentId, req);

        assertNotNull(result.getId());
        assertEquals(studentId, result.getStudentId());
        assertEquals("t1", result.getTutorId());
        assertEquals(ReservationStatus.PENDIENTE, result.getStatus());
    }

    @Test
    void createShouldRejectWhenTutorEqualsStudent() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("same");
        req.setDate(LocalDate.now(ZONE).plusDays(1));
        req.setHour("10");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("same", req));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createShouldRejectWhenDateInPast() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("t1");
        req.setDate(LocalDate.now(ZONE).minusDays(1));
        req.setHour("10");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("s1", req));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createShouldRejectWhenTutorHasNoAvailability() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("t1");
        req.setDate(LocalDate.now(ZONE).plusDays(1));
        req.setHour("10");

        when(avRepo.findByTutorIdAndDateAndStart(eq("t1"), any(), any()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("s1", req));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createShouldRejectWhenThereIsExistingReservation() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("t1");
        req.setDate(LocalDate.now(ZONE).plusDays(1));
        req.setHour("10");

        when(avRepo.findByTutorIdAndDateAndStart(eq("t1"), any(), any()))
                .thenReturn(Optional.of(new edu.eci.arsw.domain.AvailabilitySlot()));

        when(repo.existsByStudentIdAndDateAndStart(any(), any(), any())).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> service.create("s1", req));
    }

    @Test
    void createShouldTranslateDuplicateKeyToConflict() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId("t1");
        req.setDate(LocalDate.now(ZONE).plusDays(1));
        req.setHour("10");

        when(avRepo.findByTutorIdAndDateAndStart(eq("t1"), any(), any()))
                .thenReturn(Optional.of(new edu.eci.arsw.domain.AvailabilitySlot()));

        when(repo.existsByStudentIdAndDateAndStart(any(), any(), any())).thenReturn(false);
        when(repo.existsByTutorIdAndDateAndStart(any(), any(), any())).thenReturn(false);

        when(repo.save(any(Reservation.class))).thenThrow(new DuplicateKeyException("dup"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create("s1", req));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void changeStatusShouldAllowCancelWhenPendingAndMoreThan12HoursBefore() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .studentId("s1")
                .tutorId("t1")
                .date(LocalDate.now(ZONE).plusDays(2))
                .start(LocalTime.of(10, 0))
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));
        when(repo.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = service.changeStatusByStudentOrTutor("s1", id, ReservationStatus.CANCELADO);

        assertEquals(ReservationStatus.CANCELADO, result.getStatus());
    }

    @Test
    void changeStatusShouldRejectCancelWhenWrongStatus() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .studentId("s1")
                .tutorId("t1")
                .date(LocalDate.now(ZONE).plusDays(2))
                .start(LocalTime.of(10, 0))
                .status(ReservationStatus.CANCELADO)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.changeStatusByStudentOrTutor("s1", id, ReservationStatus.CANCELADO));
    }

    @Test
    void changeStatusShouldRejectCancelWhenLessThan12Hours() {
        String id = "res-1";

        ZonedDateTime now = ZonedDateTime.now(ZONE).plusHours(1);
        LocalDate date = now.toLocalDate();
        LocalTime start = now.toLocalTime().withMinute(0).withSecond(0).withNano(0);

        Reservation r = Reservation.builder()
                .id(id)
                .studentId("s1")
                .tutorId("t1")
                .date(date)
                .start(start)
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.changeStatusByStudentOrTutor("s1", id, ReservationStatus.CANCELADO));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void changeStatusShouldRejectAcceptWhenNotPending() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .studentId("s1")
                .tutorId("t1")
                .date(LocalDate.now(ZONE).plusDays(1))
                .start(LocalTime.of(10, 0))
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.changeStatusByStudentOrTutor("t1", id, ReservationStatus.ACEPTADO));
    }

    @Test
    void changeStatusShouldRejectWhenActorNotParticipant() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .studentId("s1")
                .tutorId("t1")
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.changeStatusByStudentOrTutor("other", id, ReservationStatus.CANCELADO));
    }

    @Test
    void hasActiveReservationForTutorAtShouldReturnTrueForPendingOrAccepted() {
        Reservation r = Reservation.builder()
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(repo.findByTutorIdAndDateAndStart(anyString(), any(), any()))
                .thenReturn(Optional.of(r));

        assertTrue(service.hasActiveReservationForTutorAt("t1", LocalDate.now(), LocalTime.NOON));

        r.setStatus(ReservationStatus.ACEPTADO);
        assertTrue(service.hasActiveReservationForTutorAt("t1", LocalDate.now(), LocalTime.NOON));

        r.setStatus(ReservationStatus.CANCELADO);
        assertFalse(service.hasActiveReservationForTutorAt("t1", LocalDate.now(), LocalTime.NOON));
    }

    @Test
    void hasActiveReservationForTutorAtShouldReturnFalseWhenNotFound() {
        when(repo.findByTutorIdAndDateAndStart(anyString(), any(), any()))
                .thenReturn(Optional.empty());

        assertFalse(service.hasActiveReservationForTutorAt("t1", LocalDate.now(), LocalTime.NOON));
    }

    @Test
    void setAttendedShouldUpdateWhenTutorAndClassFinished() {
        String id = "res-1";
        LocalDate date = LocalDate.now(ZONE).minusDays(1);
        LocalTime end = LocalTime.of(10, 0);

        Reservation r = Reservation.builder()
                .id(id)
                .tutorId("t1")
                .date(date)
                .start(LocalTime.of(9, 0))
                .end(end)
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));
        when(repo.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = service.setAttended("t1", id, true);

        assertTrue(result.getAttended());
    }

    @Test
    void setAttendedShouldRejectWhenNotTutor() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .tutorId("t1")
                .date(LocalDate.now(ZONE).minusDays(1))
                .start(LocalTime.of(9, 0))
                .end(LocalTime.of(10, 0))
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.setAttended("other", id, true));
    }

    @Test
    void setAttendedShouldRejectWhenStatusNotFinalizable() {
        String id = "res-1";
        Reservation r = Reservation.builder()
                .id(id)
                .tutorId("t1")
                .date(LocalDate.now(ZONE).minusDays(1))
                .start(LocalTime.of(9, 0))
                .end(LocalTime.of(10, 0))
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.setAttended("t1", id, true));
    }

    @Test
    void setAttendedShouldRejectWhenClassNotFinished() {
        String id = "res-1";
        LocalDate date = LocalDate.now(ZONE).plusDays(1);

        Reservation r = Reservation.builder()
                .id(id)
                .tutorId("t1")
                .date(date)
                .start(LocalTime.of(9, 0))
                .end(LocalTime.of(10, 0))
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class,
                () -> service.setAttended("t1", id, true));
    }

    @Test
    void myReservationsShouldDelegateToRepository() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        service.myReservations("s1", from, to);

        verify(repo).findByStudentIdAndDateBetween("s1", from, to);
    }

    @Test
    void reservationsForTutorShouldDelegateToRepository() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        service.reservationsForTutor("t1", from, to);

        verify(repo).findByTutorIdAndDateGreaterThanEqualAndDateLessThanEqual("t1", from, to);
    }

    @Test
    void findByIdShouldDelegateToRepository() {
        service.findById("id1");
        verify(repo).findById("id1");
    }
}
