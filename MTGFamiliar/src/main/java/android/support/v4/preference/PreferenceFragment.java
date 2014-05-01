/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Courtesy of https://github.com/kolavar/android-support-v4-preferenceFragment
 */

package android.support.v4.preference;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ListView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

public abstract class PreferenceFragment extends Fragment implements
		PreferenceManagerCompat.OnPreferenceTreeClickListener {

	private static final String PREFERENCES_TAG = "android:preferences";

	private PreferenceManager mPreferenceManager;
	private ListView mList;
	private boolean mHavePrefs;
	private boolean mInitDone;

	/**
	 * The starting request code given out to preference framework.
	 */
	static final int FIRST_REQUEST_CODE = 100;

	private static final int MSG_BIND_PREFERENCES = 1;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case MSG_BIND_PREFERENCES:
					bindPreferences();
					break;
			}
		}
	};

	final private Runnable mRequestFocus = new Runnable() {
		public void run() {
			mList.focusableViewAvailable(mList);
		}
	};

	/**
	 * Interface that PreferenceFragment's containing activity should
	 * implement to be able to process preference items that wish to
	 * switch to a new fragment.
	 */
	public interface OnPreferenceStartFragmentCallback {
		/**
		 * Called when the user has clicked on a Preference that has
		 * a fragment class name associated with it.  The implementation
		 * to should instantiate and switch to an instance of the given
		 * fragment.
		 */
		boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref);
	}

	@Override
	public void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		mPreferenceManager = PreferenceManagerCompat.newInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup,
							 Bundle paramBundle) {
		return paramLayoutInflater.inflate(R.layout.preference_list_frag, paramViewGroup,
				false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mHavePrefs) {
			bindPreferences();
		}

		mInitDone = true;

		if (savedInstanceState != null) {
			Bundle container = savedInstanceState.getBundle(PREFERENCES_TAG);
			if (container != null) {
				final PreferenceScreen preferenceScreen = getPreferenceScreen();
				if (preferenceScreen != null) {
					preferenceScreen.restoreHierarchyState(container);
				}
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		PreferenceManagerCompat.setOnPreferenceTreeClickListener(mPreferenceManager, this);
	}

	@Override
	public void onStop() {
		super.onStop();
		PreferenceManagerCompat.dispatchActivityStop(mPreferenceManager);
		PreferenceManagerCompat.setOnPreferenceTreeClickListener(mPreferenceManager, null);
	}

	@Override
	public void onDestroyView() {
		mList = null;
		mHandler.removeCallbacks(mRequestFocus);
		mHandler.removeMessages(MSG_BIND_PREFERENCES);
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceManagerCompat.dispatchActivityDestroy(mPreferenceManager);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		if (preferenceScreen != null) {
			Bundle container = new Bundle();
			preferenceScreen.saveHierarchyState(container);
			outState.putBundle(PREFERENCES_TAG, container);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		PreferenceManagerCompat.dispatchActivityResult(mPreferenceManager, requestCode, resultCode, data);
	}

	/**
	 * Sets the root of the preference hierarchy that this fragment is showing.
	 *
	 * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
	 */
	void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		if (PreferenceManagerCompat.setPreferences(mPreferenceManager, preferenceScreen) && preferenceScreen != null) {
			mHavePrefs = true;
			if (mInitDone) {
				postBindPreferences();
			}
		}
	}

	/**
	 * Gets the root of the preference hierarchy that this fragment is showing.
	 *
	 * @return The {@link PreferenceScreen} that is the root of the preference
	 * hierarchy.
	 */
	PreferenceScreen getPreferenceScreen() {
		return PreferenceManagerCompat.getPreferenceScreen(mPreferenceManager);
	}

	/**
	 * Inflates the given XML resource and adds the preference hierarchy to the current
	 * preference hierarchy.
	 */
	protected void addPreferencesFromResource() {
		requirePreferenceManager();

		setPreferenceScreen(PreferenceManagerCompat.inflateFromResource(mPreferenceManager, getActivity(),
				getPreferenceScreen()));
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean onPreferenceTreeClick(Preference preference) {
		//if (preference.getFragment() != null &&
		return getActivity() instanceof OnPreferenceStartFragmentCallback &&
				((OnPreferenceStartFragmentCallback) getActivity()).onPreferenceStartFragment(this, preference);
	}

	private void requirePreferenceManager() {
		if (mPreferenceManager == null) {
			throw new RuntimeException("This should be called after super.onCreate.");
		}
	}

	private void postBindPreferences() {
		if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) return;
		mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
	}

	private void bindPreferences() {
		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		if (preferenceScreen != null) {
			preferenceScreen.bind(getListView());
		}
	}

	ListView getListView() {
		ensureList();
		return mList;
	}

	private void ensureList() {
		if (mList != null) {
			return;
		}
		View root = getView();
		if (root == null) {
			throw new IllegalStateException("Content view not yet created");
		}
		View rawListView = root.findViewById(android.R.id.list);
		if (!(rawListView instanceof ListView)) {
			throw new RuntimeException(
					"Content has view with id attribute 'android.R.id.list' "
							+ "that is not a ListView class"
			);
		}
		mList = (ListView) rawListView;
		mList.setOnKeyListener(mListOnKeyListener);
		mHandler.post(mRequestFocus);
	}

	private final OnKeyListener mListOnKeyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			Object selectedItem = mList.getSelectedItem();
			if (selectedItem instanceof Preference) {
				return false;
			}
			return false;
		}

	};

	/**
	 * Clear any results from the prior fragment. We don't want them persisting past this fragment,
	 * and they should have been looked at by now anyway
	 */
	@Override
	public void onResume() {
		super.onResume();
		if ((getActivity()) != null) {
			((FamiliarActivity) getActivity()).getFragmentResults();
			((FamiliarActivity) getActivity())
					.mDrawerLayout.closeDrawer(((FamiliarActivity) getActivity()).mDrawerList);
		}
	}
}