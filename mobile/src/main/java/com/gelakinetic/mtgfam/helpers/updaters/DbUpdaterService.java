/**
 * Copyright 2012 Michael Shick
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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.NotificationHelper;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * This service takes care of updating the database off of the UI thread
 */
public class DbUpdaterService extends IntentService {

    /* Status Codes */
    private static final int STATUS_NOTIFICATION = 31;
    private static final int UPDATED_NOTIFICATION = 32;

    /* To build and display the notification */
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    /* To keep track of progress percentage when adding cards in a set */
    private Handler mHandler;
    private Runnable mProgressUpdater;
    private int mProgress;

    /**
     * Default constructor with a default name.
     * The string is used to name the worker thread, important only for debugging.
     */
    public DbUpdaterService() {
        super("com.gelakinetic.mtgfam.helpers.updaters.DbUpdaterService");
    }

    /**
     * When the service is created, set up the notification manager and the actual notification
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        mNotificationManager = NotificationManagerCompat.from(this);

        Intent intent = new Intent(this, FamiliarActivity.class);
        PendingIntent mNotificationIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationHelper.createChannels(this);
        mBuilder = new NotificationCompat.Builder(this.getApplicationContext(), NotificationHelper.NOTIFICATION_CHANNEL_UPDATE);
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.update_notification))
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(mNotificationIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setOnlyAlertOnce(true);
    }

    /**
     * This method does the heavy lifting. It opens transactional access to the database, checks the web for new files
     * to patch in, patches them as necessary, and manipulates the notification to inform the user.
     *
     * @param intent The value passed to startService(Intent), it's not used
     */
    @Override
    public void onHandleIntent(Intent intent) {

        showStatusNotification();
        switchToChecking();

        /* Try to open up a log */
        PrintWriter logWriter = null;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                /* Open the log */
                File logfile = new File(this.getApplicationContext().getExternalFilesDir(null), "mtgf_update.txt");
                logWriter = new PrintWriter(new FileWriter(logfile));
                /* Datestamp it */
                logWriter.write((new Date()).toString() + '\n');
            }
        } catch (IOException e) {
            /* Couldn't open log, oh well */
        }

        try {
            ProgressReporter reporter = new ProgressReporter();
            ArrayList<String> updatedStuff = new ArrayList<>();
            CardAndSetParser parser = new CardAndSetParser();
            boolean commitDates = true;
            boolean newRulesParsed = false;

            try {
                /* Look for updates with the banned / restricted lists and formats */
                LegalityData legalityData = parser.readLegalityJsonStream(this, logWriter);

                /* Log the date */
                if (logWriter != null) {
                    logWriter.write("mCurrentRulesDate: " + parser.mCurrentLegalityTimestamp + '\n');
                }

                if (legalityData != null) {
                    if (logWriter != null) {
                        logWriter.write("Adding new legalityData" + '\n');
                    }

                    /* Open a writable database, insert the legality data */
                    SQLiteDatabase database = DatabaseManager.getInstance(getApplicationContext(), true).openDatabase(true);
                    /* Add all the data we've downloaded */
                    CardDbAdapter.dropLegalTables(database);
                    CardDbAdapter.createLegalTables(database);

                    for (LegalityData.Format format : legalityData.mFormats) {
                        CardDbAdapter.createFormat(format.mName, database);

                        for (String legalSet : format.mSets) {
                            CardDbAdapter.addLegalSet(legalSet, format.mName, database);
                        }

                        for (String bannedCard : format.mBanlist) {
                            CardDbAdapter.addLegalCard(bannedCard, format.mName, CardDbAdapter.BANNED, database);
                        }

                        for (String restrictedCard : format.mRestrictedlist) {
                            CardDbAdapter.addLegalCard(restrictedCard, format.mName, CardDbAdapter.RESTRICTED, database);
                        }
                    }

                    /* Close the writable database */
                    DatabaseManager.getInstance(getApplicationContext(), true).closeDatabase(true);
                }

                /* Change the notification to generic "checking for updates" */
                switchToChecking();

                /* Look for new cards */
                Manifest manifest = parser.readUpdateJsonStream(logWriter);

                if (manifest != null) {
                    /* Make an arraylist of all the current set codes */
                    ArrayList<String> currentSetCodes = new ArrayList<>();
                    HashMap<String, String> storedDigests = new HashMap<>();
                    /* Get readable database access */
                    SQLiteDatabase database = DatabaseManager.getInstance(getApplicationContext(), false).openDatabase(false);
                    Cursor setCursor = CardDbAdapter.fetchAllSets(database);
                    if (setCursor != null) {
                        setCursor.moveToFirst();
                        while (!setCursor.isAfterLast()) {
                            String code = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
                            String digest = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_DIGEST));
                            storedDigests.put(code, digest);
                            currentSetCodes.add(code);
                            setCursor.moveToNext();
                        }
                        /* Cleanup */
                        setCursor.close();
                    }
                    DatabaseManager.getInstance(getApplicationContext(), false).closeDatabase(false);

                    /* Look through the manifest and drop all out of date sets */
                    database = DatabaseManager.getInstance(getApplicationContext(), true).openDatabase(true);
                    for (Manifest.ManifestEntry set : manifest.mPatches) {
                        try {
                            /* If the digest doesn't match, mark the set for dropping
                             * and remove it from currentSetCodes so it redownloads
                             */
                            if (set.mDigest != null && !storedDigests.get(set.mCode).equals(set.mDigest)) {
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
                    DatabaseManager.getInstance(getApplicationContext(), true).closeDatabase(true);

                    /* Look through the list of available patches, and if it doesn't exist in the database, add it. */
                    for (Manifest.ManifestEntry set : manifest.mPatches) {
                        if (!set.mCode.equals("DD3") && /* Never download the old Duel Deck Anthologies patch */
                                !currentSetCodes.contains(set.mCode)) { /* check to see if the patch is known already */
                            int retries = 5;
                            while (retries > 0) {
                                try {
                                    /* Change the notification to the specific set */
                                    switchToUpdating(String.format(getString(R.string.update_updating_set), set.mName));
                                    InputStream streamToRead = FamiliarActivity.getHttpInputStream(set.mURL, logWriter);
                                    if (streamToRead != null) {
                                        ArrayList<Card> cardsToAdd = new ArrayList<>();
                                        ArrayList<Expansion> setsToAdd = new ArrayList<>();

                                        GZIPInputStream gis = new GZIPInputStream(streamToRead);
                                        JsonReader reader = new JsonReader(new InputStreamReader(gis, "UTF-8"));
                                        parser.readCardJsonStream(reader, cardsToAdd, setsToAdd);
                                        streamToRead.close();
                                        updatedStuff.add(set.mName);
                                        /* Everything was successful, retries = 0 breaks the while loop */
                                        retries = 0;

                                        /* After the download, open the database */
                                        database = DatabaseManager.getInstance(getApplicationContext(), true).openDatabase(true);
                                        /* Insert the newly downloaded info */
                                        for (Expansion expansion : setsToAdd) {
                                            if (logWriter != null) {
                                                logWriter.write("Adding expansion: " + expansion.mCode_gatherer + '\n');
                                            }

                                            CardDbAdapter.createSet(expansion, database);
                                            CardDbAdapter.addTcgName(expansion.mName_tcgp, expansion.mCode_gatherer, database);
                                            CardDbAdapter.addFoilInfo(expansion.mCanBeFoil, expansion.mCode_gatherer, database);
                                        }
                                        int cardsAdded = 0;
                                        for (Card card : cardsToAdd) {
                                            CardDbAdapter.createCard(card, database);
                                            cardsAdded++;
                                            mProgress = (int) (100 * (cardsAdded / (float)cardsToAdd.size()));
                                        }
                                        /* Close the database */
                                        DatabaseManager.getInstance(getApplicationContext(), true).closeDatabase(true);
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
                switchToChecking();

                /* Parse the rules
                 * Instead of using a hardcoded string, the default lastRulesUpdate is the timestamp of when the APK was
                 * built. This is a safe assumption to make, since any market release will have the latest database baked
                 * in.
                 */

                long lastRulesUpdate = PreferenceAdapter.getLastRulesUpdate(this);

                RulesParser rp = new RulesParser(new Date(lastRulesUpdate), reporter);

                if (rp.needsToUpdate(logWriter)) {
                    switchToUpdating(getString(R.string.update_updating_rules));
                    if (rp.parseRules(logWriter)) {
                        ArrayList<RulesParser.RuleItem> rulesToAdd = new ArrayList<>();
                        ArrayList<RulesParser.GlossaryItem> glossaryItemsToAdd = new ArrayList<>();
                        rp.loadRulesAndGlossary(rulesToAdd, glossaryItemsToAdd);

                        /* Only save the timestamp of this if the update was 100% successful; if something went screwy, we
                         * should let them know and try again next update.
                         */
                        newRulesParsed = true;

                        /* Open the database */
                        SQLiteDatabase database = DatabaseManager.getInstance(getApplicationContext(), true).openDatabase(true);

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

                        /* Close the database */
                        DatabaseManager.getInstance(getApplicationContext(), true).closeDatabase(true);

                        updatedStuff.add(getString(R.string.update_added_rules));
                    }
                }

                /* Change the notification to generic "checking for updates" */
                switchToChecking();

            } catch (FamiliarDbException e1) {
                commitDates = false; /* don't commit the dates */
                if (logWriter != null) {
                    e1.printStackTrace(logWriter);
                }
            }

            /* Parse the MTR and IPG */
            MTRIPGParser mtrIpgParser = new MTRIPGParser(this);
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_MTR, logWriter)) {
                updatedStuff.add(getString(R.string.update_added_mtr));
            }
            if (logWriter != null) {
                logWriter.write("MTR date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_IPG, logWriter)) {
                updatedStuff.add(getString(R.string.update_added_ipg));
            }
            if (logWriter != null) {
                logWriter.write("IPG date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_JAR, logWriter)) {
                updatedStuff.add(getString(R.string.update_added_jar));
            }
            if (logWriter != null) {
                logWriter.write("JAR date: " + mtrIpgParser.mPrettyDate + '\n');
            }

            /* If everything went well so far, commit the date and show the update complete notification */
            if (commitDates) {
                parser.commitDates(this);

                long curTime = new Date().getTime();
                PreferenceAdapter.setLastLegalityUpdate(this, (int) (curTime / 1000));
                if (newRulesParsed) {
                    PreferenceAdapter.setLastRulesUpdate(this, curTime);
                }

                if (updatedStuff.size() > 0) {
                    showUpdatedNotification(updatedStuff);
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
        mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());
    }

    /**
     * Hide the notification in the status bar
     */
    private void cancelStatusNotification() {
        mNotificationManager.cancel(STATUS_NOTIFICATION);
    }

    /**
     * Set the notification to display "Checking for database updates"
     */
    private void switchToChecking() {
        mHandler.removeCallbacks(mProgressUpdater);
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.update_notification))
                .setProgress(0, 0, false);

        mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());
    }

    /**
     * Set the notification to display "Updating %s"
     *
     * @param title The name of the set being updated
     */
    private void switchToUpdating(String title) {

        mBuilder.setContentTitle(title);
        mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());

        /* Periodically update the progress bar */
        mProgressUpdater = new Runnable() {
            public void run() {
                mBuilder.setProgress(100, mProgress, false);
                mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());
                if (mProgress != 100) {
                    mHandler.postDelayed(mProgressUpdater, 200);
                }
            }
        };
        mHandler.postDelayed(mProgressUpdater, 200);
    }

    /**
     * Show a notification which displays what parts of the app were updated
     *
     * @param newStuff A list of strings corresponding to what was updated
     */
    private void showUpdatedNotification(List<String> newStuff) {
        if (newStuff.size() < 1) {
            return;
        }

        String body = getString(R.string.update_added) + " ";
        for (int i = 0; i < newStuff.size(); i++) {
            body += newStuff.get(i);
            if (i < newStuff.size() - 1) {
                body += ", ";
            }
        }

        mBuilder.setContentTitle(getString(R.string.app_name))
                .setContentText(body)
                .setAutoCancel(true)
                .setOngoing(false);

        mNotificationManager.notify(UPDATED_NOTIFICATION, mBuilder.build());
    }

    /**
     * This inner class is used by other parsers to pass progress percentages to the notification
     */
    private class ProgressReporter implements RulesParser.RulesProgressReporter {
        /**
         * This is used by RulesParser to report the progress for adding new rules
         *
         * @param progress An integer between 0 and 100 representing the progress percent
         */
        public void reportRulesProgress(int progress) {
            mProgress = progress;
        }
    }

}
