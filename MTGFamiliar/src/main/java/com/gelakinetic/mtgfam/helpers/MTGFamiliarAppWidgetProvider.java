package com.gelakinetic.mtgfam.helpers;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

import java.util.Set;

/**
 * This class sets up the buttons for the homescreen widget
 */
public class MTGFamiliarAppWidgetProvider extends AppWidgetProvider {

	/* An array of resource IDs for the buttons in the widget. Must stay in order with intents[] below, and
	   R.array.default_fragment_array_entries in arrays.xml */
	private static final int[] buttonResources = {
			R.id.widget_search,
			R.id.widget_life,
			R.id.widget_mana,
			R.id.widget_dice,
			R.id.widget_trade,
			R.id.widget_wish,
			R.id.widget_timer,
			R.id.widget_rules,
			R.id.widget_judge,
			R.id.widget_mojhosto};

	/* An array of String intents for the buttons in the widget. Must stay in order with buttonResources[] above, and
	   R.array.default_fragment_array_entries in arrays.xml */
	private static final String intents[] = {
			FamiliarActivity.ACTION_CARD_SEARCH,
			FamiliarActivity.ACTION_LIFE,
			FamiliarActivity.ACTION_MANA,
			FamiliarActivity.ACTION_DICE,
			FamiliarActivity.ACTION_TRADE,
			FamiliarActivity.ACTION_WISH,
			FamiliarActivity.ACTION_ROUND_TIMER,
			FamiliarActivity.ACTION_RULES,
			FamiliarActivity.ACTION_JUDGE,
			FamiliarActivity.ACTION_MOJHOSTO};

	/**
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetIds
	 */
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/* Perform this loop procedure for each App Widget that belongs to this provider */
		for (int appWidgetId : appWidgetIds) {

			/* Get the layout for the App Widget and attach an on-click listener to the buttons */
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mtgfamiliar_appwidget);

			/* Attach all the intents to all the buttons */
			for (int i = 0; i < buttonResources.length; i++) {
				Intent intentQuick = new Intent(context, FamiliarActivity.class);
				intentQuick.setAction(intents[i]);
				PendingIntent pendingIntentQuick = PendingIntent.getActivity(context, 0, intentQuick, 0);
				views.setOnClickPendingIntent(buttonResources[i], pendingIntentQuick);
			}

			showButtonsFromPreferences(context, views);

			/* Tell the AppWidgetManager to perform an update on the current app widget */
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	private void showButtonsFromPreferences(Context context, RemoteViews views) {
		String[] entries = context.getResources().getStringArray(R.array.default_fragment_array_entries);
		Set<String> buttons = (new PreferenceAdapter(context)).getWidgetButtons();

		/* Set all the buttons as gone */
		for (int resource : buttonResources) {
			views.setViewVisibility(resource, View.GONE);
		}

		/* Show the buttons selected in preferences */
		for (String button : buttons) {
			for (int i = 0; i < entries.length; i++) {
				if (button.equals(entries[i])) {
					views.setViewVisibility(buttonResources[i], View.VISIBLE);
					break;
				}
			}
		}
	}

	/**
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetId
	 * @param newOptions
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mtgfamiliar_appwidget);

		float densityDpi = context.getResources().getDisplayMetrics().densityDpi;
		float dp = newOptions.getInt("appWidgetMaxWidth") / (densityDpi / 160f);
		Log.v("widget", dp + "");

		/* In newOptions:
			appWidgetMaxHeight: 84
			appWidgetCategory: 1
			appWidgetMaxWidth: 302
			appWidgetMinHeight: 58
			appWidgetMinWidth: 224

			Anecdotally,
			6 cells: /388
			5 cells: /322
			4 cells: 204dp / 256
			3 cells: 151dp / 190
			2 cells: 98dp / 124
			1 cell : 45dp / 58
		 */

		showButtonsFromPreferences(context, views);

		/* Tell the AppWidgetManager to perform an update on the current app widget */
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}