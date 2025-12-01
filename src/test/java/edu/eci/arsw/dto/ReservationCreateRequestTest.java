package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationCreateRequestTest {

    @Test
    void gettersSettersEqualsAndHashCodeShouldWork() {
        ReservationCreateRequest r1 = new ReservationCreateRequest();
        r1.setTutorId("t1");
        r1.setDate(LocalDate.of(2025, 5, 5));
        r1.setHour("10:00");

        ReservationCreateRequest r2 = new ReservationCreateRequest();
        r2.setTutorId("t1");
        r2.setDate(LocalDate.of(2025, 5, 5));
        r2.setHour("10:00");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        assertNotEquals(null, r1);
        assertNotEquals("otro", r1);

        String ts = r1.toString();
        assertTrue(ts.contains("t1"));
        assertTrue(ts.contains("10:00"));
    }

    @Test
    void equalsShouldDetectDifferences() {
        ReservationCreateRequest base = new ReservationCreateRequest();
        base.setTutorId("t1");
        base.setDate(LocalDate.of(2025, 5, 5));
        base.setHour("10:00");

        ReservationCreateRequest diffTutor = new ReservationCreateRequest();
        diffTutor.setTutorId("t2");
        diffTutor.setDate(base.getDate());
        diffTutor.setHour(base.getHour());

        ReservationCreateRequest diffDate = new ReservationCreateRequest();
        diffDate.setTutorId(base.getTutorId());
        diffDate.setDate(LocalDate.of(2025, 5, 6));
        diffDate.setHour(base.getHour());

        ReservationCreateRequest diffHour = new ReservationCreateRequest();
        diffHour.setTutorId(base.getTutorId());
        diffHour.setDate(base.getDate());
        diffHour.setHour("11:00");

        assertNotEquals(base, diffTutor);
        assertNotEquals(base, diffDate);
        assertNotEquals(base, diffHour);
    }
}
