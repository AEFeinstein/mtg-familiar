package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.gelakinetic.mtgfam.R;
import com.github.machinarius.preferencefragment.PreferenceFragment;

import java.util.Locale;

public class PrefsFragment extends PreferenceFragment {

    public static void checkOverrideSystemLanguage(Context context) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        // Check if the system's language setting needs to be overridden
        String defaultLocale = context.getResources().getConfiguration().locale.getLanguage();
        boolean overrideSystemLanguage =
                !defaultLocale.equals(prefs.getString(context.getString(R.string.key_language), defaultLocale));

        if (overrideSystemLanguage) {
            String localeString = prefs.getString(context.getString(R.string.key_language), "");

            // Change language setting in configuration
            Locale locale = new Locale(localeString);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/* Load the preferences from an XML resource */
        addPreferencesFromResource(R.xml.preferences);
    }
}