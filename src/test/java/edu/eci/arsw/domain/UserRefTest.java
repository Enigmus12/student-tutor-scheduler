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
    void equalsHashCodeToStringAndCanEqualShouldBeCovered() {
        UserRef a = new UserRef("x", "a@b.com", "A");
        UserRef b = new UserRef("x", "a@b.com", "A");
        UserRef c = new UserRef("y", "c@d.com", "C");

        assertEquals(a, b);
        assertEquals(a, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(null, a);
        assertNotEquals("otro", a);

        assertTrue(a.canEqual(b));
        assertFalse(a.canEqual(new Object()));

        class BadUserRef extends UserRef {
            public BadUserRef(String id, String email, String name) {
                super(id, email, name);
            }

            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        UserRef bad = new BadUserRef("x", "a@b.com", "A");
        assertNotEquals(a, bad);

        String ts = a.toString();
        assertTrue(ts.contains("x"));
        assertTrue(ts.contains("a@b.com"));
    }

    @Test
    void equalsAndHashCodeForEmptyUserRefs() {
        UserRef u1 = new UserRef();
        UserRef u2 = new UserRef();

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }
}
