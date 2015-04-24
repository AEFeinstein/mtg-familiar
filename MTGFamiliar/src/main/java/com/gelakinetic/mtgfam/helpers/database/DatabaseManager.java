package com.gelakinetic.mtgfam.helpers.database;

import android.database.sqlite.SQLiteDatabase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * All databases should be opened through this singleton class, so that database access is thread-safe
 */
public class DatabaseManager {

    private static DatabaseManager mDatabaseManager;
    private static DatabaseHelper mDatabaseHelper;
    public final AtomicInteger mOpenCounter = new AtomicInteger();
    private SQLiteDatabase mDatabase;
    public boolean mTransactional;

    /**
     * Initializes the singleton DatabaseManager, mDatabaseManager, and stores the singleton DatabaseHelper
     *
     * @param helper A singleton DatabaseHelper to open databases with, later
     */
    public static synchronized void initializeInstance(DatabaseHelper helper) {
        if (mDatabaseManager == null) {
            mDatabaseManager = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    /**
     * Returns the singleton DatabaseManager, in order to open databases
     *
     * @return The singleton DatabaseManager
     */
    public static synchronized DatabaseManager getInstance() {
        if (mDatabaseManager == null) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }

        return mDatabaseManager;
    }

    /**
     * Opens a database, either a transactional one or not
     *
     * @param isTransactional Whether or not this database operation is transactional
     * @return a SQLiteDatabase to query or whatever
     */
    public synchronized SQLiteDatabase openDatabase(boolean isTransactional) {
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
            mTransactional = isTransactional;
            if (isTransactional && mDatabase != null) {
                mDatabase.execSQL("BEGIN DEFERRED TRANSACTION");
            }
        }
        return mDatabase;
    }

    /**
     * Close a database opened with this class
     */
    public synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            if (mTransactional) {
                mDatabase.execSQL("COMMIT");
            }
            mDatabase.close();
        }
    }
}