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

package com.gelakinetic.mtgfam.helpers.tcgp;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.AccessToken;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductInformation;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductMarketPrice;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.nytimes.android.external.store3.base.Fetcher;
import com.nytimes.android.external.store3.base.Persister;
import com.nytimes.android.external.store3.base.RecordProvider;
import com.nytimes.android.external.store3.base.RecordState;
import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.annotation.Nonnull;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

/**
 * Created by Adam on 1/29/2018.
 */

public class MarketPriceFetchService {

    private static final String KEY_PREFIX = "price_";
    private static final int MAX_NUM_RETRIES = 8;

    Activity mContext;
    Store<MarketPriceInfo, MarketPriceLookupParams> mStore;

    abstract class RecordingPersister<R, K> implements RecordProvider<K>, Persister<R, K> {
    }

    /**
     * TODO doc
     *
     * @param context
     */
    public MarketPriceFetchService(Activity context) {
        // Save the context
        mContext = context;

        // Create the fetcher which actually gets the data
        Fetcher<MarketPriceInfo, MarketPriceLookupParams> mFetcher = new Fetcher<MarketPriceInfo, MarketPriceLookupParams>() {
            /**
             * TODO doc
             * @param params
             * @return
             */
            @Nonnull
            @Override
            public Single<MarketPriceInfo> fetch(@Nonnull MarketPriceLookupParams params) {
                Exception lastThrownException = null;
                MarketPriceInfo infoToReturn = new MarketPriceInfo();
                if (FamiliarActivity.getNetworkState(mContext, true) == -1) { // our context contains the activity that spawned the request
                    return Single.error(new Exception(mContext.getString(R.string.no_network)));
                }
                /* try the fetch up to eight times, for different accent mark & split card combos*/
                int retry = MAX_NUM_RETRIES;
                /* then the same for multicard ordering */
                SQLiteDatabase database;
                try {
                    database = DatabaseManager.getInstance(mContext, false).openDatabase(false);
                } catch (FamiliarDbException e) {
                    return Single.error(e);
                }
                while (retry > 0) {
                    try {
                        /* If the card number wasn't given, figure it out */
                        if (params.mCardNumber == null || params.mCardNumber.equals("") || params.mCardType == null || params.mCardType.equals("") || params.mMultiverseID == -1) {
                            Cursor c = CardDbAdapter.fetchCardByNameAndSet(params.mCardName, params.mSetCode, CardDbAdapter.ALL_CARD_DATA_KEYS, database);

                            if (params.mCardNumber == null || params.mCardNumber.equals("")) {
                                params.mCardNumber = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                            }

                            if (params.mCardType == null || params.mCardType.equals("")) {
                                params.mCardType = CardDbAdapter.getTypeLine(c);
                            }

                            if (params.mMultiverseID == -1) {
                                params.mMultiverseID = CardDbAdapter.getMultiverseIdFromNameAndSet(params.mCardName, params.mSetCode, database);
                                if (params.mMultiverseID == -1) {
                                    c.close();
                                    throw new FamiliarDbException(null);
                                }
                            }
                            c.close();
                        }

                        CardDbAdapter.MultiCardType multiCardType = CardDbAdapter.isMultiCard(params.mCardNumber, params.mSetCode);

                        /* Get the TCGplayer.com set name, why can't everything be consistent? */
                        String tcgName = CardDbAdapter.getTcgName(params.mSetCode, database);
                        /* Figure out the tcgCardName, which is tricky for split cards */
                        String tcgCardName;

                        /* Set up retries for multicard ordering */
                        if (multiCardType != CardDbAdapter.MultiCardType.NOPE) {
                            /* Next time try the other order */
                            switch (retry % (MAX_NUM_RETRIES / 2)) {
                                case 0:
                                    /* Try just the a side */
                                    tcgCardName = CardDbAdapter.getNameFromSetAndNumber(params.mSetCode, params.mCardNumber.replace("b", "a"), database);
                                    break;
                                case 3:
                                    /* Try just the b side */
                                    tcgCardName = CardDbAdapter.getNameFromSetAndNumber(params.mSetCode, params.mCardNumber.replace("a", "b"), database);
                                    break;
                                case 2:
                                    /* Try the combined name in one direction */
                                    tcgCardName = CardDbAdapter.getSplitName(params.mMultiverseID, true, database);
                                    break;
                                case 1:
                                    /* Try the combined name in the other direction */
                                    tcgCardName = CardDbAdapter.getSplitName(params.mMultiverseID, false, database);
                                    break;
                                default:
                                    /* Something went wrong */
                                    tcgCardName = params.mCardName;
                                    break;
                            }
                        } else {
                            /* This isn't a multicard */
                            tcgCardName = params.mCardName;
                        }

                        /* Retry with accent marks removed */
                        if (retry <= MAX_NUM_RETRIES / 2) {
                            tcgCardName = CardDbAdapter.removeAccentMarks(tcgCardName);
                        }

                        // Tack on the number for basic lands
                        if (params.mCardType.startsWith("Basic Land")) {
                            tcgCardName += " (" + params.mCardNumber + ")";
                        }

                        TcgpApi api = new TcgpApi();
                        AccessToken token = api.getAccessToken("", "", "");
                        ProductInformation information = api.getProductInformation(tcgName, tcgCardName);
                        if (information.success) {
                            ProductMarketPrice price = api.getProductMarketPrice(information.results);
                            // TODO fill in infoToReturn
                            break;
                        }

                        /* If this is a single card, skip over a bunch of retry cases */
                        if (retry == MAX_NUM_RETRIES && multiCardType == CardDbAdapter.MultiCardType.NOPE) {
                            retry = 2;
                        }
                    } catch (Exception e) {
                        lastThrownException = e;
                    }
                    retry--;
                }
                DatabaseManager.getInstance(mContext, false).closeDatabase(false);         /* database close if something failed */
                if (null != lastThrownException) {
                    return Single.error(lastThrownException);
                } else {
                    return Single.just(infoToReturn); // Actual price
                }
            }
        };

        // Create the Persister, which also handles managing cache staleness
        RecordingPersister<MarketPriceInfo, MarketPriceLookupParams> mPersister = new RecordingPersister<MarketPriceInfo, MarketPriceLookupParams>() {

            private static final int MAX_TIME_IN_CACHE_MS = 86400000; // One day's worth of ms

            /**
             * TODO doc
             * @param cacheKey
             * @return
             */
            private File getCacheFile(MarketPriceLookupParams cacheKey) {
                return new File(mContext.getCacheDir(), KEY_PREFIX + cacheKey.mMultiverseID);
            }

            /**
             * TODO doc
             * @param cacheKey
             * @return
             */
            @Nonnull
            @Override
            public RecordState getRecordState(@Nonnull MarketPriceLookupParams cacheKey) {
                // Get the file
                File file = getCacheFile(cacheKey);
                // Make sure it exists
                if (file.exists()) {
                    // How long has it been in there?
                    long timeInCacheMs = System.currentTimeMillis() - file.lastModified();
                    if (timeInCacheMs <= MAX_TIME_IN_CACHE_MS) {
                        // File exists and isn't stale
                        return RecordState.FRESH;
                    } else {
                        // File exists, but is stale
                        return RecordState.STALE;
                    }
                } else {
                    // File doesn't exist
                    return RecordState.STALE;
                }
            }

            /**
             * TODO doc
             * @param cacheKey
             * @return
             */
            @Nonnull
            @Override
            public Maybe<MarketPriceInfo> read(@Nonnull final MarketPriceLookupParams cacheKey) {
                return Maybe.create(new MaybeOnSubscribe<MarketPriceInfo>() {
                    @Override
                    public void subscribe(MaybeEmitter<MarketPriceInfo> emitter) {
                        try {
                            // Attempt to read the cache file
                            File cacheFile = getCacheFile(cacheKey);
                            FileReader reader = new FileReader(cacheFile);
                            MarketPriceInfo info = new Gson().fromJson(reader, MarketPriceInfo.class);
                            reader.close();
                            // Cache file read, emit it
                            emitter.onSuccess(info);
                        } catch (Exception e) {
                            // Some exception occurred
                            emitter.onError(e);
                        }
                    }
                });
            }

            /**
             * TODO doc
             * @param cacheKey
             * @param marketPriceInfo
             * @return
             */
            @Nonnull
            @Override
            public Single<Boolean> write(@Nonnull MarketPriceLookupParams cacheKey, @Nonnull MarketPriceInfo marketPriceInfo) {
                try {
                    // Attempt to write the cache file
                    File cacheFile = getCacheFile(cacheKey);
                    JsonWriter writer = new JsonWriter(new FileWriter(cacheFile));
                    new Gson().toJson(marketPriceInfo, MarketPriceInfo.class, writer);
                    writer.close();
                    // The cache file was written
                    return Single.just(true);
                } catch (Exception e) {
                    // Eat the exception and return that the file wasn't written
                    return Single.just(false);
                }
            }
        };

        mStore = StoreBuilder.<MarketPriceLookupParams, MarketPriceInfo>key()
                .fetcher(mFetcher)
                .persister(mPersister)
                .networkBeforeStale().memoryPolicy(MemoryPolicy.builder().setMemorySize(1).build())
                .open();
    }

    /**
     * TODO doc
     * TODO call unsubscribe on fragment exits
     *
     * @param mCardName
     * @param mSetCode
     * @param mMultiverseID
     * @param mCardType
     * @param mCardNumber
     * @param onSuccess
     * @param onError
     */
    public void fetchMarketPrice(String mCardName, String mSetCode, int mMultiverseID, String mCardType,
                                 String mCardNumber, final Consumer<MarketPriceInfo> onSuccess,
                                 final Consumer<Throwable> onError) {
        // Bundle up the arguments
        final MarketPriceLookupParams params = new MarketPriceLookupParams(mMultiverseID, mCardName,
                mSetCode, mCardType, mCardNumber);

        // Start a new thread to perform the fetch
        new Thread(new Runnable() {
            @Override
            public void run() {
                mStore.get(params).subscribe(new Consumer<MarketPriceInfo>() {
                    @Override
                    public void accept(final MarketPriceInfo marketPriceInfo) throws Exception {
                        // Run the results on the UI thread
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    onSuccess.accept(marketPriceInfo);
                                } catch (Exception e) {
                                    // Eat it
                                }
                            }
                        });
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(final Throwable throwable) throws Exception {
                        // Run the erros on the UI thread
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    onError.accept(throwable);
                                } catch (Exception e) {
                                    // Eat it
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }
}
