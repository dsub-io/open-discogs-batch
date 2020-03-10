package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.intermed.ArtistCredit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtistCreditTest {
    ArtistCredit artistCredit;

    @BeforeEach
    void setUp() {
        artistCredit = new ArtistCredit();
    }

    @Test
    void withArtist() {
        Artist artist = new Artist();
        artist = artist.withId(1L);

        artistCredit = artistCredit.withArtist(artist.getId());
        assertEquals(artist.getId(), artistCredit.getArtist());

    }

    @Test
    void withRelease() {
        Release release = new Release(1L);

        artistCredit = artistCredit.withRelease(release.getId());
        assertEquals(release.getId(), artistCredit.getRelease());
    }

    @Test
    void withCredit() {
        String credit = "A";
        artistCredit = artistCredit.withCredit(credit);
        assertEquals(credit, artistCredit.getCredit());

    }

    @Test
    void getArtist() {
        Artist artist = new Artist();
        artist = artist.withId(1L);
        artistCredit = artistCredit.withArtist(artist.getId());
        assertEquals(artist.getId(), artistCredit.getArtist());
    }

    @Test
    void getRelease() {
        Release release = new Release(1L);
        artistCredit = artistCredit.withRelease(release.getId());
        assertEquals(release.getId(), artistCredit.getRelease());
    }

    @Test
    void getCredit() {
        String target = "321";
        artistCredit = artistCredit.withCredit(target);
        assertEquals(target, artistCredit.getCredit());
    }
}
