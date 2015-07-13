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

import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.MtgSet;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * This class is used to parse various JSON update files and populate the database
 */
class CardAndSetParser {

    /* Indices for returning patch information */
    static final int SET_CODE = 0;
    static final int SET_URL = 1;
    static final int SET_NAME = 2;

    /* Hardcoded URLs */
    private static final String PATCHES_URL = "https://sites.google.com/site/mtgfamiliar/manifests/patches.json";
    private static final String LEGALITY_URL = "https://sites.google.com/site/mtgfamiliar/manifests/legality.json";
    private static final String TCG_NAMES_URL = "https://sites.google.com/site/mtgfamiliar/manifests/TCGnames.json";

    /**
     * Used to store various dates before committing them
     */
    private String mCurrentTCGNamePatchDate = null;
    private String mCurrentPatchDate = null;
    private String mCurrentRulesDate = null;

    /**
     * If a set has a patch, and doesn't exist in the database, this is called to parse an InputStream of JSON and add
     * it into the database.
     * <p/>
     * The JSON uses single character keys, which is a silly thing I did in the name of compression. The patches are
     * zipped anyway, so it doesn't matter much, but we're stuck with it.
     * <p/>
     * There is some special processing for weird power and toughness too
     *
     * @param in               An InputStream containing valid JSON
     * @param progressReporter A percentage progress is reported through this class to be shown in the notification
     * @param cardsToAdd       An array list to place cards before adding to the database
     * @param setsToAdd        An array list to place sets before adding to the database
     * @throws IOException If something goes wrong with the InputStream, this will be thrown
     */
    public void readCardJsonStream(InputStream in, CardProgressReporter progressReporter, ArrayList<MtgCard> cardsToAdd, ArrayList<MtgSet> setsToAdd)
            throws IOException {

        JsonReader reader = new JsonReader(new InputStreamReader(in, "ISO-8859-1"));
        /* Three levels of strings for parsing nested JSON */
        String s, s1, s2;
        String pouTouStr;

        int numTotalElements = 0;
        int elementsParsed = 0;

        reader.beginObject();
        reader.nextName();
        reader.beginObject();

        while (reader.hasNext()) {

            s = reader.nextName();
            if (s.equalsIgnoreCase("v")) { /* bdd_date */
                reader.skipValue();
            }
            if (s.equalsIgnoreCase("u")) { /* bdd_version */
                reader.skipValue();
            }
            if (s.equalsIgnoreCase("s")) { /* sets */
                reader.beginObject();
                while (reader.hasNext()) {
                    s1 = reader.nextName();
                    if (s1.equalsIgnoreCase("b")) { /* set */
                        MtgSet set;

                        JsonToken jt = reader.peek();
                        if (jt.equals(JsonToken.BEGIN_OBJECT)) {
                            set = new MtgSet();
                            reader.beginObject();
                            while (reader.hasNext()) {
                                s2 = reader.nextName();
                                if (s2.equalsIgnoreCase("a")) { /* name */
                                    set.name = reader.nextString();
                                }
                                if (s2.equalsIgnoreCase("r")) { /* code_magicCards */
                                    set.codeMagicCards = reader.nextString();
                                }
                                if (s2.equalsIgnoreCase("q")) { /* code */
                                    set.code = reader.nextString();
                                }
                                if (s2.equalsIgnoreCase("y")) { /* date */
                                    set.date = reader.nextLong();
                                }
                            }
                            setsToAdd.add(set);
                            reader.endObject();
                        } else if (jt.equals(JsonToken.BEGIN_ARRAY)) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                set = new MtgSet();
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    s2 = reader.nextName();
                                    if (s2.equalsIgnoreCase("a")) { /* name */
                                        set.name = reader.nextString();
                                    }
                                    if (s2.equalsIgnoreCase("r")) { /* code_magicCards */
                                        set.codeMagicCards = reader.nextString();
                                    }
                                    if (s2.equalsIgnoreCase("q")) { /* code */
                                        set.code = reader.nextString();
                                    }
                                    if (s2.equalsIgnoreCase("y")) { /* date */
                                        set.date = reader.nextLong();
                                    }
                                }
                                setsToAdd.add(set);
                                reader.endObject();
                            }
                            reader.endArray();
                        }
                    }
                }
                reader.endObject();
            }
            if (s.equalsIgnoreCase("p")) { /* cards */

                reader.beginObject();
                while (reader.hasNext()) {
                    s1 = reader.nextName();
                    if (s1.equalsIgnoreCase("o")) { /* card */
                        MtgCard c;
                        reader.beginArray();
                        while (reader.hasNext()) {

                            reader.beginObject();
                            c = new MtgCard();
                            while (reader.hasNext()) {
                                s2 = reader.nextName();
                                if (s2.equalsIgnoreCase("a")) { /* name */
                                    c.name = reader.nextString();
                                } else if (s2.equalsIgnoreCase("b")) { /* set */
                                    c.set = reader.nextString();
                                } else if (s2.equalsIgnoreCase("c")) { /* type */
                                    c.type = reader.nextString();
                                } else if (s2.equalsIgnoreCase("d")) { /* rarity */
                                    c.rarity = reader.nextString().charAt(0);
                                } else if (s2.equalsIgnoreCase("e")) { /* manaCost */
                                    c.manaCost = reader.nextString();
                                } else if (s2.equalsIgnoreCase("f")) { /* converted_manaCost */
                                    try {
                                        c.cmc = reader.nextInt();
                                    } catch (NumberFormatException e) {
                                        reader.skipValue();
                                    }
                                } else if (s2.equalsIgnoreCase("g")) { /* power */
                                    pouTouStr = reader.nextString();
                                    try {
                                        c.power = Integer.parseInt(pouTouStr);
                                    } catch (NumberFormatException e) {
                                        switch (pouTouStr) {
                                            case "*":
                                                c.power = CardDbAdapter.STAR;
                                                break;
                                            case "1+*":
                                                c.power = CardDbAdapter.ONE_PLUS_STAR;
                                                break;
                                            case "2+*":
                                                c.power = CardDbAdapter.TWO_PLUS_STAR;
                                                break;
                                            case "7-*":
                                                c.power = CardDbAdapter.SEVEN_MINUS_STAR;
                                                break;
                                            case "*{^2}":
                                                c.power = CardDbAdapter.STAR_SQUARED;
                                                break;
                                            case "{1/2}":
                                                c.power = 0.5f;
                                                break;
                                            case "1{1/2}":
                                                c.power = 1.5f;
                                                break;
                                            case "2{1/2}":
                                                c.power = 2.5f;
                                                break;
                                            case "3{1/2}":
                                                c.power = 3.5f;
                                                break;
                                        }
                                    }
                                } else if (s2.equalsIgnoreCase("h")) { /* toughness */
                                    pouTouStr = reader.nextString();
                                    try {
                                        c.toughness = Integer.parseInt(pouTouStr);
                                    } catch (NumberFormatException e) {
                                        switch (pouTouStr) {
                                            case "*":
                                                c.toughness = CardDbAdapter.STAR;
                                                break;
                                            case "1+*":
                                                c.toughness = CardDbAdapter.ONE_PLUS_STAR;
                                                break;
                                            case "2+*":
                                                c.toughness = CardDbAdapter.TWO_PLUS_STAR;
                                                break;
                                            case "7-*":
                                                c.toughness = CardDbAdapter.SEVEN_MINUS_STAR;
                                                break;
                                            case "*{^2}":
                                                c.toughness = CardDbAdapter.STAR_SQUARED;
                                                break;
                                            case "{1/2}":
                                                c.toughness = 0.5f;
                                                break;
                                            case "1{1/2}":
                                                c.toughness = 1.5f;
                                                break;
                                            case "2{1/2}":
                                                c.toughness = 2.5f;
                                                break;
                                            case "3{1/2}":
                                                c.toughness = 3.5f;
                                                break;
                                        }
                                    }
                                } else if (s2.equalsIgnoreCase("i")) { /* loyalty */
                                    try {
                                        c.loyalty = reader.nextInt();
                                    } catch (NumberFormatException e) {
                                        reader.skipValue();
                                    }
                                } else if (s2.equalsIgnoreCase("j")) { /* ability */
                                    c.ability = reader.nextString();
                                } else if (s2.equalsIgnoreCase("k")) { /* flavor */
                                    c.flavor = reader.nextString();
                                } else if (s2.equalsIgnoreCase("l")) { /* artist */
                                    c.artist = reader.nextString();
                                } else if (s2.equalsIgnoreCase("m")) { /* number */
                                    c.number = reader.nextString();
                                } else if (s2.equalsIgnoreCase("n")) { /* color */
                                    c.color = reader.nextString();
                                } else if (s2.equalsIgnoreCase("x")) { /* multiverse id */
                                    try {
                                        c.multiverseId = reader.nextInt();
                                    } catch (NumberFormatException e) {
                                        reader.skipValue();
                                    }
                                }
                            }
                            cardsToAdd.add(c);
                            elementsParsed++;
                            progressReporter.reportJsonCardProgress(
                                    (int) Math.round(100 * elementsParsed / (double) numTotalElements));
                            reader.endObject();
                        }
                        reader.endArray();
                    }
                }
                reader.endObject();
            }
            if (s.equalsIgnoreCase("w")) { /* num_cards */
                try {
                    numTotalElements = reader.nextInt();
                } catch (NumberFormatException e) {
                    reader.skipValue();
                }
            }
        }
        reader.endObject();
        reader.close();
    }

    /**
     * This method checks the hardcoded URL and downloads a list of patches to be checked
     *
     * @param prefAdapter The preference adapter is used to get the last update time
     * @return An ArrayList of String[] which contains the {Name, URL, Set Code} for each available patch
     * @throws IOException Thrown if something goes wrong with the InputStream from the web
     */
    public ArrayList<String[]> readUpdateJsonStream(PreferenceAdapter prefAdapter) throws IOException {
        ArrayList<String[]> patchInfo = new ArrayList<>();
        URL update;
        String label;
        String label2;

        update = new URL(PATCHES_URL);
        InputStreamReader isr = new InputStreamReader(update.openStream(), "ISO-8859-1");
        JsonReader reader = new JsonReader(isr);

        reader.beginObject();
        while (reader.hasNext()) {
            label = reader.nextName();

            if (label.equals("Date")) {
                String lastUpdate = prefAdapter.getLastUpdate();
                mCurrentPatchDate = reader.nextString();
                if (lastUpdate.equals(mCurrentPatchDate)) {
                    reader.close();
                    return null;
                }
            } else if (label.equals("Patches")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    String[] setData = new String[3];
                    while (reader.hasNext()) {
                        label2 = reader.nextName();
                        switch (label2) {
                            case "Name":
                                setData[SET_NAME] = reader.nextString();
                                break;
                            case "URL":
                                setData[SET_URL] = reader.nextString();
                                break;
                            case "Code":
                                setData[SET_CODE] = reader.nextString();
                                break;
                        }
                    }
                    patchInfo.add(setData);
                    reader.endObject();
                }
                reader.endArray();
            }
        }
        reader.endObject();
        reader.close();

        return patchInfo;
    }

    /**
     * Parses the legality file and populates the database with the different formats, their respective sets, and their
     * banned and restricted lists
     *
     * @param prefAdapter The preference adapter is used to get the last update time
     * @return An object with all of the legal info, to be added to the database in one fell swoop
     * @throws IOException                                                 Thrown if something goes wrong with the InputStream
     * @throws com.gelakinetic.mtgfam.helpers.database.FamiliarDbException Thrown if something goes wrong with database writing
     */
    public LegalInfo readLegalityJsonStream(PreferenceAdapter prefAdapter)
            throws IOException, FamiliarDbException {

        LegalInfo legalInfo = new LegalInfo();

        String legalSet;
        String bannedCard;
        String restrictedCard;
        String formatName;
        String jsonArrayName;
        String jsonTopLevelName;

        URL legal = new URL(LEGALITY_URL);
        InputStream in = new BufferedInputStream(legal.openStream());

        JsonReader reader = new JsonReader(new InputStreamReader(in, "ISO-8859-1"));

        reader.beginObject();
        while (reader.hasNext()) {

            jsonTopLevelName = reader.nextName();
            if (jsonTopLevelName.equalsIgnoreCase("Date")) {
                mCurrentRulesDate = reader.nextString();

				/* compare date, maybe return, update shared prefs */
                String spDate = prefAdapter.getLegalityDate();
                if (spDate != null && spDate.equals(mCurrentRulesDate)) {
                    reader.close();
                    return null; /* dates match, nothing new here. */
                }

            } else if (jsonTopLevelName.equalsIgnoreCase("Formats")) {

                reader.beginObject();
                while (reader.hasNext()) {
                    formatName = reader.nextName();

                    legalInfo.formats.add(formatName);

                    reader.beginObject();
                    while (reader.hasNext()) {
                        jsonArrayName = reader.nextName();

                        if (jsonArrayName.equalsIgnoreCase("Sets")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                legalSet = reader.nextString();
                                legalInfo.legalSets.add(new NameAndMetadata(legalSet, formatName));
                            }
                            reader.endArray();
                        } else if (jsonArrayName.equalsIgnoreCase("Banlist")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                bannedCard = reader.nextString();
                                legalInfo.bannedCards.add(new NameAndMetadata(bannedCard, formatName));
                            }
                            reader.endArray();
                        } else if (jsonArrayName.equalsIgnoreCase("Restrictedlist")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                restrictedCard = reader.nextString();
                                legalInfo.restrictedCards.add(new NameAndMetadata(restrictedCard, formatName));
                            }
                            reader.endArray();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            }
        }
        reader.endObject();

        reader.close();

        return legalInfo;
    }

    class LegalInfo {
        ArrayList<NameAndMetadata> legalSets = new ArrayList<>();
        ArrayList<NameAndMetadata> bannedCards = new ArrayList<>();
        ArrayList<NameAndMetadata> restrictedCards = new ArrayList<>();
        ArrayList<String> formats = new ArrayList<>();
    }

    class NameAndMetadata {
        String name;
        String metadata;

        public NameAndMetadata(String restrictedCard, String formatName) {
            name = restrictedCard;
            metadata = formatName;
        }
    }

    /**
     * This method parses the mapping between set codes and the names TCGPlayer.com uses
     *
     * @param prefAdapter The preference adapter is used to get the last update time
     * @param tcgNames    A place to store tcg names before adding to the database
     * @throws IOException Thrown if something goes wrong with the InputStream
     */
    public void readTCGNameJsonStream(PreferenceAdapter prefAdapter, ArrayList<NameAndMetadata> tcgNames)
            throws IOException {
        URL update;
        String label;
        String label2;
        String name = null, code = null;

        update = new URL(TCG_NAMES_URL);
        InputStreamReader isr = new InputStreamReader(update.openStream(), "ISO-8859-1");
        JsonReader reader = new JsonReader(isr);

        reader.beginObject();
        while (reader.hasNext()) {
            label = reader.nextName();

            if (label.equals("Date")) {
                String lastUpdate = prefAdapter.getLastTCGNameUpdate();
                mCurrentTCGNamePatchDate = reader.nextString();
                if (lastUpdate.equals(mCurrentTCGNamePatchDate)) {
                    reader.close();
                    return;
                }
            } else if (label.equals("Sets")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        label2 = reader.nextName();
                        if (label2.equals("Code")) {
                            code = reader.nextString();
                        } else if (label2.equals("TCGName")) {
                            name = reader.nextString();
                        }
                    }
                    tcgNames.add(new NameAndMetadata(name, code));
                    reader.endObject();
                }
                reader.endArray();
            }
        }
        reader.endObject();
        reader.close();
    }

    /**
     * When the service is done, this method is called to commit the update dates to the shared preferences
     *
     * @param prefAdapter The preferences to write to
     */
    public void commitDates(PreferenceAdapter prefAdapter) {
        prefAdapter.setLastUpdate(mCurrentTCGNamePatchDate);
        prefAdapter.setLastTCGNameUpdate(mCurrentPatchDate);
        prefAdapter.setLegalityDate(mCurrentRulesDate);

        mCurrentTCGNamePatchDate = null;
        mCurrentPatchDate = null;
        mCurrentRulesDate = null;
    }

    /**
     * This interface is implemented by ProgressReporter in DbUpdaterService. It's used to report progress to the
     * notification
     */
    public interface CardProgressReporter {
        void reportJsonCardProgress(int progress);
    }
}
