package com.gelakinetic.mtgfam.helpers;

import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.gelakinetic.mtgfam.FamiliarConstants;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;
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

public class DataLayerListenerService extends WearableListenerService {


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.v("MTG", "change");
       /* Get the events from the DataEventBuffer */
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        /* Connect the API client */
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            return;
        }

        Log.v("MTG", "process");

        /* for each DataEvent */
        for (DataEvent event : events) {
            /* if the data changed */
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (FamiliarConstants.PATH.equals(path)) {
                    /* Get the data out of the event */
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    for (String key : dataMapItem.getDataMap().keySet()) {
                        /* Only looking for cancellation */
                        if (key.equals(FamiliarConstants.KEY_END_TIME)) {
                            if (dataMapItem.getDataMap().getLong(FamiliarConstants.KEY_END_TIME)
                                    == FamiliarConstants.CANCEL_FROM_WEAR) {
                                /* Cancel the alarm */
                                Log.v("MTG", "cancel");

                                /* Commit the endTime as -1, this will stop the toolbar display */
                                (new PreferenceAdapter(this)).setRoundTimerEnd(-1);
                                /* Cancel the alarms */
                                RoundTimerFragment.setOrCancelAlarms(this, 0, false);
                                /* Cancel the notification */
                                NotificationManagerCompat.from(this).cancel(
                                        RoundTimerFragment.TIMER_NOTIFICATION_ID);
                            }
                        }
                    }
                }
            }
        }
    }
}