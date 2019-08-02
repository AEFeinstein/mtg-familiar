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
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * This class extends SQLiteOpenHelper in order to copy in the zipped database, and create tables
 */
class DatabaseHelper extends SQLiteOpenHelper {

    /* The name of the database */
    private static final String DATABASE_NAME = "data";

    /**
     * Create a helper object to create, open, and/or manage a database. The database is not actually created or opened
     * until one of getWritableDatabase() or getReadableDatabase() is called. It also copies the zipped database if
     * the database doesn't exist, or is out of date
     *
     * @param context A context to copy the database with
     */
    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, CardDbAdapter.DATABASE_VERSION);
        if (isDbOutOfDate(context)) {
            copyDB(context);
        }
    }

    /**
     * Called when the database is created for the first time. This is where the creation of tables and the initial
     * population of the tables should happen.
     *
     * @param db the database to populate
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CardDbAdapter.DATABASE_CREATE_CARDS);
        db.execSQL(CardDbAdapter.DATABASE_CREATE_SETS);
    }

    /**
     * Called when the database needs to be upgraded. The implementation should use this method to drop tables, add
     * tables, or do anything else it needs to upgrade to the new schema version.
     *
     * @param db         the database to populate
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /* necessary to override, not doing anything */
    }

    /**
     * @return A File pointing to the database, used to inflate the internal database and check if
     * it needs updating. The database file is guaranteed to be closed
     */
    private File getDatabaseFile() throws SQLiteException {
        // Get the database file
        // https://stackoverflow.com/a/50630708/659726
        SQLiteDatabase database = this.getReadableDatabase();
        String filePath = database.getPath();
        database.close();
        this.close();
        return new File(filePath);
    }

    /**
     * Copy the internally packaged gzipped database to where Android can access it.
     *
     * @param context The Context to get the packaged gzipped database from
     */
    private void copyDB(Context context) {

        try {
            // Get the database file
            File dbFile = getDatabaseFile();

            // If the database exists, delete all the files in the database folder, including
            // any write-ahead-logs (thanks Android 9)
            if (dbFile.exists()) {
                for (File file : Objects.requireNonNull(dbFile.getParentFile()).listFiles()) {
                    if (!file.delete()) {
                        /* Couldn't delete the old database, so exit */
                        return;
                    }
                }
                PreferenceAdapter.setDatabaseVersion(context, -1);
            }

            // If the database doesn't exist anymore, inflate the internal database
            if (!dbFile.exists()) {

                GZIPInputStream gis = new GZIPInputStream(context.getResources()
                        .openRawResource(R.raw.datagz));
                FileOutputStream fos = new FileOutputStream(dbFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                PreferenceAdapter.setDatabaseVersion(context, CardDbAdapter.DATABASE_VERSION);

                /* Close the streams */
                fos.flush();
                fos.close();
                gis.close();
            }
        } catch (Resources.NotFoundException | IOException | SQLiteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function to check if the database is up to date.
     *
     * @param context The context used to get the database file
     * @return true if the database does not exist, is too small, or has a lower version than
     * DATABASE_VERSION
     */
    private boolean isDbOutOfDate(Context context) {
        try {
            File f = getDatabaseFile();
            int dbVersion = PreferenceAdapter.getDatabaseVersion(context);
            return (!f.exists() || f.length() < 1048576 || dbVersion < CardDbAdapter.DATABASE_VERSION);
        } catch (SQLiteException e) {
            // Database is locked, assume it's up to date. Can always update later
            return false;
        }
    }
}