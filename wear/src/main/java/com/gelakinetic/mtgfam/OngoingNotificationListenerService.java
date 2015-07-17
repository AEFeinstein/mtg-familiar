package com.gelakinetic.mtgfam;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OngoingNotificationListenerService extends WearableListenerService {

    private static final int NOTIFICATION_ID = 516;

    /* Services and stuff */
    private static GoogleApiClient mGoogleApiClient;
    private static Handler mHandler;
    private static NotificationManager mNotificationManager;
    private static Notification.Builder mNotificationBuilder;
    private static Vibrator mVibrator;

    /* Variables to be set by the mobile app */
    private static long mEndTime = 0;
    private static boolean mFiveMinuteWarning = false;
    private static boolean mTenMinuteWarning = false;
    private static boolean mFifteenMinuteWarning = false;

    private static final Runnable mUpdateTimeRunnable = new Runnable() {
        /**
         * This runnable is posted once a second, updates the notification, and vibrates the watch
         * if the warning is set and it is time.
         */
        @Override
        public void run() {

            Long currTime = System.currentTimeMillis();

            if (currTime >= mEndTime) {
                /* Time is up. BUZZZZZ */
                mVibrator.vibrate(600);

                /* Remove the notification and any tick updates */
                mHandler.removeCallbacks(mUpdateTimeRunnable);
                mNotificationManager.cancel(NOTIFICATION_ID);
                mEndTime = 0;
            } else {
                /* Figure out when the next tick is */
                long timeToNextSystemTick = (((currTime + 1000) / 1000) * 1000) - currTime;

                /* Do this again in one second */
                long postTime = SystemClock.uptimeMillis() + timeToNextSystemTick;
                mHandler.postAtTime(mUpdateTimeRunnable, postTime);

                /* Build the string */
                Calendar then = Calendar.getInstance();
                then.clear();
                then.add(Calendar.MILLISECOND, (int) (mEndTime - System.currentTimeMillis()));
                String messageText = String.format("%02d:%02d:%02d", then.get(Calendar.HOUR),
                        then.get(Calendar.MINUTE), then.get(Calendar.SECOND));

                /* Because the ID remains unchanged, the existing notification is updated. */
                mNotificationBuilder.setContentTitle(messageText);
                mNotificationManager.notify(
                        OngoingNotificationListenerService.NOTIFICATION_ID,
                        mNotificationBuilder.build());

                /* Vibrate a pattern if the warnings are set up */
                if (mFifteenMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 15 &&
                        then.get(Calendar.SECOND) == 0) {
                    /* Buzz */
                    mVibrator.vibrate(200);
                }
                if (mTenMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 10 &&
                        then.get(Calendar.SECOND) == 0) {
                    /* Buzz Buzz */
                    mVibrator.vibrate(new long[]{0, 200, 200, 200}, -1);
                }
                if (mFiveMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 5 &&
                        then.get(Calendar.SECOND) == 0) {
                    /* Buzz Buzz Buzz*/
                    mVibrator.vibrate(new long[]{0, 200, 200, 200, 200, 200}, -1);
                }
            }
        }
    };

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

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(this);

        mHandler = new Handler();
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

                    /* If this is a message with time data, process it */
                    if (dataMapItem.getDataMap().containsKey(FamiliarConstants.KEY_END_TIME)) {

                        /* An end time of 0 means cancel the notification */
                        if (dataMapItem.getDataMap().getLong(FamiliarConstants.KEY_END_TIME) == 0) {
                            /* Cancel the updater & notification */
                            mHandler.removeCallbacks(mUpdateTimeRunnable);
                            mNotificationManager.cancel(NOTIFICATION_ID);

                        } else {
                            /* Get when the timer should expire */
                            mEndTime = dataMapItem.getDataMap()
                                    .getLong(FamiliarConstants.KEY_END_TIME);

                            /* Create the ongoing notification */
                            mNotificationBuilder = mNotificationBuilder
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setOngoing(true)
                                    .extend(new Notification.WearableExtender()
                                            .setBackground(BitmapFactory
                                             .decodeResource(getResources(), R.drawable.background))
                                            .setHintHideIcon(true)
                                            .setContentIcon(R.mipmap.ic_launcher));

                            /* TODO add a button to cancel the timer */

                            /* Remove any tick callbacks */
                            mHandler.removeCallbacks(mUpdateTimeRunnable);
                            /* Cancel the notification */
                            mNotificationManager.cancel(NOTIFICATION_ID);
                            /* The runnable will show the notification */
                            mHandler.post(mUpdateTimeRunnable);
                        }
                    }

                    /* If this message has warnings settings data, save that */
                    if (dataMapItem.getDataMap()
                            .containsKey(FamiliarConstants.KEY_FIVE_MINUTE_WARNING)) {
                        mFiveMinuteWarning = dataMapItem.getDataMap()
                                .getBoolean(FamiliarConstants.KEY_FIVE_MINUTE_WARNING);
                        mTenMinuteWarning = dataMapItem.getDataMap()
                                .getBoolean(FamiliarConstants.KEY_TEN_MINUTE_WARNING);
                        mFifteenMinuteWarning = dataMapItem.getDataMap()
                                .getBoolean(FamiliarConstants.KEY_FIFTEEN_MINUTE_WARNING);
                    }
                }
            }
        }
    }
}
