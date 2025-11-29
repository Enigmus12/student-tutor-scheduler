package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationViewTest {

    @Test
    void builderShouldPopulateAllFields() {
        LocalDate date = LocalDate.of(2025, 6, 6);
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(12, 0);

        ReservationView v = ReservationView.builder()
                .id("res-1")
                .tutorId("tutor-1")
                .studentId("student-1")
                .date(date)
                .start(start)
                .end(end)
                .status("PENDIENTE")
                .attended(Boolean.FALSE)
                .studentName("Student")
                .studentAvatar("stu-avatar")
                .tutorName("Tutor")
                .tutorAvatar("tut-avatar")
                .build();

        assertEquals("res-1", v.getId());
        assertEquals("tutor-1", v.getTutorId());
        assertEquals("student-1", v.getStudentId());
        assertEquals(date, v.getDate());
        assertEquals(start, v.getStart());
        assertEquals(end, v.getEnd());
        assertEquals("PENDIENTE", v.getStatus());
        assertEquals(Boolean.FALSE, v.getAttended());
        assertEquals("Student", v.getStudentName());
        assertEquals("stu-avatar", v.getStudentAvatar());
        assertEquals("Tutor", v.getTutorName());
        assertEquals("tut-avatar", v.getTutorAvatar());
    }

    @Test
    void equalsHashCodeAndToStringShouldWork() {
        LocalDate date = LocalDate.of(2025, 6, 6);
        LocalTime start = LocalTime.of(11, 0);

        ReservationView a = ReservationView.builder()
                .id("x")
                .tutorId("t")
                .studentId("s")
                .date(date)
                .start(start)
                .end(start.plusHours(1))
                .status("PENDIENTE")
                .build();

        ReservationView b = ReservationView.builder()
                .id("x")
                .tutorId("t")
                .studentId("s")
                .date(date)
                .start(start)
                .end(start.plusHours(1))
                .status("PENDIENTE")
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(null, a);
        assertNotEquals("otro", a);

        String ts = a.toString();
        assertTrue(ts.contains("x"));
        assertTrue(ts.contains("PENDIENTE"));
    }
}
