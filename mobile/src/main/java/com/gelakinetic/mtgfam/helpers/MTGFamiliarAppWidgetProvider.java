package com.gelakinetic.mtgfam.helpers;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

import java.util.Set;

/**
 * This class sets up the buttons for the home screen widget
 */
public abstract class MTGFamiliarAppWidgetProvider extends AppWidgetProvider {

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
            R.id.widget_mojhosto,
            R.id.widget_judge,
            R.id.widget_profile};

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
            FamiliarActivity.ACTION_MOJHOSTO,
            FamiliarActivity.ACTION_JUDGE,
            FamiliarActivity.ACTION_PROFILE};
    int mLayout;
    private int mMaxNumButtons = 100;

    protected abstract void setLayout();

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
        setLayout();
        /* Perform this loop procedure for each App Widget that belongs to this provider */
        for (int appWidgetId : appWidgetIds) {

			/* Get the layout for the App Widget and attach an on-click listener to the buttons */
            RemoteViews views = new RemoteViews(context.getPackageName(), mLayout);

            bindButtons(context, views);

			/* 100 is a good number to start with when placing a 4x1 widget, since dimensions aren't visible here */
            showButtonsFromPreferences(context, views, mMaxNumButtons);

			/* Tell the AppWidgetManager to perform an update on the current app widget */
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Bind the buttons in the widget to their proper intents
     *
     * @param context The Context used to create the intents
     * @param views   The RemoteViews which holds the widget
     */
    private void bindButtons(Context context, RemoteViews views) {
        /* Attach all the intents to all the buttons */
        for (int i = 0; i < buttonResources.length; i++) {
            Intent intentQuick = new Intent(context, FamiliarActivity.class);
            intentQuick.setAction(intents[i]);
            PendingIntent pendingIntentQuick = PendingIntent.getActivity(context, 0, intentQuick, 0);
            views.setOnClickPendingIntent(buttonResources[i], pendingIntentQuick);
        }
    }

    /**
     * This shows the buttons selected by the user, and hides the other ones. It shows a limited number of buttons
     *
     * @param context       A context used to get the preferences
     * @param views         The RemoteViews which holds the widget
     * @param maxNumButtons The maximum number of buttons to display, so things don't get too crammed
     */
    private void showButtonsFromPreferences(Context context, RemoteViews views, int maxNumButtons) {
        String[] entries = context.getResources().getStringArray(R.array.default_fragment_array_entries);
        Set<String> buttons = (new PreferenceAdapter(context)).getWidgetButtons();

        int buttonsVisible = 0;
        if (maxNumButtons == 0) {
            maxNumButtons = 1;
        }

		/* Set all the buttons as gone */
        for (int resource : buttonResources) {
            views.setViewVisibility(resource, View.GONE);
        }

		/* Show the buttons selected in preferences */
        for (int i = 0; i < entries.length; i++) {
            if (buttons.contains(entries[i])) {
                views.setViewVisibility(buttonResources[i], View.VISIBLE);
                buttonsVisible++;
                if (buttonsVisible == maxNumButtons) {
                    return;
                }
            }
        }
    }

    /**
     * Called in response to the ACTION_APPWIDGET_OPTIONS_CHANGED broadcast when this widget has been laid out at a new
     * size. Only valid for jelly bean and beyond. Sorry ice cream sandwich :(
     *
     * @param context          The Context in which this receiver is running.
     * @param appWidgetManager A AppWidgetManager object you can call updateAppWidget(ComponentName, RemoteViews) on.
     * @param appWidgetId      The appWidgetId of the widget who's size changed.
     * @param newOptions       The new parameters for the widget
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        setLayout();
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        RemoteViews views = new RemoteViews(context.getPackageName(), mLayout);

        float densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        float dp = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) / (densityDpi / 160f);
        mMaxNumButtons = (int) (dp / 32);

		/* Show the right number of buttons, and rebind the intents.
           Buttons wont work if scaled and rotated otherwise */
        showButtonsFromPreferences(context, views, mMaxNumButtons);
        bindButtons(context, views);

		/* Tell the AppWidgetManager to perform an update on the current app widget */
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}