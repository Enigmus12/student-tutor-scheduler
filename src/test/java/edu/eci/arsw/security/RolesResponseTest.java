package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolesResponseTest {

    @Test
    void gettersSettersEqualsHashCodeToStringAndCanEqualShouldBeCovered() {
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

        RolesResponse b = new RolesResponse();
        b.setId("1");
        b.setEmail("user@example.com");
        b.setName("User");
        b.setRoles(List.of("STUDENT"));
        b.setHasRoles(true);
        b.setLastUpdated("now");

        RolesResponse c = new RolesResponse();
        c.setId("2");

        assertEquals(rr, b);
        assertEquals(rr, rr);              
        assertEquals(rr.hashCode(), b.hashCode());
        assertNotEquals(c, rr);
        assertNotEquals(null, rr);
        assertNotEquals("otro tipo", rr);

        assertTrue(rr.canEqual(b));
        assertFalse(rr.canEqual(new Object()));

        class BadRolesResponse extends RolesResponse {
            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        RolesResponse bad = new BadRolesResponse();
        bad.setId("1");
        bad.setEmail("user@example.com");
        bad.setName("User");

        assertNotEquals(rr, bad);

        String s = rr.toString();
        assertNotNull(s);
        assertTrue(s.contains("RolesResponse"));
    }

    @Test
    void equalsAndHashCodeForEmptyInstances() {
        RolesResponse a = new RolesResponse();
        RolesResponse b = new RolesResponse();

        assertEquals(a, b);                
        assertEquals(a.hashCode(), b.hashCode());
    }
}
