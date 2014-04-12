package com.gelakinetic.mtgfam.helpers.database;

import android.database.sqlite.SQLiteDatabase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Adam Feinstein on 4/10/2014.
 */
public class DatabaseManager {

	private AtomicInteger mOpenCounter = new AtomicInteger();

	private static DatabaseManager instance;
	private static DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mDatabase;
	private boolean mTransactional;

	public static synchronized void initializeInstance(DatabaseHelper helper) {
		if (instance == null) {
			instance = new DatabaseManager();
			mDatabaseHelper = helper;
		}
	}

	public static synchronized DatabaseManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
					" is not initialized, call initializeInstance(..) method first.");
		}

		return instance;
	}

	public synchronized SQLiteDatabase openDatabase(boolean isTransactional) {
		if (mOpenCounter.incrementAndGet() == 1) {
			// Opening new database
			mDatabase = mDatabaseHelper.getWritableDatabase();
			mTransactional = isTransactional;
			if (isTransactional) {
				mDatabase.execSQL("BEGIN DEFERRED TRANSACTION");
			}
		}
		return mDatabase;
	}

	public synchronized void closeDatabase() {
		if (mOpenCounter.decrementAndGet() == 0) {
			if (mTransactional) {
				mDatabase.execSQL("COMMIT");
			}
			// Closing database
			mDatabase.close();

		}
	}
}