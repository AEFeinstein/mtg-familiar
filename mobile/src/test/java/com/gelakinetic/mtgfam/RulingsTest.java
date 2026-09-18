package com.gelakinetic.mtgfam;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.helpers.ScryfallRulingsHelper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for Card Rulings.
 */
public class RulingsTest {

    /**
     * Phase 1 (Fail First): Verify that the legacy Gatherer scraping implementation
     * fails to extract any rulings because Gatherer redesigned its site to Next.js
     * and removed the legacy table element 'div[id*=rulingsContainer] > table > tbody > tr'.
     */
    @Test
    public void testGathererScraperFailsToFindRulings() throws Exception {
        Document document = Jsoup.connect("https://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=1").get();
        Elements rulingTable = document.select("div[id*=rulingsContainer] > table > tbody > tr");
        assertEquals("Gatherer scraper fails to locate rulings due to Next.js redesign", 0, rulingTable.size());
    }

    /**
     * Test Case 1: Fetching rulings via the primary Multiverse ID endpoint.
     */
    @Test
    public void testPrimaryMultiverseIdEndpoint() throws Exception {
        // Ankh of Mishra has Multiverse ID 1 and multiple official rulings on Scryfall
        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.fetchRulings(1, "lea", "1", null);
        assertNotNull(rulings);
        assertFalse("Primary Multiverse ID endpoint must return rulings for Ankh of Mishra", rulings.isEmpty());
        assertTrue("Ruling should contain date in YYYY-MM-DD format", rulings.get(0).toString().matches("^\\d{4}-\\d{2}-\\d{2}.*"));
    }

    /**
     * Test Case 2: Fetching rulings via the fallback Set Code and Collector Number endpoint.
     */
    @Test
    public void testFallbackSetAndNumberEndpoint() throws Exception {
        // Multiverse ID 0 simulates a card with no multiverse ID (or 404), triggering fallback
        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.fetchRulings(0, "lea", "1", null);
        assertNotNull(rulings);
        assertFalse("Fallback set and number endpoint must return rulings", rulings.isEmpty());
        assertTrue("Ruling should contain date in YYYY-MM-DD format", rulings.get(0).toString().matches("^\\d{4}-\\d{2}-\\d{2}.*"));
    }

    /**
     * Test Case 3: Official WotC ruling source formatting (no tag appended to date).
     */
    @Test
    public void testOfficialWotcRulingSource() throws Exception {
        String json = "{" +
                "  \"object\": \"list\"," +
                "  \"has_more\": false," +
                "  \"data\": [" +
                "    {" +
                "      \"object\": \"ruling\"," +
                "      \"source\": \"wotc\"," +
                "      \"published_at\": \"2004-10-04\"," +
                "      \"comment\": \"This triggers on any land entering.\"" +
                "    }" +
                "  ]" +
                "}";

        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.parseRulingsJson(json);
        assertEquals(1, rulings.size());
        assertEquals("2004-10-04: This triggers on any land entering.", rulings.get(0).toString());
    }

    /**
     * Test Case 4: Scryfall ruling source formatting (appends '(Scryfall)' tag).
     */
    @Test
    public void testScryfallRulingSource() throws Exception {
        String json = "{" +
                "  \"object\": \"list\"," +
                "  \"has_more\": false," +
                "  \"data\": [" +
                "    {" +
                "      \"object\": \"ruling\"," +
                "      \"source\": \"scryfall\"," +
                "      \"published_at\": \"2022-10-07\"," +
                "      \"comment\": \"This card text was updated in an errata.\"" +
                "    }" +
                "  ]" +
                "}";

        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.parseRulingsJson(json);
        assertEquals(1, rulings.size());
        assertEquals("2022-10-07 (Scryfall): This card text was updated in an errata.", rulings.get(0).toString());
    }

    /**
     * Test Case 5: Unknown future ruling source formatting (e.g. 'commander_rc' -> '(Commander Rc)').
     */
    @Test
    public void testUnknownFutureRulingSource() throws Exception {
        String json = "{" +
                "  \"object\": \"list\"," +
                "  \"has_more\": false," +
                "  \"data\": [" +
                "    {" +
                "      \"object\": \"ruling\"," +
                "      \"source\": \"commander_rc\"," +
                "      \"published_at\": \"2025-01-15\"," +
                "      \"comment\": \"Format philosophy clarification.\"" +
                "    }" +
                "  ]" +
                "}";

        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.parseRulingsJson(json);
        assertEquals(1, rulings.size());
        assertEquals("2025-01-15 (Commander Rc): Format philosophy clarification.", rulings.get(0).toString());
    }

    /**
     * Test Case 6: Empty data response (HTTP 200 with empty list), triggering 'No rulings for this card'.
     */
    @Test
    public void testNoRulingsEmptyDataResponse() throws Exception {
        String json = "{\"object\":\"list\",\"has_more\":false,\"data\":[]}";
        ArrayList<CardViewFragment.Ruling> rulings = ScryfallRulingsHelper.parseRulingsJson(json);
        assertEquals(0, rulings.size());
    }

    /**
     * Test Case 7: API Error and malformed JSON handling (throws IOException to trigger Snackbar).
     */
    @Test
    public void testApiErrorAndMalformedJsonHandling() {
        // 1. Scryfall error object response
        String errorJson = "{\"object\":\"error\",\"code\":\"not_found\",\"status\":404,\"details\":\"Card not found\"}";
        try {
            ScryfallRulingsHelper.parseRulingsJson(errorJson);
            fail("Should throw IOException on Scryfall error response");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Card not found"));
        }

        // 2. Malformed JSON
        try {
            ScryfallRulingsHelper.parseRulingsJson("{not valid json}");
            fail("Should throw IOException on malformed JSON");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Malformed JSON"));
        }

        // 3. Null / empty string
        try {
            ScryfallRulingsHelper.parseRulingsJson("");
            fail("Should throw IOException on empty JSON string");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Empty response"));
        }
    }

    /**
     * Test Case 8: Ruling toString string generation.
     */
    @Test
    public void testRulingToStringFormatting() {
        CardViewFragment.Ruling rulingWotc = new CardViewFragment.Ruling("2024-01-01", "Power > 2 & Toughness < 4.");
        assertEquals("2024-01-01: Power > 2 & Toughness < 4.", rulingWotc.toString());

        CardViewFragment.Ruling rulingScryfall = new CardViewFragment.Ruling("2020-06-01 (Scryfall)", "Companion errata.");
        assertEquals("2020-06-01 (Scryfall): Companion errata.", rulingScryfall.toString());
    }

    /**
     * Test Case 9: Ruling HTML string generation with bold date header and safe character escaping via HtmlUtils.
     */
    @Test
    public void testRulingToHtmlStringFormatting() {
        CardViewFragment.Ruling rulingWotc = new CardViewFragment.Ruling("2024-01-01", "Power > 2 & Toughness < 4.");
        assertEquals("<b>2024-01-01:</b> Power &gt; 2 &amp; Toughness &lt; 4.", rulingWotc.toHtmlString());

        CardViewFragment.Ruling rulingScryfall = new CardViewFragment.Ruling("2020-06-01 (Scryfall)", "Companion errata.");
        assertEquals("<b>2020-06-01 (Scryfall):</b> Companion errata.", rulingScryfall.toHtmlString());
    }
}