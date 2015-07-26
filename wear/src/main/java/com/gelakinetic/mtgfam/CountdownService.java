package com.gelakinetic.mtgfam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;

public class CountdownService extends Service {

    /* An ID to track the notification */
    private static final int NOTIFICATION_ID = 516;

    /* Booleans which determine vibrator warnings */
    private static boolean mFiveMinuteWarning;
    private static boolean mFifteenMinuteWarning;
    private static boolean mTenMinuteWarning;

    /* System services & the notification builder */
    private static Vibrator mVibrator;
    private static NotificationManager mNotificationManager;
    private static Notification.Builder mNotificationBuilder;

    /* For use with stopSelfResult() */
    private int mStartId;

    /* The count down timer which updates the notification */
    private CountDownTimer mCountDownTimer;

    /* The receiver which gets messages from OngoingNotificationListenerService */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        /**
         * When this service receives a broadcast from OngoingNotificationListenerService,
         * process it
         *
         * @param context   The context in which the receiver is running
         * @param intent    The intent being received
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            processIntent(intent);
        }
    };

    /**
     * This replaced the old onStart(). It registers the broadcast receiver, sets up system
     * services, builds the initial notification, and processes the incoming intent
     *
     * @param intent  The intent the service was created with
     * @param flags   Additional data about this start request. Currently either 0,
     *                START_FLAG_REDELIVERY, or START_FLAG_RETRY.
     * @param startId A unique integer representing this specific request to start.
     *                Use with stopSelfResult(int).
     * @return Service.START_REDELIVER_INTENT, if the service is killed, it will be
     * restarted with the initial intent
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /* Register the broadcast receiver */
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(OngoingNotificationListenerService.BROADCAST_ACTION));

        /* Set up system services */
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        /* Build the initial notification */
        /* TODO add a button to cancel the timer? */
        mNotificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setOngoing(true)
                .extend(new Notification.WearableExtender()
                        .setBackground(BitmapFactory
                                .decodeResource(this.getResources(), R.drawable.background))
                        .setHintHideIcon(true)
                        .setContentIcon(R.mipmap.ic_launcher));

        /* Process the initial intent. This will create the notification */
        processIntent(intent);

        /* Save the start id */
        mStartId = startId;

        /* Return, making sure to restart with the same intent if the service is killed */
        return Service.START_REDELIVER_INTENT;
    }

    /**
     * Return the communication channel to the service. May return null if clients can not bind to
     * the service. The returned IBinder is usually for a complex interface that has been described
     * using aidl.
     *
     * @param intent - The Intent that was used to bind to this service, as given to
     *               Context.bindService. Note that any extras that were included with the Intent
     *               at that point will not be seen here.
     * @return Return an IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Intents come in when the service is started & through the broadcast receiver.
     * This processes them just the same, setting up warning booleans and managing the
     * countdown timer.
     *
     * @param intent The intent to process
     */
    private void processIntent(Intent intent) {
        /* If the intent is null, don't bother */
        if (intent == null) {
            return;
        }

        Bundle extras = intent.getExtras();

        /* If keys exist, process them */
        if (extras.containsKey(FamiliarConstants.KEY_FIVE_MINUTE_WARNING)) {
            mFiveMinuteWarning = extras.getBoolean(FamiliarConstants.KEY_FIVE_MINUTE_WARNING);
        }
        if (extras.containsKey(FamiliarConstants.KEY_TEN_MINUTE_WARNING)) {
            mTenMinuteWarning = extras.getBoolean(FamiliarConstants.KEY_TEN_MINUTE_WARNING);
        }
        if (extras.containsKey(FamiliarConstants.KEY_FIFTEEN_MINUTE_WARNING)) {
            mFifteenMinuteWarning = extras.getBoolean(FamiliarConstants.KEY_FIFTEEN_MINUTE_WARNING);
        }
        if (extras.containsKey(FamiliarConstants.KEY_END_TIME)) {

            /* Stop any current countdowns and remove the notification */
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
            mNotificationManager.cancel(NOTIFICATION_ID);

            long endTime = extras.getLong(FamiliarConstants.KEY_END_TIME);
            if (endTime == 0) {
                /* an end time of 0 means kill the notification */
                unregisterReceiver(mBroadcastReceiver);
                CountdownService.this.stopSelfResult(mStartId);
            } else {
                /* Display the notification */
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());

                /* Figure out how far we are away from the end time.
                 * Assume mobile and wear clocks are in sync */
                long millisInFuture = endTime - System.currentTimeMillis();
                mCountDownTimer = new CountDownTimer(millisInFuture, 1000) {
                    /**
                     * This should tick once every second. It updates the notification
                     *
                     * @param millisUntilFinished The time until the countdown finishes
                     */
                    @Override
                    public void onTick(long millisUntilFinished) {

                        /* Build the string */
                        long secondsLeft = (millisUntilFinished / 1000) % 60;
                        long minutesLeft = (millisUntilFinished / (1000 * 60)) % 60;
                        long hoursLeft = (millisUntilFinished / (1000 * 60 * 60));
                        String messageText = String.format("%02d:%02d:%02d",
                                hoursLeft, minutesLeft, secondsLeft);

                        /* Because the ID remains unchanged,
                         * the existing notification is updated. */
                        mNotificationBuilder.setContentTitle(messageText);
                        mNotificationManager.notify(
                                NOTIFICATION_ID,
                                mNotificationBuilder.build());

                        /* Vibrate a pattern if the warnings are set up */
                        if (mFifteenMinuteWarning &&
                                hoursLeft == 0 &&
                                minutesLeft == 15 &&
                                secondsLeft == 0) {
                            /* Buzz */
                            mVibrator.vibrate(200);
                            mFifteenMinuteWarning = false;
                        } else if (mTenMinuteWarning &&
                                hoursLeft == 0 &&
                                minutesLeft == 10 &&
                                secondsLeft == 0) {
                            /* Buzz Buzz */
                            mVibrator.vibrate(new long[]{0, 200, 200, 200}, -1);
                            mTenMinuteWarning = false;
                        } else if (mFiveMinuteWarning &&
                                hoursLeft == 0 &&
                                minutesLeft == 5 &&
                                secondsLeft == 0) {
                            /* Buzz Buzz Buzz*/
                            mVibrator.vibrate(new long[]{0, 200, 200, 200, 200, 200}, -1);
                            mFiveMinuteWarning = false;
                        }
                    }

                    /**
                     * The countdown finished. Give one good vibration, remove the notification,
                     * clean up the service, and call it a day.
                     */
                    @Override
                    public void onFinish() {
                        /* BUZZ */
                        mVibrator.vibrate(600);
                        /* Cleanup */
                        mNotificationManager.cancel(NOTIFICATION_ID);
                        unregisterReceiver(mBroadcastReceiver);
                        CountdownService.this.stopSelfResult(mStartId);
                    }
                };

                /* Start the countdown */
                mCountDownTimer.start();
            }
        }
    }
}
