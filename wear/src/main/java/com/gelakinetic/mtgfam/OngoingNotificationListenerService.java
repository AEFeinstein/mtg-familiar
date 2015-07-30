package com.gelakinetic.mtgfam;

import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OngoingNotificationListenerService extends WearableListenerService {

    /* An action to send broadcasts with */
    static final String BROADCAST_ACTION = "com.gelakinetic.mtgfam";

    /* The google api service */
    private static GoogleApiClient mGoogleApiClient;

    /**
     * Called by the system when the service is first created. Do not call this method directly
     * Sets up vibration and notification services, the google api client, and the handler
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * The service should clean up any resources it holds (threads, registered receivers, etc) at
     * this point. Upon return, there will be no more calls in to this Service object and it is
     * effectively dead. Do not call this method directly.
     * <p/>
     * Also disconnects the google api client.
     * Also it doesn't clean up the handler. That runs until the timer expires
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    /**
     * Called when there is new data from the mobile app. Data can be a timer start or stop,
     * or the 5 / 10 / 15 minute warnings being changed
     *
     * @param dataEvents The new data
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        /* Get the events from the DataEventBuffer */
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        /* Make sure the google api client is connected */
        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                return;
            }
        }

        /* for each DataEvent */
        for (DataEvent event : events) {
            /* if the data changed */
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (FamiliarConstants.PATH.equals(path)) {
                    /* Get the data out of the event */
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    boolean justUpdating = true;
                    Intent extraData = new Intent();
                    for (String key : dataMapItem.getDataMap().keySet()) {
                        /* If the end time is nonzero, that means we're starting.
                         * Otherwise, we're just updating the activity.
                         */
                        if (key.equals(FamiliarConstants.KEY_END_TIME)) {
                            if (dataMapItem.getDataMap().getLong(FamiliarConstants.KEY_END_TIME)
                                    != FamiliarConstants.CANCEL_FROM_MOBILE) {
                                justUpdating = false;
                            }
                        }
                        /* Transfer the data from the dataMap to an intent */
                        extraData.putExtras(dataMapItem.getDataMap().toBundle());
                    }

                    if (justUpdating) {
                        /* If we're updating, send a broadcast to the existing service */
                        Intent broadcast = new Intent(BROADCAST_ACTION);
                        broadcast.putExtras(extraData);
                        sendBroadcast(broadcast);
                    } else {
                        /* If we're starting the timer, start the service */
                        Intent serviceIntent = new Intent(this, CountdownService.class);
                        serviceIntent.putExtras(extraData);
                        this.startService(serviceIntent);
                    }
                }
            }
        }
    }
}
