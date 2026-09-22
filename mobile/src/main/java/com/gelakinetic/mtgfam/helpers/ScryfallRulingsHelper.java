package com.gelakinetic.mtgfam.helpers;

import android.content.Context;

import androidx.annotation.Nullable;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Helper class to fetch and parse card rulings from the Scryfall API.
 */
public class ScryfallRulingsHelper {

    /**
     * Parses a JSON string returned by Scryfall's rulings API into a list of Ruling objects.
     *
     * @param jsonString The raw JSON response string from Scryfall.
     * @return An ArrayList of parsed rulings.
     * @throws IOException If the JSON is malformed, represents an error, or is empty.
     */
    public static ArrayList<CardViewFragment.Ruling> parseRulingsJson(String jsonString) throws IOException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IOException("Empty response from rulings API");
        }
        return parseRulingsJson(new StringReader(jsonString));
    }

    /**
     * Parses a JSON reader returned by Scryfall's rulings API into a list of Ruling objects.
     *
     * @param reader The Reader containing JSON response from Scryfall.
     * @return An ArrayList of parsed rulings.
     * @throws IOException If the JSON is malformed, represents an error, or is empty.
     */
    public static ArrayList<CardViewFragment.Ruling> parseRulingsJson(Reader reader) throws IOException {
        if (reader == null) {
            throw new IOException("Null reader for rulings API");
        }

        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseReader(reader);
        } catch (JsonSyntaxException e) {
            throw new IOException("Malformed JSON response from rulings API", e);
        }

        if (!rootElement.isJsonObject()) {
            throw new IOException("Invalid JSON response from rulings API");
        }

        JsonObject root = rootElement.getAsJsonObject();
        if (root.has("object") && "error".equalsIgnoreCase(root.get("object").getAsString())) {
            String details = root.has("details") ? root.get("details").getAsString() : "Scryfall API error";
            throw new IOException(details);
        }

        if (!root.has("data") || !root.get("data").isJsonArray()) {
            throw new IOException("Unexpected response format: missing data array");
        }

        JsonArray dataArray = root.getAsJsonArray("data");
        ArrayList<CardViewFragment.Ruling> rulingsList = new ArrayList<>();

        for (JsonElement element : dataArray) {
            if (element.isJsonObject()) {
                JsonObject item = element.getAsJsonObject();
                String publishedAt = item.has("published_at") ? item.get("published_at").getAsString() : "";
                String comment = item.has("comment") ? item.get("comment").getAsString() : "";
                String source = item.has("source") ? item.get("source").getAsString() : "wotc";

                String dateDisplay;
                if ("wotc".equalsIgnoreCase(source)) {
                    dateDisplay = publishedAt;
                } else {
                    dateDisplay = publishedAt + " (" + formatSource(source) + ")";
                }

                rulingsList.add(new CardViewFragment.Ruling(dateDisplay, comment));
            }
        }

        return rulingsList;
    }

    /**
     * Formats a source identifier into a human-readable title-cased string.
     * E.g. "scryfall" -> "Scryfall", "commander_rc" -> "Commander Rc".
     *
     * @param source The raw source name from Scryfall API.
     * @return The formatted source name.
     */
    public static String formatSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "";
        }
        String[] parts = source.trim().split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase(Locale.US));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Fetches card rulings from Scryfall using Familiar's getHttpInputStream,
     * trying the primary Multiverse ID endpoint first and falling back to
     * set code and collector number if needed.
     *
     * @param multiverseId The multiverse ID of the card.
     * @param setCode      The Scryfall set code.
     * @param number       The collector number.
     * @param context      Context used to resolve User-Agent, or null.
     * @return An ArrayList of parsed rulings.
     * @throws IOException If a network error occurs.
     */
    public static ArrayList<CardViewFragment.Ruling> fetchRulings(
            int multiverseId,
            String setCode,
            String number,
            @Nullable Context context) throws IOException {

        // 1. Try Primary endpoint if multiverseId is valid
        if (multiverseId > 0) {
            String primaryUrl = "https://api.scryfall.com/cards/multiverse/" + multiverseId + "/rulings";
            InputStream stream = FamiliarActivity.getHttpInputStream(
                    primaryUrl,
                    null,
                    context,
                    Collections.singletonMap("Accept", "application/json;q=0.9,*/*;q=0.8")
            );
            if (stream != null) {
                try (InputStream is = stream;
                     InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return parseRulingsJson(isr);
                }
            }
        }

        // 2. Try Fallback endpoint using set code and collector number
        if (setCode != null && !setCode.trim().isEmpty() && number != null && !number.trim().isEmpty()) {
            String fallbackUrl = "https://api.scryfall.com/cards/" +
                    setCode.trim().toLowerCase(Locale.US) + "/" +
                    number.trim().toLowerCase(Locale.US) + "/rulings";
            InputStream stream = FamiliarActivity.getHttpInputStream(
                    fallbackUrl,
                    null,
                    context,
                    Collections.singletonMap("Accept", "application/json;q=0.9,*/*;q=0.8")
            );
            if (stream != null) {
                try (InputStream is = stream;
                     InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return parseRulingsJson(isr);
                }
            }
        }

        return new ArrayList<>();
    }
}