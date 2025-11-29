package edu.eci.arsw.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilitySlotTest {

    @Test
    void builderShouldPopulateAllFieldsAndBuilderToString() {
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

        String builderString = AvailabilitySlot.builder()
                .id("X")
                .tutorId("T")
                .toString();
        assertNotNull(builderString);
    }

    @Test
    void settersEqualsHashCodeToStringAndCanEqualShouldBeCovered() {
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
        assertEquals(a, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(null, a);
        assertNotEquals("otro tipo", a);

        assertTrue(a.canEqual(b));
        assertFalse(a.canEqual(new Object()));

        class BadSlot extends AvailabilitySlot {
            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        AvailabilitySlot bad = new BadSlot();
        bad.setId("id");
        bad.setTutorId("tutor");

        assertNotEquals(a, bad);

        b.setTutorId("otro");
        assertNotEquals(a, b);

        String ts = a.toString();
        assertTrue(ts.contains("id"));
        assertTrue(ts.contains("tutor"));
    }

    @Test
    void equalsAndHashCodeForEmptySlots() {
        AvailabilitySlot a = new AvailabilitySlot();
        AvailabilitySlot b = new AvailabilitySlot();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
