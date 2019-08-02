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
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This simple shows a dialog which configures the widget when it is placed.
 */
public class MtgAppWidgetConfigure extends Activity {

    private String[] mLaunchers;
    private Integer[] mSelectedIndices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get all the widget buttons */
        mLaunchers = getResources().getStringArray(R.array.default_fragment_array_entries);

        /* Figure out which ones are already selected */
        Set<String> defaults = PreferenceAdapter.getWidgetButtons(this);
        if (null == defaults) {
            return;
        }
        ArrayList<Integer> selectedIndicesTmp = new ArrayList<>();
        for (int i = 0; i < mLaunchers.length; i++) {
            if (defaults.contains(mLaunchers[i])) {
                selectedIndicesTmp.add(i);
            }
        }
        mSelectedIndices = new Integer[selectedIndicesTmp.size()];
        selectedIndicesTmp.toArray(mSelectedIndices);

        /* Build the dialog */
        MaterialDialog.Builder adb = new MaterialDialog.Builder(this);
        adb
                .items((CharSequence[]) mLaunchers)
                .alwaysCallMultiChoiceCallback()
                .itemsCallbackMultiChoice(mSelectedIndices, (dialog, which, text) -> {
                    mSelectedIndices = which;
                    return true;
                })
                .positiveText(R.string.dialog_ok)
                .onPositive((dialog, which) -> finishAndUpdateWidget())
                .title(R.string.pref_widget_mode_title);

        Dialog d = adb.build();
        /* Set the onDismissListener to finish the activity and refresh the widget */
        d.setOnDismissListener(dialogInterface -> finishAndUpdateWidget());
        d.show();
    }

    /**
     * This finishes the Activity and updates the widget. Appropriately named.
     */
    private void finishAndUpdateWidget() {
        /* Set the preferences from the dialog */
        HashSet<String> selectedButtons = new HashSet<>();
        for (Integer mSelectedIndex : mSelectedIndices) {
            selectedButtons.add(mLaunchers[mSelectedIndex]);
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
