package io.dsub.dumpdbmgmt.entity;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Profile(value = "test")
class ArtistTest {

    Artist artist;
    Set<String> urls;
    Set<String> nameVar;

    @BeforeEach
    void setUp() {
        this.nameVar = Collections.synchronizedSet(new HashSet<>());
        this.nameVar.add("nameVar_1");
        this.nameVar.add("nameVar_2");
        this.nameVar.add("nameVar_3");
        this.nameVar.add("nameVar_4");

        this.urls = Collections.synchronizedSet(new HashSet<>());
        this.urls.add("https://newItemOne.com");
        this.urls.add("https://newItemTwo.com");
        this.urls.add("https://newItemThree.com");
        this.urls.add("https://newItemFour.com");

        this.artist = new Artist();
        this.artist = artist.withId(10L);
        this.artist = artist.withDataQuality("Needs Vote");
        this.artist = artist.withName("Aphex");
        this.artist = artist.withNameVariations(nameVar);
        this.artist = artist.withRealName("Aphex Real Name");
        this.artist = artist.withUrls(urls);
        this.artist = artist.withProfile("Aphex Profile");
    }

    @Test
    void getId() {
        assertEquals(artist.getId(), 10L);
    }

    @Test
    void getName() {
        assertEquals(artist.getName(), "Aphex");
    }

    @Test
    void getRealName() {
        assertEquals(artist.getRealName(), "Aphex Real Name");
    }

    @Test
    void getProfile() {
        assertEquals(artist.getProfile(), "Aphex Profile");
    }

    @Test
    void getDataQuality() {
        assertEquals(artist.getDataQuality(), "Needs Vote");
    }

    @Test
    void getUrls() {
        assertNotNull(artist.getUrls());
        assertEquals(artist.getUrls(), this.urls);
        Artist instance = artist.withUrls(urls);
        assertEquals(artist.getUrls(), instance.getUrls());
    }

    @Test
    void getNameVariations() {
        assertNotNull(artist.getNameVariations());
        assertEquals(artist.getNameVariations(), this.nameVar);
        Artist instance = artist.withUrls(nameVar);
        assertEquals(artist.getNameVariations(), instance.getNameVariations());
    }

    @Test
    void withUrls() {
        Artist instance = artist.withUrls(urls);
        assertEquals(instance, artist);
        instance = instance.withUrls(null);
        assertEquals(instance, artist);
        assertNull(instance.getUrls());
    }

    @Test
    void withNameVariations() {
        Artist instance = artist.withNameVariations(nameVar);
        assertEquals(instance, artist);
        instance = instance.withNameVariations(null);
        assertEquals(instance, artist);
        assertNull(instance.getNameVariations());
    }

    @Test
    void withId() {
        Artist instance = new Artist();
        instance = instance.withId(null);
        assertNull(instance.getId());
        instance = artist.withId(10L);
        assertEquals (10L, instance.getId());
        instance = instance.withId(null);
        assertNotEquals(instance, artist);
    }

    @Test
    void withName() {
        Artist instance = artist.withName("New Name");
        assertNotEquals(instance, artist);
    }

    @Test
    void withRealName() {
        Artist instance = artist.withRealName(null);
        assertNotEquals(instance, artist);
        assertNull(instance.getRealName());
    }

    @Test
    void withProfile() {
        Artist instance = artist.withProfile(null);
        assertNotEquals(instance, artist);
        assertNull(instance.getProfile());
        instance = instance.withProfile("new");
        assertEquals("new", instance.getProfile());
    }

    @Test
    void withDataQuality() {
        Artist instance = artist.withDataQuality("Correct");
        assertNotEquals(artist, instance);
        assertEquals("Correct", instance.getDataQuality());
    }

    @Test
    void testEquals() {
        Artist instance = artist.withProfile("New Profile");
        boolean result = instance.equals(artist);
        assertEquals(instance, instance);
        assertFalse(result);

        instance = artist.withProfile(artist.getProfile());
        result = instance.equals(artist);

        assertTrue(result);
    }

    @Test
    void testHashCode() {
        Integer hashCode = artist.hashCode();
        Artist instance = artist.withId(artist.getId());
        assertEquals(instance.hashCode(), hashCode);
        instance = instance.withId(1L);
        assertNotEquals(instance.hashCode(), hashCode);
    }

    @Test
    void testToString() {
        String result = artist.toString();
        assertTrue(result.contains(artist.getName())
                && result.contains(artist.getProfile())
                && result.contains(artist.getDataQuality())
                && result.contains(artist.getRealName())
                && result.contains(String.valueOf(artist.getId())));
    }

    @Test
    void testWithAddAliasArtist() {
        for (int i = 1; i < 11; i++) {
           artist = artist.withAddAliasArtists((long)i);
        }
        assertEquals(10, artist.getAliases().length);
    }

    @Test
    void testWithRemoveAliasArtist() {
        for (int i = 1; i < 11; i++) {
            artist = artist.withAddAliasArtists((long)i);
        }
        artist = artist.withRemoveAliasArtist(6L);
        assertEquals(9, artist.getAliases().length);
    }
}
