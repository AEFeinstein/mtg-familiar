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
import android.database.sqlite.SQLiteDatabase;
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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
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
        try {
            PreferenceAdapter mPrefAdapter = new PreferenceAdapter(this);

            ProgressReporter reporter = new ProgressReporter();
            ArrayList<String> updatedStuff = new ArrayList<>();
            CardAndSetParser parser = new CardAndSetParser();
            boolean commitDates = true;
            boolean newRulesParsed = false;

            try {

                showStatusNotification();

                ArrayList<MtgCard> cardsToAdd = new ArrayList<>();
                ArrayList<MtgSet> setsToAdd = new ArrayList<>();
                ArrayList<CardAndSetParser.NameAndMetadata> tcgNames = new ArrayList<>();

				/* Look for updates with the banned / restricted lists and formats */
                CardAndSetParser.LegalInfo legalInfo = parser.readLegalityJsonStream(mPrefAdapter);
                /* Look for new cards */
                ArrayList<String[]> patchInfo = parser.readUpdateJsonStream(mPrefAdapter);
                if (patchInfo != null) {
                    /* Get readable database access */
                    SQLiteDatabase database = DatabaseManager.getInstance(getApplicationContext(), false).openDatabase(false);

                    /* Look through the list of available patches, and if it doesn't exist in the database, add it. */
                    for (String[] set : patchInfo) {
                        if (!CardDbAdapter.doesSetExist(set[CardAndSetParser.SET_CODE], database)) { /* this is fine, readable */
                            try {
                                /* Change the notification to the specific set */
                                switchToUpdating(String.format(getString(R.string.update_updating_set),
                                        set[CardAndSetParser.SET_NAME]));
                                GZIPInputStream gis = new GZIPInputStream(
                                        new URL(set[CardAndSetParser.SET_URL]).openStream());
                                parser.readCardJsonStream(gis, reporter, cardsToAdd, setsToAdd);
                                updatedStuff.add(set[CardAndSetParser.SET_NAME]);
                            } catch (IOException e) {
                                /* Eat it */
                            }
                            /* Change the notification to generic "checking for updates" */
                            switchToChecking();
                        }
                    }
                    /* close the database */
                    DatabaseManager.getInstance(getApplicationContext(), false).closeDatabase(false);
                }
                /* Look for new TCGPlayer.com versions of set names */
                parser.readTCGNameJsonStream(mPrefAdapter, tcgNames);

				/* Parse the rules
                 * Instead of using a hardcoded string, the default lastRulesUpdate is the timestamp of when the APK was
				 * built. This is a safe assumption to make, since any market release will have the latest database baked
				 * in.
				 */

                long lastRulesUpdate = mPrefAdapter.getLastRulesUpdate();

                RulesParser rp = new RulesParser(new Date(lastRulesUpdate), reporter);
                ArrayList<RulesParser.RuleItem> rulesToAdd = new ArrayList<>();
                ArrayList<RulesParser.GlossaryItem> glossaryItemsToAdd = new ArrayList<>();

                if (rp.needsToUpdate()) {
                    switchToUpdating(getString(R.string.update_updating_rules));
                    if (rp.parseRules()) {
                        rp.loadRulesAndGlossary(rulesToAdd, glossaryItemsToAdd);

						/* Only save the timestamp of this if the update was 100% successful; if something went screwy, we
                         * should let them know and try again next update.
						 */
                        newRulesParsed = true;
                        updatedStuff.add(getString(R.string.update_added_rules));
                    }
                    switchToChecking();
                }

                /* Open a writable database, as briefly as possible */

                if (legalInfo != null ||
                        setsToAdd.size() > 0 ||
                        cardsToAdd.size() > 0 ||
                        tcgNames.size() > 0 ||
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

                    /* Add card and set data */
                    for (MtgSet set : setsToAdd) {
                        CardDbAdapter.createSet(set, database);
                    }

                    for (MtgCard card : cardsToAdd) {
                        CardDbAdapter.createCard(card, database);
                    }

                    /* Add tcg name data */
                    for (CardAndSetParser.NameAndMetadata tcgName : tcgNames) {
                        CardDbAdapter.addTcgName(tcgName.name, tcgName.metadata, database);
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
                cancelStatusNotification();
            } catch (FamiliarDbException | IOException e1) {
                commitDates = false; /* don't commit the dates */
            }

			/* Parse the MTR and IPG */
            MTRIPGParser mtrIpgParser = new MTRIPGParser(mPrefAdapter, this);
            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_MTR)) {
                updatedStuff.add(getString(R.string.update_added_mtr));
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_IPG)) {
                updatedStuff.add(getString(R.string.update_added_ipg));
            }

            if (mtrIpgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MODE_JAR)) {
                updatedStuff.add(getString(R.string.update_added_jar));
            }

			/* If everything went well so far, commit the date and show the update complete notification */
            if (commitDates) {
                showUpdatedNotification(updatedStuff);

                parser.commitDates(mPrefAdapter);

                long curTime = new Date().getTime();
                mPrefAdapter.setLastLegalityUpdate((int) (curTime / 1000));
                if (newRulesParsed) {
                    mPrefAdapter.setLastRulesUpdate(curTime);
                }
            } else {
                cancelStatusNotification();
            }
        } catch (Exception e) {
            /* Generally pokemon handling is bad, but I don't want to leave a notification */
            cancelStatusNotification();
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

        mProgressUpdater = new Runnable() {
            public void run() {
                mBuilder.setProgress(100, mProgress, false);
                mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());
                mHandler.postDelayed(mProgressUpdater, 200);
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
