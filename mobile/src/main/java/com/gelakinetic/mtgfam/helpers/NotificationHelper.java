package com.gelakinetic.mtgfam.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.gelakinetic.mtgfam.R;

/**
 * Created by Adam on 9/12/2017.
 */

public class NotificationHelper {
    public static final String NOTIFICATION_CHANNEL_UPDATE = "channel_update";
    public static final String NOTIFICATION_CHANNEL_ROUND_TIMER = "channel_round_timer";

    public static void createChannels(Context context) {

        // For Oreo and above, create a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use NotificationManager, not NotificationManagerCompat
            NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));

            // Create the channel for displaying the update notification, nearly default
            manager.createNotificationChannel(
                    new NotificationChannel(
                            NotificationHelper.NOTIFICATION_CHANNEL_UPDATE,
                            context.getString(R.string.pref_cat_updates),
                            NotificationManager.IMPORTANCE_LOW));

            // Create the channel for displaying round timer updates, high priority, no sound
            // Sound is handled in RoundTimerBroadcastReceiver's onReceive(), case TIMER_RING_ALARM
            NotificationChannel roundTimerChannel = new NotificationChannel(
                    NotificationHelper.NOTIFICATION_CHANNEL_ROUND_TIMER,
                    context.getString(R.string.pref_cat_timer),
                    NotificationManager.IMPORTANCE_HIGH);
            roundTimerChannel.setSound(null, null);
            manager.createNotificationChannel(roundTimerChannel);
        }
    }
}
