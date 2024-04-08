/*
 * Copyright 2012 Michael Shick
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
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.helpers.updaters;

import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.ContentLoadingProgressBar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarActivityDialogFragment;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * This service takes care of updating the database off of the UI thread
 */
public class DbUpdater {

    /* Create an interface to respond with the result after processing */
    public interface OnProcessedListener {
        void updateTitle(int idx, String title);

        void updateProgress(int idx, int progress);

        void finish(String updatedStuffString);
    }

    /* The activity that is running this update */
    private final FamiliarActivity mContext;

    /* The dialog fragment shown for this update */
    private FamiliarActivityDialogFragment mUpdateDialog;

    /* An executor service to run the update off the main thread */
    private final ExecutorService mExecutor;

    /* A handler to communicate progress between threads */
    private final Handler mHandler;

    /**
     * Constructor
     */
    public DbUpdater(FamiliarActivity ctx) {
        mContext = ctx;
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * This will start the update process on a separate thread and display a progress dialog
     */
    public void checkForUpdates() {

        final OnProcessedListener listener = new OnProcessedListener() {

            @Override
            public void updateTitle(int idx, String title) {
                mHandler.post(() -> {
                    /* Update the UI here */
                    Dialog d = mUpdateDialog.getDialog();
                    if (null != d) {
                        switch (idx) {
                            case 0:
                                ((AppCompatTextView) d.findViewById(R.id.progress_title_1)).setText(title);
                                break;
                            case 1:
                                ((AppCompatTextView) d.findViewById(R.id.progress_title_2)).setText(title);
                                break;
                        }
                    }
                });
            }

            @Override
            public void updateProgress(int idx, int progress) {
                mHandler.post(() -> {
                    /* Update the UI here */
                    Dialog d = mUpdateDialog.getDialog();
                    if (null != d) {
                        switch (idx) {
                            case 0:
                                ((ContentLoadingProgressBar) d.findViewById(R.id.progress_bar_1)).setProgress(progress);
                                break;
                            case 1:
                                ((ContentLoadingProgressBar) d.findViewById(R.id.progress_bar_2)).setProgress(progress);
                                break;
                        }
                    }
                });
            }

            @Override
            public void finish(String updatedStuffString) {
                mHandler.post(() -> {
                    /* Remove the dialog */
                    mContext.removeDialogFragment(mContext.getSupportFragmentManager());

                    if (null != updatedStuffString) {
                        /* Notify the activity of changes */
                        mContext.onReceiveDatabaseUpdate();

                        /* Show new dialog */
                        mContext.showDialogFragment(FamiliarActivityDialogFragment.DIALOG_UPDATE_RESULT, updatedStuffString);
                    }
                });
            }
        };

        /* Show the dialog for updates */
        mUpdateDialog = mContext.showDialogFragment(FamiliarActivityDialogFragment.DIALOG_UPDATE);

        /* Start the update in another thread */
        mExecutor.execute(() -> checkForUpdatesBackground(listener));
    }

    /**
     * This method does the heavy lifting. It opens transactional access to the database, checks the web for new files
     * to patch in, patches them as necessary, and updates the progress dialog to inform the user.
     */
    public void checkForUpdatesBackground(OnProcessedListener listener) {

        /* Set up progress for the entire update */
        int numItemsChecked = 0;
        int numItemsToCheck = 6; /* 1 for comprehensive rules, 5 judge docs, sets added later */
        listener.updateProgress(0, 0);

        /* Change the dialog to "updating cards" */
        listener.updateTitle(0, mContext.getString(R.string.update_updating_cards));
        listener.updateTitle(1, "");
        listener.updateProgress(1, 0);

        /* Try to open up a log */
        PrintWriter logWriter = null;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                /* Open the log */
                File logfile = new File(mContext.getExternalFilesDir(null), "mtgf_update.txt");
                logWriter = new PrintWriter(new FileWriter(logfile));
                /* Datestamp it */
                logWriter.write((new Date()).toString() + '\n');
            }
        } catch (IOException e) {
            /* Couldn't open log, oh well */
        }

        try {
            ArrayList<String> updatedStuff = new ArrayList<>();
            CardAndSetParser parser = new CardAndSetParser();
            boolean commitDates = true;
            boolean newRulesParsed = false;

            /* Look for new cards */
            Manifest manifest = parser.readUpdateJsonStream(mContext, logWriter);

            if (manifest != null) {
                /* Make an arraylist of all the current set codes */
                ArrayList<String> currentSetCodes = new ArrayList<>();
                HashMap<String, String> storedDigests = new HashMap<>();
                Cursor setCursor = null;
                FamiliarDbHandle setsHandle = new FamiliarDbHandle();
                try {
                    /* Get readable database access */
                    SQLiteDatabase database = DatabaseManager.openDatabase(mContext, false, setsHandle);
                    setCursor = CardDbAdapter.fetchAllSets(database, false, false);
                    if (setCursor != null) {
                        setCursor.moveToFirst();
                        while (!setCursor.isAfterLast()) {
                            String code = CardDbAdapter.getStringFromCursor(setCursor, CardDbAdapter.KEY_CODE);
                            String digest = CardDbAdapter.getStringFromCursor(setCursor, CardDbAdapter.KEY_DIGEST);
                            storedDigests.put(code, digest);
                            currentSetCodes.add(code);
                            setCursor.moveToNext();
                        }
                    }
                } catch (SQLiteException | FamiliarDbException e) {
                    commitDates = false; /* don't commit the dates */
                    if (logWriter != null) {
                        e.printStackTrace(logWriter);
                    }
                } finally {
                    if (null != setCursor) {
                        setCursor.close();
                    }
                    DatabaseManager.closeDatabase(mContext, setsHandle);
                }

                /* Look through the manifest and drop all out of date sets */
                FamiliarDbHandle manifestHandle = new FamiliarDbHandle();
                try {
                    SQLiteDatabase database = DatabaseManager.openDatabase(mContext, true, manifestHandle);
                    for (Manifest.ManifestEntry set : manifest.mPatches) {
                        try {
                            /* If the digest doesn't match, mark the set for dropping
                             * and remove it from currentSetCodes so it re-downloads
                             */
                            if (set.mDigest != null && !Objects.requireNonNull(storedDigests.get(set.mCode)).equals(set.mDigest)) {
                                if (logWriter != null) {
                                    logWriter.write("Dropping expansion: " + set.mCode + '\n');
                                }
                                currentSetCodes.remove(set.mCode);
                                CardDbAdapter.dropSetAndCards(set.mCode, database);
                            }
                        } catch (NullPointerException e) {
                            /* eat it */
                        }
                    }
                } catch (SQLiteException | FamiliarDbException e) {
                    commitDates = false; /* don't commit the dates */
                    if (logWriter != null) {
                        e.printStackTrace(logWriter);
                    }
                } finally {
                    DatabaseManager.closeDatabase(mContext, manifestHandle);
                }

                /* Count the number of sets to add */
                for (Manifest.ManifestEntry set : manifest.mPatches) {
                    if (!set.mCode.equals("DD3") && !currentSetCodes.contains(set.mCode)) {
                        numItemsToCheck++;
                    }
                }

                /* Look through the list of available patches, and if it doesn't exist in the database, add it. */
                for (Manifest.ManifestEntry set : manifest.mPatches) {
                    if (!set.mCode.equals("DD3") && /* Never download the old Duel Deck Anthologies patch */
                            !currentSetCodes.contains(set.mCode)) { /* check to see if the patch is known already */
                        int retries = 5;
                        while (retries > 0) {
                            try (InputStream streamToRead = FamiliarActivity.getHttpInputStream(set.mURL, logWriter, mContext)) {
                                /* Update the dialog to the set being added */
                                listener.updateTitle(1, String.format(mContext.getString(R.string.update_updating_set), set.mName));
                                listener.updateProgress(1, 0);
                                if (streamToRead != null) {
                                    ArrayList<Card> cardsToAdd = new ArrayList<>();
                                    ArrayList<Expansion> setsToAdd = new ArrayList<>();

                                    GZIPInputStream gis = new GZIPInputStream(streamToRead);
                                    JsonReader reader = new JsonReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
                                    parser.readCardJsonStream(reader, cardsToAdd, setsToAdd);
                                    updatedStuff.add(set.mName);
                                    /* Everything was successful, retries = 0 breaks the while loop */
                                    retries = 0;

                                    /* After the download, open the database */
                                    FamiliarDbHandle expansionHandle = new FamiliarDbHandle();
                                    try {
                                        SQLiteDatabase database = DatabaseManager.openDatabase(mContext, true, expansionHandle);
                                        /* Insert the newly downloaded info */
                                        for (Expansion expansion : setsToAdd) {
                                            if (logWriter != null) {
                                                logWriter.write("Adding expansion: " + expansion.mCode_gatherer + '\n');
                                            }

                                            CardDbAdapter.createSet(expansion, database);
                                        }
                                        int cardsAdded = 0;
                                        for (Card card : cardsToAdd) {
                                            CardDbAdapter.createCard(card, database);
                                            cardsAdded++;
                                            listener.updateProgress(1, (int) (100 * (cardsAdded / (float) cardsToAdd.size())));
                                        }

                                    } catch (SQLiteException | FamiliarDbException e) {
                                        commitDates = false; /* don't commit the dates */
                                        if (logWriter != null) {
                                            e.printStackTrace(logWriter);
                                        }
                                    } finally {
                                        /* Close the database */
                                        DatabaseManager.closeDatabase(mContext, expansionHandle);
                                    }
                                }
                            } catch (IOException e) {
                                if (logWriter != null) {
                                    logWriter.print("Retry " + retries + '\n');
                                    e.printStackTrace(logWriter);
                                }
                            }
                            retries--;
                        }

                        /* Update total progress after this set is updated */
                        numItemsChecked++;
                        listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));
                    }
                }
            }

            /* Look for updates with the banned / restricted lists and formats */
            LegalityData legalityData = parser.readLegalityJsonStream(mContext, logWriter);

            /* Log the date */
            if (logWriter != null) {
                logWriter.write("mCurrentRulesDate: " + parser.mCurrentLegalityTimestamp + '\n');
            }

            if (legalityData != null) {
                if (logWriter != null) {
                    logWriter.write("Adding new legalityData" + '\n');
                }

                /* Open a writable database, insert the legality data */
                FamiliarDbHandle legalHandle = new FamiliarDbHandle();
                try {
                    SQLiteDatabase database = DatabaseManager.openDatabase(mContext, true, legalHandle);
                    /* Add all the data we've downloaded */
                    CardDbAdapter.dropLegalTables(database);
                    CardDbAdapter.createLegalTables(database);

                    for (LegalityData.Format format : legalityData.mFormats) {
                        for (String legalSet : format.mSets) {
                            CardDbAdapter.addLegalSet(legalSet, format.mName, database);
                        }
                    }
                } catch (SQLiteException | FamiliarDbException e) {
                    commitDates = false; /* don't commit the dates */
                    if (logWriter != null) {
                        e.printStackTrace(logWriter);
                    }
                } finally {
                    /* Close the writable database */
                    DatabaseManager.closeDatabase(mContext, legalHandle);
                }
            }

            /* Change the dialog to "updating comprehensive rules" */
            listener.updateTitle(0, mContext.getString(R.string.update_updating_rules));
            listener.updateProgress(1, 0);

            /* Parse the rules
             * Instead of using a hardcoded string, the default lastRulesUpdate is the timestamp of when the APK was
             * built. This is a safe assumption to make, since any market release will have the latest database baked
             * in.
             */

            long lastRulesUpdate = PreferenceAdapter.getLastRulesUpdate(mContext);

            RulesParser rp = new RulesParser(new Date(lastRulesUpdate), listener);

            if (rp.needsToUpdate(mContext, logWriter)) {
                if (rp.parseRules(mContext, logWriter)) {
                    ArrayList<RulesParser.RuleItem> rulesToAdd = new ArrayList<>();
                    ArrayList<RulesParser.GlossaryItem> glossaryItemsToAdd = new ArrayList<>();
                    rp.loadRulesAndGlossary(rulesToAdd, glossaryItemsToAdd);

                    /* Only save the timestamp of this if the update was 100% successful; if something went screwy, we
                     * should let them know and try again next update.
                     */
                    newRulesParsed = true;

                    /* Open the database */
                    FamiliarDbHandle rulesHandle = new FamiliarDbHandle();
                    try {

                        SQLiteDatabase database = DatabaseManager.openDatabase(mContext, true, rulesHandle);

                        /* Add stored rules */
                        if (rulesToAdd.size() > 0 || glossaryItemsToAdd.size() > 0) {
                            CardDbAdapter.dropRulesTables(database);
                            CardDbAdapter.createRulesTables(database);
                        }

                        for (RulesParser.RuleItem rule : rulesToAdd) {
                            CardDbAdapter.insertRule(rule.category, rule.subcategory, rule.entry, rule.text, rule.position, database);
                        }

                        for (RulesParser.GlossaryItem term : glossaryItemsToAdd) {
                            CardDbAdapter.insertGlossaryTerm(term.term, term.definition, database);
                        }
                        updatedStuff.add(mContext.getString(R.string.update_added_rules));
                    } catch (SQLiteException | FamiliarDbException e) {
                        commitDates = false; /* don't commit the dates */
                        if (logWriter != null) {
                            e.printStackTrace(logWriter);
                        }
                    } finally {
                        DatabaseManager.closeDatabase(mContext, rulesHandle);
                    }
                }
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            /* Change the dialog to  "Updating Judge Docs" */
            listener.updateTitle(0, mContext.getString(R.string.update_updating_docs));
            listener.updateProgress(1, 0);

            int numDocsToProcess = 5;
            int numProcessedDocs = 0;

            /* Parse the MTR and IPG */
            MTRIPGParser mtrIpgParser = new MTRIPGParser(mContext);

            numProcessedDocs++;
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_MTR, listener, logWriter, numProcessedDocs, numDocsToProcess)) {
                updatedStuff.add(mContext.getString(R.string.update_added_mtr));
            }
            if (logWriter != null) {
                logWriter.write("MTR date: " + mtrIpgParser.mPrettyDate + '\n');
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            numProcessedDocs++;
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_IPG, listener, logWriter, numProcessedDocs, numDocsToProcess)) {
                updatedStuff.add(mContext.getString(R.string.update_added_ipg));
            }
            if (logWriter != null) {
                logWriter.write("IPG date: " + mtrIpgParser.mPrettyDate + '\n');
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            numProcessedDocs++;
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_JAR, listener, logWriter, numProcessedDocs, numDocsToProcess)) {
                updatedStuff.add(mContext.getString(R.string.update_added_jar));
            }
            if (logWriter != null) {
                logWriter.write("JAR date: " + mtrIpgParser.mPrettyDate + '\n');
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            numProcessedDocs++;
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_DMTR, listener, logWriter, numProcessedDocs, numDocsToProcess)) {
                updatedStuff.add(mContext.getString(R.string.update_added_dmtr));
            }
            if (logWriter != null) {
                logWriter.write("DMTR date: " + mtrIpgParser.mPrettyDate + '\n');
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            numProcessedDocs++;
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_DIPG, listener, logWriter, numProcessedDocs, numDocsToProcess)) {
                updatedStuff.add(mContext.getString(R.string.update_added_dipg));
            }
            if (logWriter != null) {
                logWriter.write("DIPG date: " + mtrIpgParser.mPrettyDate + '\n');
            }
            numItemsChecked++;
            listener.updateProgress(0, (int) (100 * numItemsChecked / (float) numItemsToCheck));

            /* If everything went well so far, commit the date and show the update complete dialog */
            if (commitDates) {
                parser.commitDates(mContext);

                long curTime = new Date().getTime();
                PreferenceAdapter.setLastLegalityUpdate(mContext, (int) (curTime / 1000));
                if (newRulesParsed) {
                    PreferenceAdapter.setLastRulesUpdate(mContext, curTime);
                }

                if (updatedStuff.size() > 0) {
                    listener.finish(getUpdatedStuffString(updatedStuff));
                } else {
                    listener.finish(null);
                }
            }
        } catch (Exception e) {
            /* Generally pokemon handling is bad, but I don't want miss anything */
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
        }

        /* close the log */
        if (logWriter != null) {
            logWriter.close();
        }
    }

    /**
     * Generate a string with a list of all items which were updated
     *
     * @param newStuff A strings to show the user in a dialog
     */
    private String getUpdatedStuffString(List<String> newStuff) {
        if (newStuff.size() < 1) {
            return null;
        }

        StringBuilder body = new StringBuilder(mContext.getString(R.string.update_added)).append(" ");
        boolean first = true;
        for (String stuff : newStuff) {
            if (first) {
                first = false;
            } else {
                body.append(", ");
            }
            body.append(stuff);
        }

        return body.toString();
    }
}
