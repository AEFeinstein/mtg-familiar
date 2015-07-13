package com.gelakinetic.mtgfam.helpers.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * All databases should be opened through this class, so that database access is thread-safe
 */
public class DatabaseManager {

    /**
     * Routing all database access through one point failed when the database was accessed while the
     * updater service was running (transactional open). With this private class, there are now two
     * entry points: a writable transactional one, and a readable one.
     */
    private static class AtomicDatabase {
        private SQLiteDatabase mDatabase;
        private final AtomicInteger mOpenCounter = new AtomicInteger();
        private final boolean mTransactional;
        private DatabaseManager mDatabaseManager;
        private DatabaseHelper mDatabaseHelper;

        /**
         * Constructor, marks if this object is transactional or not
         *
         * @param isTransactional true if the object is transactional, false otherwise
         */
        public AtomicDatabase(boolean isTransactional) {
            mTransactional = isTransactional;
        }

        /**
         * Initializes the DatabaseManagers, mDatabaseManager, and stores the DatabaseHelper
         *
         * @param context A context to initialize with
         */
        synchronized void initializeInstance(Context context) {
            if (mDatabaseManager == null) {
                mDatabaseManager = new DatabaseManager();
                mDatabaseHelper = new DatabaseHelper(context);
            }
        }

        /**
         * Initializes, if necessary, the DatabaseManager, then returns it
         *
         * @param context A context to create a DatabaseManager with, if necessary
         * @return The DatabaseManager
         */
        synchronized DatabaseManager getInstance(Context context) {
            if (mDatabaseManager == null) {
                initializeInstance(context);
            }

            return mDatabaseManager;
        }

        /**
         * Opens a database
         *
         * @return a SQLiteDatabase to query or whatever
         */
        public synchronized SQLiteDatabase openDatabase() {
            if (mOpenCounter.incrementAndGet() == 1) {
                // Opening new database
                if (mTransactional) {
                    mDatabase = mDatabaseHelper.getWritableDatabase();
                    if (mDatabase != null) {
                        mDatabase.execSQL("BEGIN EXCLUSIVE TRANSACTION");
                    }
                } else {
                    mDatabase = mDatabaseHelper.getReadableDatabase();
                }
            }
            return mDatabase;
        }

        /**
         * Close a database opened with this object
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

    private static final AtomicDatabase mDatabase = new AtomicDatabase(false);
    private static final AtomicDatabase mTransactionalDatabase = new AtomicDatabase(true);

    /**
     * Initializes the DatabaseManagers, mDatabaseManager, and stores the singleton DatabaseHelper
     *
     * @param context A singleton DatabaseHelper to open databases with, later
     */
    public static synchronized void initializeInstance(Context context) {
        mDatabase.initializeInstance(context);
        mTransactionalDatabase.initializeInstance(context);
    }

    /**
     * Returns a DatabaseManager, in order to open databases
     *
     * @param context         A context to construct a DatabaseHelper if necessary
     * @param isTransactional Whether we should get a transactional instance or not
     * @return The DatabaseManager
     */
    public static synchronized DatabaseManager getInstance(Context context,
                                                           boolean isTransactional) {
        if (isTransactional) {
            return mTransactionalDatabase.getInstance(context);

        } else {
            return mDatabase.getInstance(context);

        }
    }

    /**
     * Opens a database, either a transactional one or not
     *
     * @param isTransactional Whether or not this database operation is transactional
     */
    public synchronized SQLiteDatabase openDatabase(boolean isTransactional) {
        if (isTransactional) {
            return mTransactionalDatabase.openDatabase();
        } else {
            return mDatabase.openDatabase();
        }
    }

    /**
     * Close a database opened with this class
     *
     * @param isTransactional Whether we should close the transactional database or not
     */
    public synchronized void closeDatabase(boolean isTransactional) {
        if (isTransactional) {
            mTransactionalDatabase.closeDatabase();
        } else {
            mDatabase.closeDatabase();
        }
    }
}