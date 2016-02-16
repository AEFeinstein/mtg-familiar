package com.gelakinetic.mtgfam.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

/**
 * Created by Adam on 1/24/2016.
 */
public class TutorCards {

    private final FamiliarActivity mActivity;

    public TutorCards(FamiliarActivity familiarActivity) {
        mActivity = familiarActivity;
    }

    /**
     * TODO
     */
    private class ServiceStatus {
        boolean isReady;
    }

    /**
     * TODO
     */
    private class SearchPostResult {
        boolean isResult;
        long wait;
        String id;
    }

    /**
     * TODO
     */
    private class SearchResult {
        boolean isResult;
        SearchResultInfo info;
    }

    /**
     * TODO
     */
    private class SearchResultInfo {
        long multiverseid;
        long other[];
    }

    private static final int REQUEST_IMAGE_CAPTURE = 65;

    /**
     * TODO document
     */
    public void startTutorCardsSearch() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * TODO document
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            /* Show some background activity is happening */
            mActivity.setLoading();

            /* Get the bitmap from the camera */
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            /* Start the search with the bitmap in a task */
            TutorCardsTask tutorCardsTask = new TutorCardsTask();
            tutorCardsTask.execute(imageBitmap);
        }
    }

    /**
     * TODO document
     * TODO fix SSL stuff
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    void getServiceStatus() throws IOException, NoSuchAlgorithmException {
        /* Get an httpclient and create the GET */
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        HttpGet httpGet = new HttpGet("https://tutor.cards/api/status");

        /* Execute the GET and get the response. */
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity getEntity = response.getEntity();

        /* If there is a response, process it */
        if (getEntity != null) {
            InputStreamReader inStream = new InputStreamReader(getEntity.getContent());
            BufferedReader br = new BufferedReader(inStream);
            try {
                String stringResponse = br.readLine();
                Log.v("getServiceStatus", stringResponse);
                ServiceStatus status = (new Gson()).fromJson(stringResponse, ServiceStatus.class);
                Log.v("TutorCards", "isReady: " + status.isReady);
            } finally {
                inStream.close();
            }
        }
    }

    /**
     * TODO document
     * TODO fix SSL stuff
     *
     * @param bitmap
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    SearchPostResult postSearchRequest(Bitmap bitmap) throws NoSuchAlgorithmException,
            IOException {
        SearchPostResult result = null;
        /* Get an httpclient and create the POST */
        // Do not do this in production!!!
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        HttpPost httppost = new HttpPost("https://tutor.cards/api/search");

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
                Log.v("postSearchRequest", stringResponse);
                result = (new Gson()).fromJson(stringResponse, SearchPostResult.class);
            } finally {
                inStream.close();
            }
        }
        return result;
    }

    /**
     * TODO Document
     * TODO fix SSL stuff
     *
     * @param id
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    SearchResult getResult(String id) throws IOException, NoSuchAlgorithmException {
        SearchResult result = null;

        /* Get an httpclient and create the GET */
        // Do not do this in production!!!
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                // TODO Auto-generated method stub
                return true;
            }
        });
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        HttpGet httpGet = new HttpGet("https://tutor.cards/api/result/" + id);
        Log.v("getResult", "https://tutor.cards/api/result/" + id);

        /* Execute the GET and get the response. */
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity getEntity = response.getEntity();

        /* If there is a response, process it */
        if (getEntity != null) {
            InputStreamReader inStream = new InputStreamReader(getEntity.getContent());
            BufferedReader br = new BufferedReader(inStream);
            try {
                String stringResponse = br.readLine();
                Log.v("getResult", stringResponse);
                result = (new Gson()).fromJson(stringResponse, SearchResult.class);
            } finally {
                inStream.close();
            }
        }
        return result;
    }

    private class TutorCardsTask extends AsyncTask<Bitmap, Void, Void> {

        /**
         * TODO
         *
         * @param params
         * @return
         */
        @Override
        protected Void doInBackground(Bitmap... params) {
            try {
                /* Post the search request with the image */
                SearchPostResult searchPostResult = postSearchRequest(params[0]);

                /* If this isn't the result already */
                if (!searchPostResult.isResult) {

                    /* Spin around in the background as long as the API requests */
                    long stopTime = System.currentTimeMillis() + searchPostResult.wait;
                    while (System.currentTimeMillis() < stopTime) {
                        ; /* Spin around */
                    }

                    /* Get the actual result from the API */
                    SearchResult searchResult = getResult(searchPostResult.id);

                    /* debug print */
                    if(searchResult.isResult) {
                        Log.v("TutorCards", searchResult.info.multiverseid + "");
                        for (long other : searchResult.info.other) {
                            Log.v("TutorCards other", other + "");
                        }
                        mActivity.receiveTutorCardsResult(searchResult.info.multiverseid);
                    }
                }
            } catch (NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
