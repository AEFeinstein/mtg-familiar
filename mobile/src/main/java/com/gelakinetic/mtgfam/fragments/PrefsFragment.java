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