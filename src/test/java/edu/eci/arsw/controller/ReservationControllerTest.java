package edu.eci.arsw.controller;

import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ReservationCreateRequest;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import edu.eci.arsw.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private AuthorizationService authz;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ReservationController controller;

    private static final String TOKEN = "Bearer token";
    private static final String STUDENT_ID = "student-1";
    private static final String TUTOR_ID = "tutor-1";

    /**
     * Importante: NO usar Mockito aquí.
     * Así evitamos UnnecessaryStubbingException.
     */
    private RolesResponse mockMe(String id) {
        RolesResponse me = new RolesResponse();
        me.setId(id);
        return me;
    }

    // ========== create() ==========

    @Test
    void create_shouldCreateReservationForStudent() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId(TUTOR_ID);
        req.setDate(LocalDate.of(2025, 1, 2));
        req.setHour("10:00");

        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation created = Reservation.builder()
                .id("res-1")
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(reservationService.create(STUDENT_ID, req)).thenReturn(created);

        ResponseEntity<Reservation> response = controller.create(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created, response.getBody());
        verify(authz).requireRole(TOKEN, "STUDENT");
        verify(reservationService).create(STUDENT_ID, req);
    }

    @Test
    void create_shouldAllowDifferentStatuses() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setTutorId(TUTOR_ID);
        req.setDate(LocalDate.of(2025, 1, 3));
        req.setHour("11:00");

        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation created = Reservation.builder()
                .id("res-2")
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(reservationService.create(STUDENT_ID, req)).thenReturn(created);

        ResponseEntity<Reservation> response = controller.create(TOKEN, req);

        assertEquals(ReservationStatus.ACEPTADO, response.getBody().getStatus());
    }

    @Test
    void create_shouldFailWhenNotStudentRole() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        doThrow(new RuntimeException("Forbidden"))
                .when(authz).requireRole(TOKEN, "STUDENT");

        assertThrows(RuntimeException.class, () -> controller.create(TOKEN, req));
        verify(reservationService, never()).create(anyString(), any());
    }

    @Test
    void create_shouldPropagateServiceError() {
        ReservationCreateRequest req = new ReservationCreateRequest();
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(reservationService.create(STUDENT_ID, req))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> controller.create(TOKEN, req));
    }

    // ========== my() ==========

    @Test
    void my_shouldReturnReservationsForStudent_withNoRange() {
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        List<Reservation> reservations = List.of(
                Reservation.builder().id("r1").studentId(STUDENT_ID).build()
        );
        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(reservations);

        List<Reservation> result = controller.my(TOKEN, null, null);

        assertEquals(1, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Reservation.class));
    }

    @Test
    void my_shouldReturnReservationsForStudent_withRange() {
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        List<Reservation> reservations = List.of(
                Reservation.builder().id("r1").studentId(STUDENT_ID).build(),
                Reservation.builder().id("r2").studentId(STUDENT_ID).build()
        );
        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(reservations);

        List<Reservation> result = controller.my(TOKEN, "2025-01-01", "2025-01-31");

        assertEquals(2, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Reservation.class));
    }

    @Test
    void my_shouldFailWhenAuthThrows() {
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.my(TOKEN, null, null));
        verify(mongoTemplate, never()).find(any(Query.class), eq(Reservation.class));
    }

    @Test
    void my_shouldPropagateMongoError() {
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenThrow(new RuntimeException("Mongo error"));

        assertThrows(RuntimeException.class,
                () -> controller.my(TOKEN, null, null));
    }

    // ========== forMe() ==========

    @Test
    void forMe_shouldReturnReservationsForTutor_withNoRange() {
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        List<Reservation> reservations = List.of(
                Reservation.builder().id("r1").tutorId(TUTOR_ID).build()
        );
        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(reservations);

        List<Reservation> result = controller.forMe(TOKEN, null, null);

        assertEquals(1, result.size());
        verify(mongoTemplate).find(any(Query.class), eq(Reservation.class));
    }

    @Test
    void forMe_shouldReturnReservationsForTutor_withRange() {
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(List.of());

        List<Reservation> result = controller.forMe(TOKEN, "2025-01-01", "2025-01-31");

        assertNotNull(result);
        verify(mongoTemplate).find(any(Query.class), eq(Reservation.class));
    }

    @Test
    void forMe_shouldFailWhenAuthThrows() {
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.forMe(TOKEN, null, null));
    }

    @Test
    void forMe_shouldPropagateMongoError() {
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenThrow(new RuntimeException("Mongo error"));

        assertThrows(RuntimeException.class,
                () -> controller.forMe(TOKEN, null, null));
    }

    // ========== cancel() ==========

    @Test
    void cancel_shouldChangeStatusToCancelled() {
        String id = "res-1";
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .status(ReservationStatus.CANCELADO)
                .build();

        when(reservationService.changeStatusByStudentOrTutor(
                STUDENT_ID, id, ReservationStatus.CANCELADO))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.cancel(TOKEN, id);

        assertEquals(ReservationStatus.CANCELADO, response.getBody().getStatus());
    }

    @Test
    void cancel_shouldWorkForTutorAlso() {
        String id = "res-2";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .status(ReservationStatus.CANCELADO)
                .build();

        when(reservationService.changeStatusByStudentOrTutor(
                TUTOR_ID, id, ReservationStatus.CANCELADO))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.cancel(TOKEN, id);

        assertEquals(ReservationStatus.CANCELADO, response.getBody().getStatus());
    }

    @Test
    void cancel_shouldFailWhenAuthThrows() {
        String id = "res-1";
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.cancel(TOKEN, id));
        verify(reservationService, never())
                .changeStatusByStudentOrTutor(anyString(), anyString(), any());
    }

    @Test
    void cancel_shouldPropagateServiceError() {
        String id = "res-1";
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(reservationService.changeStatusByStudentOrTutor(
                STUDENT_ID, id, ReservationStatus.CANCELADO))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> controller.cancel(TOKEN, id));
    }

    // ========== accept() ==========

    @Test
    void accept_shouldChangeStatusToAcceptedForTutor() {
        String id = "res-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(reservationService.changeStatusByStudentOrTutor(
                TUTOR_ID, id, ReservationStatus.ACEPTADO))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.accept(TOKEN, id);

        assertEquals(ReservationStatus.ACEPTADO, response.getBody().getStatus());
        verify(authz).requireRole(TOKEN, "TUTOR");
    }

    @Test
    void accept_shouldReturnUpdatedReservation() {
        String id = "res-2";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .status(ReservationStatus.ACEPTADO)
                .tutorId(TUTOR_ID)
                .build();

        when(reservationService.changeStatusByStudentOrTutor(
                TUTOR_ID, id, ReservationStatus.ACEPTADO))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.accept(TOKEN, id);

        assertEquals(id, response.getBody().getId());
    }

    @Test
    void accept_shouldFailWhenNotTutorRole() {
        String id = "res-1";
        doThrow(new RuntimeException("Forbidden"))
                .when(authz).requireRole(TOKEN, "TUTOR");

        assertThrows(RuntimeException.class, () -> controller.accept(TOKEN, id));
        verify(reservationService, never())
                .changeStatusByStudentOrTutor(anyString(), anyString(), any());
    }

    @Test
    void accept_shouldPropagateServiceError() {
        String id = "res-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(reservationService.changeStatusByStudentOrTutor(
                TUTOR_ID, id, ReservationStatus.ACEPTADO))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> controller.accept(TOKEN, id));
    }

    // ========== attended() ==========

    @Test
    void attended_shouldUpdateAttendanceTrue() {
        String id = "res-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .attended(true)
                .build();

        when(reservationService.setAttended(TUTOR_ID, id, true))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.attended(TOKEN, id, true);

        assertTrue(response.getBody().getAttended());
    }

    @Test
    void attended_shouldUpdateAttendanceFalse() {
        String id = "res-2";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation updated = Reservation.builder()
                .id(id)
                .attended(false)
                .build();

        when(reservationService.setAttended(TUTOR_ID, id, false))
                .thenReturn(updated);

        ResponseEntity<Reservation> response = controller.attended(TOKEN, id, false);

        assertFalse(response.getBody().getAttended());
    }

    @Test
    void attended_shouldFailWhenAuthThrows() {
        String id = "res-1";
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.attended(TOKEN, id, true));
        verify(reservationService, never())
                .setAttended(anyString(), anyString(), anyBoolean());
    }

    @Test
    void attended_shouldPropagateServiceError() {
        String id = "res-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(reservationService.setAttended(TUTOR_ID, id, true))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> controller.attended(TOKEN, id, true));
    }

    // ========== canChat() ==========

    @Test
    void canChat_shouldReturnFalseWhenSameUser() {
        String other = STUDENT_ID;
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        ResponseEntity<Map<String, Object>> response =
                controller.canChat(TOKEN, other);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("canChat"));
        assertEquals("No puedes chatear contigo mismo", body.get("reason"));
    }

    @Test
    void canChat_shouldReturnTrueWhenReservationExists() {
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation r = Reservation.builder()
                .id("res-1")
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(List.of(r));

        ResponseEntity<Map<String, Object>> response =
                controller.canChat(TOKEN, TUTOR_ID);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("canChat"));
        assertEquals(1, body.get("reservationCount"));
    }

    @Test
    void canChat_shouldReturnFalseWhenNoReservations() {
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(mongoTemplate.find(any(Query.class), eq(Reservation.class)))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response =
                controller.canChat(TOKEN, TUTOR_ID);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("canChat"));
        assertEquals("No existe una reserva activa entre estos usuarios",
                body.get("reason"));
    }

    @Test
    void canChat_shouldFailWhenAuthThrows() {
        when(authz.me(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class,
                () -> controller.canChat(TOKEN, TUTOR_ID));
    }

    // ========== getOne() ==========

    @Test
    void getOne_shouldReturnReservationWhenStudentParticipant() {
        String id = "res-1";
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation r = Reservation.builder()
                .id(id)
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .date(LocalDate.of(2025, 1, 2))
                .start(LocalTime.of(10, 0))
                .end(LocalTime.of(11, 0))
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(reservationService.findById(id)).thenReturn(Optional.of(r));

        ResponseEntity<Map<String, Object>> response = controller.getOne(TOKEN, id);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(id, body.get("id"));
        assertEquals("ACEPTADO", body.get("status"));
        assertEquals(STUDENT_ID, body.get("studentId"));
        assertEquals(TUTOR_ID, body.get("tutorId"));
    }

    @Test
    void getOne_shouldReturnReservationWhenTutorParticipant() {
        String id = "res-2";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation r = Reservation.builder()
                .id(id)
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .date(LocalDate.of(2025, 1, 3))
                .start(LocalTime.of(12, 0))
                .end(LocalTime.of(13, 0))
                .status(ReservationStatus.PENDIENTE)
                .build();

        when(reservationService.findById(id)).thenReturn(Optional.of(r));

        ResponseEntity<Map<String, Object>> response = controller.getOne(TOKEN, id);

        assertEquals(id, response.getBody().get("id"));
    }

    @Test
    void getOne_shouldFailWhenReservationNotFound() {
        String id = "res-404";
        RolesResponse me = mockMe(STUDENT_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(reservationService.findById(id)).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.getOne(TOKEN, id));
    }

    @Test
    void getOne_shouldFailWhenUserNotParticipant() {
        String id = "res-1";
        RolesResponse me = mockMe("other-user");
        when(authz.me(TOKEN)).thenReturn(me);

        Reservation r = Reservation.builder()
                .id(id)
                .studentId(STUDENT_ID)
                .tutorId(TUTOR_ID)
                .build();

        when(reservationService.findById(id)).thenReturn(Optional.of(r));

        org.springframework.web.server.ResponseStatusException ex =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> controller.getOne(TOKEN, id));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
