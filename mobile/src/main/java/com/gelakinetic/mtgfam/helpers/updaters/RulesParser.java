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

import com.gelakinetic.mtgfam.FamiliarActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class RulesParser {

    /* URL and delimiting tokens */
    private static final String SOURCE = "https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/rules/MagicCompRules.txt";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String RULES_TOKEN = "RULES_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String GLOSSARY_TOKEN = "GLOSSARY_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String EOF_TOKEN = "EOF_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";

    /* Instance variables */
    private final Date mLastUpdated;
    private final DbUpdater.OnProcessedListener mProgressReporter;
    private final ArrayList<RuleItem> mRules;
    private final ArrayList<GlossaryItem> mGlossary;

    /**
     * Default Constructor
     *
     * @param lastUpdated When the rules were last updated
     * @param listener    Progress is reported to the notification through this object
     */
    public RulesParser(Date lastUpdated, DbUpdater.OnProcessedListener listener) {
        this.mLastUpdated = lastUpdated;
        this.mProgressReporter = listener;

        this.mRules = new ArrayList<>();
        this.mGlossary = new ArrayList<>();
    }

    /**
     * Attempts to get the URL for the latest version of the rules and determine if an update is necessary. If it finds
     * the file and its date is newer than this.mLastUpdated, true will be returned. Otherwise, it will return false. If
     * true is returned, this.rulesUrl will be populated.
     *
     * @param context a context to open the HttpInputStream with
     * @return Whether or this the rules need updating.
     */
    public boolean needsToUpdate(Context context, PrintWriter logWriter) {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(FamiliarActivity.getHttpInputStream(SOURCE, logWriter, context), StandardCharsets.UTF_8))) {

            /* First line will be the date formatted as YYYY-MM-DD */
            String line = bufferedReader.readLine();
            String[] parts = line.split("-");
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            /* Log the date */
            if (logWriter != null) {
                String patchDate = DateFormat.getDateInstance().format(c.getTime());
                logWriter.write("mCurrentRulesPatchDate: " + patchDate + '\n');
            }

            return c.getTime().after(this.mLastUpdated);
        } catch (IOException | NullPointerException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            return false;
        }
    }

    /**
     * Attempts to fetch the latest version of the rules and load it into the database. If the process is successful,
     * true will be returned. Otherwise, false will be returned. This method should only be called if needsToUpdate()
     * returns true.
     *
     * @return Whether or not the parsing is successful
     */
    public boolean parseRules(Context context, PrintWriter logWriter) {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(FamiliarActivity.getHttpInputStream(SOURCE, logWriter, context), StandardCharsets.UTF_8))) {
            RuleItem currentRule = null;
            GlossaryItem currentTerm = null;
            int position = -1;
            this.mRules.clear();
            this.mGlossary.clear();

            String line = bufferedReader.readLine().trim();
            while (!line.equals(RULES_TOKEN)) {
                /* Burn through lines until we hit the rules token */
                line = bufferedReader.readLine().trim();
            }

            line = bufferedReader.readLine(); /* Step past the token */

            while (!line.equals(GLOSSARY_TOKEN)) {
                /* Parse the line */
                if (line.length() == 0) {
                    if (currentRule != null) {
                        /* Rule is over and we have one; add it to the list and null it */
                        this.mRules.add(currentRule);
                        currentRule = null;
                    }
                } else {
                    if (Character.isDigit(line.charAt(0))) {
                        /* If the line starts with a number, it's the start of a rule */
                        int category, subcategory;
                        String entry, text;

                        String numberToken = line.split(" ")[0];
                        String[] subTokens = numberToken.split("\\.");

                        int rawCategory = Integer.parseInt(subTokens[0]);
                        if (rawCategory >= 100) {
                            category = rawCategory / 100;
                            subcategory = rawCategory % 100;
                        } else {
                            category = rawCategory;
                            subcategory = -1;
                        }

                        if (subTokens.length > 1) {
                            entry = subTokens[1];
                        } else {
                            entry = null;
                        }

                        text = line.substring(numberToken.length()).trim();
                        text = text.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}").replace("{W/P}", "{WP}");

                        if (entry == null) {
                            position = -1;
                        } else {
                            position++;
                        }

                        currentRule = new RuleItem(category, subcategory, entry, text, position);
                    } else {
                        if (currentRule != null) {
                            currentRule.addExample(line.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}")
                                    .replace("{W/P}", "{WP}"));
                        }
                    }
                }

                /* Then move to the next line */
                line = bufferedReader.readLine().trim();
            }

            line = bufferedReader.readLine().trim(); /* Step past the token */

            while (!line.equals(EOF_TOKEN)) {
                /* Parse the line */
                if (line.length() == 0) {
                    if (currentTerm != null) {
                        /* Term is over and we have one; add it to the list and null it */
                        mGlossary.add(currentTerm);
                        currentTerm = null;
                    }
                } else {
                    if (currentTerm == null) {
                        currentTerm = new GlossaryItem(line);
                    } else {
                        currentTerm.addDefinitionLine(line.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}")
                                .replace("{W/P}", "{WP}"));
                    }
                }

                /* Then move to the next line */
                line = bufferedReader.readLine().trim();
            }
            if (currentTerm != null) {
                /* Document is over but we still have a term; add it to the list */
                mGlossary.add(currentTerm);
            }

            return true;
        } catch (IOException | NullPointerException e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            return false;
        }
    }

    /**
     * Loads in the rules and glossary to the database, updating the count as it goes. The expected usage is that the
     * main activity will have a progress dialog that gets updated as the count does.
     *
     * @param rulesToAdd         A place to store rules before inserting into the database
     * @param glossaryItemsToAdd A place to store glossary items before inserting into the database
     */
    public void loadRulesAndGlossary(ArrayList<RuleItem> rulesToAdd, ArrayList<GlossaryItem> glossaryItemsToAdd) {
        int numTotalElements = mRules.size() + mGlossary.size();
        int elementsParsed = 0;
        mProgressReporter.onUpdateProgress(0);

        for (RuleItem rule : mRules) {
            rulesToAdd.add(rule);
            elementsParsed++;
            mProgressReporter.onUpdateProgress((int) Math.round(100 * elementsParsed / (double) numTotalElements));
        }

        for (GlossaryItem term : mGlossary) {
            glossaryItemsToAdd.add(term);
            elementsParsed++;
            mProgressReporter.onUpdateProgress((int) Math.round(100 * elementsParsed / (double) numTotalElements));
        }
    }

    /**
     * Nested class which encapsulates all necessary information about a rule
     */
    static class RuleItem {
        public final int category;
        public final int subcategory;
        public final String entry;
        public final int position;
        public String text;

        /**
         * Constructor which populates the rule
         *
         * @param category    self-explanatory
         * @param subcategory self-explanatory
         * @param entry       self-explanatory
         * @param text        self-explanatory
         * @param position    self-explanatory
         */
        RuleItem(int category, int subcategory, String entry, String text, int position) {
            this.category = category;
            this.subcategory = subcategory;
            this.entry = entry;
            this.text = text;
            this.position = position;
        }

        /**
         * Adds line breaks and an example to the text
         *
         * @param example self-explanatory
         */
        void addExample(String example) {
            this.text += "<br><br>" + example.trim();
        }
    }

    /**
     * Nested class which encapsulates all necessary information about a glossary entry
     */
    static class GlossaryItem {
        public final String term;
        public String definition;

        /**
         * Default constructor
         *
         * @param term The Glossary term
         */
        GlossaryItem(String term) {
            this.term = term;
            this.definition = "";
        }

        /**
         * @param line The Glossary Definition
         */
        void addDefinitionLine(String line) {
            if (this.definition.length() > 0) {
                this.definition += "<br>";
            }
            this.definition += line.trim();
        }
    }
}
