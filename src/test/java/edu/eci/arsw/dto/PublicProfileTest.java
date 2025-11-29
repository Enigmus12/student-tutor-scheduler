package edu.eci.arsw.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicProfileTest {

    @Test
    void builderAllArgsEqualsHashCodeCanEqualAndBuilderToString() {
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
        assertEquals(p1, p1);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertTrue(p1.canEqual(p2));
        assertFalse(p1.canEqual(new Object()));
        assertNotEquals(null, p1);
        assertNotEquals("otro", p1);

        class BadPublicProfile extends PublicProfile {
            public BadPublicProfile(String id, String sub, String name, String email, String avatarUrl) {
                super(id, sub, name, email, avatarUrl);
            }

            @Override
            protected boolean canEqual(Object other) {
                return false;
            }
        }
        PublicProfile bad = new BadPublicProfile("id1", "sub1", "User",
                "user@example.com", "avatar");
        assertNotEquals(p1, bad);

        // cubre PublicProfile.PublicProfileBuilder.toString()
        String builderString = PublicProfile.builder()
                .id("X")
                .email("x@y.com")
                .toString();
        assertNotNull(builderString);
    }

    @Test
    void settersGettersAndEmptyEqualityShouldWork() {
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

        PublicProfile empty1 = new PublicProfile();
        PublicProfile empty2 = new PublicProfile();
        assertEquals(empty1, empty2);
        assertEquals(empty1.hashCode(), empty2.hashCode());

        String ts = p.toString();
        assertTrue(ts.contains("mail@example.com"));
    }

    @Test
    void equalsShouldDetectDifferences() {
        PublicProfile base = new PublicProfile("id", "sub", "Name", "a@b.com", "av");

        PublicProfile diffId = new PublicProfile("other", "sub", "Name", "a@b.com", "av");
        PublicProfile diffSub = new PublicProfile("id", "other", "Name", "a@b.com", "av");
        PublicProfile diffName = new PublicProfile("id", "sub", "Other", "a@b.com", "av");
        PublicProfile diffMail = new PublicProfile("id", "sub", "Name", "x@y.com", "av");
        PublicProfile diffAvatar = new PublicProfile("id", "sub", "Name", "a@b.com", "other");

        assertNotEquals(base, diffId);
        assertNotEquals(base, diffSub);
        assertNotEquals(base, diffName);
        assertNotEquals(base, diffMail);
        assertNotEquals(base, diffAvatar);
    }
}
