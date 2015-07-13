package com.gelakinetic.mtgfam.helpers;

import android.app.Application;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.exception.CacheLoadingException;
import com.octo.android.robospice.persistence.exception.CacheSavingException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is the service which will fetch price information asynchronously
 */
public class PriceFetchService extends SpiceService {

    /**
     * Creates a CacheManager with a custom Persister which handles PriceInfo objects
     *
     * @param application The application which will run the service
     * @return A CacheManager with a custom InFileObjectPersister to handle PriceInfo objects
     * @throws CacheCreationException thrown by new InFileObjectPersister()
     */
    @Override
    public CacheManager createCacheManager(Application application) throws CacheCreationException {
        CacheManager cacheManager = new CacheManager();

        InFileObjectPersister<PriceInfo> priceInfoPersister = new InFileObjectPersister<PriceInfo>(application,
                PriceInfo.class) {

            /**
             * This will check to see if a file for this key exists in the cache, and return that information if it does
             *
             * @param cacheKey    The key to lookup in the cache
             * @param maxTimeInCacheBeforeExpiry    How long data lives in the cache before being requested again
             * @return A PriceInfo object, or null if the object was nto in the cache
             * @throws CacheLoadingException Thrown if there is an IOException when reading from the cache
             */
            @Override
            public PriceInfo loadDataFromCache(Object cacheKey, long maxTimeInCacheBeforeExpiry)
                    throws CacheLoadingException {
                File file = getCacheFile(cacheKey);
                if (file.exists()) {
                    long timeInCache = System.currentTimeMillis() - file.lastModified();
                    if (maxTimeInCacheBeforeExpiry == 0 || timeInCache <= maxTimeInCacheBeforeExpiry) {
                        try {
                            return new PriceInfo(fileToBytes(file));
                        } catch (FileNotFoundException e) {
                            return null;
                        } catch (IOException e) {
                            throw new CacheLoadingException(e);
                        }
                    }
                }
                return null;
            }

            /**
             * Save some fresh data into the cache, and return it too
             * @param data    The PriceInfo data to cache
             * @param cacheKey    The key to cache it with
             * @return The same data that was passed in
             * @throws CacheSavingException
             */
            @Override
            public PriceInfo saveDataToCacheAndReturnData(final PriceInfo data, final Object cacheKey)
                    throws CacheSavingException {
                try {
                    if (isAsyncSaveEnabled()) {

                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    BufferedOutputStream bos = new BufferedOutputStream(
                                            new FileOutputStream(getCacheFile(cacheKey)));
                                    bos.write(data.toBytes());
                                    bos.flush();
                                    bos.close();
                                } catch (IOException e) {
                                    /* Eat it */
                                }
                            }
                        }.start();
                    } else {
                        BufferedOutputStream bos = new BufferedOutputStream(
                                new FileOutputStream(getCacheFile(cacheKey)));
                        bos.write(data.toBytes());
                        bos.flush();
                        bos.close();
                    }
                } catch (IOException e) {
                    throw new CacheSavingException(e);
                }
                return data;
            }

            /**
             * Read a PriceInfo object out of a File
             * @param file The file in the cache
             * @return The PriceInfo
             * @throws CacheLoadingException Thrown if there is an IOException
             */
            @Override
            protected PriceInfo readCacheDataFromFile(File file) throws CacheLoadingException {
                if (file.exists()) {
                    try {
                        return new PriceInfo(fileToBytes(file));
                    } catch (FileNotFoundException e) {
                        return null;
                    } catch (IOException e) {
                        throw new CacheLoadingException(e);
                    }
                }
                return null;
            }
        };

        priceInfoPersister.setAsyncSaveEnabled(true);
        cacheManager.addPersister(priceInfoPersister);
        return cacheManager;
    }

    /**
     * Convenience function to read a file's contents into a byte[], and return it
     *
     * @param file The file to read
     * @return The files bytes in byte[] form
     * @throws IOException Thrown if there is an exception reading the file
     */
    private byte[] fileToBytes(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = new FileInputStream(file);
        if (ios.read(buffer) == -1) {
            throw new IOException("EOF reached while trying to read the whole file");
        }
        ios.close();
        return buffer;
    }
}