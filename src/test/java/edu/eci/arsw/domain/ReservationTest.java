// src/test/java/edu/eci/arsw/domain/ReservationTest.java
package edu.eci.arsw.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void builderShouldPopulateAllFieldsAndBuilderToString() {
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

        String builderString = Reservation.builder()
                .id("X")
                .tutorId("T")
                .studentId("S")
                .toString();
        assertNotNull(builderString);
    }

    @Test
    void settersEqualsHashCodeToStringAndCanEqualShouldBeCovered() {
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
        assertEquals(a, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(null, a);
        assertNotEquals("otro tipo", a);

        assertTrue(a.canEqual(b));
        assertFalse(a.canEqual(new Object()));

        class BadReservation extends Reservation {
            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        Reservation bad = new BadReservation();
        bad.setId("id");
        bad.setTutorId("t1");

        assertNotEquals(a, bad);

        b.setStudentId("otro");
        assertNotEquals(a, b);

        String ts = a.toString();
        assertTrue(ts.contains("id"));
        assertTrue(ts.contains("t1"));
        assertTrue(ts.contains("s1"));
    }

    @Test
    void equalsAndHashCodeForEmptyReservations() {
        Reservation r1 = new Reservation();
        Reservation r2 = new Reservation();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void reservationStatusEnumShouldBeCovered() {
        for (ReservationStatus st : ReservationStatus.values()) {
            assertEquals(st, ReservationStatus.valueOf(st.name()));
        }
    }
}
