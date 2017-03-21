/**
 * Copyright 2011 Adam Feinstein
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.updaters;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.Patch;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.GathererScraper.PrefixedFieldNamingStrategy;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * This class is used to parse various JSON update files and populate the database
 */
class CardAndSetParser {
    /* Hardcoded URLs */
    private static final String PATCHES_URL = "https://raw.githubusercontent.com/aefeinstein/GathererScraper/master/patches.json";
    private static final String LEGALITY_URL = "https://raw.githubusercontent.com/aefeinstein/GathererScraper/master/legality.json";

    /**
     * Used to store various dates before committing them
     */
    long mCurrentPatchTimestamp = 0;
    long mCurrentRulesTimestamp = 0;

    public static Gson getGson() {
        GsonBuilder reader = new GsonBuilder();
        reader.setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")));
        reader.disableHtmlEscaping();
        reader.setPrettyPrinting();
        return reader.create();
    }

    /**
     * If a set has a patch, and doesn't exist in the database, this is called to parse an InputStream of JSON and add
     * it into the database.
     * <p/>
     * The JSON uses single character keys, which is a silly thing I did in the name of compression. The patches are
     * zipped anyway, so it doesn't matter much, but we're stuck with it.
     * <p/>
     * There is some special processing for weird power and toughness too
     *
     * @param reader           A JsonRead to parse from
     * @param progressReporter A percentage progress is reported through this class to be shown in the notification
     * @param cardsToAdd       An array list to place cards before adding to the database
     * @param setsToAdd        An array list to place sets before adding to the database
     * @throws IOException If something goes wrong with the InputStream, this will be thrown
     */
    public void readCardJsonStream(JsonReader reader, CardProgressReporter progressReporter, ArrayList<MtgCard> cardsToAdd, ArrayList<Expansion> setsToAdd)
            throws IOException {

        ArrayList<MtgCard> tempCardsToAdd = new ArrayList<>();

        Gson gson = CardAndSetParser.getGson();

        Patch patch = gson.fromJson(reader, Patch.class);
        if(patch != null)
        {


            int numTotalElements = patch.mCards.size();
            int elementsParsed = 0;
            for(Card card : patch.mCards)
            {
                tempCardsToAdd.add(new MtgCard(card));
                elementsParsed++;
                progressReporter.reportJsonCardProgress((int) Math.round(100 * elementsParsed / (double) numTotalElements));
            }

            /* Calculate the color identity for all cards just downloaded */
            for (MtgCard card : tempCardsToAdd) {
                card.calculateColorIdentity(tempCardsToAdd);
            }

            /* Stage the sets and cards for database addition. */
            if(setsToAdd != null) {
                 setsToAdd.add(patch.mExpansion);
            }
            if(cardsToAdd != null) {
                cardsToAdd.addAll(tempCardsToAdd);
            }
        }


    }

    /**
     * This method checks the hardcoded URL and downloads a list of patches to be checked
     *
     * @param logWriter A writer to print debug statements when things go wrong
     * @return An ArrayList of String[] which contains the {Name, URL, Set Code} for each available patch
     */
    public Manifest readUpdateJsonStream(PrintWriter logWriter) {
        Manifest manifest = null;

        try {
            InputStream stream = FamiliarActivity.getHttpInputStream(PATCHES_URL, logWriter);
            if (stream == null) {
                throw new IOException("No Stream");
            }
            InputStreamReader isr = new InputStreamReader(stream);

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
     * @param prefAdapter The preference adapter is used to get the last update time
     * @param logWriter   A writer to print debug statements when things go wrong
     * @return An object with all of the legal info, to be added to the database in one fell swoop
     */
    public LegalityData readLegalityJsonStream(PreferenceAdapter prefAdapter, PrintWriter logWriter) {

        LegalityData legalityDatas = null;

        try {
            InputStream stream = FamiliarActivity.getHttpInputStream(LEGALITY_URL, logWriter);
            if (stream == null) {
                throw new IOException("No Stream");
            }
            JsonReader reader = new JsonReader(new InputStreamReader(stream, "ISO-8859-1"));
            Gson gson = CardAndSetParser.getGson();

            legalityDatas = gson.fromJson(reader, LegalityData.class);

            mCurrentRulesTimestamp = legalityDatas.mTimestamp;
            long spDate = prefAdapter.getLegalityTimestamp();
            if (spDate >= mCurrentRulesTimestamp) {
                legalityDatas = null; /* dates match, nothing new here. */
            }
        } catch (IOException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            legalityDatas = null;
        }
        return legalityDatas;
    }

    /**
     * When the service is done, this method is called to commit the update dates to the shared preferences
     *
     * @param prefAdapter The preferences to write to
     */
    public void commitDates(PreferenceAdapter prefAdapter) {
        prefAdapter.setLastUpdateTimestamp(mCurrentPatchTimestamp);
        prefAdapter.setLegalityTimestamp(mCurrentRulesTimestamp);
        mCurrentPatchTimestamp = 0;
        mCurrentRulesTimestamp = 0;
    }

    /**
     * Parse a string containing a card Power or Toughness and transform it to a float value.
     * @param powerToughness The Power or the Toughness of the card
     * @return The Power/Toughness transformed into a float.
     */
    public float parsePowerToughness(String powerToughness)
    {
        float retval = 0.0f;

        try {
            retval = Integer.parseInt(powerToughness);
        }
        catch (NumberFormatException e) {
            switch (powerToughness) {
                case "*":
                    retval = CardDbAdapter.STAR;
                    break;
                case "1+*":
                    retval = CardDbAdapter.ONE_PLUS_STAR;
                    break;
                case "2+*":
                    retval = CardDbAdapter.TWO_PLUS_STAR;
                    break;
                case "7-*":
                    retval = CardDbAdapter.SEVEN_MINUS_STAR;
                    break;
                case "*{^2}":
                    retval = CardDbAdapter.STAR_SQUARED;
                    break;
                case "{1/2}":
                    retval = 0.5f;
                    break;
                case "1{1/2}":
                    retval = 1.5f;
                    break;
                case "2{1/2}":
                    retval = 2.5f;
                    break;
                case "3{1/2}":
                    retval = 3.5f;
                    break;
                default:
                    break;
            }
        }

        return retval;
    }


    /**
     * This interface is implemented by ProgressReporter in DbUpdaterService. It's used to report progress to the
     * notification
     */
    public interface CardProgressReporter {
        void reportJsonCardProgress(int progress);
    }

    class LegalInfo {
        final ArrayList<NameAndMetadata> legalSets = new ArrayList<>();
        final ArrayList<NameAndMetadata> bannedCards = new ArrayList<>();
        final ArrayList<NameAndMetadata> restrictedCards = new ArrayList<>();
        final ArrayList<String> formats = new ArrayList<>();
    }

    class NameAndMetadata {
        final String name;
        final String metadata;

        public NameAndMetadata(String restrictedCard, String formatName) {
            name = restrictedCard;
            metadata = formatName;
        }
    }
}
