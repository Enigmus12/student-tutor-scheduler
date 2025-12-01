package edu.eci.arsw.controller;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.dto.BulkAvailabilityRequest;
import edu.eci.arsw.dto.DayAvailabilityUpdateRequest;
import edu.eci.arsw.security.AuthorizationService;
import edu.eci.arsw.security.RolesResponse;
import edu.eci.arsw.service.AvailabilityService;
import edu.eci.arsw.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private AuthorizationService authz;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private AvailabilityController controller;

    private static final String TOKEN = "Bearer token";
    private static final String TUTOR_ID = "tutor-1";

    /**
     * Ahora NO usamos Mockito aquí, así evitamos UnnecessaryStubbingException.
     */
    private RolesResponse mockMe(String id) {
        RolesResponse me = new RolesResponse();
        me.setId(id);
        return me;
    }

    // ========== bulk() ==========

    @Test
    void bulk_shouldCreateSlotsForTutor() {
        BulkAvailabilityRequest req = new BulkAvailabilityRequest();
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        List<AvailabilitySlot> slots = List.of(
                AvailabilitySlot.builder().id("s1").tutorId(TUTOR_ID).build(),
                AvailabilitySlot.builder().id("s2").tutorId(TUTOR_ID).build()
        );
        when(availabilityService.bulkCreate(TUTOR_ID, req)).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response = controller.bulk(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(slots, response.getBody());
        verify(authz).requireRole(TOKEN, "TUTOR");
        verify(availabilityService).bulkCreate(TUTOR_ID, req);
    }

    @Test
    void bulk_shouldReturnEmptyListWhenServiceReturnsNone() {
        BulkAvailabilityRequest req = new BulkAvailabilityRequest();
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.bulkCreate(TUTOR_ID, req)).thenReturn(Collections.emptyList());

        ResponseEntity<List<AvailabilitySlot>> response = controller.bulk(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(availabilityService).bulkCreate(TUTOR_ID, req);
    }

    @Test
    void bulk_shouldFailWhenRoleIsNotTutor() {
        BulkAvailabilityRequest req = new BulkAvailabilityRequest();
        doThrow(new RuntimeException("Forbidden")).when(authz).requireRole(TOKEN, "TUTOR");

        assertThrows(RuntimeException.class, () -> controller.bulk(TOKEN, req));
        verify(availabilityService, never()).bulkCreate(anyString(), any());
    }

    @Test
    void bulk_shouldPropagateServiceError() {
        BulkAvailabilityRequest req = new BulkAvailabilityRequest();
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.bulkCreate(TUTOR_ID, req))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> controller.bulk(TOKEN, req));
    }

    // ========== my() ==========

    @Test
    void my_shouldReturnSlotsForTutorInRange() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        List<AvailabilitySlot> slots = List.of(
                AvailabilitySlot.builder().id("s1").build()
        );
        when(availabilityService.mySlots(TUTOR_ID, from, to)).thenReturn(slots);

        ResponseEntity<List<AvailabilitySlot>> response = controller.my(TOKEN, from, to);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(slots, response.getBody());
        verify(authz).requireRole(TOKEN, "TUTOR");
        verify(availabilityService).mySlots(TUTOR_ID, from, to);
    }

    @Test
    void my_shouldAllowEmptyResult() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.mySlots(TUTOR_ID, from, to)).thenReturn(Collections.emptyList());

        ResponseEntity<List<AvailabilitySlot>> response = controller.my(TOKEN, from, to);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void my_shouldFailWhenRoleIsNotTutor() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);

        doThrow(new RuntimeException("Forbidden")).when(authz).requireRole(TOKEN, "TUTOR");

        assertThrows(RuntimeException.class, () -> controller.my(TOKEN, from, to));
        verify(availabilityService, never()).mySlots(anyString(), any(), any());
    }

    @Test
    void my_shouldPropagateServiceError() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 7);

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.mySlots(TUTOR_ID, from, to))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> controller.my(TOKEN, from, to));
    }

    // ========== delete() ==========

    @Test
    void delete_shouldRemoveSlotWhenFoundWithActiveReservation() {
        String slotId = "slot-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id(slotId)
                .tutorId(TUTOR_ID)
                .date(LocalDate.of(2025, 1, 2))
                .start(LocalTime.of(10, 0))
                .build();

        when(availabilityService.mySlots(anyString(), any(), any()))
                .thenReturn(List.of(slot));

        when(reservationService.hasActiveReservationForTutorAt(
                TUTOR_ID, slot.getDate(), slot.getStart())).thenReturn(true);

        ResponseEntity<Void> response = controller.delete(TOKEN, slotId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(availabilityService).deleteOwnSlot(TUTOR_ID, slotId, true);
    }

    @Test
    void delete_shouldRemoveEvenWhenSlotNotFound() {
        String slotId = "slot-unknown";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.mySlots(anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        ResponseEntity<Void> response = controller.delete(TOKEN, slotId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(availabilityService).deleteOwnSlot(TUTOR_ID, slotId, false);
        verify(reservationService, never())
                .hasActiveReservationForTutorAt(anyString(), any(), any());
    }

    @Test
    void delete_shouldFailWhenRoleIsNotTutor() {
        String slotId = "slot-1";
        doThrow(new RuntimeException("Forbidden")).when(authz).requireRole(TOKEN, "TUTOR");

        assertThrows(RuntimeException.class, () -> controller.delete(TOKEN, slotId));
        verify(availabilityService, never()).deleteOwnSlot(anyString(), anyString(), anyBoolean());
    }

    @Test
    void delete_shouldPropagateServiceError() {
        String slotId = "slot-1";
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.mySlots(anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Error deleting"))
                .when(availabilityService).deleteOwnSlot(TUTOR_ID, slotId, false);

        assertThrows(RuntimeException.class, () -> controller.delete(TOKEN, slotId));
    }

    // ========== replaceDay() ==========

    @SuppressWarnings("unchecked") 
    @Test
    void replaceDay_shouldReplaceHoursRespectingReservations() {
        LocalDate date = LocalDate.of(2025, 1, 2);
        DayAvailabilityUpdateRequest req = new DayAvailabilityUpdateRequest();
        req.getHours().addAll(List.of("10:00", "11:00"));

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        AvailabilitySlot s1 = AvailabilitySlot.builder()
                .tutorId(TUTOR_ID)
                .date(date)
                .start(LocalTime.of(10, 0))
                .build();
        AvailabilitySlot s2 = AvailabilitySlot.builder()
                .tutorId(TUTOR_ID)
                .date(date)
                .start(LocalTime.of(12, 0))
                .build();

        when(availabilityService.slotsForDay(TUTOR_ID, date))
                .thenReturn(List.of(s1, s2));

        when(reservationService.hasActiveReservationForTutorAt(TUTOR_ID, date, s1.getStart()))
                .thenReturn(true);
        when(reservationService.hasActiveReservationForTutorAt(TUTOR_ID, date, s2.getStart()))
                .thenReturn(false);

        ResponseEntity<Void> response = controller.replaceDay(TOKEN, date, req);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ArgumentCaptor<List<LocalTime>> hoursCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Set<LocalTime>> protectedCaptor = ArgumentCaptor.forClass(Set.class);

        verify(availabilityService).replaceDay(eq(TUTOR_ID), eq(date),
                hoursCaptor.capture(), protectedCaptor.capture());

        assertEquals(2, hoursCaptor.getValue().size());
        assertTrue(protectedCaptor.getValue().contains(LocalTime.of(10, 0)));
        assertFalse(protectedCaptor.getValue().contains(LocalTime.of(12, 0)));
    }

    @Test
    void replaceDay_shouldAllowEmptyRequestedHours() {
        LocalDate date = LocalDate.of(2025, 1, 2);
        DayAvailabilityUpdateRequest req = new DayAvailabilityUpdateRequest(); 
        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.slotsForDay(TUTOR_ID, date))
                .thenReturn(Collections.emptyList());

        ResponseEntity<Void> response = controller.replaceDay(TOKEN, date, req);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(availabilityService).replaceDay(eq(TUTOR_ID), eq(date),
                anyList(), anySet());
    }

    @Test
    void replaceDay_shouldFailOnInvalidHourFormat() {
        LocalDate date = LocalDate.of(2025, 1, 2);
        DayAvailabilityUpdateRequest req = new DayAvailabilityUpdateRequest();
        req.getHours().add("invalidHour");

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        assertThrows(RuntimeException.class, () -> controller.replaceDay(TOKEN, date, req));
    }

    @Test
    void replaceDay_shouldPropagateServiceError() {
        LocalDate date = LocalDate.of(2025, 1, 2);
        DayAvailabilityUpdateRequest req = new DayAvailabilityUpdateRequest();
        req.getHours().add("10:00");

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.slotsForDay(TUTOR_ID, date))
                .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Error replace"))
                .when(availabilityService)
                .replaceDay(eq(TUTOR_ID), eq(date), anyList(), anySet());

        assertThrows(RuntimeException.class, () -> controller.replaceDay(TOKEN, date, req));
    }

    // ========== addAvailability() ==========

    @Test
    void addAvailability_shouldAddNewHours() {
        Map<String, Object> req = new HashMap<>();
        req.put("date", "2025-01-02");
        req.put("hours", List.of("10", "11"));

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.addAvailability(eq(TUTOR_ID),
                eq(LocalDate.of(2025, 1, 2)),
                anyList()))
                .thenReturn(2);

        ResponseEntity<Map<String, Object>> response = controller.addAvailability(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Disponibilidad agregada", body.get("message"));
        assertEquals("2025-01-02", body.get("date"));
        assertEquals(2, body.get("addedCount"));
        assertEquals(2, body.get("requestedCount"));
    }

    @Test
    void addAvailability_shouldHandlePartialAdditions() {
        Map<String, Object> req = new HashMap<>();
        req.put("date", "2025-01-02");
        req.put("hours", List.of("10", "11"));

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        when(availabilityService.addAvailability(eq(TUTOR_ID),
                eq(LocalDate.of(2025, 1, 2)),
                anyList()))
                .thenReturn(1);

        ResponseEntity<Map<String, Object>> response = controller.addAvailability(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("addedCount"));
        assertEquals(2, body.get("requestedCount"));
    }

    @Test
    void addAvailability_shouldFailOnInvalidDate() {
        Map<String, Object> req = new HashMap<>();
        req.put("date", "fecha-mala");
        req.put("hours", List.of("10"));

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        assertThrows(RuntimeException.class, () -> controller.addAvailability(TOKEN, req));
    }

    @Test
    void addAvailability_shouldFailOnInvalidHour() {
        Map<String, Object> req = new HashMap<>();
        req.put("date", "2025-01-02");
        req.put("hours", List.of("invalid-hour"));

        RolesResponse me = mockMe(TUTOR_ID);
        when(authz.me(TOKEN)).thenReturn(me);

        assertThrows(IllegalArgumentException.class, () -> controller.addAvailability(TOKEN, req));
    }
}
