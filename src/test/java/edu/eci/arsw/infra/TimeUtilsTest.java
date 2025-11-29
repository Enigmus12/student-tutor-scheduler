package edu.eci.arsw.infra;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void isOnTheHourShouldBeTrueOnlyForExactHours() {
        assertTrue(TimeUtils.isOnTheHour(LocalTime.of(10, 0)));
        assertFalse(TimeUtils.isOnTheHour(LocalTime.of(10, 15)));
        assertFalse(TimeUtils.isOnTheHour(LocalTime.of(10, 0, 5)));
    }

    @Test
    void parseHourShouldSupportHHmmAndHHmmss() {
        assertEquals(LocalTime.of(10, 0, 0), TimeUtils.parseHour("10:00"));
        assertEquals(LocalTime.of(10, 0, 30), TimeUtils.parseHour("10:00:30"));
    }

    @Test
    void isPastShouldCompareUsingZone() {
        ZoneId zone = ZoneId.of("America/Bogota");
        LocalDate yesterday = LocalDate.now(zone).minusDays(1);
        LocalDate tomorrow = LocalDate.now(zone).plusDays(1);

        assertTrue(TimeUtils.isPast(yesterday, LocalTime.NOON, zone));
        assertFalse(TimeUtils.isPast(tomorrow, LocalTime.NOON, zone));
    }
}
