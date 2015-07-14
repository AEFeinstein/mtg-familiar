package com.gelakinetic.mtgfam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

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

    static final int NOTIFICATION_ID = 516;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        Log.v("MTG", "OngoingNotificationListenerService onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        Log.v("MTG", "OngoingNotificationListenerService onDestroy");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        Log.v("MTG", "onDataChanged");

        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("MTG", "Service failed to connect to GoogleApiClient.");
                return;
            }
        }

        for (DataEvent event : events) {
            Log.v("MTG", event.toString());
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (FamiliarConstants.PATH.equals(path)) {
                    // Get the data out of the event
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    long endTime = dataMapItem.getDataMap().getLong(FamiliarConstants.KEY_END_TIME);

                    // Build the intent to display our custom notification
                    Intent notificationIntent = new Intent(this, FamiliarDisplayActivity.class);
                    notificationIntent.putExtra(FamiliarDisplayActivity.EXTRA_END_TIME, endTime);
                    PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

//                    // Create the ongoing notification
//                    Notification.Builder notificationBuilder =
//                            new Notification.Builder(this)
////                                    .setSmallIcon(R.mipmap.ic_launcher)
////                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                                    .setOngoing(true)
//                                    .setContentText("Text")
//                                    .setContentTitle("Title")
//                                    .setContentInfo("Info")
//                                    .extend(new Notification.WearableExtender()
//                                            .setDisplayIntent(notificationPendingIntent)
//                                            .setBackground(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                                            .setContentIcon(R.mipmap.ic_launcher))
//                                    .setOnlyAlertOnce(true);
//
//                    // Build the notification and show it
//                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//                    notificationManager.cancel(NOTIFICATION_ID);
//                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                    // Create the ongoing notification
                    Notification.Builder notificationBuilder =
                            new Notification.Builder(this)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setLargeIcon(BitmapFactory.decodeResource(
                                            getResources(), R.mipmap.ic_launcher))
                                    .setOngoing(true)
                                    .extend(new Notification.WearableExtender()
                                            .setDisplayIntent(notificationPendingIntent));

                    // Build the notification and show it
                    NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                    Log.v("MTG", "Notification Notified");
                }
            }
        }
    }
}
