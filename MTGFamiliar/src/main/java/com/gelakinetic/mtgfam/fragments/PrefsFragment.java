package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

public class PrefsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Load the preferences from an XML resource */
		addPreferencesFromResource();
	}
}