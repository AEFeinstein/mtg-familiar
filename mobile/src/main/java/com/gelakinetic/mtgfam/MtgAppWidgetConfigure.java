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

package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * This simple shows a dialog which configures the widget when it is placed.
 */
public class MtgAppWidgetConfigure extends Activity {

    private String[] mLaunchers;
    private boolean[] mLaunchersSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get all the widget buttons */
        mLaunchers = getResources().getStringArray(R.array.default_fragment_array_entries);
        mLaunchersSelected = new boolean[mLaunchers.length];

        /* Figure out which ones are already selected */
        Set<String> defaults = PreferenceAdapter.getWidgetButtons(this);
        if (null == defaults) {
            return;
        }

        for (int i = 0; i < mLaunchers.length; i++) {
            mLaunchersSelected[i] = defaults.contains(mLaunchers[i]);
        }

        /* Build the dialog */
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setMultiChoiceItems(mLaunchers, mLaunchersSelected, (dialog, which, isChecked) -> mLaunchersSelected[which] = isChecked)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> finishAndUpdateWidget())
                .setTitle(R.string.pref_widget_mode_title)
                .setOnDismissListener(dialogInterface -> finishAndUpdateWidget())
                .create()
                .show();
    }

    /**
     * This finishes the Activity and updates the widget. Appropriately named.
     */
    private void finishAndUpdateWidget() {
        /* Set the preferences from the dialog */
        HashSet<String> selectedButtons = new HashSet<>();
        for (int i = 0; i < mLaunchers.length; i++) {
            if (mLaunchersSelected[i]) {
                selectedButtons.add(mLaunchers[i]);
            }
        }
        PreferenceAdapter.setWidgetButtons(this, selectedButtons);

        /* Get the widget id */
        int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        /* Tell the widget to update */
        Intent intent = new Intent(this, MTGFamiliarAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        assert AppWidgetManager.getInstance(getApplication()) != null;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
        assert appWidgetManager != null;
        int[] ids = appWidgetManager.getAppWidgetIds(
                new ComponentName(getApplication(), MTGFamiliarAppWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        /* Return */
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
