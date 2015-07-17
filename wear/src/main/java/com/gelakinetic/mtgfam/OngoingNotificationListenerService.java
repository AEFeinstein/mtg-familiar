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

    public static final int NOTIFICATION_ID = 516;
    public static final String UPDATE_TIMER_INFO = "com.gelakinetic.mtgfam.timer";
    private GoogleApiClient mGoogleApiClient;

    /**
     * TODO
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        Log.v("MTG", "OngoingNotificationListenerService onCreate");
    }

    /**
     * TODO
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        Log.v("MTG", "OngoingNotificationListenerService onDestroy");
    }

    /**
     * TODO
     * @param dataEvents asd
     */
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
                    /* Get the data out of the event */
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    /* Either set or clear the timer, or just update warning settings */
                    if (dataMapItem.getDataMap().containsKey(FamiliarConstants.KEY_END_TIME)) {
                        long endTime = dataMapItem.getDataMap().getLong(FamiliarConstants.KEY_END_TIME);

                        if (endTime == 0) {
                            /* An end time of 0 means cancel the notification */

                            Log.v("MTG", "Broadcasting kill signal");
                            Intent intent = new Intent(UPDATE_TIMER_INFO);
                            intent.putExtra(FamiliarDisplayActivity.EXTRA_END_TIME, 0);
                            sendBroadcast(intent);

                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.cancel(NOTIFICATION_ID);

                        } else {
                            /* Build the intent to display our custom notification */
                            Intent notificationIntent = new Intent(this, FamiliarDisplayActivity.class);
                            notificationIntent.putExtra(FamiliarDisplayActivity.EXTRA_END_TIME, endTime);
                            notificationIntent.putExtra(FamiliarDisplayActivity.EXTRA_FIVE_MINUTE_WARNING,
                                    dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_FIVE_MINUTE_WARNING));
                            notificationIntent.putExtra(FamiliarDisplayActivity.EXTRA_TEN_MINUTE_WARNING,
                                    dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_TEN_MINUTE_WARNING));
                            notificationIntent.putExtra(FamiliarDisplayActivity.EXTRA_FIFTEEN_MINUTE_WARNING,
                                    dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_FIFTEEN_MINUTE_WARNING));

                            PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                                    this,
                                    0,
                                    notificationIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                            /* Create the ongoing notification */
                            Notification.Builder notificationBuilder = new Notification.Builder(this)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setOngoing(true)
                                    .extend(new Notification.WearableExtender()
                                            .setDisplayIntent(notificationPendingIntent)
                                            .setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.background))
                                            .setHintHideIcon(true)
                                            .setContentIcon(R.mipmap.ic_launcher));

                            /* Cancel the notification */
                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.cancel(NOTIFICATION_ID);
                            /* Kill any running activities */
                            Intent intent = new Intent(UPDATE_TIMER_INFO);
                            intent.putExtra(FamiliarDisplayActivity.EXTRA_END_TIME, 0);
                            sendBroadcast(intent);
                            /* Build the notification and show it */
                            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                            Log.v("MTG", "Notification Notified");
                        }
                    } else if (dataMapItem.getDataMap().containsKey(FamiliarConstants.KEY_FIVE_MINUTE_WARNING)) {
                        Log.v("MTG", "just broadcasting settings");
                        /* Broadcast the 5/10/15 minute warning preferences */
                        Intent intent = new Intent(UPDATE_TIMER_INFO);
                        intent.putExtra(FamiliarDisplayActivity.EXTRA_FIVE_MINUTE_WARNING,
                                dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_FIVE_MINUTE_WARNING));
                        intent.putExtra(FamiliarDisplayActivity.EXTRA_TEN_MINUTE_WARNING,
                                dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_TEN_MINUTE_WARNING));
                        intent.putExtra(FamiliarDisplayActivity.EXTRA_FIFTEEN_MINUTE_WARNING,
                                dataMapItem.getDataMap().getBoolean(FamiliarConstants.KEY_FIFTEEN_MINUTE_WARNING));
                        sendBroadcast(intent);
                    }
                }
            }
        }
    }
}
