package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DayAvailabilityUpdateRequestTest {

    @Test
    void defaultConstructorShouldCreateNonNullHoursList() {
        DayAvailabilityUpdateRequest req = new DayAvailabilityUpdateRequest();
        assertNotNull(req.getHours());
        assertTrue(req.getHours().isEmpty());

        req.getHours().add("08:00");
        assertEquals(List.of("08:00"), req.getHours());
    }

    @Test
    void equalsHashCodeAndToStringShouldWork() {
        DayAvailabilityUpdateRequest a = new DayAvailabilityUpdateRequest();
        a.getHours().addAll(List.of("08:00", "09:00"));

        DayAvailabilityUpdateRequest b = new DayAvailabilityUpdateRequest();
        b.setHours(List.of("08:00", "09:00"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(null, a);
        assertNotEquals("otro", a);

        String ts = a.toString();
        assertTrue(ts.contains("08:00"));
    }

    @Test
    void equalsShouldDetectDifferentHours() {
        DayAvailabilityUpdateRequest a = new DayAvailabilityUpdateRequest();
        a.setHours(List.of("08:00"));

        DayAvailabilityUpdateRequest b = new DayAvailabilityUpdateRequest();
        b.setHours(List.of("09:00"));

        assertNotEquals(a, b);
    }
}
