package com.gelakinetic.mtgfam.helpers;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import android.support.v7.app.AppCompatActivity;
import android.net.Uri;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;

public class AppIndexingWrapper{
    
    public GoogleApiClient mGoogleApiClient;

    /**
     * Creates and returns the action describing this page view
     *
     * @param fragment   The fragment for this page view. 
     * @return An action describing the fragment's page view
     */
    private static Action getAppIndexAction(CardViewFragment fragment, String packageName){
        Thing object = new Thing.Builder()
                .setType("http://schema.org/Thing")         /* Optional, any valid schema.org type */
                .setName(fragment.mCardName + " (" + fragment.mSetName + ")") /* Required, title field */
                .setDescription(fragment.mDescription)               /* Required, description field */
                /* Required, deep link in the android-app:// format */
                .setUrl(Uri.parse("android-app://" + packageName + "/card/multiverseid/" + fragment.mMultiverseId))
                .build();

        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .build();
    }

    public static void startAppIndexing(AppIndexingWrapper appIndexingWrapper, CardViewFragment fragment, String packageName){
        AppIndex.AppIndexApi.start(appIndexingWrapper.mGoogleApiClient, getAppIndexAction(fragment, packageName));
    }

    public static void endAppIndexing(AppIndexingWrapper appIndexingWrapper, CardViewFragment fragment, String packageName){
        AppIndex.AppIndexApi.end(appIndexingWrapper.mGoogleApiClient, getAppIndexAction(fragment, packageName));
    }

    public AppIndexingWrapper(AppCompatActivity activity) {
	mGoogleApiClient = new GoogleApiClient.Builder(activity)
		.addApi(AppIndex.API)
                .build();
    }

    public void connect(){
	mGoogleApiClient.connect();
    }

    public void disconnect(){
	mGoogleApiClient.disconnect();
    }

    public void disconnectIfConnected(){
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void connectIfDisconnected(){
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }
}
