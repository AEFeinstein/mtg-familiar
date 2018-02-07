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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.AccessToken;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductDetails;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductInformation;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductMarketPrice;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.nytimes.android.external.store3.base.Fetcher;
import com.nytimes.android.external.store3.base.Persister;
import com.nytimes.android.external.store3.base.RecordProvider;
import com.nytimes.android.external.store3.base.RecordState;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import javax.annotation.Nonnull;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

public class MarketPriceFetcher {

    private static final String KEY_PREFIX = "price_";
    private static final int MAX_NUM_RETRIES = 8;

    private final FamiliarActivity mActivity;
    private final Store<MarketPriceInfo, MtgCard> mStore;
    private int mNumPriceRequests = 0;

    abstract class RecordingPersister<Rec, Key> implements RecordProvider<Key>, Persister<Rec, Key> {
    }

    /**
     * Constructor. Set up a MarketPriceFetcher with the given Activity as a Context
     *
     * @param context The Activity context used for strings, preferences, and the like
     */
    public MarketPriceFetcher(FamiliarActivity context) {
        /* Save the context */
        mActivity = context;

        /* Create the fetcher which actually gets the data */
        Fetcher<MarketPriceInfo, MtgCard> mFetcher = new Fetcher<MarketPriceInfo, MtgCard>() {
            /**
             * This method fetches the price from TCGPlayer.com's API. It makes network calls, so
             * it must not be called from the UI thread
             *
             * @param params The card to fetch price info for
             * @return A Single with either the price info or a thrown exception
             */
            @Nonnull
            @Override
            public Single<MarketPriceInfo> fetch(@Nonnull MtgCard params) {
                Exception lastThrownException = null;
                if (FamiliarActivity.getNetworkState(mActivity, false) == -1) { /* our context contains the activity that spawned the request */
                    return Single.error(new Exception(mActivity.getString(R.string.no_network)));
                }

                /* Initialize the API */
                TcgpApi api = new TcgpApi();
                String tokenStr = PreferenceAdapter.getTcgpApiToken(mActivity);
                Date expirationDate = PreferenceAdapter.getTcgpApiTokenExpirationDate(mActivity);
                try {
                    /* If we don't have a token or it expired */
                    if (tokenStr.isEmpty() || expirationDate.before(new Date())) {
                        /* Request a token. This will initialize the TcgpApi object */
                        AccessToken token;
                        token = api.getAccessToken(TcgpKeys.PUBLIC_KEY, TcgpKeys.PRIVATE_KEY, TcgpKeys.ACCESS_TOKEN);
                        /* Save the token and expiration date */
                        PreferenceAdapter.setTcgpApiToken(mActivity, token.access_token);
                        PreferenceAdapter.setTcgpApiTokenExpirationDate(mActivity, token.expires);
                    } else {
                        /* Make sure the token hasn't expired */
                        api.setToken(tokenStr);
                    }
                } catch (IOException e) {
                    return Single.error(e);
                }

                /* try the fetch up to eight times, for different accent mark & split card combos*/
                int retry = MAX_NUM_RETRIES;
                /* then the same for multicard ordering */
                SQLiteDatabase database;
                try {
                    database = DatabaseManager.getInstance(mActivity, false).openDatabase(false);
                } catch (FamiliarDbException e) {
                    return Single.error(e);
                }
                while (retry > 0) {
                    try {
                        /* If the card number wasn't given, figure it out */
                        if (params.mNumber == null || params.mNumber.equals("") || params.mType == null || params.mType.equals("") || params.mMultiverseId == -1) {
                            Cursor c = CardDbAdapter.fetchCardByNameAndSet(params.mName, params.mExpansion, CardDbAdapter.ALL_CARD_DATA_KEYS, database);

                            if (params.mNumber == null || params.mNumber.equals("")) {
                                params.mNumber = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                            }

                            if (params.mType == null || params.mType.equals("")) {
                                params.mType = CardDbAdapter.getTypeLine(c);
                            }

                            if (params.mMultiverseId == -1) {
                                params.mMultiverseId = CardDbAdapter.getMultiverseIdFromNameAndSet(params.mName, params.mExpansion, database);
                                if (params.mMultiverseId == -1) {
                                    c.close();
                                    throw new FamiliarDbException(null);
                                }
                            }
                            c.close();
                        }

                        CardDbAdapter.MultiCardType multiCardType = CardDbAdapter.isMultiCard(params.mNumber, params.mExpansion);

                        /* Get the TCGplayer.com set name, why can't everything be consistent? */
                        String tcgSetName = CardDbAdapter.getTcgName(params.mExpansion, database);
                        /* Figure out the tcgCardName, which is tricky for split cards */
                        String tcgCardName;

                        /* Set up retries for multicard ordering */
                        if (multiCardType != CardDbAdapter.MultiCardType.NOPE) {
                            /* Next time try the other order */
                            switch (retry % (MAX_NUM_RETRIES / 2)) {
                                case 0:
                                    /* Try just the a side */
                                    tcgCardName = CardDbAdapter.getNameFromSetAndNumber(params.mExpansion, params.mNumber.replace("b", "a"), database);
                                    break;
                                case 3:
                                    /* Try just the b side */
                                    tcgCardName = CardDbAdapter.getNameFromSetAndNumber(params.mExpansion, params.mNumber.replace("a", "b"), database);
                                    break;
                                case 2:
                                    /* Try the combined name in one direction */
                                    tcgCardName = CardDbAdapter.getSplitName(params.mMultiverseId, true, database);
                                    break;
                                case 1:
                                    /* Try the combined name in the other direction */
                                    tcgCardName = CardDbAdapter.getSplitName(params.mMultiverseId, false, database);
                                    break;
                                default:
                                    /* Something went wrong */
                                    tcgCardName = params.mName;
                                    break;
                            }
                        } else {
                            /* This isn't a multicard */
                            tcgCardName = params.mName;
                        }

                        /* Retry with accent marks removed */
                        if (retry <= MAX_NUM_RETRIES / 2) {
                            tcgCardName = CardDbAdapter.removeAccentMarks(tcgCardName);
                        }

                        /* Tack on the number for basic lands */
                        if (params.mType.startsWith("Basic Land")) {
                            tcgCardName += " (" + params.mNumber + ")";
                        }

                        /* Query the API, one step at a time */
                        ProductInformation information = api.getProductInformation(tcgCardName, tcgSetName);
                        if (information.success && information.results.length > 0) {
                            ProductMarketPrice price = api.getProductMarketPrice(information.results);
                            if (price.success && price.results.length > 0) {
                                ProductDetails details = api.getProductDetails(information.results);
                                if (details.success && details.results.length > 0) {
                                    /* database close if all is good */
                                    DatabaseManager.getInstance(mActivity, false).closeDatabase(false);

                                    /* Return a new MarketPriceInfo */
                                    return Single.just(new MarketPriceInfo(price.results, details.results));
                                }
                            }
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
                /* database close if something failed */
                DatabaseManager.getInstance(mActivity, false).closeDatabase(false);
                if (null != lastThrownException) {
                    return Single.error(lastThrownException);
                } else {
                    return Single.error(new Exception(mActivity.getString(R.string.price_error_unknown)));
                }
            }
        };

        /* Create the Persister, which also handles managing cache staleness */
        RecordingPersister<MarketPriceInfo, MtgCard> mPersister = new RecordingPersister<MarketPriceInfo, MtgCard>() {

            private static final int MAX_TIME_IN_CACHE_MS = 86400000; /* One day's worth of ms */

            /**
             * Given a key, return a cache file
             *
             * @param cacheKey The cache key, a MtgCard object
             * @return A File for this key
             */
            private File getCacheFile(MtgCard cacheKey) {
                return new File(mActivity.getCacheDir(), (KEY_PREFIX + cacheKey.mName + "-" + cacheKey.mExpansion).replaceAll("\\W+", ""));
            }

            /**
             * Given a cache key, return whether it's FRESH or STALE. A record is stale after 24hrs
             * MISSING is another record state, but the calling function doesn't check against it,
             * so don't use it
             *
             * @param cacheKey The cache key, a MtgCard object
             * @return RecordState.FRESH or RecordState.STALE
             */
            @Nonnull
            @Override
            public RecordState getRecordState(@Nonnull MtgCard cacheKey) {
                /* Get the file */
                File file = getCacheFile(cacheKey);
                /* Make sure it exists */
                if (file.exists()) {
                    /* How long has it been in there? */
                    long timeInCacheMs = System.currentTimeMillis() - file.lastModified();
                    if (timeInCacheMs <= MAX_TIME_IN_CACHE_MS) {
                        /* File exists and isn't stale */
                        return RecordState.FRESH;
                    } else {
                        /* File exists, but is stale */
                        return RecordState.STALE;
                    }
                } else {
                    /* File doesn't exist */
                    return RecordState.STALE;
                }
            }

            /**
             * Read the cache file associated with the given key and return the cached
             * MarketPriceInfo if possible
             *
             * @param cacheKey The cache key, a MtgCard object
             * @return A Maybe either with the read MarketPriceInfo or an exception
             */
            @Nonnull
            @Override
            public Maybe<MarketPriceInfo> read(@Nonnull final MtgCard cacheKey) {
                return Maybe.create(new MaybeOnSubscribe<MarketPriceInfo>() {
                    @Override
                    public void subscribe(MaybeEmitter<MarketPriceInfo> emitter) {
                        try {
                            /* Attempt to read the cache file */
                            File cacheFile = getCacheFile(cacheKey);
                            FileReader reader = new FileReader(cacheFile);
                            MarketPriceInfo info = new Gson().fromJson(reader, MarketPriceInfo.class);
                            reader.close();
                            /* Cache file read, emit it */
                            emitter.onSuccess(info);
                        } catch (Exception e) {
                            /* Some exception occurred */
                            emitter.onError(e);
                        }
                    }
                });
            }

            /**
             * Write a MarketPriceInfo to the cache with the given key
             *
             * @param cacheKey The cache key, a MtgCard object
             * @param marketPriceInfo A MarketPriceInfo to write
             * @return a Single with true if the write succeeded, or false if it didn't
             */
            @Nonnull
            @Override
            public Single<Boolean> write(@Nonnull MtgCard cacheKey, @Nonnull MarketPriceInfo marketPriceInfo) {
                try {
                    /* Attempt to write the cache file */
                    File cacheFile = getCacheFile(cacheKey);
                    JsonWriter writer = new JsonWriter(new FileWriter(cacheFile));
                    new Gson().toJson(marketPriceInfo, MarketPriceInfo.class, writer);
                    writer.close();
                    /* The cache file was written */
                    return Single.just(true);
                } catch (Exception e) {
                    /* Eat the exception and return that the file wasn't written */
                    return Single.just(false);
                }
            }
        };

        mStore = StoreBuilder.<MtgCard, MarketPriceInfo>key()
                .fetcher(mFetcher)
                .persister(mPersister)
                .networkBeforeStale()
                .open();
    }

    /**
     * This function fetches the price for a given MtgCard and calls the appropriate callbacks.
     * It ensures the network operations are called on a non-UI thread and the result callbacks are
     * called on the UI thread.
     *
     * @param card      A MtgCard to fetch data for. It must have a mName and mExpansion populated
     * @param onSuccess A Consumer callback to be called when the price is fetched
     * @param onError   A Consumer callback to be called when an error occurs
     */
    public void fetchMarketPrice(final MtgCard card, final Consumer<MarketPriceInfo> onSuccess,
                                 final Consumer<Throwable> onError) {

        if (null == card.mName || card.mName.isEmpty() || null == card.mExpansion || card.mExpansion.isEmpty()) {
            throw new IllegalArgumentException("card must have a name and expansion to fetch price");
        }

        /* Show the loading animation */
        mNumPriceRequests++;
        mActivity.setLoading();

        /* Start a new thread to perform the fetch */
        new Thread(new Runnable() {
            /**
             * This runnable gets the card price from either the cache or network, and runs on a
             * non-UI thread
             */
            @Override
            public void run() {
                mStore.get(card).subscribe(new Consumer<MarketPriceInfo>() {
                    /**
                     * This callback is called when a MarketPriceInfo is fetched either from the
                     * network or cache. The callback runs on a non-UI thread, but invokes the given
                     * callback on a UI thread
                     *
                     * @param marketPriceInfo The fetched MarketPriceInfo
                     * @throws Exception If something goes terribly wrong
                     */
                    @Override
                    public void accept(final MarketPriceInfo marketPriceInfo) throws Exception {
                        /* Run the results on the UI thread */
                        mActivity.runOnUiThread(new Runnable() {
                            /**
                             * This runs the given success callback on the UI thread
                             */
                            @Override
                            public void run() {
                                mNumPriceRequests--;
                                if (0 == mNumPriceRequests) {
                                    mActivity.clearLoading();
                                }
                                try {
                                    onSuccess.accept(marketPriceInfo);
                                } catch (Exception e) {
                                    /* Snatch defeat from the jaws of victory */
                                    try {
                                        onError.accept(e);
                                    } catch (Exception e2) {
                                        /* eat it */
                                    }
                                }
                            }
                        });
                    }
                }, new Consumer<Throwable>() {
                    /**
                     * This callback is called when an exception is thrown when fetching a
                     * MarketPriceInfo. The callback runs on a non-UI thread, but invokes the given
                     * callback on a UI thread
                     *
                     * @param throwable The Throwable that caused the process to fail
                     * @throws Exception If something goes terribly wrong
                     */
                    @Override
                    public void accept(final Throwable throwable) throws Exception {
                        /* Run the erros on the UI thread */
                        mActivity.runOnUiThread(new Runnable() {
                            /**
                             * This runs the given error callback on the UI thread
                             */
                            @Override
                            public void run() {
                                mNumPriceRequests--;
                                if (0 == mNumPriceRequests) {
                                    mActivity.clearLoading();
                                }

                                try {
                                    onError.accept(throwable);
                                } catch (Exception e) {
                                    /* Eat it */
                                }
                            }
                        });
                    }
                });
            }
        }).start();
    }
}
