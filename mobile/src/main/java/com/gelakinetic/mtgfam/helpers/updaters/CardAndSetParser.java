/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.updaters;

import android.content.Context;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.GathererScraper.JsonTypes.Patch;
import com.gelakinetic.GathererScraper.PrefixedFieldNamingStrategy;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * This class is used to parse various JSON update files and populate the database
 */
class CardAndSetParser {
    /* Hardcoded URLs */
    private static final String PATCHES_URL = "https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/patches-v2/patches.json";
    private static final String LEGALITY_URL = "https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/patches-v2/legality.json";

    /**
     * Used to store various dates before committing them
     */
    long mCurrentLegalityTimestamp = 0;

    private static Gson getGson() {
        GsonBuilder reader = new GsonBuilder();
        reader.setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")));
        reader.disableHtmlEscaping();
        reader.setPrettyPrinting();
        return reader.create();
    }

    /**
     * If a set has a patch, and doesn't exist in the database, this is called to parse an InputStream of JSON and add
     * it into the database.
     * <p>
     * The JSON uses single character keys, which is a silly thing I did in the name of compression. The patches are
     * zipped anyway, so it doesn't matter much, but we're stuck with it.
     * <p>
     * There is some special processing for weird power and toughness too
     *
     * @param reader     A JsonRead to parse from
     * @param cardsToAdd An array list to place cards before adding to the database
     * @param setsToAdd  An array list to place sets before adding to the database
     */
    public void readCardJsonStream(JsonReader reader, ArrayList<Card> cardsToAdd, ArrayList<Expansion> setsToAdd) {

        Gson gson = CardAndSetParser.getGson();

        Patch patch = gson.fromJson(reader, Patch.class);
        if (patch != null) {
            cardsToAdd.addAll(patch.mCards);

            /* Stage the sets and cards for database addition. */
            if (setsToAdd != null) {
                setsToAdd.add(patch.mExpansion);
            }
        }
    }

    /**
     * This method checks the hardcoded URL and downloads a list of patches to be checked
     *
     * @param logWriter A writer to print debug statements when things go wrong
     * @param context   The context to manage preferences with
     * @return An ArrayList of String[] which contains the {Name, URL, Set Code} for each available patch
     */
    public Manifest readUpdateJsonStream(Context context, PrintWriter logWriter) {
        Manifest manifest;

        try {
            InputStream stream = FamiliarActivity.getHttpInputStream(PATCHES_URL, logWriter, context);
            if (stream == null) {
                throw new IOException("No Stream");
            }
            InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);

            JsonReader reader = new JsonReader(isr);
            Gson gson = CardAndSetParser.getGson();

            manifest = gson.fromJson(reader, Manifest.class);
        } catch (IOException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            manifest = null;
        }
        return manifest;
    }

    /**
     * Parses the legality file and populates the database with the different formats, their respective sets, and their
     * banned and restricted lists
     *
     * @param context   The context to manage preferences with
     * @param logWriter A writer to print debug statements when things go wrong
     * @return An object with all of the legal info, to be added to the database in one fell swoop
     */
    public LegalityData readLegalityJsonStream(Context context, PrintWriter logWriter) {

        LegalityData legalityData;

        try {
            InputStream stream = FamiliarActivity.getHttpInputStream(LEGALITY_URL, logWriter, context);
            if (stream == null) {
                throw new IOException("No Stream");
            }
            JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            Gson gson = CardAndSetParser.getGson();

            legalityData = gson.fromJson(reader, LegalityData.class);

            mCurrentLegalityTimestamp = legalityData.mTimestamp;
            long spDate = PreferenceAdapter.getLegalityTimestamp(context);
            if (spDate >= mCurrentLegalityTimestamp) {
                legalityData = null; /* dates match, nothing new here. */
            }
        } catch (IOException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            legalityData = null;
        }
        return legalityData;
    }

    /**
     * When the service is done, this method is called to commit the update dates to the shared preferences
     *
     * @param context the Context to manage preferences with
     */
    public void commitDates(Context context) {
        PreferenceAdapter.setLegalityTimestamp(context, mCurrentLegalityTimestamp);
        mCurrentLegalityTimestamp = 0;
    }
}
