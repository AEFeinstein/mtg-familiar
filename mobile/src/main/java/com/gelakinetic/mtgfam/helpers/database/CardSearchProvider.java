/**
 * Copyright 2011 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers.database;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Provides access to the card database. Used for the search widget
 */
public class CardSearchProvider extends ContentProvider {

    // The Authority
    public static final String AUTHORITY = "com.gelakinetic.mtgfam.helpers.database.CardSearchProvider";

    // UriMatcher stuff
    private static final int SEARCH_SUGGEST = 0;
    private static final int REFRESH_SHORTCUT = 1;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // to get suggestions...
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    }

    private SQLiteDatabase mDatabase;

    /**
     * In lieu of a constructor
     *
     * @return true if there were no problems, false if otherwise
     */
    @Override
    public synchronized boolean onCreate() {
        assert getContext() != null;
//        DatabaseManager.initializeInstance(new DatabaseHelper(getContext().getApplicationContext()));
//        mDatabase = DatabaseManager.getInstance(false).openDatabase(false); // TODO when to call closeDatabase(false)?
        /* Don't use the DatabaseManager, since the OS may open and close this one with reckless abandon */
        mDatabase = (new DatabaseHelper(getContext()).getReadableDatabase());
        return true;
    }

    /**
     * Handles all the database searches and suggestion queries from the Search
     * Manager. When requesting a specific card, the uri alone is required. When
     * searching all of the database for matches, the selectionArgs argument
     * must carry the search query as the first element. All other arguments are ignored.
     *
     * @param uri           The URI to query. This will be the full URI sent by the client; if the client is requesting
     *                      a specific record, the URI will end in a record number that the implementation should parse
     *                      and add to a WHERE or HAVING clause, specifying that _id value.
     * @param projection    The list of columns to put into the cursor. If null all columns are included.
     * @param selection     A selection criteria to apply when filtering rows. If null then all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs, in
     *                      order that they appear in the selection. The values will be bound as Strings.
     * @param sortOrder     How the rows in the cursor should be sorted. If null then the provider is free to define the
     *                      sort order.
     * @return a Cursor pointing to the queried data
     */
    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                                     String sortOrder) {
        String query;
        // Use the UriMatcher to see what kind of query we have and format the db query accordingly
        try {
            switch (sURIMatcher.match(uri)) {
                case SEARCH_SUGGEST: {
                    if (selectionArgs == null || selectionArgs[0] == null) {
                        return null;
                        //throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
                    }
                    query = selectionArgs[0].toLowerCase();

                    return CardDbAdapter.getCardsByNamePrefix(query, mDatabase);
                }
                case REFRESH_SHORTCUT: {
                    String rowId1 = uri.getLastPathSegment();
                    String[] columns3 = new String[]{BaseColumns._ID, CardDbAdapter.KEY_NAME,
                            SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

                    return CardDbAdapter.getCardByRowId(rowId1, columns3, mDatabase);
                }
                default:
                    throw new IllegalArgumentException("Unknown Uri: " + uri);
            }
        } catch (FamiliarDbException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * This method is required in order to query the supported types. It's also
     * useful in our own query() method to determine the type of Uri received.
     *
     * @param uri the URI to query.
     * @return a MIME type string, or null if there is no type.
     */
    @Override
    public synchronized String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    /**
     * This must be implemented, but it's unsupported.
     *
     * @param uri    The content:// URI of the insertion request. This must not be null.
     * @param values A set of column_name/value pairs to add to the database. This must not be null.
     * @return nothing
     */
    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    /**
     * This must be implemented, but it's unsupported.
     *
     * @param uri           The full URI to query, including a row ID (if a specific record is requested).
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs An optional restriction to apply to rows when deleting.
     * @return nothing
     */
    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /**
     * This must be implemented, but it's unsupported.
     *
     * @param uri           The URI to query. This can potentially have a record ID if this is an update request for a
     *                      specific record.
     * @param values        A set of column_name/value pairs to update in the database. This must not be null.
     * @param selection     An optional filter to match rows to update.
     * @param selectionArgs An optional restriction to apply to rows when deleting.
     * @return nothing
     */
    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
