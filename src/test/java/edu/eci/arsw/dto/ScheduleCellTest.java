package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCellTest {

    @Test
    void allArgsConstructorAndGettersShouldWork() {
        ScheduleCell cell = new ScheduleCell(
                "2025-01-01",
                "08:00",
                "DISPONIBLE",
                "res-1",
                "student-1"
        );

        assertEquals("2025-01-01", cell.getDate());
        assertEquals("08:00", cell.getHour());
        assertEquals("DISPONIBLE", cell.getStatus());
        assertEquals("res-1", cell.getReservationId());
        assertEquals("student-1", cell.getStudentId());
    }

    @Test
    void settersEqualsHashCodeAndToStringShouldWork() {
        ScheduleCell a = new ScheduleCell();
        a.setDate("2025-02-02");
        a.setHour("09:00");
        a.setStatus("ACTIVA");
        a.setReservationId("r-1");
        a.setStudentId("s-1");

        ScheduleCell b = new ScheduleCell("2025-02-02", "09:00", "ACTIVA", "r-1", "s-1");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(null, a);
        assertNotEquals("otro", a);

        String ts = a.toString();
        assertTrue(ts.contains("2025-02-02"));
        assertTrue(ts.contains("ACTIVA"));
    }

    @Test
    void equalsShouldDetectDifferences() {
        ScheduleCell base = new ScheduleCell("2025-02-02", "09:00", "ACTIVA", "r-1", "s-1");
        ScheduleCell diffDate = new ScheduleCell("2025-02-03", "09:00", "ACTIVA", "r-1", "s-1");
        ScheduleCell diffHour = new ScheduleCell("2025-02-02", "10:00", "ACTIVA", "r-1", "s-1");
        ScheduleCell diffStatus = new ScheduleCell("2025-02-02", "09:00", "CANCELADO", "r-1", "s-1");

        assertNotEquals(base, diffDate);
        assertNotEquals(base, diffHour);
        assertNotEquals(base, diffStatus);
    }
}
