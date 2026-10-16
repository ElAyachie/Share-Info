package com.example.shareinfo.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class TopicTest {

    @Test
    public void cleansCorporateSuffixes() {
        assertEquals("Apple", Topic.cleanName("Apple Inc.", "AAPL"));
        assertEquals("Zeta Global", Topic.cleanName("Zeta Global Holdings Corp", "ZETA"));
        assertEquals("SK Hynix", Topic.cleanName("SK Hynix Inc - ADR", "SKHY"));
        assertEquals("Nebius", Topic.cleanName("Nebius Group N.V. ", "NBIS"));
        assertEquals("Walt Disney", Topic.cleanName("The Walt Disney Co", "DIS"));
        assertEquals("Bitcoin", Topic.cleanName("Bitcoin USD", "BTC"));
        assertEquals("AAPL", Topic.cleanName("", "AAPL"));
        assertEquals("AAPL", Topic.cleanName(null, "AAPL"));
    }

    @Test
    public void buildsSearchQueries() {
        Topic apple = Topic.stock("aapl", "Apple Inc.");
        assertEquals("AAPL", apple.symbol);
        assertEquals("\"Apple\" stock", apple.newsQuery());
        assertEquals("$AAPL", apple.socialQuery());

        Topic cards = Topic.custom("  pokemon   cards ");
        assertEquals("pokemon cards", cards.name);
        assertEquals("pokemon cards", cards.newsQuery());
        assertNull(cards.marketSymbol);
        assertEquals("OTHER:pokemon cards", cards.id());
    }

    @Test
    public void roundTripsThroughJson() {
        Topic bitcoin = Topic.crypto("BTC", "Bitcoin USD");
        Topic restored = Topic.fromJson(bitcoin.toJson());
        assertEquals(bitcoin, restored);
        assertEquals("BTC-USD", restored.marketSymbol);
        assertEquals("Bitcoin", restored.name);

        Topic cards = Topic.fromJson(Topic.custom("pokemon cards").toJson());
        assertNull(cards.symbol);
        assertEquals("pokemon cards", cards.name);
    }

    @Test
    public void identityIgnoresDisplayName() {
        assertEquals(Topic.stock("TSLA", "Tesla Inc"), Topic.stock("TSLA", "TSLA"));
        assertNotEquals(Topic.stock("BTC", "x"), Topic.crypto("BTC", "Bitcoin"));
    }
}
