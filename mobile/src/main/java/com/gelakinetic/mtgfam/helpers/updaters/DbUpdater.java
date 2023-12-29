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
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.helpers.updaters;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
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

    // Create an interface to respond with the result after processing
    public interface OnProcessedListener {
        void onChecking();

        void onUpdateTitle(String title);

        void onUpdateProgress(int progress);

        void onFinish(String updatedStuffString);
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
            public void onChecking() {
                mHandler.post(() -> {
                    MaterialDialog d = (MaterialDialog) mUpdateDialog.getDialog();
                    if (null != d) {
                        d.setTitle(mContext.getString(R.string.update_notification));
                        d.setProgress(0);
                    }
                });
            }

            @Override
            public void onUpdateTitle(String title) {
                mHandler.post(() -> {
                    // Update the UI here
                    MaterialDialog d = (MaterialDialog) mUpdateDialog.getDialog();
                    if (null != d) {
                        d.setTitle(title);
                    }
                });
            }

            @Override
            public void onUpdateProgress(int progress) {
                mHandler.post(() -> {
                    // Update the UI here
                    MaterialDialog d = (MaterialDialog) mUpdateDialog.getDialog();
                    if (null != d) {
                        d.setProgress(progress);
                    }
                });
            }

            @Override
            public void onFinish(String updatedStuffString) {
                mHandler.post(() -> {
                    if (null != updatedStuffString) {
                        /* Notify the activity of changes */
                        mContext.onReceiveDatabaseUpdate();
                        onUpdateTitle(updatedStuffString);
                        MaterialDialog d = (MaterialDialog) mUpdateDialog.getDialog();
                        if (null != d) {
                            d.setProgress(100);
                            d.setCancelable(true);
                            d.setActionButton(DialogAction.POSITIVE, R.string.dialog_ok);
                            d.setOnKeyListener((dialog, keyCode, event) -> {
                                mContext.removeDialogFragment(mContext.getSupportFragmentManager());
                                return true;
                            });
                        }
                    } else {
                        // Remove the dialog
                        mContext.removeDialogFragment(mContext.getSupportFragmentManager());
                    }
                });
            }
        };

        showStatusNotification();
        mExecutor.execute(() -> checkForUpdatesBackground(listener));
    }

    /**
     * This method does the heavy lifting. It opens transactional access to the database, checks the web for new files
     * to patch in, patches them as necessary, and manipulates the notification to inform the user.
     */
    public void checkForUpdatesBackground(OnProcessedListener listener) {
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

            /* Change the notification to generic "checking for updates" */
            listener.onChecking();

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

                /* Look through the list of available patches, and if it doesn't exist in the database, add it. */
                for (Manifest.ManifestEntry set : manifest.mPatches) {
                    if (!set.mCode.equals("DD3") && /* Never download the old Duel Deck Anthologies patch */
                            !currentSetCodes.contains(set.mCode)) { /* check to see if the patch is known already */
                        int retries = 5;
                        while (retries > 0) {
                            try (InputStream streamToRead = FamiliarActivity.getHttpInputStream(set.mURL, logWriter, mContext)) {
                                /* Change the notification to the specific set */
                                listener.onUpdateTitle(String.format(mContext.getString(R.string.update_updating_set), set.mName));
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
                                            listener.onUpdateProgress((int) (100 * (cardsAdded / (float) cardsToAdd.size())));
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
                    }
                }
            }

            /* Change the notification to generic "checking for updates" */
            listener.onChecking();

            /* Parse the rules
             * Instead of using a hardcoded string, the default lastRulesUpdate is the timestamp of when the APK was
             * built. This is a safe assumption to make, since any market release will have the latest database baked
             * in.
             */

            long lastRulesUpdate = PreferenceAdapter.getLastRulesUpdate(mContext);

            RulesParser rp = new RulesParser(new Date(lastRulesUpdate), listener);

            if (rp.needsToUpdate(mContext, logWriter)) {
                listener.onUpdateTitle(mContext.getString(R.string.update_updating_rules));
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

            /* Change the notification to generic "checking for updates" */
            listener.onChecking();

            /* Parse the MTR and IPG */
            MTRIPGParser mtrIpgParser = new MTRIPGParser(mContext);
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_MTR, logWriter)) {
                updatedStuff.add(mContext.getString(R.string.update_added_mtr));
            }
            if (logWriter != null) {
                logWriter.write("MTR date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_IPG, logWriter)) {
                updatedStuff.add(mContext.getString(R.string.update_added_ipg));
            }
            if (logWriter != null) {
                logWriter.write("IPG date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_JAR, logWriter)) {
                updatedStuff.add(mContext.getString(R.string.update_added_jar));
            }
            if (logWriter != null) {
                logWriter.write("JAR date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_DMTR, logWriter)) {
                updatedStuff.add(mContext.getString(R.string.update_added_dmtr));
            }
            if (logWriter != null) {
                logWriter.write("DMTR date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_DIPG, logWriter)) {
                updatedStuff.add(mContext.getString(R.string.update_added_dipg));
            }
            if (logWriter != null) {
                logWriter.write("DIPG date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            /* If everything went well so far, commit the date and show the update complete notification */
            if (commitDates) {
                parser.commitDates(mContext);

                long curTime = new Date().getTime();
                PreferenceAdapter.setLastLegalityUpdate(mContext, (int) (curTime / 1000));
                if (newRulesParsed) {
                    PreferenceAdapter.setLastRulesUpdate(mContext, curTime);
                }

                if (updatedStuff.size() > 0) {
                    listener.onFinish(getUpdatedStuffString(updatedStuff));
                } else {
                    listener.onFinish(null);
                }
            }
        } catch (Exception e) {
            /* Generally pokemon handling is bad, but I don't want miss anything */
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
        }

        /* Always cancel the status notification */
        cancelStatusNotification();

        /* close the log */
        if (logWriter != null) {
            logWriter.close();
        }
    }

    /**
     * Show the notification in the status bar
     */
    private void showStatusNotification() {
        mUpdateDialog = mContext.showDialogFragment(FamiliarActivityDialogFragment.DIALOG_UPDATE);
    }

    /**
     * Hide the notification in the status bar
     */
    private void cancelStatusNotification() {
        mContext.removeDialogFragment(mContext.getSupportFragmentManager());
    }

    /**
     * Show a notification which displays what parts of the app were updated
     * <p>
     * Note, MissingPermission is suppressed here because requestNotificationPermission() is called
     * before creating the service
     *
     * @param newStuff A list of strings corresponding to what was updated
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
