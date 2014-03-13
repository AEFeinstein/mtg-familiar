/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:/* www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gelakinetic.mtgfam;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.fragments.ManaPoolFragment;
import com.gelakinetic.mtgfam.fragments.MoJhoStoFragment;
import com.gelakinetic.mtgfam.fragments.PrefsFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchService;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.ZipUtils;
import com.gelakinetic.mtgfam.helpers.updaters.DbUpdaterService;
import com.octo.android.robospice.SpiceManager;

import org.jetbrains.annotations.NotNull;

public class FamiliarActivity extends FragmentActivity {
	/* Tags for fragments */
	public static final String DIALOG_TAG = "dialog";
	public static final String FRAGMENT_TAG = "fragment";

	/* Constants used for launching activities, receiving results */
	public static final String ACTION_ROUND_TIMER = "android.intent.action.ROUND_TIMER";
	private static final int TTS_DATA_CHECK_CODE = 42;

	/* Constant used for action bar quick search */
//	public static final String ACTION_SEARCH = "android.intent.action.SEARCH";

	/* Constants used for launching from the widget */
	public static final String ACTION_CARD_SEARCH = "android.intent.action.CARD_SEARCH";
	public static final String ACTION_LIFE = "android.intent.action.LIFE";
	public static final String ACTION_DICE = "android.intent.action.DICE";
	public static final String ACTION_TRADE = "android.intent.action.TRADE";
	public static final String ACTION_MANA = "android.intent.action.MANA";
	public static final String ACTION_WISH = "android.intent.action.WISH";
	public static final String ACTION_RULES = "android.intent.action.RULES";
	public static final String ACTION_JUDGE = "android.intent.action.JUDGE";
	public static final String ACTION_MOJHOSTO = "android.intent.action.MOJHOSTO";

	/* Constants used for displaying dialogs */
	public static final int ABOUT_DIALOG = 100;
	public static final int CHANGE_LOG_DIALOG = 101;
	public static final int DONATE_DIALOG = 102;
	public static final int TTS_DIALOG = 103;

	/* PayPal URL */
	@SuppressWarnings("SpellCheckingInspection")
	private static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
			"&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD" +
			"&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";

	/* Drawer elements */
	public DrawerLayout mDrawerLayout;
	public ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	public boolean mIsMenuVisible;

	/* Used to pass results between fragments */
	private Bundle mFragResults;

	/* What the drawer menu will be */
	final DrawerEntry[] mPageEntries = {
			new DrawerEntry(R.string.main_pages, 0, true),
			new DrawerEntry(R.string.main_card_search, R.drawable.ic_drawer_search, false),
			new DrawerEntry(R.string.main_life_counter, R.drawable.ic_drawer_life, false),
			new DrawerEntry(R.string.main_mana_pool, R.drawable.ic_drawer_mana, false),
			new DrawerEntry(R.string.main_dice, R.drawable.ic_drawer_dice, false),
			new DrawerEntry(R.string.main_trade, R.drawable.ic_drawer_trade, false),
			new DrawerEntry(R.string.main_wishlist, R.drawable.ic_drawer_wishlist, false),
			new DrawerEntry(R.string.main_timer, R.drawable.ic_drawer_timer, false),
			new DrawerEntry(R.string.main_rules, R.drawable.ic_drawer_rules, false),
			new DrawerEntry(R.string.main_judges_corner, R.drawable.ic_drawer_judge, false),
			new DrawerEntry(R.string.main_mojhosto, R.drawable.ic_drawer_mojhosto, false),
			new DrawerEntry(R.string.main_extras, 0, true),
			new DrawerEntry(R.string.main_settings_title, R.drawable.ic_drawer_settings, false),
			new DrawerEntry(R.string.main_force_update_title, R.drawable.ic_drawer_download, false),
			new DrawerEntry(R.string.main_donate_title, R.drawable.ic_drawer_good, false),
			new DrawerEntry(R.string.main_about, R.drawable.ic_drawer_about, false),
			new DrawerEntry(R.string.main_whats_new_title, R.drawable.ic_drawer_help, false),
			new DrawerEntry(R.string.main_export_data_title, R.drawable.ic_action_save, false),
			new DrawerEntry(R.string.main_import_data_title, R.drawable.ic_action_collection, false)
	};

	/* Timer setup */
	public boolean mUpdatingRoundTimer;
	public long mRoundEndTime;
	public Handler mRoundTimerUpdateHandler;

	/* Spice setup */
	public final SpiceManager mSpiceManager = new SpiceManager(PriceFetchService.class);
	public PreferenceAdapter mPreferenceAdapter;
	private int mCurrentFrag;

	/* Timer to determine user inactivity for screen dimming in the life counter */
	private Handler mInactivityHandler = new Handler();
	private boolean mUserInactive = false;
	private Runnable userInactive = new Runnable() {
		@Override
		public void run() {
			mUserInactive = true;
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
			if (fragment instanceof FamiliarFragment) {
				((FamiliarFragment) fragment).onUserInactive();
			}
		}
	};

	/**
	 * Start the Spice Manager when the activity starts
	 */
	@Override
	protected void onStart() {
		super.onStart();
		mSpiceManager.start(this);
	}

	/**
	 * Stop the Spice Manager when the activity stops
	 */
	@Override
	protected void onStop() {
		super.onStop();
		mSpiceManager.shouldStop();
	}

	/**
	 * Called whenever we call invalidateOptionsMenu(). This hides action bar items when the drawer is open
	 *
	 * @param menu The menu to hide or show items in
	 * @return True if the menu is to be displayed, false otherwise
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean drawerVisible = mDrawerLayout.isDrawerVisible(mDrawerList);
		mIsMenuVisible = !drawerVisible;
		for (int i = 0; i < menu.size(); i++) {
			menu.getItem(i).setVisible(!drawerVisible);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Set up drawer menu
	 * Run updater service
	 * Check for, and potentially start, round timer
	 * Check for TTS support
	 *
	 * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this
	 *                           Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
	 *                           Note: Otherwise it is null.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mPreferenceAdapter = new PreferenceAdapter(this);

		/* Set up a listener to update the home screen widget whenever the user changes the preference */
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
				new SharedPreferences.OnSharedPreferenceChangeListener() {
					@Override
					public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
						if (s.equals("widgetButtons")) {
							Intent intent = new Intent(FamiliarActivity.this, MTGFamiliarAppWidgetProvider.class);
							intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
							/* Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
							   since it seems the onUpdate() is only fired on that: */
							int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(
									new ComponentName(getApplication(), MTGFamiliarAppWidgetProvider.class));
							intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
							sendBroadcast(intent);
						}
					}
				}
		);

		/* Create the handler to update the timer in the action bar */
		mRoundTimerUpdateHandler = new Handler();

		/* Check if we should make the timer notification */
		mRoundEndTime = mPreferenceAdapter.getRoundTimerEnd();
		if (mRoundEndTime != -1) {
			RoundTimerFragment.showTimerRunningNotification(this, mRoundEndTime);
			RoundTimerFragment.setOrCancelAlarms(this, mRoundEndTime, true);
		}
		mUpdatingRoundTimer = false;

		/* Check for TTS support, the result will be caught in onActivityResult()
		 * Launching this intent will cause the activity lifecycle to call onPause() and onResume() twice  */
		if (mPreferenceAdapter.getTtsShowDialog()) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
		}

		/* Get the drawer layout and list */
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		/* set a custom shadow that overlays the main content when the drawer opens */
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		/* set up the drawer's list view with items and click listener */
		DrawerEntryArrayAdapter pagesAdapter = new DrawerEntryArrayAdapter(this, mPageEntries);

		mDrawerList.setAdapter(pagesAdapter);
		mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				/* FamiliarFragments will automatically close the drawer when they hit onResume(). It's more precise
				   than a delayed handler. Other options have to close the drawer themselves */
				boolean shouldCloseDrawer = false;
				switch (mPageEntries[i].mNameResource) {
					case R.string.main_extras:
					case R.string.main_pages: {
						/* It's a header */
						mDrawerList.setItemChecked(mCurrentFrag, true);
						return; /* don't close the drawer or change a selection */
					}
					case R.string.main_mana_pool:
					case R.string.main_dice:
					case R.string.main_trade:
					case R.string.main_wishlist:
					case R.string.main_timer:
					case R.string.main_rules:
					case R.string.main_judges_corner:
					case R.string.main_mojhosto:
					case R.string.main_card_search:
					case R.string.main_life_counter: {
						selectItem(mPageEntries[i].mNameResource);
						break;
					}
					case R.string.main_settings_title: {
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.addToBackStack(null);
						ft.replace(R.id.fragment_container, new PrefsFragment(), FamiliarActivity.FRAGMENT_TAG);
						ft.commit();
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_force_update_title: {
						mPreferenceAdapter.setLastLegalityUpdate(0);
						startService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_donate_title: {
						showDialogFragment(DONATE_DIALOG);
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_about: {
						showDialogFragment(ABOUT_DIALOG);
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_whats_new_title: {
						showDialogFragment(CHANGE_LOG_DIALOG);
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_export_data_title: {
						ZipUtils.exportData(FamiliarActivity.this);
						shouldCloseDrawer = true;
						break;
					}
					case R.string.main_import_data_title: {
						ZipUtils.importData(FamiliarActivity.this);
						shouldCloseDrawer = true;
						break;
					}
				}

				mDrawerList.setItemChecked(mCurrentFrag, true);
				if (shouldCloseDrawer) {
					(new Handler()).postDelayed(new Runnable() {
						public void run() {
							mDrawerLayout.closeDrawer(mDrawerList);
						}
					}, 50);
				}
			}
		});

		/* enable ActionBar app icon to behave as action to toggle nav drawer */
		assert getActionBar() != null;
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setTitle("");

		/* ActionBarDrawerToggle ties together the the proper interactions between the sliding drawer and the action
		bar app icon */
		mDrawerToggle = new ActionBarDrawerToggle(
				this, /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
				R.string.drawer_open, /* "open drawer" description for accessibility */
				R.string.drawer_close /* "close drawer" description for accessibility */
		) {

			/**
			 * When the drawer is opened, make sure to hide the keyboard
			 * @param drawerView the drawer view, ignored
			 */
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				hideKeyboard();
			}

			/**
			 * If the menu is visible and the menu is open(ing), or the menu is not visible and the drawer is closed,
			 * invalidate the options menu to either hide or show the action bar icons
			 *
			 * @param drawerView The drawer view, ignored
			 * @param slideOffset How open the sliding menu is, between 0 and 1
			 */
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
				if ((mIsMenuVisible && slideOffset > 0) || (slideOffset == 0 && !mIsMenuVisible)) {
					invalidateOptionsMenu();
				}
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		/* Check to see if the change log should be shown */
		PackageInfo pInfo;
		try {
			assert getPackageManager() != null;
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

			int lastVersion = mPreferenceAdapter.getLastVersion();
			if (pInfo.versionCode != lastVersion) {
				/* Clear the spice cache on upgrade. This way, no cached values w/o foil prices will exist*/
				try {
					mSpiceManager.removeAllDataFromCache();
				} catch (NullPointerException e) {
					/* eat it. tasty */
				}
				showDialogFragment(CHANGE_LOG_DIALOG);
				mPreferenceAdapter.setLastVersion(pInfo.versionCode);
			}
		} catch (PackageManager.NameNotFoundException e) {
			/* Eat it, don't show change log */
		}

		/* The activity can be launched a few different ways. Check the intent and show the appropriate fragment */
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			/* Do a search by name, launched from the quick search */
			String query = intent.getStringExtra(SearchManager.QUERY);
			ResultListFragment resultListFragment = new ResultListFragment();
			Bundle args = new Bundle();
			SearchCriteria sc = new SearchCriteria();
			sc.name = query;
			args.putSerializable(SearchViewFragment.CRITERIA, sc);
			resultListFragment.setArguments(args);

			FragmentManager fm = getSupportFragmentManager();
			if (fm != null) {
				/* Begin a new transaction */
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(R.id.fragment_container, resultListFragment, FamiliarActivity.FRAGMENT_TAG);
				ft.commit();
			}
			mCurrentFrag = 1;
			mDrawerList.setItemChecked(mCurrentFrag, true);
		}
		else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			/* User clicked a card in the quick search autocomplete, jump right to it */
			Uri data = intent.getData();
			CardViewFragment cardViewFragment = new CardViewFragment();
			Bundle args = new Bundle();
			assert data != null;
			args.putLong(CardViewFragment.CARD_ID, Long.parseLong(data.getLastPathSegment()));
			cardViewFragment.setArguments(args);

			FragmentManager fm = getSupportFragmentManager();
			if (fm != null) {
				/* Begin a new transaction */
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(R.id.fragment_container, cardViewFragment, FamiliarActivity.FRAGMENT_TAG);
				ft.commit();
			}
			mCurrentFrag = 1;
			mDrawerList.setItemChecked(mCurrentFrag, true);
		}
		else if (ACTION_ROUND_TIMER.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_timer);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_CARD_SEARCH.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_card_search);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_LIFE.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_life_counter);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_DICE.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_dice);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_TRADE.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_trade);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_MANA.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_mana_pool);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_WISH.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_wishlist);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_RULES.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_rules);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_JUDGE.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_judges_corner);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else if (ACTION_MOJHOSTO.equals(intent.getAction())) {
			if (savedInstanceState == null) {
				selectItem(R.string.main_mojhosto);
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}
		else {
			/* App launched as regular, show the default fragment TODO (should check preferences) */
			if (savedInstanceState == null) {

				String defaultFragment = mPreferenceAdapter.getDefaultFragment();

				if (defaultFragment.equals(this.getString(R.string.main_card_search))) {
					selectItem(R.string.main_card_search);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_life_counter))) {
					selectItem(R.string.main_life_counter);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mana_pool))) {
					selectItem(R.string.main_mana_pool);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_dice))) {
					selectItem(R.string.main_dice);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_trade))) {
					selectItem(R.string.main_trade);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_wishlist))) {
					selectItem(R.string.main_wishlist);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_timer))) {
					selectItem(R.string.main_timer);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_rules))) {
					selectItem(R.string.main_rules);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_judges_corner))) {
					selectItem(R.string.main_judges_corner);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mojhosto))) {
					selectItem(R.string.main_mojhosto);
				}
				else {
					selectItem(R.string.main_card_search);
				}
				mDrawerList.setItemChecked(mCurrentFrag, true);
			}
		}

		/* Run the updater service */
		if (mPreferenceAdapter.getAutoUpdate()) {
			/* Only update the banning list if it hasn't been updated recently */
			long curTime = System.currentTimeMillis();
			int updateFrequency = Integer.valueOf(mPreferenceAdapter.getUpdateFrequency());
			int lastLegalityUpdate = mPreferenceAdapter.getLastLegalityUpdate();
			/* days to ms */
			if (((curTime / 1000) - lastLegalityUpdate) > (updateFrequency * 24 * 60 * 60)) {
				startService(new Intent(this, DbUpdaterService.class));
			}
		}
	}

	/**
	 * Check to see if we should display the round timer in the actionbar, and start the inactivity timer
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mRoundEndTime != -1) {
			startUpdatingDisplay();
		}
		mInactivityHandler.postDelayed(userInactive, 15000);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * Select an item from the drawer menu. This will highlight the entry and manage fragment transactions.
	 *
	 * @param resId The string resource ID of the entry
	 */
	private void selectItem(int resId) {

		int position = 0;
		for (DrawerEntry entry : mPageEntries) {
			if (resId == entry.mNameResource) {
				break;
			}
			position++;
		}

		Fragment newFrag = null;
		mCurrentFrag = position;
		/* Pick the new fragment */
		switch (resId) {
			case R.string.main_card_search: {
				newFrag = new SearchViewFragment();
				break;
			}
			case R.string.main_life_counter: {
				newFrag = new LifeCounterFragment();
				break;
			}
			case R.string.main_mana_pool: {
				newFrag = new ManaPoolFragment();
				break;
			}
			case R.string.main_dice: {
				newFrag = new DiceFragment();
				break;
			}
			case R.string.main_trade: {
				//TODO
				break;
			}
			case R.string.main_wishlist: {
				//TODO
				break;
			}
			case R.string.main_timer: {
				newFrag = new RoundTimerFragment();
				break;
			}
			case R.string.main_rules: {
				//TODO
				break;
			}
			case R.string.main_judges_corner: {
				//TODO
				break;
			}
			case R.string.main_mojhosto: {
				newFrag = new MoJhoStoFragment();
				break;
			}
			default:
				return;
		}

		if (newFrag == null) {
			return;
		}

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft;
		if (fm != null) {
			/* Remove any current fragments on the back stack */
			for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
				fm.popBackStack();
			}

			/* Begin a new transaction */
			ft = fm.beginTransaction();

			/* Replace or add the fragment */
			ft.replace(R.id.fragment_container, newFrag, FamiliarActivity.FRAGMENT_TAG);
			ft.commit();
		}
	}

	/**
	 * mDrawerToggle.syncState() must be called during onPostCreate()
	 *
	 * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this
	 *                           Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
	 *                           Note: Otherwise it is null.
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		/* Sync the toggle state after onRestoreInstanceState has occurred. */
		mDrawerToggle.syncState();
	}

	/**
	 * The drawer toggle should be notified of any configuration changes
	 *
	 * @param newConfig The new device configuration.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		/* Pass any configuration change to the drawer toggles */
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * If a fragment has a result when it pops off the back stack, store it in the activity
	 * It can be checked by the next FamiliarFragment to be displayed with getFragmentResults
	 * The Bundle is cleared in the fragment's onResume() if it isn't accessed
	 *
	 * @param result The bundle of results to save
	 */
	public void setFragmentResult(Bundle result) {
		mFragResults = result;
	}

	/**
	 * This will return a Bundle which a fragment has stored in the activity, and then
	 * null the Bundle immediately
	 *
	 * @return a Bundle stored by the prior fragment
	 */
	public Bundle getFragmentResults() {
		if (mFragResults != null) {
			Bundle res = mFragResults;
			mFragResults = null;
			return res;
		}
		return null;
	}

	/**
	 * Called when a key down event has occurred. Checks if the fragment should handle the key event or if the
	 * activity should
	 *
	 * @param keyCode The key type for the event. We only care about KEYCODE_SEARCH
	 * @param event   description of the key event
	 * @return True if the event was handled, false if otherwise
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
			/* Check to see if the current fragment did anything with the search key */
			return ((FamiliarFragment) f).onInterceptSearchKey() || super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * This function hides the soft keyboard if it is being displayed. It's nice for switching fragments
	 */
	public void hideKeyboard() {
		try {
			InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			assert getCurrentFocus() != null;
			inputManager
					.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		} catch (NullPointerException e) {
			/* eat it */
		}
	}

	/**
	 * A friendly reminder to not use the regular FragmentManager, ever
	 *
	 * @return Return? How about an exception!
	 */
	@Override
	@NotNull
	public android.app.FragmentManager getFragmentManager() {
		throw new IllegalAccessError("Use .getSupportFragmentManager()");
	}

	/**
	 * This nested class encapsulates the necessary information for an entry in the drawer menu
	 */
	public class DrawerEntry {
		final int mNameResource;
		final int mIconResource;
		final boolean mIsHeader;

		public DrawerEntry(int nameResource, int iconResource, boolean isHeader) {
			mNameResource = nameResource;
			mIconResource = iconResource;
			mIsHeader = isHeader;
		}
	}

	/**
	 * This nested class is the adapter which populates the listView in the drawer menu. It handles both entries and
	 * headers
	 */
	public class DrawerEntryArrayAdapter extends ArrayAdapter<DrawerEntry> {
		private final Context context;
		private final DrawerEntry[] values;

		/**
		 * Constructor. The context will be used to inflate views later. The array of values will be used to populate
		 * the views
		 *
		 * @param context The application's context, used to inflate views later.
		 * @param values  An array of DrawerEntries which will populate the list
		 */
		public DrawerEntryArrayAdapter(Context context, DrawerEntry[] values) {
			super(context, R.layout.drawer_list_item, values);
			this.context = context;
			this.values = values;
		}

		/**
		 * Called to get a view for an entry in the listView
		 *
		 * @param position    The position of the listView to populate
		 * @param convertView The old view to reuse, if possible. Since the layouts for entries and headers are
		 *                    different, this will be ignored
		 * @param parent      The parent this view will eventually be attached to
		 * @return The view for the data at this position
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int layout;
			if (values[position].mIsHeader) {
				layout = R.layout.drawer_list_header;
			}
			else {
				layout = R.layout.drawer_list_item;
			}
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(layout, parent, false);

			assert rowView != null;
			if (values[position].mIsHeader) {
				((TextView) rowView.findViewById(R.id.drawer_header_name)).setText(values[position].mNameResource);
				rowView.setFocusable(false);
				rowView.setFocusableInTouchMode(false);
			}
			else {
				((TextView) rowView.findViewById(R.id.drawer_entry_name)).setText(values[position].mNameResource);
				((ImageView) rowView.findViewById(R.id.drawer_entry_icon))
						.setImageResource(values[position].mIconResource);
			}

			if (position + 1 >= values.length || values[position + 1].mIsHeader) {
				rowView.findViewById(R.id.divider).setVisibility(View.INVISIBLE);
			}
			return rowView;
		}
	}

	/**
	 * Removes a fragment with the DIALOG_TAG. The FragmentManager is a parameter so that it plays nice with nested
	 * fragments and getChildFragmentManager()
	 *
	 * @param fragmentManager The FragmentManager to use for this transaction
	 */
	public void removeDialogFragment(FragmentManager fragmentManager) {
		if (fragmentManager != null) {
			Fragment prev = fragmentManager.findFragmentByTag(FamiliarActivity.DIALOG_TAG);
			if (prev != null) {
				if (prev instanceof DialogFragment) {
					((DialogFragment) prev).dismiss();
				}
				FragmentTransaction ft = fragmentManager.beginTransaction();
				ft.remove(prev);
				ft.commit();
			}
		}
	}

	/**
	 * Dismiss any currently showing dialogs, then show the requested one.
	 *
	 * @param id the ID of the dialog to show
	 */
	void showDialogFragment(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		removeDialogFragment(getSupportFragmentManager());

		/* Create and show the dialog. */
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			/**
			 * Overridden to create the specific dialogs
			 * @param savedInstanceState The last saved instance state of the Fragment, or null if this is a freshly
			 *                           created Fragment.
			 *
			 * @return The new dialog instance to be displayed. All dialogs are created with the AlertDialog builder, so
			 * onCreateView() does not need to be implemented
			 */
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);
				AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

				assert getPackageManager() != null;

				switch (id) {
					case ABOUT_DIALOG: {

						/* Set the title with the package version if possible */
						try {
							builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name) + " " +
									getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
						} catch (PackageManager.NameNotFoundException e) {
							builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name));
						}

						/* Set the neutral button */
						builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

						/* Set the custom view, with some images below the text */
						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialogLayout = inflater.inflate(R.layout.activity_dialog_about,
								(ViewGroup) findViewById(R.id.dialog_layout_root));
						assert dialogLayout != null;
						TextView text = (TextView) dialogLayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_about_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());
						builder.setView(dialogLayout);

						return builder.create();
					}
					case CHANGE_LOG_DIALOG: {
						try {
							builder.setTitle(getString(R.string.main_whats_new_in_title) + " " +
									getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
						} catch (PackageManager.NameNotFoundException e) {
							builder.setTitle(R.string.main_whats_new_title);
						}

						builder.setNeutralButton(R.string.dialog_enjoy, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

						builder.setMessage(ImageGetterHelper.formatHtmlString(getString(R.string.main_whats_new_text)));
						return builder.create();
					}
					case DONATE_DIALOG: {
						/* Set the title */
						builder.setTitle(R.string.main_donate_dialog_title);
						/* Set the buttons button */
						builder.setNegativeButton(R.string.dialog_thanks_anyway, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						builder.setPositiveButton(R.string.main_donate_title, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
										.parse(PAYPAL_URL));
								startActivity(myIntent);
								dialog.cancel();
							}
						});

						/* Set the custom view */
						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialogLayout = inflater.inflate(R.layout.activity_dialog_about,
								(ViewGroup) findViewById(R.id.dialog_layout_root));

						/* Set the text */
						assert dialogLayout != null;
						TextView text = (TextView) dialogLayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_donate_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());

						/* Set the image view */
						ImageView payPal = (ImageView) dialogLayout.findViewById(R.id.imageview1);
						payPal.setImageResource(R.drawable.paypal_icon);
						payPal.setOnClickListener(new View.OnClickListener() {

							public void onClick(View v) {
								Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
										.parse(PAYPAL_URL));

								startActivity(myIntent);
							}
						});
						dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);

						builder.setView(dialogLayout);
						return builder.create();
					}
					case TTS_DIALOG: {
						/* Then display a dialog informing them of TTS */

						builder.setTitle(R.string.main_tts_warning_title)
								.setMessage(R.string.main_tts_warning_text)
								.setPositiveButton(R.string.install_tts, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										Intent installIntent = new Intent();
										installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
										startActivity(installIntent);
										dialogInterface.dismiss();
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.dismiss();
									}
								});

						return builder.create();
					}
					default: {
						setShowsDialog(false);
						return null;
					}
				}
			}

			/**
			 * When the change log dismisses, check to see if we should bounce the drawer. It will open in 100ms, then
			 * close in 1000ms
			 *
			 * @param dialog A DialogInterface for the dismissed dialog
			 */
			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);
				if (id == CHANGE_LOG_DIALOG) {
					if (mPreferenceAdapter.getBounceDrawer()) {
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								mDrawerLayout.openDrawer(mDrawerList);
								new Handler().postDelayed(new Runnable() {
									@Override
									public void run() {
										mDrawerLayout.closeDrawer(mDrawerList);
										mPreferenceAdapter.setBounceDrawer(false);
									}
								}, 1000);
							}
						}, 100);
					}
				}
			}
		};
		newFragment.show(getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}

	/**
	 * This runnable is posted with a handler every second. It displays the time left in the action bar as the title
	 * If the time runs out, it will stop updating the display and notify the fragment, if it is a RoundTimerFragment
	 */
	public final Runnable timerUpdate = new Runnable() {

		@Override
		public void run() {
			if (mRoundEndTime > System.currentTimeMillis()) {
				long timeLeftSeconds = (mRoundEndTime - System.currentTimeMillis()) / 1000;
				String timeLeftStr;

				if (timeLeftSeconds <= 0) {
					timeLeftStr = "00:00:00";
				}
				else {
					/* This is a slight hack to handle the fact that it always rounds down. It will start the timer at
					   50:00 instead of 49:59, or whatever */
					timeLeftSeconds++;
					timeLeftStr = String.format("%02d:%02d:%02d",
							timeLeftSeconds / 3600,
							(timeLeftSeconds % 3600) / 60,
							timeLeftSeconds % 60);
				}

				if (mUpdatingRoundTimer) {
					assert getActionBar() != null;
					getActionBar().setTitle(timeLeftStr);
				}
				mRoundTimerUpdateHandler.postDelayed(timerUpdate, 1000);
			}
			else {
				stopUpdatingDisplay();
				Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
				if (fragment instanceof RoundTimerFragment) {
					((RoundTimerFragment) fragment).timerEnded();
				}
			}
		}
	};

	/**
	 * Show the timer in the action bar, set up the handler to update the displayed time every second.
	 */
	public void startUpdatingDisplay() {
		mRoundEndTime = mPreferenceAdapter.getRoundTimerEnd();
		mUpdatingRoundTimer = true;

		mRoundTimerUpdateHandler.removeCallbacks(timerUpdate);
		mRoundTimerUpdateHandler.postDelayed(timerUpdate, 1);

		assert getActionBar() != null;
		getActionBar().setDisplayShowTitleEnabled(true);
	}

	/**
	 * Clear the timer from the action bar, stop the handler from updating it.
	 */
	public void stopUpdatingDisplay() {
		mRoundEndTime = -1;
		mUpdatingRoundTimer = false;

		mRoundTimerUpdateHandler.removeCallbacks(timerUpdate);
		assert getActionBar() != null;
		getActionBar().setDisplayShowTitleEnabled(false);
	}

	/**
	 * Called when another activity returns and this one is displayed. Since the app is fragment based, this is only
	 * called when querying for TTS support, or when returning from a ringtone picker. The preference fragment does
	 * not handle the ringtone result correctly, so it must be caught here.
	 *
	 * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to
	 *                    identify who this result came from.
	 * @param resultCode  The integer result code returned by the child activity through its setResult().
	 * @param data        An Intent, which can return result data to the caller (various data can be attached to Intent
	 *                    "extras").
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/* The ringtone picker in the preference fragment and RoundTimerFragment will send a result here */
		if (data != null && data.getExtras() != null) {
			if (data.getExtras().keySet().contains(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)) {
				Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri != null) {
					mPreferenceAdapter.setTimerSound(uri.toString());
				}
			}
		}

		if (requestCode == TTS_DATA_CHECK_CODE) {
			/* So we don't display this dialog again and bother the user */
			mPreferenceAdapter.setTtsShowDialog(false);
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				/* missing data, install it */
				showDialogFragment(TTS_DIALOG);
			}
		}
	}

	/**
	 * Handle options item presses. In this case, the home button opens and closes the drawer
	 *
	 * @param item The item selected
	 * @return True if the click was acted upon, false otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
					mDrawerLayout.closeDrawer(mDrawerList);
				}
				else {
					mDrawerLayout.openDrawer(mDrawerList);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called whenever the user does anything. On an interaction, reset the inactivity timer and notifies the
	 * FamiliarFragment if it was inactive. The inactivity timer will notify the FamiliarFragment of inactivity
	 */
	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		if (mUserInactive) {
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
			if (fragment instanceof FamiliarFragment) {
				mUserInactive = true;
				((FamiliarFragment) fragment).onUserActive();
			}
		}
		mInactivityHandler.removeCallbacks(userInactive);
		mInactivityHandler.postDelayed(userInactive, 30000);
		mUserInactive = false;
	}
}