package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;

/**
 * This simple Activity pops a toast and dies when the user adds a widget to the home screen
 */
public class MtgAppWidgetConfigure extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Pop the toast notifying the user of configuring the widget in settings */
		Toast.makeText(this, getString(R.string.widget_configure_reminder), Toast.LENGTH_LONG).show();

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
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
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
