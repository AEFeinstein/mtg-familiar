package com.gelakinetic.mtgfam.helpers.updaters;

import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class RulesParser {

    /**
     * Returned from fetchAndLoad() if everything works correctly.
     */
    public static final int SUCCESS = 0;
    /* URL and delimiting tokens */
    private static final String SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/MagicCompRules.txt";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String RULES_TOKEN = "RULES_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String GLOSSARY_TOKEN = "GLOSSARY_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String EOF_TOKEN = "EOF_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
    /**
     * Returned from fetchAndLoad() if some of the rules/terms failed, but some succeeded.
     */
    private static final int ERRORS = 1;
    /**
     * Returned from fetchAndLoad() if a catastrophic failure occurs.
     */
    private static final int FAILURE = 2;
    /* Instance variables */
    private final Date mLastUpdated;
    private final RulesProgressReporter mProgressReporter;
    private final ArrayList<RuleItem> mRules;
    private final ArrayList<GlossaryItem> mGlossary;
    private InputStream mInputStream;
    private BufferedReader mBufferedReader;


    /**
     * Default Constructor
     *
     * @param lastUpdated    When the rules were last updated
     * @param progressReport Progress is reported to the notification through this object
     */
    public RulesParser(Date lastUpdated, RulesProgressReporter progressReport) {
        this.mLastUpdated = lastUpdated;
        this.mInputStream = null;
        this.mBufferedReader = null;
        this.mProgressReporter = progressReport;

        this.mRules = new ArrayList<>();
        this.mGlossary = new ArrayList<>();
    }

    /**
     * Attempts to get the URL for the latest version of the rules and determine if an update is necessary. If it finds
     * the file and its date is newer than this.mLastUpdated, true will be returned. Otherwise, it will return false. If
     * true is returned, this.rulesUrl will be populated.
     *
     * @return Whether or this the rules need updating.
     */
    public boolean needsToUpdate() {
        URL url;

        try {
            url = new URL(SOURCE);
            this.mInputStream = url.openStream();
            this.mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));

			/*First line will be the date formatted as YYYY-MM-DD */
            String line = this.mBufferedReader.readLine();
            String[] parts = line.split("-");
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            if (c.getTime().after(this.mLastUpdated)) {
                return true;
            } else {
                closeReader();
                return false;
            }
        } catch (MalformedURLException e) {
            closeReader();
            return false;
        } catch (IOException e) {
            closeReader();
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
    public boolean parseRules() {
        if (this.mBufferedReader == null) {
            /* This should only be the case if we called parseRules() before needsToUpdate()
			 * or if needsToUpdate() returned false */
            return false;
        }

        try {
            RuleItem currentRule = null;
            GlossaryItem currentTerm = null;
            int position = -1;
            this.mRules.clear();
            this.mGlossary.clear();

            String line = this.mBufferedReader.readLine().trim();
            while (!line.equals(RULES_TOKEN)) {
				/* Burn through lines until we hit the rules token */
                line = mBufferedReader.readLine().trim();
            }

            line = mBufferedReader.readLine(); /* Step past the token */

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
                line = mBufferedReader.readLine().trim();
            }

            line = mBufferedReader.readLine().trim(); /* Step past the token */

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
                line = mBufferedReader.readLine().trim();
            }

            return true;
        } catch (IOException e) {
            return false;
        } finally {
            closeReader();
        }
    }

    /**
     * Loads in the rules and glossary to the database, updating the count as it goes. The expected usage is that the
     * main activity will have a progress dialog that gets updated as the count does.
     *
     * @return SUCCESS if nothing goes wrong, ERRORS if some errors occur but some data is loaded, and FAILURE if
     * everything fails and no data is loaded.
     */
    public int loadRulesAndGlossary(SQLiteDatabase database) {
        try {
            CardDbAdapter.dropRulesTables(database);
            CardDbAdapter.createRulesTables(database);

            int statusCode = SUCCESS;
            int numTotalElements = mRules.size() + mGlossary.size();
            int elementsParsed = 0;
            mProgressReporter.reportRulesProgress((int) Math.round(100 * elementsParsed / (double) numTotalElements));

            for (RuleItem rule : mRules) {
                try {
                    CardDbAdapter.insertRule(rule.category, rule.subcategory, rule.entry, rule.text, rule.position, database);
                } catch (FamiliarDbException sqe) {
                    statusCode = ERRORS;
                } finally {
                    elementsParsed++;
                    mProgressReporter.reportRulesProgress(
                            (int) Math.round(100 * elementsParsed / (double) numTotalElements));
                }
            }

            for (GlossaryItem term : mGlossary) {
                try {
                    CardDbAdapter.insertGlossaryTerm(term.term, term.definition, database);

                } catch (FamiliarDbException sqe) {
                    statusCode = ERRORS;
                } finally {
                    elementsParsed++;
                    mProgressReporter.reportRulesProgress(
                            (int) Math.round(100 * elementsParsed / (double) numTotalElements));
                }
            }
            return statusCode;
        } catch (FamiliarDbException sqe) {
            return FAILURE;
        }
    }

    /**
     * Convenience method to close input streams
     */
    private void closeReader() {
        try {
            this.mInputStream.close();
            this.mBufferedReader.close();
        } catch (IOException e) {
			/* eat it */
        }

        this.mInputStream = null;
        this.mBufferedReader = null;
    }

    /**
     * This interface is implemented by ProgressReporter in DbUpdaterService. It's used to report progress to the
     * notification
     */
    public interface RulesProgressReporter {
        void reportRulesProgress(int progress);
    }

    /**
     * Nested class which encapsulates all necessary information about a rule
     */
    private class RuleItem {
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
        public RuleItem(int category, int subcategory, String entry, String text, int position) {
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
        public void addExample(String example) {
            this.text += "<br><br>" + example.trim();
        }
    }

    /**
     * Nested class which encapsulates all necessary information about a glossary entry
     */
    private class GlossaryItem {
        public final String term;
        public String definition;

        /**
         * Default constructor
         *
         * @param term The Glossary term
         */
        public GlossaryItem(String term) {
            this.term = term;
            this.definition = "";
        }

        /**
         * @param line The Glossary Definition
         */
        public void addDefinitionLine(String line) {
            if (this.definition.length() > 0) {
                this.definition += "<br>";
            }
            this.definition += line.trim();
        }
    }
}
