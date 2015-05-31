package com.gelakinetic.mtgfam.helpers.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class extends SQLiteOpenHelper in order to copy in the zipped database, and create tables
 */
class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Create a helper object to create, open, and/or manage a database. The database is not actually created or opened
     * until one of getWritableDatabase() or getReadableDatabase() is called. It also copies the zipped database if
     * the database doesn't exist, or is out of date
     *
     * @param context A context to copy the database with
     */
    public DatabaseHelper(Context context) {
        super(context, CardDbAdapter.DATABASE_NAME, null, CardDbAdapter.DATABASE_VERSION);
        if (CardDbAdapter.isDbOutOfDate(context)) {
            CardDbAdapter.copyDB(context);
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
}