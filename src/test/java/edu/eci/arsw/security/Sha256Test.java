package edu.eci.arsw.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sha256Test {

    @Test
    void hashShouldReturnHexString() {
        String h = Sha256.hash("hello");

        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                h
        );

        assertEquals(64, h.length());
        assertTrue(h.matches("[0-9a-f]{64}"));
    }

    @Test
    void hashShouldReturnLiteralForNull() {
        assertEquals("NULL", Sha256.hash(null));
    }

    @Test
    void privateConstructorShouldBeInvokedViaReflection() throws Exception {
        var ctor = Sha256.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
