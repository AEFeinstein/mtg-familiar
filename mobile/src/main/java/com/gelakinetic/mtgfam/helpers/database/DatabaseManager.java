/*
 * Copyright 2017 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * All databases should be opened through this class, so that database access is thread-safe
 */
public class DatabaseManager {

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
     * @param handle          This is set to a value unique to this database access and must be used
     *                        to close the access later
     * @return A SQLiteDatabase object used for database access
     * @throws FamiliarDbException if the database can't be opened
     */
    public synchronized SQLiteDatabase openDatabase(boolean isTransactional, @NonNull FamiliarDbHandle handle) throws FamiliarDbException {
        if (isTransactional) {
            return mTransactionalDatabase.openDatabase(handle);
        } else {
            return mDatabase.openDatabase(handle);
        }
    }

    /**
     * Close a database opened with this class
     *
     * @param isTransactional Whether we should close the transactional database or not
     * @param handle          The handle from openDatabase, used to close this instance
     */
    public synchronized void closeDatabase(boolean isTransactional, @NonNull FamiliarDbHandle handle) {
        if (isTransactional) {
            mTransactionalDatabase.closeDatabase(handle);
        } else {
            mDatabase.closeDatabase(handle);
        }
    }

    /**
     * Routing all database access through one point failed when the database was accessed while the
     * updater service was running (transactional open). With this private class, there are now two
     * entry points: a writable transactional one, and a readable one.
     */
    private static class AtomicDatabase {
        private final static ArrayList<Integer> mOpenHandles = new ArrayList<>();
        private final boolean mTransactional;
        private static SQLiteDatabase mDatabase;
        private static DatabaseManager mDatabaseManager;
        private static DatabaseHelper mDatabaseHelper;

        /**
         * Constructor, marks if this object is transactional or not
         *
         * @param isTransactional true if the object is transactional, false otherwise
         */
        AtomicDatabase(boolean isTransactional) {
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
         * Opens a database and sets the handle through a parameter
         *
         * @param handle This is set to a value unique to this database access and must be used
         *               to close the access later
         * @return a SQLiteDatabase to query or whatever
         * @throws FamiliarDbException if the database can't be opened
         */
        synchronized SQLiteDatabase openDatabase(@NonNull FamiliarDbHandle handle) throws FamiliarDbException {
            // Assign this open a handle
            if (mOpenHandles.isEmpty()) {
                // Start with a nonzero value
                handle.setHandle(1);
            } else {
                // Or use one more than the last value
                handle.setHandle(mOpenHandles.get(mOpenHandles.size() - 1) + 1);
            }

            try {
                // Only open a database if one isn't open already, i.e. there are no handles
                if (mOpenHandles.isEmpty()) {
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
                // Add the handle to the collection of open handles only if the open was successful
                mOpenHandles.add(handle.getHandle());
                return mDatabase;
            } catch (SQLiteException e) {
                throw new FamiliarDbException(e);
            }
        }

        /**
         * Close a database opened with this object
         *
         * @param handle The handle from openDatabase, used to close this instance
         */
        synchronized void closeDatabase(@NonNull FamiliarDbHandle handle) {
            // If there was a successful open with this handle
            if (mOpenHandles.contains(handle.getHandle())) {
                // Remove the handle from the collection of open handles
                mOpenHandles.remove(handle.getHandle());
                // Close the database
                if (mOpenHandles.isEmpty()) {
                    if (mTransactional) {
                        mDatabase.execSQL("COMMIT");
                    }
                    mDatabase.close();
                }
            }
        }
    }
}
