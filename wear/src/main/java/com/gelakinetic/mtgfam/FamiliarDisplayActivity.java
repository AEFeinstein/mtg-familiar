package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

import java.util.Calendar;

public class FamiliarDisplayActivity extends Activity {

    public static final String EXTRA_END_TIME = "EXTRA_END_TIME";
    private TextView mTextView;
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private long mEndTime = 0;
    private Handler mHandler;

    private Runnable mUpdateTimeRunnable = new Runnable() {
        @Override
        public void run() {

            Log.v("MTG", "FamiliarDisplayActivity posted");

            if (System.currentTimeMillis() < mEndTime) {
                Calendar then = Calendar.getInstance();
                then.clear();
                then.add(Calendar.MILLISECOND, (int) (mEndTime - System.currentTimeMillis()));
                String messageText = String.format("%02d:%02d:%02d", then.get(Calendar.HOUR), then.get(Calendar.MINUTE), then.get(Calendar.SECOND));

                // Sets an ID for the notification, so it can be updated
                mNotificationBuilder
                        .setContentTitle(messageText)
                        .setOngoing(true);

                // Because the ID remains unchanged, the existing notification is updated.
                mNotificationManager.notify(
                        OngoingNotificationListenerService.NOTIFICATION_ID,
                        mNotificationBuilder.build());

                mTextView.setText(messageText);

                /* Do it again in one second */
                mHandler.postDelayed(mUpdateTimeRunnable, 1000);
            } else {
                mNotificationManager.cancel(OngoingNotificationListenerService.NOTIFICATION_ID);
                ((Vibrator)(getSystemService(Context.VIBRATOR_SERVICE))).vibrate(500);
                // TODO vibrate for 5/10/15? update mobile manifest with permission
                mEndTime = 0;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        mTextView = (TextView) findViewById(R.id.text);

        Log.v("MTG", "FamiliarDisplayActivity onCreate");
        Intent intent = getIntent();
        if (intent != null) {
            mEndTime = intent.getLongExtra(EXTRA_END_TIME, 0);

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationBuilder = new Notification.Builder(this)
//                    .setOnlyAlertOnce(true)
//                    .setOngoing(true)
//                    .extend(new Notification.WearableExtender()
//                            .setBackground(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                            .setContentIcon(R.mipmap.ic_launcher));

            mNotificationBuilder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    getResources(), R.mipmap.ic_launcher))
                            .setOngoing(true);

            mNotificationManager.notify(
                    OngoingNotificationListenerService.NOTIFICATION_ID,
                    mNotificationBuilder.build());

            mHandler = new android.os.Handler();
            mHandler.post(mUpdateTimeRunnable);
        }
    }
}