package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicProfileTest {

    @Test
    void builderAndAllArgsConstructorShouldProduceEquivalentObjects() {
        PublicProfile p1 = PublicProfile.builder()
                .id("id1")
                .sub("sub1")
                .name("User")
                .email("user@example.com")
                .avatarUrl("avatar")
                .build();

        PublicProfile p2 = new PublicProfile("id1", "sub1", "User",
                "user@example.com", "avatar");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void settersAndGettersShouldWork() {
        PublicProfile p = new PublicProfile();
        p.setId("id2");
        p.setSub("sub2");
        p.setName("Name");
        p.setEmail("mail@example.com");
        p.setAvatarUrl("avatar2");

        assertEquals("id2", p.getId());
        assertEquals("sub2", p.getSub());
        assertEquals("Name", p.getName());
        assertEquals("mail@example.com", p.getEmail());
        assertEquals("avatar2", p.getAvatarUrl());

        assertNotEquals(null, p);
        assertNotEquals("otro", p.toString());
        assertTrue(p.toString().contains("mail@example.com"));
    }
}
