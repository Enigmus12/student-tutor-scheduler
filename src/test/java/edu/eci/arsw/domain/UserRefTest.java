package edu.eci.arsw.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRefTest {

    @Test
    void constructorsAndGettersShouldWork() {
        UserRef ref = new UserRef("id1", "user@example.com", "User");

        assertEquals("id1", ref.getId());
        assertEquals("user@example.com", ref.getEmail());
        assertEquals("User", ref.getName());

        UserRef empty = new UserRef();
        empty.setId("id2");
        empty.setEmail("mail@example.com");
        empty.setName("Name");

        assertEquals("id2", empty.getId());
        assertEquals("mail@example.com", empty.getEmail());
        assertEquals("Name", empty.getName());
    }

    @Test
    void equalsHashCodeAndToStringShouldWork() {
        UserRef a = new UserRef("x", "a@b.com", "A");
        UserRef b = new UserRef("x", "a@b.com", "A");
        UserRef c = new UserRef("y", "c@d.com", "C");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(null, a);
        assertNotEquals("otro", a);

        String ts = a.toString();
        assertTrue(ts.contains("x"));
        assertTrue(ts.contains("a@b.com"));
    }
}
