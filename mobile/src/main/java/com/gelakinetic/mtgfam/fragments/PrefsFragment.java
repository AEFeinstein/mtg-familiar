package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.github.machinarius.preferencefragment.PreferenceFragment;

import java.util.Locale;

public class PrefsFragment extends PreferenceFragment {

    public static void checkOverrideSystemLanguage(Context context) {

        // Check if the system's language setting needs to be overridden
        String defaultLocale = context.getResources().getConfiguration().locale.getLanguage();
        boolean overrideSystemLanguage = !defaultLocale.equals(PreferenceAdapter.getLanguage(context));

        if (overrideSystemLanguage) {
            String localeString = PreferenceAdapter.getLanguage(context);

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