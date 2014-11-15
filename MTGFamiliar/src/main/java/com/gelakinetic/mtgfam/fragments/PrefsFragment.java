package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;

import java.util.Locale;

public class PrefsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Load the preferences from an XML resource */
		addPreferencesFromResource();
	}

	public static void checkOverrideSystemLanguage(Context context) {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		// testing
		Toast.makeText(context, "Language: " + prefs.getString("language", "default"), Toast.LENGTH_LONG);

		// Check if the system's language setting needs to be overridden
		String defaultLocale = context.getResources().getConfiguration().locale.getLanguage();
		boolean overrideSystemLanguage =
				defaultLocale == prefs.getString("language", defaultLocale);

		if (overrideSystemLanguage) {
			String localeString = prefs.getString("language", "");

			// Change language setting in configuration
			Locale locale = new Locale(localeString);
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			context.getResources().updateConfiguration(config,
					context.getResources().getDisplayMetrics());
		}
	}
}