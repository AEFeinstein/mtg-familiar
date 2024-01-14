/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.gelakinetic.mtgfam.R;

public class NotificationHelper {
    public static final String NOTIFICATION_CHANNEL_UPDATE = "channel_update";
    public static final String NOTIFICATION_CHANNEL_ROUND_TIMER = "channel_round_timer";

    public static void createChannels(Context context) {

        // For Oreo and above, create a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use NotificationManager, not NotificationManagerCompat
            NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));

            if (null == manager) {
                return;
            }
            // Create the channel for displaying the update notification, nearly default
            manager.createNotificationChannel(
                    new NotificationChannel(
                            NotificationHelper.NOTIFICATION_CHANNEL_UPDATE,
                            context.getString(R.string.pref_cat_updates),
                            NotificationManager.IMPORTANCE_DEFAULT));

            // Create the channel for displaying round timer updates, high priority, no sound
            // Sound is handled in RoundTimerBroadcastReceiver's onReceive(), case TIMER_RING_ALARM
            NotificationChannel roundTimerChannel = new NotificationChannel(
                    NotificationHelper.NOTIFICATION_CHANNEL_ROUND_TIMER,
                    context.getString(R.string.main_timer),
                    NotificationManager.IMPORTANCE_HIGH);
            roundTimerChannel.setSound(null, null);
            manager.createNotificationChannel(roundTimerChannel);
        }
    }
}
