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

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.MtgSet;
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
import java.net.HttpURLConnection;
import java.net.URL;
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

        mBuilder = new NotificationCompat.Builder(this.getApplicationContext());
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
            PreferenceAdapter mPrefAdapter = new PreferenceAdapter(this);

            ProgressReporter reporter = new ProgressReporter();
            ArrayList<String> updatedStuff = new ArrayList<>();
            CardAndSetParser parser = new CardAndSetParser();
            boolean commitDates = true;
            boolean newRulesParsed = false;

            try {
                ArrayList<MtgCard> cardsToAdd = new ArrayList<>();
                ArrayList<MtgSet> setsToAdd = new ArrayList<>();
                ArrayList<String> setsToDrop = new ArrayList<>();
                ArrayList<CardAndSetParser.NameAndMetadata> tcgNames = new ArrayList<>();

                /* Look for updates with the banned / restricted lists and formats */
                CardAndSetParser.LegalInfo legalInfo = parser.readLegalityJsonStream(mPrefAdapter, logWriter);

                /* Log the date */
                if (logWriter != null) {
                    logWriter.write("mCurrentRulesDate: " + parser.mCurrentRulesDate + '\n');
                }

                /* Get the MD5 digests for the patches */
                HashMap<String, String> patchDigests = parser.readDigestStream(logWriter);
                if (logWriter != null && patchDigests != null) {
                    logWriter.write("patchDigests: " + patchDigests.size() + '\n');
                }

                /* Look for new cards */
                ArrayList<String[]> patchInfo = parser.readUpdateJsonStream(logWriter);

                /* Log the date */
                if (logWriter != null) {
                    logWriter.write("mCurrentPatchDate: " + parser.mCurrentPatchDate + '\n');
                }

                if (patchInfo != null) {
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

                    /* Look through the list of available patches, and if it doesn't exist in the database, add it. */
                    for (String[] set : patchInfo) {
                        if (!set[CardAndSetParser.SET_CODE].equals("DD3")) { /* Never download the old Duel Deck Anthologies patch */
                            try {
                                /* If the digest doesn't match, mark the set for dropping
                                 * and remove it from currentSetCodes so it redownloads
                                 */
                                if (patchDigests != null &&
                                        !storedDigests.get(set[CardAndSetParser.SET_CODE])
                                                .equals(patchDigests.get(set[CardAndSetParser.SET_CODE]))) {
                                    currentSetCodes.remove(set[CardAndSetParser.SET_CODE]);
                                    setsToDrop.add(set[CardAndSetParser.SET_CODE]);
                                }
                            } catch (NullPointerException e) {
                                /* eat it */
                            }
                            if (!currentSetCodes.contains(set[CardAndSetParser.SET_CODE])) { /* check to see if the patch is known already */
                                int retries = 5;
                                while (retries > 0) {
                                    HttpURLConnection connection = null;
                                    try {
                                        /* Change the notification to the specific set */
                                        switchToUpdating(String.format(getString(R.string.update_updating_set),
                                                set[CardAndSetParser.SET_NAME]));
                                        URL urlToRead = new URL(set[CardAndSetParser.SET_URL]);
                                        connection = (HttpURLConnection) urlToRead.openConnection();
                                        connection.setInstanceFollowRedirects(true);
                                        InputStream streamToRead = connection.getInputStream();
                                        GZIPInputStream gis = new GZIPInputStream(streamToRead);
                                        JsonReader reader = new JsonReader(new InputStreamReader(gis, "ISO-8859-1"));
                                        parser.readCardJsonStream(reader, reporter, cardsToAdd, setsToAdd, patchDigests);
                                        streamToRead.close();
                                        updatedStuff.add(set[CardAndSetParser.SET_NAME]);
                                         /* Everything was successful, retries = 0 breaks the while loop */
                                        retries = 0;
                                    } catch (IOException e) {
                                        if (logWriter != null) {
                                            logWriter.print("Retry " + retries + '\n');
                                            e.printStackTrace(logWriter);
                                        }
                                        if (connection != null) {
                                            connection.disconnect();
                                        }
                                    }
                                    if (connection != null) {
                                        connection.disconnect();
                                    }
                                    retries--;
                                }
                            }
                        }
                    }

                    /* Look for new TCGPlayer.com versions of set names, only if patchInfo exists
                     * and there is a new set to add
                     */
                    if (setsToAdd.size() > 0) {
                        parser.readTCGNameJsonStream(mPrefAdapter, tcgNames, logWriter);

                        /* Log the date */
                        if (logWriter != null) {
                            logWriter.write("mCurrentTCGNamePatchDate: " + parser.mCurrentTCGNamePatchDate + '\n');
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

                long lastRulesUpdate = mPrefAdapter.getLastRulesUpdate();

                RulesParser rp = new RulesParser(new Date(lastRulesUpdate), reporter);
                ArrayList<RulesParser.RuleItem> rulesToAdd = new ArrayList<>();
                ArrayList<RulesParser.GlossaryItem> glossaryItemsToAdd = new ArrayList<>();

                if (rp.needsToUpdate(logWriter)) {
                    switchToUpdating(getString(R.string.update_updating_rules));
                    if (rp.parseRules(logWriter)) {
                        rp.loadRulesAndGlossary(rulesToAdd, glossaryItemsToAdd);

                        /* Only save the timestamp of this if the update was 100% successful; if something went screwy, we
                         * should let them know and try again next update.
                         */
                        newRulesParsed = true;
                        updatedStuff.add(getString(R.string.update_added_rules));
                    }
                    switchToChecking();
                }

                if (logWriter != null) {
                    logWriter.write("legalInfo: " + ((legalInfo == null) ? "null" : "not null") + '\n');
                    logWriter.write("setsToAdd: " + setsToAdd.size() + '\n');
                    logWriter.write("cardsToAdd: " + cardsToAdd.size() + '\n');
                    logWriter.write("tcgNames: " + tcgNames.size() + '\n');
                    logWriter.write("rulesToAdd: " + rulesToAdd.size() + '\n');
                    logWriter.write("glossaryItemsToAdd: " + glossaryItemsToAdd.size() + '\n');
                }

                /* Open a writable database, as briefly as possible */
                if (legalInfo != null ||
                        setsToDrop.size() > 0 ||
                        setsToAdd.size() > 0 ||
                        cardsToAdd.size() > 0 ||
                        rulesToAdd.size() > 0 ||
                        glossaryItemsToAdd.size() > 0) {
                    SQLiteDatabase database = DatabaseManager.getInstance(getApplicationContext(), true).openDatabase(true);
                    /* Add all the data we've downloaded */
                    if (legalInfo != null) {
                        CardDbAdapter.dropLegalTables(database);
                        CardDbAdapter.createLegalTables(database);
                        for (String format : legalInfo.formats) {
                            CardDbAdapter.createFormat(format, database);
                        }
                        for (CardAndSetParser.NameAndMetadata legalSet : legalInfo.legalSets) {
                            CardDbAdapter.addLegalSet(legalSet.name, legalSet.metadata, database);
                        }
                        for (CardAndSetParser.NameAndMetadata bannedCard : legalInfo.bannedCards) {
                            CardDbAdapter.addLegalCard(bannedCard.name, bannedCard.metadata, CardDbAdapter.BANNED, database);
                        }
                        for (CardAndSetParser.NameAndMetadata restrictedCard : legalInfo.restrictedCards) {
                            CardDbAdapter.addLegalCard(restrictedCard.name, restrictedCard.metadata, CardDbAdapter.RESTRICTED, database);
                        }
                    }

                    /* Drop any out of date sets */
                    for (String code : setsToDrop) {
                        CardDbAdapter.dropSetAndCards(code, database);
                    }

                    /* Add set data */
                    for (MtgSet set : setsToAdd) {
                        CardDbAdapter.createSet(set, database);

                        /* Add the corresponding TCG name */
                        for (CardAndSetParser.NameAndMetadata tcgName : tcgNames) {
                            if (tcgName.metadata.equalsIgnoreCase(set.code)) {
                                CardDbAdapter.addTcgName(tcgName.name, tcgName.metadata, database);
                                break;
                            }
                        }
                    }

                    /* Add cards */
                    for (MtgCard card : cardsToAdd) {
                        CardDbAdapter.createCard(card, database);
                    }

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

                    /* Close the writable database */
                    DatabaseManager.getInstance(getApplicationContext(), true).closeDatabase(true);
                }
            } catch (FamiliarDbException e1) {
                commitDates = false; /* don't commit the dates */
                if (logWriter != null) {
                    e1.printStackTrace(logWriter);
                }
            }

            /* Parse the MTR and IPG */
            MTRIPGParser mtrIpgParser = new MTRIPGParser(mPrefAdapter, this);
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
                parser.commitDates(mPrefAdapter);

                long curTime = new Date().getTime();
                mPrefAdapter.setLastLegalityUpdate((int) (curTime / 1000));
                if (newRulesParsed) {
                    mPrefAdapter.setLastRulesUpdate(curTime);
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
    protected class ProgressReporter implements CardAndSetParser.CardProgressReporter,
            RulesParser.RulesProgressReporter {
        /**
         * This is used by CardAndSetParser to report the progress for adding a set
         *
         * @param progress An integer between 0 and 100 representing the progress percent
         */
        public void reportJsonCardProgress(int progress) {
            mProgress = progress;
        }

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
