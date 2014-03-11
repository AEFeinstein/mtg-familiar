package com.gelakinetic.mtgfam.helpers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

public class MTGFamiliarAppWidgetProvider extends AppWidgetProvider {

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/* Perform this loop procedure for each App Widget that belongs to this provider */
		for (int appWidgetId : appWidgetIds) {

			/* Get the layout for the App Widget and attach an on-click listener to the buttons */
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mtgfamiliar_appwidget);

			int buttons[] = {
					R.id.widget_search,
					R.id.widget_life,
					R.id.widget_dice,
					R.id.widget_timer,
					R.id.widget_trade};

			String intents[] = {
					FamiliarActivity.ACTION_CARD_SEARCH,
					FamiliarActivity.ACTION_LIFE,
					FamiliarActivity.ACTION_DICE,
					FamiliarActivity.ACTION_ROUND_TIMER,
					FamiliarActivity.ACTION_TRADE};

			for(int i=0; i < buttons.length; i++) {
				Intent intentQuick = new Intent(context, FamiliarActivity.class);
				intentQuick.setAction(intents[i]);
				PendingIntent pendingIntentQuick = PendingIntent.getActivity(context, 0, intentQuick, 0);
				views.setOnClickPendingIntent(buttons[i], pendingIntentQuick);
			}

			/* Tell the AppWidgetManager to perform an update on the current app widget */
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}