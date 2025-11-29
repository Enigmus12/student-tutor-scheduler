package edu.eci.arsw.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void builderShouldPopulateAllFields() {
        LocalDate date = LocalDate.of(2025, 3, 3);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(15, 0);
        Instant created = Instant.now();
        Instant updated = created.plusSeconds(120);

        Reservation r = Reservation.builder()
                .id("res-1")
                .tutorId("tutor-1")
                .studentId("student-1")
                .date(date)
                .start(start)
                .end(end)
                .status(ReservationStatus.PENDIENTE)
                .attended(Boolean.TRUE)
                .createdAt(created)
                .updatedAt(updated)
                .version(1L)
                .build();

        assertEquals("res-1", r.getId());
        assertEquals("tutor-1", r.getTutorId());
        assertEquals("student-1", r.getStudentId());
        assertEquals(date, r.getDate());
        assertEquals(start, r.getStart());
        assertEquals(end, r.getEnd());
        assertEquals(ReservationStatus.PENDIENTE, r.getStatus());
        assertEquals(Boolean.TRUE, r.getAttended());
        assertEquals(created, r.getCreatedAt());
        assertEquals(updated, r.getUpdatedAt());
        assertEquals(1L, r.getVersion());
    }

    @Test
    void settersEqualsAndHashCodeShouldWork() {
        LocalDate date = LocalDate.of(2025, 4, 4);
        LocalTime start = LocalTime.of(9, 0);

        Reservation a = new Reservation();
        a.setId("id");
        a.setTutorId("t1");
        a.setStudentId("s1");
        a.setDate(date);
        a.setStart(start);
        a.setEnd(start.plusHours(1));
        a.setStatus(ReservationStatus.ACEPTADO);
        a.setAttended(null);

        Reservation b = Reservation.builder()
                .id("id")
                .tutorId("t1")
                .studentId("s1")
                .date(date)
                .start(start)
                .end(start.plusHours(1))
                .status(ReservationStatus.ACEPTADO)
                .attended(null)
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(null, a);
        assertNotEquals("otro tipo", a);

        b.setStudentId("otro");
        assertNotEquals(a, b);
    }

    @Test
    void toStringShouldContainIds() {
        Reservation r = Reservation.builder()
                .id("res-2")
                .tutorId("t-2")
                .studentId("s-2")
                .build();

        String ts = r.toString();
        assertTrue(ts.contains("res-2"));
        assertTrue(ts.contains("t-2"));
        assertTrue(ts.contains("s-2"));
    }
}
