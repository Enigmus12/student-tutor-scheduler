package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationViewTest {

    @Test
    void builderShouldPopulateAllFieldsAndBuilderToString() {
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

        // cubre ReservationView.ReservationViewBuilder.toString()
        String builderString = ReservationView.builder()
                .id("X")
                .tutorId("T")
                .toString();
        assertNotNull(builderString);
    }

    @Test
    void equalsHashCodeAndToStringShouldCoverAllImportantBranches() {
        LocalDate date = LocalDate.of(2025, 6, 6);
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = start.plusHours(1);

        ReservationView base = new ReservationView(
                "id",
                "tutor",
                "student",
                date,
                start,
                end,
                "PENDIENTE",
                Boolean.TRUE,
                "Student",
                "stu-avatar",
                "Tutor",
                "tut-avatar"
        );

        ReservationView same = new ReservationView(
                "id",
                "tutor",
                "student",
                date,
                start,
                end,
                "PENDIENTE",
                Boolean.TRUE,
                "Student",
                "stu-avatar",
                "Tutor",
                "tut-avatar"
        );

        assertEquals(base, same);
        assertEquals(base, base);
        assertEquals(base.hashCode(), same.hashCode());
        assertNotEquals(null, base);
        assertNotEquals("otro", base);

        // diferencias campo por campo para cubrir ramas de equals
        assertNotEquals(base, new ReservationView("other", "tutor", "student", date, start, end,
                "PENDIENTE", Boolean.TRUE, "Student", "stu-avatar", "Tutor", "tut-avatar")); // id

        assertNotEquals(base, new ReservationView("id", "otherTutor", "student", date, start, end,
                "PENDIENTE", Boolean.TRUE, "Student", "stu-avatar", "Tutor", "tut-avatar"));   // tutorId

        assertNotEquals(base, new ReservationView("id", "tutor", "otherStudent", date, start, end,
                "PENDIENTE", Boolean.TRUE, "Student", "stu-avatar", "Tutor", "tut-avatar"));   // studentId

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date.plusDays(1), start, end, "PENDIENTE", Boolean.TRUE,
                "Student", "stu-avatar", "Tutor", "tut-avatar"));                             // date

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start.plusHours(1), end, "PENDIENTE", Boolean.TRUE,
                "Student", "stu-avatar", "Tutor", "tut-avatar"));                             // start

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end.plusHours(1), "PENDIENTE", Boolean.TRUE,
                "Student", "stu-avatar", "Tutor", "tut-avatar"));                             // end

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "CANCELADO", Boolean.TRUE,
                "Student", "stu-avatar", "Tutor", "tut-avatar"));                             // status

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "PENDIENTE", Boolean.FALSE,
                "Student", "stu-avatar", "Tutor", "tut-avatar"));                             // attended

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "PENDIENTE", Boolean.TRUE,
                "OtherStudent", "stu-avatar", "Tutor", "tut-avatar"));                        // studentName

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "PENDIENTE", Boolean.TRUE,
                "Student", "otherAvatar", "Tutor", "tut-avatar"));                            // studentAvatar

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "PENDIENTE", Boolean.TRUE,
                "Student", "stu-avatar", "OtherTutor", "tut-avatar"));                        // tutorName

        assertNotEquals(base, new ReservationView("id", "tutor", "student",
                date, start, end, "PENDIENTE", Boolean.TRUE,
                "Student", "stu-avatar", "Tutor", "otherTutorAvatar"));                       // tutorAvatar

        // rama canEqual false
        class BadReservationView extends ReservationView {
            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        ReservationView bad = new BadReservationView();
        bad.setId("id");
        bad.setTutorId("tutor");
        bad.setStudentId("student");
        assertNotEquals(base, bad);

        String ts = base.toString();
        assertTrue(ts.contains("id"));
        assertTrue(ts.contains("PENDIENTE"));
    }

    @Test
    void hashCodeShouldHandleNullFieldsAsWell() {
        ReservationView empty1 = new ReservationView();
        ReservationView empty2 = new ReservationView();

        assertEquals(empty1, empty2);
        assertEquals(empty1.hashCode(), empty2.hashCode());

        int h1 = empty1.hashCode();
        empty1.setId("some-id");
        int h2 = empty1.hashCode();

        assertNotEquals(h1, h2);
    }
}
