/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.gelakinetic.mtgfam.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import com.gelakinetic.mtgfam.R;

import java.util.HashMap;

/**
 * This fragment houses tabs which house other fragments. These sub-fragments are a collection of utilities for Judges
 */
public class JudgesCornerFragment extends FamiliarFragment {
	/* Key and constants for displaying the MTR and IPG */
	public static final String HTML_DOC = "html";
	public static final String MTR_LOCAL_FILE = "MTR.html";
	public static final String IPG_LOCAL_FILE = "IPG.html";
    public static final String JAR_LOCAL_FILE = "JAR.html";
	/* Constants to keep track of tabs */
	private static final String TAG_MTR = "MTR";
	private static final String TAG_IPG = "IPG";
    private static final String TAG_JAR = "JAR";
	private static final String TAG_COUNTER = "COUNTER";
	/* UI elements */
	private TabHost mTabHost;

	/**
	 * Set up the tab UI and the tabs
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
	 * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
	 *                           fragment should not add the view itself, but this can be used to generate the
	 *                           LayoutParams of the view.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
	 *                           here.
	 * @return The view to display
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View myView = inflater.inflate(R.layout.judges_corner_frag, container, false);
		assert myView != null;
		mTabHost = (TabHost) myView.findViewById(android.R.id.tabhost);
		mTabHost.setup();

		TabManager mTabManager = new TabManager(this, mTabHost);

		Bundle MtrBundle = new Bundle();
		MtrBundle.putString(HTML_DOC, MTR_LOCAL_FILE);

		Bundle IpgBundle = new Bundle();
		IpgBundle.putString(HTML_DOC, IPG_LOCAL_FILE);

        Bundle JarBundle = new Bundle();
        JarBundle.putString(HTML_DOC, JAR_LOCAL_FILE);

		mTabManager.addTab(mTabHost.newTabSpec(TAG_MTR)
				.setIndicator(getString(R.string.judges_corner_MTR)), HtmlDocFragment.class, MtrBundle);
		mTabManager.addTab(mTabHost.newTabSpec(TAG_IPG)
				.setIndicator(getString(R.string.judges_corner_IPG)), HtmlDocFragment.class, IpgBundle);
        mTabManager.addTab(mTabHost.newTabSpec(TAG_JAR)
                .setIndicator(getString(R.string.judges_corner_JAR)), HtmlDocFragment.class, JarBundle);
		mTabManager.addTab(mTabHost.newTabSpec(TAG_COUNTER)
				.setIndicator(getString(R.string.judges_corner_counter)), DeckCounterFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		return myView;
	}

	/**
	 * Save the current tab so that it is still displayed on rotation
	 *
	 * @param outState Bundle in which to place your saved state.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
	}

	/**
	 * This is a helper class that implements a generic mechanism for associating fragments with the tabs in a tab host.
	 * It relies on a trick. Normally a tab host has a simple API for supplying a View or Intent that each tab will
	 * show. This is not sufficient for switching between fragments. So instead we make the content part of the tab host
	 * 0dp high (it is not shown) and the TabManager supplies its own dummy view to show as the tab content. It listens
	 * to changes in tabs, and takes care of switch to the correct fragment shown in a separate content area whenever
	 * the selected tab changes.
	 */
	public static class TabManager implements TabHost.OnTabChangeListener {
		private final FamiliarFragment mFragment;
		private final TabHost mTabHost;
		private final int mContainerId;
		private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
		TabInfo mLastTab;

		/**
		 * Constructor
		 *
		 * @param fragment The fragment this TabManager will manage tabs for
		 * @param tabHost  A TabHost which exists in the fragment's view
		 */
		public TabManager(FamiliarFragment fragment, TabHost tabHost) {
			mFragment = fragment;
			mTabHost = tabHost;
			mContainerId = R.id.realtabcontent;
			mTabHost.setOnTabChangedListener(this);
		}

		/**
		 * Add a tab to this fragment
		 *
		 * @param tabSpec A tab spec with the string tag, created by this fragment's TabHost
		 * @param aClass  The class this tab will show
		 * @param args    Any arguments which are passed to the fragment class in this tab
		 */
		public void addTab(TabHost.TabSpec tabSpec, Class<?> aClass, Bundle args) {
			tabSpec.setContent(new DummyTabFactory(mFragment.getActivity()));
			String tag = tabSpec.getTag();

			TabInfo info = new TabInfo(tag, aClass, args);

			/* Check to see if we already have a fragment for this tab, probably from a previously saved state.  If so,
			 * deactivate it, because our initial state is that a tab isn't shown.
			 */
			info.fragment = mFragment.getChildFragmentManager().findFragmentByTag(tag);
			if (info.fragment != null && !info.fragment.isDetached()) {
				FragmentTransaction ft = mFragment.getChildFragmentManager().beginTransaction();
				ft.detach(info.fragment);
				ft.commit();
			}

			mTabs.put(tag, info);
			mTabHost.addTab(tabSpec);
		}

		/**
		 * Callback when a tab changes. It will cycle out views if the user selected a new tab.
		 *
		 * @param tabId The string tag for the changed tab
		 */
		@Override
		public void onTabChanged(String tabId) {
			TabInfo newTab = mTabs.get(tabId);
			if (mLastTab != newTab) {
				FragmentTransaction ft = mFragment.getChildFragmentManager().beginTransaction();
				if (mLastTab != null) {
					if (mLastTab.fragment != null) {
						ft.detach(mLastTab.fragment);
					}
				}
				if (newTab != null) {
					if (newTab.fragment == null) {
						newTab.fragment = Fragment.instantiate(mFragment.getActivity(),
								newTab.mClass.getName(), newTab.args);
						ft.add(mContainerId, newTab.fragment, newTab.tag);
					}
					else {
						ft.attach(newTab.fragment);
					}
				}

				mLastTab = newTab;
				ft.commit();
				mFragment.getChildFragmentManager().executePendingTransactions();
			}
		}

		/**
		 * This inner class encapsulates all the necessary information for a tab
		 */
		static final class TabInfo {
			private final String tag;
			private final Class<?> mClass;
			private final Bundle args;
			private Fragment fragment;

			/**
			 * Constructor
			 *
			 * @param _tag   A string tag for this tab
			 * @param _class The class this tab will host
			 * @param _args  Any arguments to pass to this tab's class
			 */
			TabInfo(String _tag, Class<?> _class, Bundle _args) {
				tag = _tag;
				mClass = _class;
				args = _args;
			}
		}

		/**
		 * This inner class creates views for tabs
		 */
		static class DummyTabFactory implements TabHost.TabContentFactory {
			private final Context mContext;

			/**
			 * Constructor
			 *
			 * @param context a Context to create views with
			 */
			public DummyTabFactory(Context context) {
				mContext = context;
			}

			/**
			 * Create a view for a tab
			 *
			 * @param tag The string tag to keep track of this tab
			 * @return The view to be displayed by this tab
			 */
			@Override
			public View createTabContent(String tag) {
				View v = new View(mContext);
				v.setMinimumWidth(0);
				v.setMinimumHeight(0);
				return v;
			}
		}
	}
}

