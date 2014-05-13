package com.gelakinetic.mtgfam.helpers;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.R;

/**
 * Created by afeinstein on 5/13/2014.
 */
public class MTGFamiliarAppWidgetProviderLight extends MTGFamiliarAppWidgetProvider {

    /**
     * Called in response to the ACTION_APPWIDGET_UPDATE broadcast when this AppWidget provider is being asked to
     * provide RemoteViews for a set of AppWidgets. Override this method to implement your own AppWidget functionality.
     *
     * @param context          The Context in which this receiver is running.
     * @param appWidgetManager A AppWidgetManager object you can call updateAppWidget(ComponentName, RemoteViews) on.
     * @param appWidgetIds     The appWidgetIds for which an update is needed. Note that this may be all of the
     *                         AppWidget instances for this provider, or just a subset of them.
     */
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/* Perform this loop procedure for each App Widget that belongs to this provider */
        for (int appWidgetId : appWidgetIds) {

			/* Get the layout for the App Widget and attach an on-click listener to the buttons */
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mtgfamiliar_appwidget_light);

            bindButtons(context, views);

			/* 100 is a good number to start with when placing a 4x1 widget, since dimensions aren't visible here */
            showButtonsFromPreferences(context, views, mMaxNumButtons);

			/* Tell the AppWidgetManager to perform an update on the current app widget */
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
