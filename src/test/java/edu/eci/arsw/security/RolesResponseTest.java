package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolesResponseTest {

    @Test
    void gettersAndSettersShouldWork() {
        RolesResponse rr = new RolesResponse();
        rr.setId("1");
        rr.setEmail("user@example.com");
        rr.setName("User");
        rr.setRoles(List.of("STUDENT"));
        rr.setHasRoles(true);
        rr.setLastUpdated("now");

        assertEquals("1", rr.getId());
        assertEquals("user@example.com", rr.getEmail());
        assertEquals("User", rr.getName());
        assertEquals(List.of("STUDENT"), rr.getRoles());
        assertTrue(rr.isHasRoles());
        assertEquals("now", rr.getLastUpdated());
    }

    @Test
    void equalsHashCodeAndToStringShouldBeCovered() {
        RolesResponse a = new RolesResponse();
        a.setId("1");
        a.setEmail("user@example.com");
        a.setName("User");
        a.setRoles(List.of("STUDENT"));
        a.setHasRoles(true);
        a.setLastUpdated("now");

        RolesResponse b = new RolesResponse();
        b.setId("1");
        b.setEmail("user@example.com");
        b.setName("User");
        b.setRoles(List.of("STUDENT"));
        b.setHasRoles(true);
        b.setLastUpdated("now");

        RolesResponse c = new RolesResponse();
        c.setId("2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(null, a);
        assertNotEquals("otro tipo", a);

        String s = a.toString();
        assertNotNull(s);
        assertTrue(s.contains("RolesResponse"));
    }
}
