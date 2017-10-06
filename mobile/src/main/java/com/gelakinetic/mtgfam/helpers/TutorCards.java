package com.gelakinetic.mtgfam.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Base64;

import com.gelakinetic.mtgfam.BuildConfig;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

/**
 * This class uses the API for https://tutor.cards to search for a card by image
 */
public class TutorCards {

    /* A random request code when requesting an image from the camera using an intent */
    private static final int REQUEST_IMAGE_CAPTURE = 65;
    /* The temporary image filename */
    private static final String TMP_IMG_FILENAME = "tmp.jpg";
    /* The FamiliarActivity which is hosting this object */
    private final FamiliarActivity mActivity;
    /* Whether or not Tutor.cards is ready to process */
    private boolean mIsReady;

    /**
     * Default constructor
     *
     * @param familiarActivity The Familiar Activity which is hosting this object
     */
    public TutorCards(@NotNull FamiliarActivity familiarActivity) {
        mActivity = familiarActivity;
    }

    /**
     * This starts the tutor.cards process. It starts an AsyncTask which will make sure the service
     * is up, then if it is, launch the camera.
     * It also displays a notice that the search is powered by TutorCards for the first three
     * searches.
     */
    public void startTutorCardsSearch() {

        /* Show the loading animation */
        mActivity.setLoading();

        /* For the first three searches, tell the user it's powered by TutorCards */
        int numSearches = PreferenceAdapter.getNumTutorCardsSearches(mActivity);
        if (numSearches < 3) {
            numSearches++;
            PreferenceAdapter.setNumTutorCardsSearches(mActivity, numSearches);
            ToastWrapper.makeText(mActivity, R.string.tutor_cards_notice, ToastWrapper.LENGTH_LONG).show();
        }

        /* Start the process */
        (new TutorCardsStartTask()).execute();
    }

    /**
     * When the requested picture is taken, the result is passed to the hosting FamiliarActivity,
     * which calls this function. Here the image is taken and passed to an AsyncTask to perform
     * a query.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its
     *                    setResult().
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            /* Show some background activity is happening */
            mActivity.setLoading();

            /* Start the search with the bitmap in a task */
            TutorCardsTask tutorCardsTask = new TutorCardsTask();
            tutorCardsTask.execute(BitmapFactory.decodeFile(getImageFile().getAbsolutePath()));
        }
    }

    /**
     * Checks if tutor.cards is up or down by sending an HTTP GET to https://api.tutor.cards/v1/status
     * This must not be called from the UI thread.
     *
     * @return true if the service is up, false if it is down
     * @throws IOException
     */
    private boolean getServiceStatus() throws IOException {

        boolean retVal = false;

        /* Get an httpclient and create the GET */
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://api.tutor.cards/v1/status");

        /* Execute the GET and get the response. */
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity getEntity = response.getEntity();

        /* If there is a response, process it */
        if (getEntity != null) {
            InputStreamReader inStream = new InputStreamReader(getEntity.getContent());
            BufferedReader br = new BufferedReader(inStream);
            try {
                String stringResponse = br.readLine();
                TutorData status = (new Gson()).fromJson(stringResponse, TutorData.class);
                retVal = status.isReady;
            } catch (Exception e) {
                /* false will be returned later */
            } finally {
                inStream.close();
            }
        }
        return retVal;
    }

    /**
     * Send a bitmap image to tutor.cards for analysis. The service does not respond with a result,
     * but responds with an ID to use to fetch the result later.
     * This must not be called from the UI thread.
     *
     * @param bitmap The bitmap to send to tutor.cards to analyze
     * @return A TutorData with the id of the query. The actual result will be fetched later
     * @throws IOException
     */
    private TutorData postSearchRequest(Bitmap bitmap) throws
            IOException {
        TutorData result = null;
        /* Get an httpclient and create the POST */
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost("https://api.tutor.cards/v1/search");

        /* Resize the bitmap so the shorter length is 320px */
        int oldHeight = bitmap.getHeight();
        int oldWidth = bitmap.getWidth();
        int newHeight, newWidth;
        if (oldHeight < oldWidth) {
            float ratio = oldWidth / (float) oldHeight;
            newHeight = 320;
            newWidth = (int) (320 * ratio);
        } else {
            float ratio = oldHeight / (float) oldWidth;
            newWidth = 320;
            newHeight = (int) (320 * ratio);
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        /* Turn the scaled bitmap into a JPG, 75% quality */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);

        /* Convert the JPG into Base64 */
        String imageString = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(),
                Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);

        /* Cleanup, don't need these anymore */
        scaledBitmap.recycle();
        bitmap.recycle();

        /* Add the image data as a parameter to the POST */
        List<NameValuePair> postParams = new ArrayList<>(1);
        postParams.add(new BasicNameValuePair("img", imageString));
        httppost.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

        /* Execute the POST and get a response */
        final HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStreamReader inStream = new InputStreamReader(entity.getContent());
            BufferedReader br = new BufferedReader(inStream);
            try {
                String stringResponse = br.readLine();
                result = (new Gson()).fromJson(stringResponse, TutorData.class);
            } catch (Exception e) {
                result = null;
            } finally {
                inStream.close();
            }
        }
        return result;
    }

    /**
     * After we send an image to tutor.cards, this function queries the service with the ID
     * returned after sending the image and queries for a result.
     * This must not be called from the UI thread.
     *
     * @param id The ID to send to tutor.cards to fetch a result
     * @return A TutorData containing the multiverseID from the analyzed image, or null
     * @throws IOException
     */
    private TutorData getResult(String id) throws IOException {
        TutorData result = null;

        /* Get an httpclient and create the GET */
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("https://api.tutor.cards/v1/result/" + id);

        /* Execute the GET and get the response. */
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity getEntity = response.getEntity();

        /* If there is a response, process it */
        if (getEntity != null) {
            InputStreamReader inStream = new InputStreamReader(getEntity.getContent());
            BufferedReader br = new BufferedReader(inStream);
            try {
                String stringResponse = br.readLine();
                result = (new Gson()).fromJson(stringResponse, TutorData.class);
            } catch (Exception e) {
                result = null;
            } finally {
                inStream.close();
            }
        }
        return result;
    }

    /**
     * @return The temporary image file used for searching
     */
    private File getImageFile() {
        return new File(mActivity.getExternalFilesDir(null), TMP_IMG_FILENAME);
    }

    /*
     * Private classes which match the JSON returned by the tutor.cards service
     */
    private class TutorData {
        final boolean isReady = false;
        final boolean isResult = false;
        final long wait = 0;
        final String id = null;
        final TutorDataInfo info = null;
    }

    private class TutorDataInfo {
        final long multiverseid = 0;
        final long similar[] = null;
        final long[] all = null;
    }

    private class TutorCardsStartTask extends AsyncTask<Void, Void, Void> {

        /**
         * This function checks the status of the tutor.cards service on a non-UI thread and saves
         * it in a private boolean
         *
         * @param params Unused
         * @return Unused
         */
        @Override
        protected Void doInBackground(Void... params) {
            /* Make sure the service is up first */
            if (!mIsReady) {
                try {
                    mIsReady = getServiceStatus();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        /**
         * If the tutor.cards service is up, this starts a camera intent to take a picture on
         * the UI thread
         *
         * @param aVoid Unused
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mIsReady) {
                /* If the service is up, start the camera */
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                /* Tell the camera to use the temporary image file */
                Uri uri = FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".FileProvider", getImageFile());
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
                    mActivity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } else {
                /* Service isn't ready, clear the loading animation and pop a toast */
                mActivity.clearLoading();
                ToastWrapper.makeText(mActivity, R.string.tutor_cards_fail, ToastWrapper.LENGTH_SHORT).show();
            }
        }
    }

    private class TutorCardsTask extends AsyncTask<Bitmap, Void, Void> {

        private boolean mError = false;

        /**
         * This function sends a bitmap to tutor.cards for analysis using postSearchRequest().
         * It then uses the query ID, after waiting, to fetch the result using getResult()
         *
         * @param params The Bitmap to be sent to TutorCards
         * @return nothing
         */
        @Override
        protected Void doInBackground(Bitmap... params) {
            try {
                /* Post the search request with the image */
                TutorData searchPostResult = postSearchRequest(params[0]);

                /* Now that the image has been posted, delete the local copy */
                //noinspection ResultOfMethodCallIgnored
                getImageFile().delete();

                if (searchPostResult == null) {
                    /* Can't do much with a null result */
                    mError = true;
                    return null;
                }

                int tries = 0;

                /* If this isn't the result already */
                while (!searchPostResult.isResult && tries < 5) {

                    /* Sleep for the requested time */
                    try {
                        Thread.sleep(searchPostResult.wait);
                    } catch (InterruptedException ie) {
                        /* Eat it */
                    }

                    /* Get the actual result from the API */
                    searchPostResult = getResult(searchPostResult.id);

                    if (searchPostResult == null) {
                        /* Can't do much with a null result */
                        mError = true;
                        return null;
                    }

                    /* Send the result to the activity */
                    if (searchPostResult.isResult) {
                        /* Negative multiverse ids are for promos. Find the first positive id */
                        if (searchPostResult.info.multiverseid < 0) {
                            for (Long otherId : searchPostResult.info.all) {
                                if (otherId > 0) {
                                    mActivity.receiveTutorCardsResult(otherId);
                                    return null;
                                }
                            }
                        } else {
                            mActivity.receiveTutorCardsResult(searchPostResult.info.multiverseid);
                            /* Return, we're done here */
                            return null;
                        }
                    }

                    /* if isResult is false, then this while loop continues */
                    /* Increment the number of tries */
                    tries++;
                }

                if (searchPostResult.isResult) {
                    /* while loop was just skipped over, since the result is already here */
                    /* Negative multiverse ids are for promos. Find the first positive id */
                    if (searchPostResult.info.multiverseid < 0) {
                        for (Long otherId : searchPostResult.info.all) {
                            if (otherId > 0) {
                                mActivity.receiveTutorCardsResult(otherId);
                                return null;
                            }
                        }
                    } else {
                        mActivity.receiveTutorCardsResult(searchPostResult.info.multiverseid);
                        /* Return, we're done here */
                        return null;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            /* Shouldn't reach here, flag the error */
            mError = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mError) {
                mActivity.clearLoading();
                ToastWrapper.makeText(mActivity, R.string.tutor_cards_fail, ToastWrapper.LENGTH_SHORT).show();
            }
        }
    }
}
