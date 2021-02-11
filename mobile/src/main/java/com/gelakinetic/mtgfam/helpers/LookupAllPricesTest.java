/*
 * Copyright 2018 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers;

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Log;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceFetcher;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("unused")
public class LookupAllPricesTest extends AsyncTask<FamiliarActivity, Void, Void> {

    private static final String DAPT_TAG = "DAPT";
    private int totalElapsedSuccess = 0;
    private int totalSuccess = 0;
    private int totalElapsedFailure = 0;
    private int totalFailure = 0;
    private final FamiliarDbHandle mHandle = new FamiliarDbHandle();

    /**
     * Get all cards from the database, then look up all of their prices
     *
     * @param activities The activity which started this task
     * @return nothing
     */
    @Override
    protected Void doInBackground(FamiliarActivity... activities) {

        // Save the activity
        FamiliarActivity activity = activities[0];

        // Delete all caches
        try {
            File cacheDir = activity.getExternalCacheDir();
            for (File cacheFile : Objects.requireNonNull(Objects.requireNonNull(cacheDir).listFiles())) {
                //noinspection ResultOfMethodCallIgnored
                cacheFile.delete();
            }
            cacheDir = activity.getCacheDir();
            for (File cacheFile : Objects.requireNonNull(Objects.requireNonNull(cacheDir).listFiles())) {
                //noinspection ResultOfMethodCallIgnored
                cacheFile.delete();
            }
        } catch (NullPointerException e) {
            // Eh
        }

        try {
            // Search for all cards
            SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, mHandle);
            SearchCriteria criteria = new SearchCriteria();
            criteria.superTypes = new ArrayList<>(1);
            criteria.superTypes.add("!asdl");
            String[] returnTypes = {
                    CardDbAdapter.KEY_NAME,
                    CardDbAdapter.KEY_SET,
                    CardDbAdapter.KEY_SUBTYPE,
                    CardDbAdapter.KEY_SUPERTYPE,
                    CardDbAdapter.KEY_NUMBER};
            String orderByStr = CardDbAdapter.KEY_SET + " ASC, " + CardDbAdapter.KEY_NUMBER + " ASC";
            Cursor allCards = CardDbAdapter.Search(criteria, false, returnTypes, false, orderByStr, database);

            if (null != allCards) {
                // Log how many cards there are to lookup
                allCards.moveToLast();
                Log.d(DAPT_TAG, "Checking " + allCards.getPosition() + " prices");
                allCards.moveToFirst();

                // Try to lookup all prices
                lookupCard(activity.mMarketPriceStore, allCards, activity);
            }
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Given a fetcher and a cursor pointed to card data in the database, lookup the card price
     *
     * @param fetcher The fetcher to fetch the card price with
     * @param cursor  The cursor pointing to card data in the database
     */
    private void lookupCard(final MarketPriceFetcher fetcher, final Cursor cursor, final FamiliarActivity activity) {
        // Make an MtgCard object from the cursor row
        try {
            MtgCard toLookup = new MtgCard(activity,
                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME)),
                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_SET)),
                    false, 0);

            // Start the lookup and log the time
            long start = System.currentTimeMillis();
            fetcher.fetchMarketPrice(toLookup,
                    marketPriceInfo -> {
                        // Timing
                        long elapsed = System.currentTimeMillis() - start;
                        totalElapsedSuccess += elapsed;
                        totalSuccess++;

                        // Debug print
                        String priceStr = "";
                        if (marketPriceInfo.hasNormalPrice()) {
                            priceStr = String.format(Locale.US, "$%.2f", marketPriceInfo.getPrice(false, MarketPriceInfo.PriceType.MARKET).price);
                        } else if (marketPriceInfo.hasFoilPrice()) {
                            priceStr = String.format(Locale.US, "$%.2f", marketPriceInfo.getPrice(true, MarketPriceInfo.PriceType.MARKET).price);
                        }
                        Log.d(DAPT_TAG, "Success [" + toLookup.getExpansion() + "] " + toLookup.getName() + " in " + elapsed + "ms : " + priceStr);

                        // Move to the next
                        fetchNext(fetcher, cursor, activity);
                    },
                    throwable -> {
                        // Timing
                        long elapsed = System.currentTimeMillis() - start;
                        totalElapsedFailure += elapsed;
                        totalFailure++;

                        // Debug print
                        Log.d(DAPT_TAG, "Failure [" + toLookup.getExpansion() + "] " + toLookup.getName() + " in " + elapsed + "ms, " + throwable.getMessage());

                        // Move to the next
                        fetchNext(fetcher, cursor, activity);
                    },
                    () -> {
                    });
        } catch (InstantiationException e) {

            // Debug print
            Log.d(DAPT_TAG, "Failure [" + cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_SET)) + "] " +
                    cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME)) + ", " + e.getMessage());

            // Move to the next
            fetchNext(fetcher, cursor, activity);
        }
    }

    /**
     * Try to move the cursor to the next card and start looking up the price
     *
     * @param fetcher The fetcher to fetch the card price with
     * @param cursor  The cursor to advance
     */
    private void fetchNext(MarketPriceFetcher fetcher, Cursor cursor, FamiliarActivity activity) {
        cursor.moveToNext();
        if (!cursor.isAfterLast()) {
            lookupCard(fetcher, cursor, activity);
        } else {
            Log.d(DAPT_TAG, totalSuccess + " successes (avg " + (totalElapsedSuccess / (double) totalSuccess) + "ms)");
            Log.d(DAPT_TAG, totalFailure + " failures (avg " + (totalElapsedFailure / (double) totalFailure) + "ms)");
            cursor.close();
            DatabaseManager.closeDatabase(activity, mHandle);
        }

    }
}
