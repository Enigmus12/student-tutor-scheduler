package edu.eci.arsw.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilitySlotTest {

    @Test
    void builderShouldPopulateAllFields() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(9, 0);
        Instant created = Instant.now();
        Instant updated = created.plusSeconds(60);

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id("slot-1")
                .tutorId("tutor-1")
                .date(date)
                .start(start)
                .end(end)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        assertEquals("slot-1", slot.getId());
        assertEquals("tutor-1", slot.getTutorId());
        assertEquals(date, slot.getDate());
        assertEquals(start, slot.getStart());
        assertEquals(end, slot.getEnd());
        assertEquals(created, slot.getCreatedAt());
        assertEquals(updated, slot.getUpdatedAt());
    }

    @Test
    void settersEqualsAndHashCodeShouldWork() {
        LocalDate date = LocalDate.of(2025, 2, 2);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        AvailabilitySlot a = new AvailabilitySlot();
        a.setId("id");
        a.setTutorId("tutor");
        a.setDate(date);
        a.setStart(start);
        a.setEnd(end);

        AvailabilitySlot b = new AvailabilitySlot("id", "tutor", date, start, end, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // ramas de equals: null y tipo distinto
        assertNotEquals(a, null);
        assertNotEquals(a, "otro tipo");

        // si cambiamos algún campo ya no deben ser iguales
        b.setTutorId("otro");
        assertNotEquals(a, b);
    }

    @Test
    void toStringShouldContainKeyFields() {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id("slot-2")
                .tutorId("tutor-2")
                .build();

        String ts = slot.toString();
        assertTrue(ts.contains("slot-2"));
        assertTrue(ts.contains("tutor-2"));
    }
}
