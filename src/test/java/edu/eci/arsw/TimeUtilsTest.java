package edu.eci.arsw;

import edu.eci.arsw.infra.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {
    @Test
    void testOnTheHour() {
        assertTrue(TimeUtils.isOnTheHour(LocalTime.of(10, 0)));
        assertFalse(TimeUtils.isOnTheHour(LocalTime.of(10, 30)));
    }
}
