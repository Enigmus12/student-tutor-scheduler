package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BulkAvailabilityRequestTest {

    @Test
    void gettersSettersEqualsHashCodeToStringAndCanEqualShouldBeCovered() {
        BulkAvailabilityRequest r1 = new BulkAvailabilityRequest();
        r1.setFromDate(LocalDate.of(2025, 1, 1));
        r1.setToDate(LocalDate.of(2025, 1, 31));
        r1.setFromHour("08:00");
        r1.setToHour("12:00");
        r1.setDaysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        r1.setTimezone("America/Bogota");

        BulkAvailabilityRequest r2 = new BulkAvailabilityRequest();
        r2.setFromDate(LocalDate.of(2025, 1, 1));
        r2.setToDate(LocalDate.of(2025, 1, 31));
        r2.setFromHour("08:00");
        r2.setToHour("12:00");
        r2.setDaysOfWeek(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        r2.setTimezone("America/Bogota");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        assertEquals(r1, r1);

        assertNotEquals(null, r1);
        assertNotEquals("otro", r1);

        assertTrue(r1.canEqual(r2));
        assertFalse(r1.canEqual(new Object()));

        class BadBulkAvailabilityRequest extends BulkAvailabilityRequest {
            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        BulkAvailabilityRequest bad = new BadBulkAvailabilityRequest();
        bad.setFromDate(r1.getFromDate());
        bad.setToDate(r1.getToDate());
        bad.setFromHour(r1.getFromHour());
        bad.setToHour(r1.getToHour());
        bad.setDaysOfWeek(r1.getDaysOfWeek());
        bad.setTimezone(r1.getTimezone());

        assertNotEquals(r1, bad);

        String ts = r1.toString();
        assertTrue(ts.contains("fromHour=08:00"));
        assertTrue(ts.contains("toHour=12:00"));
    }
}
