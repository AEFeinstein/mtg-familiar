package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * This simple shows a dialog which configures the widget when it is placed.
 */
public class MtgAppWidgetConfigure extends Activity {

    private PreferenceAdapter mPrefAdapter;
    private String[] mLaunchers;
    private boolean[] mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefAdapter = new PreferenceAdapter(this);

        /* Get all the widget buttons */
        mLaunchers = getResources().getStringArray(R.array.default_fragment_array_entries);

        /* Figure out which ones are already selected */
        Set<String> defaults = mPrefAdapter.getWidgetButtons();
        mSelected = new boolean[mLaunchers.length];
        for (int i = 0; i < mLaunchers.length; i++) {
            mSelected[i] = defaults.contains(mLaunchers[i]);
        }

        /* Build the dialog */
        AlertDialogPro.Builder adb = new AlertDialogPro.Builder(this);
        adb
                .setMultiChoiceItems(mLaunchers, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        mSelected[i] = b;
                    }
                })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finishAndUpdateWidget();
                    }
                })
                .setTitle(R.string.pref_widget_mode_title);

        Dialog d = adb.create();
        /* Set the onDismissListener to finish the activity and refresh the widget */
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finishAndUpdateWidget();
            }
        });
        d.show();
    }

    /**
     * This finishes the Activity and updates the widget. Appropriately named.
     */
    private void finishAndUpdateWidget() {
        /* Set the preferences from the dialog */
        HashSet<String> selectedButtons = new HashSet<>();
        for (int i = 0; i < mSelected.length; i++) {
            if (mSelected[i]) {
                selectedButtons.add(mLaunchers[i]);
            }
        }
        mPrefAdapter.setWidgetButtons(selectedButtons);

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
        int ids[] = appWidgetManager.getAppWidgetIds(
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
