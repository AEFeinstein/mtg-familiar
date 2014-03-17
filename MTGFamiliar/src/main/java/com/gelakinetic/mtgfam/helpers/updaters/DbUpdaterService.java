/**
 Copyright 2012 Michael Shick

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.helpers.updaters;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * This service takes care of updating the database off of the UI thread
 */
public class DbUpdaterService extends IntentService {

	/* Throw this switch to re-parse the entire database from a custom URL (currently UpToRTR.json.gzip
	 * THIS SHOULD NEVER EVER EVER BE TRUE IN A PLAY STORE RELEASE
	 */
	private static final boolean RE_PARSE_DATABASE = false;

	/* Status Codes */
	private static final int STATUS_NOTIFICATION = 31;
	private static final int UPDATED_NOTIFICATION = 32;

	/* To build and display the notification */
	private NotificationManager mNotificationManager;
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

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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
		CardDbAdapter dbHelper;
		PreferenceAdapter mPrefAdapter = new PreferenceAdapter(this);

		ProgressReporter reporter = new ProgressReporter();
		ArrayList<String> updatedStuff = new ArrayList<String>();
		CardAndSetParser parser = new CardAndSetParser();
		boolean commitDates = true;
		boolean newRulesParsed = false;

		try {
			/* Get database access, which auto-opens it, close it, and open a transactional write */
			dbHelper = new CardDbAdapter(this);
			dbHelper.close();
			dbHelper.openTransactional();

			showStatusNotification();

			if (RE_PARSE_DATABASE) {
				/* Blow up the database and download and build it all over again */
				dbHelper.dropCreateDB();
				parser.readLegalityJsonStream(dbHelper, mPrefAdapter, RE_PARSE_DATABASE);
				GZIPInputStream upToGIS = new GZIPInputStream(
						new URL("https://sites.google.com/site/mtgfamiliar/patches/UpToRTR.json.gzip").openStream());
				switchToUpdating(String.format(getString(R.string.update_updating_set), "EVERYTHING!!"));
				parser.readCardJsonStream(upToGIS, reporter, dbHelper);
				parser.readTCGNameJsonStream(mPrefAdapter, dbHelper, RE_PARSE_DATABASE);
			}
			else {
				/* Look for updates with the banned / restricted lists and formats */
				parser.readLegalityJsonStream(dbHelper, mPrefAdapter, RE_PARSE_DATABASE);
				/* Look for new cards */
				ArrayList<String[]> patchInfo = parser.readUpdateJsonStream(mPrefAdapter);
				if (patchInfo != null) {
					/* Look through the list of available patches, and if it doesn't exist in the database, add it. */
					for (String[] set : patchInfo) {
						if (!dbHelper.doesSetExist(set[CardAndSetParser.SET_CODE])) {
							try {
								/* Change the notification to the specific set */
								switchToUpdating(String.format(getString(R.string.update_updating_set),
										set[CardAndSetParser.SET_NAME]));
								GZIPInputStream gis = new GZIPInputStream(
										new URL(set[CardAndSetParser.SET_URL]).openStream());
								parser.readCardJsonStream(gis, reporter, dbHelper);
								updatedStuff.add(set[CardAndSetParser.SET_NAME]);
							} catch (MalformedURLException e) {
								/* Eat it */
							} catch (IOException e) {
								/* Eat it */
							}
							/* Change the notification to generic "checking for updates" */
							switchToChecking();
						}
					}

					/* Look for new TCGPlayer.com versions of set names */
					parser.readTCGNameJsonStream(mPrefAdapter, dbHelper, RE_PARSE_DATABASE);
				}
			}

			/* Parse the rules
			 * Instead of using a hardcoded string, the default lastRulesUpdate is the timestamp of when the APK was
			 * built. This is a safe assumption to make, since any market release will have the latest database baked
			 * in.
			 */

			long lastRulesUpdate;
			if (RE_PARSE_DATABASE) {
				lastRulesUpdate = 0; /* Long, long time ago */
			}
			else {
				lastRulesUpdate = mPrefAdapter.getLastRulesUpdate();
			}

			RulesParser rp = new RulesParser(new Date(lastRulesUpdate), dbHelper, reporter);
			if (rp.needsToUpdate()) {
				if (rp.parseRules()) {
					switchToUpdating(getString(R.string.update_updating_rules));
					int code = rp.loadRulesAndGlossary();

					/* Only save the timestamp of this if the update was 100% successful; if something went screwy, we
					 * should let them know and try again next update.
					 */
					if (code == RulesParser.SUCCESS) {
						newRulesParsed = true;
						updatedStuff.add(getString(R.string.update_added_rules));
					}

					switchToChecking();
				}
			}

			dbHelper.closeTransactional();

			cancelStatusNotification();
		} catch (MalformedURLException e1) {
			commitDates = false; /* don't commit the dates */
		} catch (IOException e) {
			commitDates = false;
		} catch (FamiliarDbException e) {
			commitDates = false;
		}

		/* Parse the MTR and IPG */

		boolean mtrUpdated = false;
		long lastMTRUpdate = mPrefAdapter.getLastMTRUpdate();
		MTRIPGParser mtrParser = new MTRIPGParser(new Date(lastMTRUpdate), this);
		if (mtrParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MTR_IPG_MODE.MODE_MTR)) {
			mtrUpdated = true;
			updatedStuff.add(getString(R.string.update_added_mtr));
		}

		boolean ipgUpdated = false;
		long lastIPGUpdate = mPrefAdapter.getLastIPGUpdate();
		MTRIPGParser ipgParser = new MTRIPGParser(new Date(lastIPGUpdate), this);
		if (ipgParser.performMtrIpgUpdateIfNeeded(MTRIPGParser.MTR_IPG_MODE.MODE_IPG)) {
			ipgUpdated = true;
			updatedStuff.add(getString(R.string.update_added_ipg));
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
			if (mtrUpdated) {
				mPrefAdapter.setLastMTRUpdate(curTime);
			}
			if (ipgUpdated) {
				mPrefAdapter.setLastIPGUpdate(curTime);
			}
		}
	}

	/**
	 * Show the notification in the status bar
	 */
	void showStatusNotification() {
		mNotificationManager.notify(STATUS_NOTIFICATION, mBuilder.build());
	}

	/**
	 * Hide the notification in the status bar
	 */
	void cancelStatusNotification() {
		mNotificationManager.cancel(STATUS_NOTIFICATION);
	}

	/**
	 * Set the notification to display "Checking for database updates"
	 */
	void switchToChecking() {
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
	void switchToUpdating(String title) {

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
	void showUpdatedNotification(List<String> newStuff) {
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
