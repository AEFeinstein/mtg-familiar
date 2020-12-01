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
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.appcompat.content.res.AppCompatResources;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

import java.util.Set;

/**
 * This class sets up the buttons for the home screen widget
 */
public abstract class MTGFamiliarAppWidgetProvider extends AppWidgetProvider {

    private static class WidgetEntry {
        final int buttonResource;
        final int vectorResourceLight;
        final int vectorResourceDark;
        final String intentAction;

        WidgetEntry(int btnRes, int imgResLight, int imgResDark, String intent) {
            this.buttonResource = btnRes;
            this.vectorResourceLight = imgResLight;
            this.vectorResourceDark = imgResDark;
            this.intentAction = intent;
        }
    }

    private static final WidgetEntry[] widgetEntries = {
            new WidgetEntry(R.id.widget_search, R.drawable.ic_drawer_search_light, R.drawable.ic_drawer_search_dark, FamiliarActivity.ACTION_CARD_SEARCH),
            new WidgetEntry(R.id.widget_life, R.drawable.ic_drawer_life_light, R.drawable.ic_drawer_life_dark, FamiliarActivity.ACTION_LIFE),
            new WidgetEntry(R.id.widget_mana, R.drawable.ic_drawer_mana_light, R.drawable.ic_drawer_mana_dark, FamiliarActivity.ACTION_MANA),
            new WidgetEntry(R.id.widget_dice, R.drawable.ic_drawer_dice_light, R.drawable.ic_drawer_dice_dark, FamiliarActivity.ACTION_DICE),
            new WidgetEntry(R.id.widget_trade, R.drawable.ic_drawer_trade_light, R.drawable.ic_drawer_trade_dark, FamiliarActivity.ACTION_TRADE),
            new WidgetEntry(R.id.widget_wish, R.drawable.ic_drawer_wishlist_light, R.drawable.ic_drawer_wishlist_dark, FamiliarActivity.ACTION_WISH),
            new WidgetEntry(R.id.widget_deck, R.drawable.ic_drawer_deck_light, R.drawable.ic_drawer_deck_dark, FamiliarActivity.ACTION_DECKLIST),
            new WidgetEntry(R.id.widget_timer, R.drawable.ic_drawer_timer_light, R.drawable.ic_drawer_timer_dark, FamiliarActivity.ACTION_ROUND_TIMER),
            new WidgetEntry(R.id.widget_rules, R.drawable.ic_drawer_rules_light, R.drawable.ic_drawer_rules_dark, FamiliarActivity.ACTION_RULES),
            new WidgetEntry(R.id.widget_mojhosto, R.drawable.ic_drawer_mojhosto_light, R.drawable.ic_drawer_mojhosto_dark, FamiliarActivity.ACTION_MOJHOSTO),
            new WidgetEntry(R.id.widget_judge, R.drawable.ic_drawer_judge_light, R.drawable.ic_drawer_judge_dark, FamiliarActivity.ACTION_JUDGE)
    };

    int mLayout;

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

            int maxNumButtons = PreferenceAdapter.getNumWidgetButtons(context, appWidgetId);

            /* 100 is a good number to start with when placing a 4x1 widget, since dimensions aren't visible here */
            showButtonsFromPreferences(context, views, maxNumButtons);

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
        for (WidgetEntry entry : widgetEntries) {

            int vectorResource;
            if (mLayout == R.layout.mtgfamiliar_appwidget_dark) {
                vectorResource = entry.vectorResourceDark;
            } else {
                vectorResource = entry.vectorResourceLight;
            }

            /* Draw the vector image */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                views.setImageViewResource(entry.buttonResource, vectorResource);
            } else {
                Drawable d = AppCompatResources.getDrawable(context, vectorResource);
                if (d != null) {
                    Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(),
                            d.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(b);
                    d.setBounds(0, 0, c.getWidth(), c.getHeight());
                    d.draw(c);
                    views.setImageViewBitmap(entry.buttonResource, b);
                }
            }

            /* Set the listener */
            Intent intentQuick = new Intent(context, FamiliarActivity.class);
            intentQuick.setAction(entry.intentAction);
            PendingIntent pendingIntentQuick = PendingIntent.getActivity(context, 0, intentQuick, 0);
            views.setOnClickPendingIntent(entry.buttonResource, pendingIntentQuick);
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
        Set<String> buttons = PreferenceAdapter.getWidgetButtons(context);
        if (null == buttons) {
            return;
        }

        int buttonsVisible = 0;
        if (maxNumButtons == 0) {
            maxNumButtons = 1;
        }

        /* Set all the buttons as gone */
        for (WidgetEntry entry : widgetEntries) {
            views.setViewVisibility(entry.buttonResource, View.GONE);
        }

        /* Show the buttons selected in preferences */
        for (int i = 0; i < entries.length; i++) {
            if (buttons.contains(entries[i])) {
                views.setViewVisibility(widgetEntries[i].buttonResource, View.VISIBLE);
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

        int maxNumButtons = (int) ((float) newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) / 48);

        /* Show the right number of buttons, and rebind the intents.
           Buttons wont work if scaled and rotated otherwise */
        showButtonsFromPreferences(context, views, maxNumButtons);
        bindButtons(context, views);

        /* Tell the AppWidgetManager to perform an update on the current app widget */
        appWidgetManager.updateAppWidget(appWidgetId, views);

        /* Save the number of buttons visible for this widget id */
        PreferenceAdapter.setNumWidgetButtons(context, appWidgetId, maxNumButtons);
    }
}