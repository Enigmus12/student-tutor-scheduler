package edu.eci.arsw.service;

import edu.eci.arsw.domain.AvailabilitySlot;
import edu.eci.arsw.domain.Reservation;
import edu.eci.arsw.domain.ReservationStatus;
import edu.eci.arsw.dto.ScheduleCell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private MongoTemplate mongo;

    @InjectMocks
    private ScheduleService service;

    @Test
    void weekForTutorShouldRejectMissingParameters() {
        LocalDate weekStart = LocalDate.now();

        ResponseStatusException ex1 =
                assertThrows(ResponseStatusException.class,
                        () -> service.weekForTutor(null, weekStart));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        assertThrows(ResponseStatusException.class,
                () -> service.weekForTutor(" ", weekStart));
        assertThrows(ResponseStatusException.class,
                () -> service.weekForTutor("t1", null));
    }

    @Test
    void weekForTutorShouldMarkAvailabilityAsDisponibleWhenNoReservations() {
        LocalDate weekStart = LocalDate.of(2025, 1, 6); // lunes

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .tutorId("t1")
                .date(weekStart)
                .start(LocalTime.of(10, 0))
                .build();

        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(AvailabilitySlot.class)))
                .thenReturn(List.of(slot));
        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(Reservation.class)))
                .thenReturn(Collections.emptyList());

        List<ScheduleCell> cells = service.weekForTutor("t1", weekStart);

        assertEquals(7 * 24, cells.size());

        ScheduleCell cellAt10 = cells.stream()
                .filter(c -> c.getDate().equals(weekStart.toString()) && c.getHour().startsWith("10:00"))
                .findFirst()
                .orElseThrow();

        assertEquals("DISPONIBLE", cellAt10.getStatus());
        assertNull(cellAt10.getReservationId());
    }

    @Test
    void weekForTutorShouldOverrideAvailabilityWithReservationStatus() {
        LocalDate weekStart = LocalDate.of(2025, 1, 6);

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .tutorId("t1")
                .date(weekStart)
                .start(LocalTime.of(10, 0))
                .build();

        Reservation res = Reservation.builder()
                .id("res-1")
                .tutorId("t1")
                .studentId("s1")
                .date(weekStart)
                .start(LocalTime.of(10, 0))
                .status(ReservationStatus.ACEPTADO)
                .build();

        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(AvailabilitySlot.class)))
                .thenReturn(List.of(slot));
        when(mongo.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(Reservation.class)))
                .thenReturn(List.of(res));

        List<ScheduleCell> cells = service.weekForTutor("t1", weekStart);

        ScheduleCell cell = cells.stream()
                .filter(c -> c.getDate().equals(weekStart.toString()) && c.getHour().startsWith("10:00"))
                .findFirst()
                .orElseThrow();

        assertEquals(ReservationStatus.ACEPTADO.name(), cell.getStatus());
        assertEquals("res-1", cell.getReservationId());
        assertEquals("s1", cell.getStudentId());
    }
}
