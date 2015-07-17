package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

import java.util.Calendar;

public class FamiliarDisplayActivity extends Activity {

    public static final String EXTRA_END_TIME = "EXTRA_END_TIME";
    public static final String EXTRA_FIVE_MINUTE_WARNING = "FIVE_MIN_WARNING";
    public static final String EXTRA_TEN_MINUTE_WARNING = "TEN_MIN_WARNING";
    public static final String EXTRA_FIFTEEN_MINUTE_WARNING = "FIFTEEN_MIN_WARNING";

    private TextView mTextView;
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private Handler mHandler;
    private RoundTimerInfoReceiver mRoundTimerInfoReceiver;

    /* Variables modifiable by the broadcast receiver */
    public long mEndTime = 0;
    public boolean mFiveMinuteWarning = false;
    public boolean mTenMinuteWarning = false;
    public boolean mFifteenMinuteWarning = false;
    public boolean mStopHandler = false;

    private Runnable mUpdateTimeRunnable = new Runnable() {
        /**
         * TODO
         */
        @Override
        public void run() {

            Long currTime = System.currentTimeMillis();

            if(mStopHandler || currTime >= mEndTime) {
                Log.v("MTG", "shut it down");
                /* Cancel the notification */
                mNotificationManager.cancel(OngoingNotificationListenerService.NOTIFICATION_ID);
                /* Finish the activity */
                FamiliarDisplayActivity.this.finish();
                /* Don't post another runnable */
                if(!mStopHandler) {
                    /* timer ran out, vibrate a bit */
                    ((Vibrator) (getSystemService(Context.VIBRATOR_SERVICE))).vibrate(600);
                }
                mEndTime = 0;
                mHandler.removeCallbacks(mUpdateTimeRunnable);
            }
            else if (currTime < mEndTime) {
                Log.v("MTG", "FamiliarDisplayActivity posted");

                long timeToNextSystemTick = (((currTime + 1000) / 1000) * 1000) - currTime;
                /* Do it again in one second */
                long postTime = SystemClock.uptimeMillis() + timeToNextSystemTick;
                mHandler.postAtTime(mUpdateTimeRunnable, postTime);

                Calendar then = Calendar.getInstance();
                then.clear();
                then.add(Calendar.MILLISECOND, (int) (mEndTime - System.currentTimeMillis()));
                String messageText = String.format("%02d:%02d:%02d", then.get(Calendar.HOUR), then.get(Calendar.MINUTE), then.get(Calendar.SECOND));

                /* Sets an ID for the notification, so it can be updated */
                mNotificationBuilder
                        .setContentTitle(messageText)
                        .setOngoing(true);

                /* Because the ID remains unchanged, the existing notification is updated. */
                mNotificationManager.notify(
                        OngoingNotificationListenerService.NOTIFICATION_ID,
                        mNotificationBuilder.build());

                mTextView.setText(messageText);

                /* Vibrate a pattern if the warnings are set up */
                if (mFifteenMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 15 &&
                        then.get(Calendar.SECOND) == 0) {
                    ((Vibrator) (getSystemService(Context.VIBRATOR_SERVICE))).vibrate(200);
                }
                if (mTenMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 10 &&
                        then.get(Calendar.SECOND) == 0) {
                    ((Vibrator) (getSystemService(Context.VIBRATOR_SERVICE))).vibrate(new long[]{0, 200, 200, 200}, -1);
                }
                if (mFiveMinuteWarning &&
                        then.get(Calendar.HOUR) == 0 &&
                        then.get(Calendar.MINUTE) == 5 &&
                        then.get(Calendar.SECOND) == 0) {
                    ((Vibrator) (getSystemService(Context.VIBRATOR_SERVICE))).vibrate(new long[]{0, 200, 200, 200, 200, 200}, -1);
                }
            }
        }
    };

    /**
     * TODO
     *
     * @param savedInstanceState asd
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        Log.v("MTG", "FamiliarDisplayActivity onCreate");

        mTextView = (TextView) findViewById(R.id.text);

        //Register BroadcastReceiver to receive event from our service
        mRoundTimerInfoReceiver = new RoundTimerInfoReceiver(this);
        IntentFilter intentFilter = new IntentFilter(OngoingNotificationListenerService.UPDATE_TIMER_INFO);
        registerReceiver(mRoundTimerInfoReceiver, intentFilter);

        Intent intent = getIntent();
        if (intent != null) {
            mEndTime = intent.getLongExtra(EXTRA_END_TIME, 0);

            mFiveMinuteWarning = intent.getBooleanExtra(EXTRA_FIVE_MINUTE_WARNING, false);
            mTenMinuteWarning = intent.getBooleanExtra(EXTRA_TEN_MINUTE_WARNING, false);
            mFifteenMinuteWarning = intent.getBooleanExtra(EXTRA_FIFTEEN_MINUTE_WARNING, false);
            Log.v("MTG", "warnings " + mFiveMinuteWarning + " " + mTenMinuteWarning + " " + mFifteenMinuteWarning);

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (mEndTime > 0) {
                mNotificationBuilder = new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .extend(new Notification.WearableExtender()
                                .setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.background))
                                .setHintHideIcon(true)
                                .setContentIcon(R.mipmap.ic_launcher));

                /* TODO add action to cancel round timer, send data to mobile */

                mNotificationManager.notify(
                        OngoingNotificationListenerService.NOTIFICATION_ID,
                        mNotificationBuilder.build());

                mHandler = new android.os.Handler();
                mHandler.post(mUpdateTimeRunnable);
            } else if(mEndTime == 0){
                /* Stop the handler, which will finish the activity & remove the notification */
                mStopHandler = true;
            }
        }
    }

    /**
     * TODO
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mRoundTimerInfoReceiver);
    }
}