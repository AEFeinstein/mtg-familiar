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
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.fragments.ManaPoolFragment;
import com.gelakinetic.mtgfam.fragments.MoJhoStoFragment;
import com.gelakinetic.mtgfam.fragments.PrefsFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;
import com.gelakinetic.mtgfam.fragments.RulesFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.IndeterminateRefreshLayout;
import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchService;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.ZipUtils;
import com.gelakinetic.mtgfam.helpers.database.DatabaseHelper;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.updaters.DbUpdaterService;
import com.octo.android.robospice.SpiceManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FamiliarActivity extends FragmentActivity {
	/* Tags for fragments */
	public static final String DIALOG_TAG = "dialog";
	public static final String FRAGMENT_TAG = "fragment";

	/* Constants used for launching activities, receiving results */
	public static final String ACTION_ROUND_TIMER = "android.intent.action.ROUND_TIMER";
	private static final int TTS_DATA_CHECK_CODE = 42;

	/* Constants used for launching fragments */
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
	private static final int ABOUT_DIALOG = 100;
	public static final int CHANGE_LOG_DIALOG = 101;
	private static final int DONATE_DIALOG = 102;
	private static final int TTS_DIALOG = 103;
	public int dialogShowing = 0;

	/* Constants used for saving state */
	private static final String CURRENT_FRAG = "CURRENT_FRAG";
	private static final String IS_REFRESHING = "IS_REFRESHING";

	/* PayPal URL */
	@SuppressWarnings("SpellCheckingInspection")
	private static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
			"&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD" +
			"&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
	private static final String GITHUB_URL = "https://github.com/AEFeinstein/mtg-familiar";

	/* Drawer elements */
	public DrawerLayout mDrawerLayout;
	public ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	public boolean mIsMenuVisible;

	/* UI elements */
	private IndeterminateRefreshLayout mRefreshLayout;

	/* Used to pass results between fragments */
	private Bundle mFragResults;

	/* What the drawer menu will be */
	private final DrawerEntry[] mPageEntries = {
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
	private boolean mUpdatingRoundTimer;
	private long mRoundEndTime;
	private Handler mRoundTimerUpdateHandler;

	/* Spice setup */
	public final SpiceManager mSpiceManager = new SpiceManager(PriceFetchService.class);
	public PreferenceAdapter mPreferenceAdapter;
	private int mCurrentFrag;

	/* Timer to determine user inactivity for screen dimming in the life counter */
	private final Handler mInactivityHandler = new Handler();
	private boolean mUserInactive = false;
	private final Runnable userInactive = new Runnable() {
		@Override
		public void run() {
			mUserInactive = true;
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
			if (fragment instanceof FamiliarFragment) {
				((FamiliarFragment) fragment).onUserInactive();
			}
		}
	};

	/* Listen for changes to preferences */
	private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
			if (s.equals(getString(R.string.key_widgetButtons))) {
				Intent intent = new Intent(FamiliarActivity.this, MTGFamiliarAppWidgetProvider.class);
				intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				assert AppWidgetManager.getInstance(getApplication()) != null;
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
				assert appWidgetManager != null;
				int ids[] = appWidgetManager.getAppWidgetIds(
						new ComponentName(getApplication(), MTGFamiliarAppWidgetProvider.class));
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
				sendBroadcast(intent);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPreferenceAdapter.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
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

		DatabaseManager.initializeInstance(new DatabaseHelper(getApplicationContext()));

		mRefreshLayout = ((IndeterminateRefreshLayout) findViewById(R.id.fragment_container));

		/* Set up a listener to update the home screen widget whenever the user changes the preference */
		mPreferenceAdapter.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

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
						selectItem(mPageEntries[i].mNameResource, null);
						break;
					}
					case R.string.main_settings_title: {
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.addToBackStack(null);
						ft.replace(R.id.fragment_container, new PrefsFragment(), FamiliarActivity.FRAGMENT_TAG);
						ft.commit();
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
				R.string.main_drawer_open, /* "open drawer" description for accessibility */
				R.string.main_drawer_close /* "close drawer" description for accessibility */
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

				/* Clear the mtr and ipg on update, to replace them with the newly colored versions, but only if we're
				 * updating to 3.0 (v23) */
				if (pInfo.versionCode == 23) {
					File mtr = new File(getFilesDir(), JudgesCornerFragment.MTR_LOCAL_FILE);
					File ipg = new File(getFilesDir(), JudgesCornerFragment.IPG_LOCAL_FILE);
					if (mtr.exists()) {
						if (!mtr.delete()) {
							Toast.makeText(this, mtr.getName() + " " + getString(R.string.not_deleted),
									Toast.LENGTH_LONG).show();
						}
						if (!ipg.delete()) {
							Toast.makeText(this, ipg.getName() + " " + getString(R.string.not_deleted),
									Toast.LENGTH_LONG).show();
						}
					}
				}
			}
		} catch (PackageManager.NameNotFoundException e) {
			/* Eat it, don't show change log */
		}

		/* The activity can be launched a few different ways. Check the intent and show the appropriate fragment */
		/* Only launch a fragment if the app isn't being recreated, i.e. savedInstanceState is null */
		if (savedInstanceState == null) {
			Intent intent = getIntent();
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				/* Do a search by name, launched from the quick search */
				String query = intent.getStringExtra(SearchManager.QUERY);
				Bundle args = new Bundle();
				SearchCriteria sc = new SearchCriteria();
				sc.name = query;
				args.putSerializable(SearchViewFragment.CRITERIA, sc);
				selectItem(R.string.main_card_search, args);
			}
			else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				/* User clicked a card in the quick search autocomplete, jump right to it */
				Uri data = intent.getData();
				Bundle args = new Bundle();
				assert data != null;
				args.putLong(CardViewFragment.CARD_ID, Long.parseLong(data.getLastPathSegment()));
				selectItem(R.string.main_card_search, args);
			}
			else if (ACTION_ROUND_TIMER.equals(intent.getAction())) {
				selectItem(R.string.main_timer, null);
			}
			else if (ACTION_CARD_SEARCH.equals(intent.getAction())) {
				selectItem(R.string.main_card_search, null);
			}
			else if (ACTION_LIFE.equals(intent.getAction())) {
				selectItem(R.string.main_life_counter, null);
			}
			else if (ACTION_DICE.equals(intent.getAction())) {
				selectItem(R.string.main_dice, null);
			}
			else if (ACTION_TRADE.equals(intent.getAction())) {
				selectItem(R.string.main_trade, null);
			}
			else if (ACTION_MANA.equals(intent.getAction())) {
				selectItem(R.string.main_mana_pool, null);
			}
			else if (ACTION_WISH.equals(intent.getAction())) {
				selectItem(R.string.main_wishlist, null);
			}
			else if (ACTION_RULES.equals(intent.getAction())) {
				selectItem(R.string.main_rules, null);
			}
			else if (ACTION_JUDGE.equals(intent.getAction())) {
				selectItem(R.string.main_judges_corner, null);
			}
			else if (ACTION_MOJHOSTO.equals(intent.getAction())) {
				selectItem(R.string.main_mojhosto, null);
			}
			else {
			/* App launched as regular, show the default fragment */

				String defaultFragment = mPreferenceAdapter.getDefaultFragment();

				if (defaultFragment.equals(this.getString(R.string.main_card_search))) {
					selectItem(R.string.main_card_search, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_life_counter))) {
					selectItem(R.string.main_life_counter, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mana_pool))) {
					selectItem(R.string.main_mana_pool, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_dice))) {
					selectItem(R.string.main_dice, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_trade))) {
					selectItem(R.string.main_trade, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_wishlist))) {
					selectItem(R.string.main_wishlist, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_timer))) {
					selectItem(R.string.main_timer, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_rules))) {
					selectItem(R.string.main_rules, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_judges_corner))) {
					selectItem(R.string.main_judges_corner, null);
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mojhosto))) {
					selectItem(R.string.main_mojhosto, null);
				}
				else {
					selectItem(R.string.main_card_search, null);
				}
			}

			mDrawerList.setItemChecked(mCurrentFrag, true);
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

	/**
	 * Select an item from the drawer menu. This will highlight the entry and manage fragment transactions.
	 *
	 * @param resId The string resource ID of the entry
	 */
	private void selectItem(int resId, Bundle args) {

		int position = 0;
		for (DrawerEntry entry : mPageEntries) {
			if (resId == entry.mNameResource) {
				break;
			}
			position++;
		}

		if (mCurrentFrag == position) {
			/* This is the same fragment, just close the menu */
			mDrawerLayout.closeDrawer(mDrawerList);
			return;
		}

		mCurrentFrag = position;
		Fragment newFrag;
		/* Pick the new fragment */
		switch (resId) {
			case R.string.main_card_search: {
				/* If this is a quick search intent, launch either the card view or result list directly */
				if (args != null && args.containsKey(CardViewFragment.CARD_ID)) {
					newFrag = new CardViewFragment();
				}
				else if (args != null && args.containsKey(SearchViewFragment.CRITERIA)) {
					newFrag = new ResultListFragment();
				}
				else {
					newFrag = new SearchViewFragment();
				}
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
				newFrag = new TradeFragment();
				break;
			}
			case R.string.main_wishlist: {
				newFrag = new WishlistFragment();
				break;
			}
			case R.string.main_timer: {
				newFrag = new RoundTimerFragment();
				break;
			}
			case R.string.main_rules: {
				newFrag = new RulesFragment();
				break;
			}
			case R.string.main_judges_corner: {
				newFrag = new JudgesCornerFragment();
				break;
			}
			case R.string.main_mojhosto: {
				newFrag = new MoJhoStoFragment();
				break;
			}
			default:
				return;
		}

		if (args != null) {
			newFrag.setArguments(args);
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
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(layout, parent, false);
			}

			assert convertView != null;
			if (values[position].mIsHeader) {
				/* Make sure the recycled view is the right type, inflate a new one if necessary */
				if (convertView.findViewById(R.id.drawer_header_name) == null) {
					convertView = getLayoutInflater().inflate(layout, parent, false);
				}
				assert convertView != null;
				((TextView) convertView.findViewById(R.id.drawer_header_name)).setText(values[position].mNameResource);
				convertView.setFocusable(false);
				convertView.setFocusableInTouchMode(false);
			}
			else {
				/* Make sure the recycled view is the right type, inflate a new one if necessary */
				if (convertView.findViewById(R.id.drawer_entry_name) == null) {
					convertView = getLayoutInflater().inflate(layout, parent, false);
				}
				assert convertView != null;
				((TextView) convertView.findViewById(R.id.drawer_entry_name)).setText(values[position].mNameResource);
				((ImageView) convertView.findViewById(R.id.drawer_entry_icon))
						.setImageResource(values[position].mIconResource);
			}

			if (position + 1 >= values.length || values[position + 1].mIsHeader) {
				convertView.findViewById(R.id.divider).setVisibility(View.GONE);
			}
			else {
				convertView.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			}
			return convertView;
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
		dialogShowing = 0;
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

		dialogShowing = id;
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
						View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
						assert dialogLayout != null;
						TextView text = (TextView) dialogLayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_about_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());
						dialogLayout.findViewById(R.id.image_button).setVisibility(View.GONE);
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

						/* Set the custom view, with some images below the text */
						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
						assert dialogLayout != null;
						TextView text = (TextView) dialogLayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_whats_new_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());

						dialogLayout.findViewById(R.id.imageview1).setVisibility(View.GONE);
						dialogLayout.findViewById(R.id.imageview2).setVisibility(View.GONE);
						dialogLayout.findViewById(R.id.image_button).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
								startActivity(myIntent);
							}
						});
						builder.setView(dialogLayout);

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
								Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL));
								startActivity(myIntent);
								dialog.cancel();
							}
						});

						/* Set the custom view */
						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);

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
						dialogLayout.findViewById(R.id.image_button).setVisibility(View.GONE);

						builder.setView(dialogLayout);
						return builder.create();
					}
					case TTS_DIALOG: {
						/* Then display a dialog informing them of TTS */

						builder.setTitle(R.string.main_tts_warning_title)
								.setMessage(R.string.main_tts_warning_text)
								.setPositiveButton(R.string.main_install_tts, new DialogInterface.OnClickListener() {
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
								}, 2000);
							}
						}, 500);
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
	private final Runnable timerUpdate = new Runnable() {

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

	/**
	 * Save the current fragment.
	 *
	 * @param outState a Bundle in which to save the state
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_FRAG, mCurrentFrag);
		outState.putBoolean(IS_REFRESHING, mRefreshLayout.mRefreshing);
		mRefreshLayout.setRefreshing(false);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Restore the current fragment, and highlight it
	 *
	 * @param savedInstanceState a Bundle which contains the saved state
	 */
	@Override
	protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.containsKey(CURRENT_FRAG)) {
			mCurrentFrag = savedInstanceState.getInt(CURRENT_FRAG);
			mDrawerList.setItemChecked(mCurrentFrag, true);

			mRefreshLayout.setRefreshing(savedInstanceState.getBoolean(IS_REFRESHING));
		}
	}

	/**
	 * Show the indeterminate loading bar
	 */
	public void setLoading() {
		mRefreshLayout.setRefreshing(true);
	}

	/**
	 * Hide the indeterminate loading bar
	 */
	public void clearLoading() {
		mRefreshLayout.setRefreshing(false);
	}
}