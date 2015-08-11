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

import android.app.Dialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.fragments.ManaPoolFragment;
import com.gelakinetic.mtgfam.fragments.MoJhoStoFragment;
import com.gelakinetic.mtgfam.fragments.PrefsFragment;
import com.gelakinetic.mtgfam.fragments.ProfileFragment;
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
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.ZipUtils;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.lruCache.ImageCache;
import com.gelakinetic.mtgfam.helpers.updaters.DbUpdaterService;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.octo.android.robospice.SpiceManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class FamiliarActivity extends AppCompatActivity {
    /* Tags for fragments */
    public static final String DIALOG_TAG = "dialog";
    public static final String FRAGMENT_TAG = "fragment";

    /* Constants used for launching activities, receiving results */
    public static final String ACTION_ROUND_TIMER = "android.intent.action.ROUND_TIMER";

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
    public static final String ACTION_PROFILE = "android.intent.action.PROFILE";

    /* Constants used for displaying dialogs */
    private static final int DIALOG_ABOUT = 100;
    private static final int DIALOG_CHANGE_LOG = 101;
    private static final int DIALOG_DONATE = 102;
    private static final int DIALOG_TTS = 103;

    /* Constants used for saving state */
    private static final String CURRENT_FRAG = "CURRENT_FRAG";
    private static final String IS_REFRESHING = "IS_REFRESHING";

    /* PayPal URL */
    @SuppressWarnings("SpellCheckingInspection")
    private static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
            "&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD" +
            "&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
    /* Timer to determine user inactivity for screen dimming in the life counter */
    private static final long INACTIVITY_MS = 30000;
    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;
    private static final String IMAGE_CACHE_DIR = "images";
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
            } else if (s.equals(getString(R.string.key_theme)) || s.endsWith(getString(R.string.key_language))) {
                /* Restart the activity for theme & language changes */
                FamiliarActivity.this.finish();
                startActivity(new Intent(FamiliarActivity.this, FamiliarActivity.class).setAction(Intent.ACTION_MAIN));
            } else if (s.endsWith(getString(R.string.key_imageCacheSize))) {
                /* Close the old cache */
                mImageCache.flush();
                mImageCache.close();

				/* Set up the image cache */
                ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(FamiliarActivity.this, IMAGE_CACHE_DIR);
                cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
                cacheParams.diskCacheSize = 1024 * 1024 * mPreferenceAdapter.getImageCacheSize();
                addImageCache(getSupportFragmentManager(), cacheParams);
            }
        }
    };
    /* Spice setup */
    public final SpiceManager mSpiceManager = new SpiceManager(PriceFetchService.class);
    /* What the drawer menu will be */
    private final DrawerEntry[] mPageEntries = {
            new DrawerEntry(R.string.main_card_search, R.attr.ic_drawer_search, false),
            new DrawerEntry(R.string.main_life_counter, R.attr.ic_drawer_life, false),
            new DrawerEntry(R.string.main_mana_pool, R.attr.ic_drawer_mana, false),
            new DrawerEntry(R.string.main_dice, R.attr.ic_drawer_dice, false),
            new DrawerEntry(R.string.main_trade, R.attr.ic_drawer_trade, false),
            new DrawerEntry(R.string.main_wishlist, R.attr.ic_drawer_wishlist, false),
            new DrawerEntry(R.string.main_timer, R.attr.ic_drawer_timer, false),
            new DrawerEntry(R.string.main_rules, R.attr.ic_drawer_rules, false),
            new DrawerEntry(R.string.main_judges_corner, R.attr.ic_drawer_judge, false),
            new DrawerEntry(R.string.main_mojhosto, R.attr.ic_drawer_mojhosto, false),
            new DrawerEntry(R.string.main_profile, R.attr.ic_drawer_profile, false),
            new DrawerEntry(0, 0, true),
            new DrawerEntry(R.string.main_settings_title, R.attr.ic_drawer_settings, false),
            new DrawerEntry(R.string.main_force_update_title, R.attr.ic_drawer_download, false),
            new DrawerEntry(R.string.main_donate_title, R.attr.ic_drawer_good, false),
            new DrawerEntry(R.string.main_about, R.attr.ic_drawer_about, false),
            new DrawerEntry(R.string.main_whats_new_title, R.attr.ic_drawer_help, false),
            new DrawerEntry(R.string.main_export_data_title, R.attr.ic_drawer_save, false),
            new DrawerEntry(R.string.main_import_data_title, R.attr.ic_drawer_load, false),
    };
    private final Handler mInactivityHandler = new Handler();
    /* Drawer elements */
    public DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    public boolean mIsMenuVisible;
    public PreferenceAdapter mPreferenceAdapter;
    /* Image caching */
    public ImageCache mImageCache;
    private ActionBarDrawerToggle mDrawerToggle;
    /* UI elements */
    private IndeterminateRefreshLayout mRefreshLayout;
    /* Used to pass results between fragments */
    private Bundle mFragResults;
    /* Timer setup */
    private boolean mUpdatingRoundTimer;
    private long mRoundEndTime;
    private Handler mRoundTimerUpdateHandler;
    private int mCurrentFrag;
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
    private DrawerEntryArrayAdapter mPagesAdapter;

    public GoogleApiClient mGoogleApiClient;

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
        ToastWrapper.cancelToast();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        mPreferenceAdapter.unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    /**
     * Called whenever we call supportInvalidateOptionsMenu(). This hides action bar items when the drawer is open
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
        PrefsFragment.checkOverrideSystemLanguage(this);
        mPreferenceAdapter = new PreferenceAdapter(this);

        /* Figure out what theme the app is currently in, and change it if necessary */
        int resourceId = getResourceIdFromAttr(R.attr.color_drawer_background);
        String themeString = "";
        int otherTheme = 0;
        if (resourceId == R.color.drawer_background_dark) {
            otherTheme = R.style.Theme_light;
            themeString = getString(R.string.pref_theme_dark);
        } else if (resourceId == R.color.drawer_background_light) {
            otherTheme = R.style.Theme_dark;
            themeString = getString(R.string.pref_theme_light);
        }

		/* Switch the theme if the preference does not match the current theme */
        if (!themeString.equals(mPreferenceAdapter.getTheme())) {
            this.setTheme(otherTheme);
        }

        /* Set the system bar color programatically, for lollipop+ */
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(getResourceIdFromAttr(R.attr.colorPrimaryDark_attr)));
        }

        setContentView(R.layout.activity_main);

        DatabaseManager.initializeInstance(getApplicationContext());

        mRefreshLayout = ((IndeterminateRefreshLayout) findViewById(R.id.fragment_container));
        mRefreshLayout.setColors(
                getResources().getColor(getResourceIdFromAttr(R.attr.color_common)),
                getResources().getColor(getResourceIdFromAttr(R.attr.color_uncommon)),
                getResources().getColor(getResourceIdFromAttr(R.attr.color_rare)),
                getResources().getColor(getResourceIdFromAttr(R.attr.color_mythic)));

        /* Set default preferences manually so that the listener doesn't do weird things on init */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

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

		/* Get the drawer layout and list */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

		/* set a custom shadow that overlays the main content when the drawer opens */
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		/* set up the drawer's list view with items and click listener */
        mPagesAdapter = new DrawerEntryArrayAdapter(this, mPageEntries);

        mDrawerList.setAdapter(mPagesAdapter);
        mDrawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                boolean shouldCloseDrawer = false;
                switch (mPageEntries[i].mNameResource) {
                    case R.string.main_force_update_title: {
                        if (getNetworkState(true) != -1) {
                            SQLiteDatabase database = DatabaseManager.getInstance(FamiliarActivity.this, true).openDatabase(true);
                            try {
                                CardDbAdapter.dropCreateDB(database);
                                mPreferenceAdapter.setLastLegalityUpdate(0);
                                mPreferenceAdapter.setLastIPGUpdate(0);
                                mPreferenceAdapter.setLastMTRUpdate(0);
                                mPreferenceAdapter.setLastJARUpdate(0);
                                mPreferenceAdapter.setLastRulesUpdate(0);
                                mPreferenceAdapter.setLastTCGNameUpdate("");
                                mPreferenceAdapter.setLastUpdate("");
                                mPreferenceAdapter.setLegalityDate("");
                                startService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                            } catch (FamiliarDbException e) {
                                e.printStackTrace();
                            }
                            DatabaseManager.getInstance(FamiliarActivity.this, true).closeDatabase(true);
                        }
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
                    return true;
                }
                return false;
            }
        });
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
                        break; /* don't close the drawer or change a selection */
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
                    case R.string.main_life_counter:
                    case R.string.main_profile: {
                        selectItem(mPageEntries[i].mNameResource, null, true, false);
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
                        if (getNetworkState(true) != -1) {
                            mPreferenceAdapter.setLastLegalityUpdate(0);
                            startService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                        }
                        shouldCloseDrawer = true;
                        break;
                    }
                    case R.string.main_donate_title: {
                        showDialogFragment(DIALOG_DONATE);
                        shouldCloseDrawer = true;
                        break;
                    }
                    case R.string.main_about: {
                        showDialogFragment(DIALOG_ABOUT);
                        shouldCloseDrawer = true;
                        break;
                    }
                    case R.string.main_whats_new_title: {
                        showDialogFragment(DIALOG_CHANGE_LOG);
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            //toolbar.setCollapsible(true);
            /* I don't like styling in java, but I can't get it to work other ways */
            if (mPreferenceAdapter.getTheme().equals(getString(R.string.pref_theme_light))) {
                toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
            } else {
                toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat);
            }
            toolbar.setSubtitleTextColor(getResources().getColor(android.R.color.white));
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            setSupportActionBar(toolbar);
        }
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar,
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
                    supportInvalidateOptionsMenu();
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("");

        boolean isDeepLink = false;

		/* The activity can be launched a few different ways. Check the intent and show the appropriate fragment */
        /* Only launch a fragment if the app isn't being recreated, i.e. savedInstanceState is null */
        if (savedInstanceState == null) {
            isDeepLink = processIntent(getIntent());
        }

		/* Check to see if the change log should be shown */
        if (!isDeepLink) {
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
                    if (lastVersion != 0) {
                        showDialogFragment(DIALOG_CHANGE_LOG);
                    }
                    mPreferenceAdapter.setLastVersion(pInfo.versionCode);

                    /* Clear the mtr and ipg on update, to replace them with the newly colored versions, but only if we're
                     * updating to 3.0.1 (v24) */
                    if (pInfo.versionCode <= 24) {
                        File mtr = new File(getFilesDir(), JudgesCornerFragment.MTR_LOCAL_FILE);
                        File ipg = new File(getFilesDir(), JudgesCornerFragment.IPG_LOCAL_FILE);
                        File jar = new File(getFilesDir(), JudgesCornerFragment.JAR_LOCAL_FILE);
                        if (mtr.exists()) {
                            if (!mtr.delete()) {
                                ToastWrapper.makeText(this, mtr.getName() + " " + getString(R.string.not_deleted),
                                        ToastWrapper.LENGTH_LONG).show();
                            }
                            if (!ipg.delete()) {
                                ToastWrapper.makeText(this, ipg.getName() + " " + getString(R.string.not_deleted),
                                        ToastWrapper.LENGTH_LONG).show();
                            }
                            if (!jar.delete()) {
                                ToastWrapper.makeText(this, jar.getName() + " " + getString(R.string.not_deleted),
                                        ToastWrapper.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                /* Eat it, don't show change log */
            }
        }

		/* Run the updater service if there is a network connection */
        if (getNetworkState(false) != -1 && mPreferenceAdapter.getAutoUpdate()) {
			/* Only update the banning list if it hasn't been updated recently */
            long curTime = System.currentTimeMillis();
            int updateFrequency = Integer.valueOf(mPreferenceAdapter.getUpdateFrequency());
            int lastLegalityUpdate = mPreferenceAdapter.getLastLegalityUpdate();
			/* days to ms */
            if (((curTime / 1000) - lastLegalityUpdate) > (updateFrequency * 24 * 60 * 60)) {
                startService(new Intent(this, DbUpdaterService.class));
            }
        }

		/* Set up the image cache */
        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory
        cacheParams.diskCacheSize = 1024 * 1024 * mPreferenceAdapter.getImageCacheSize();
        addImageCache(getSupportFragmentManager(), cacheParams);

        /* Set up app indexing */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppIndex.API)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    private boolean processIntent(Intent intent) {
        boolean isDeepLink = false;

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			/* Do a search by name, launched from the quick search */
            String query = intent.getStringExtra(SearchManager.QUERY);
            Bundle args = new Bundle();
            SearchCriteria sc = new SearchCriteria();
            sc.name = query;
            args.putSerializable(SearchViewFragment.CRITERIA, sc);
            selectItem(R.string.main_card_search, args, false, true); /* Don't clear backstack, do force the intent */

        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {

            boolean shouldSelectItem = true;

            Uri data = intent.getData();
            Bundle args = new Bundle();
            assert data != null;

            boolean shouldClearFragmentStack = true; /* Clear backstack for deep links */
            if (data.getAuthority().toLowerCase().contains("gatherer.wizards")) {
                SQLiteDatabase database = DatabaseManager.getInstance(this, false).openDatabase(false);
                try {
                    String queryParam;
                    if ((queryParam = data.getQueryParameter("multiverseid")) != null) {
                        Cursor cursor = CardDbAdapter.fetchCardByMultiverseId(Long.parseLong(queryParam),
                                new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);
                        if (cursor.getCount() != 0) {
                            isDeepLink = true;
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                                    new long[]{cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                        }
                        cursor.close();
                        if (args.size() == 0) {
                            throw new Exception("Not Found");
                        }
                    } else if ((queryParam = data.getQueryParameter("name")) != null) {
                        Cursor cursor = CardDbAdapter.fetchCardByName(queryParam,
                                new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);
                        if (cursor.getCount() != 0) {
                            isDeepLink = true;
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                                    new long[]{cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                        }
                        cursor.close();
                        if (args.size() == 0) {
                            throw new Exception("Not Found");
                        }
                    } else {
                        throw new Exception("Not Found");
                    }
                } catch (Exception e) {
                    /* empty cursor, just return */
                    ToastWrapper.makeText(this, R.string.no_results_found, ToastWrapper.LENGTH_LONG).show();
                    this.finish();
                    shouldSelectItem = false;
                } finally {
                    DatabaseManager.getInstance(this, false).closeDatabase(false);
                }
            } else if (data.getAuthority().contains("CardSearchProvider")) {
    			/* User clicked a card in the quick search autocomplete, jump right to it */
                args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                        new long[]{Long.parseLong(data.getLastPathSegment())});
                shouldClearFragmentStack = false; /* Don't clear backstack for search intents */
            } else {
                /* User clicked a deep link, jump to the card(s) */
                isDeepLink = true;

                SQLiteDatabase database = DatabaseManager.getInstance(this, false).openDatabase(false);
                try {
                    Cursor cursor = null;
                    boolean screenLaunched = false;
                    if (data.getScheme().toLowerCase().equals("card") &&
                            data.getAuthority().toLowerCase().equals("multiverseid")) {
                        if (data.getLastPathSegment() == null) {
                            /* Home screen deep link */
                            launchHomeScreen();
                            screenLaunched = true;
                            shouldSelectItem = false;
                        } else {
                            try {
                                cursor = CardDbAdapter.fetchCardByMultiverseId(Long.parseLong(data.getLastPathSegment()),
                                        new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);
                            } catch (NumberFormatException e) {
                                cursor = null;
                            }
                        }
                    }

                    if (cursor != null) {
                        if (cursor.getCount() != 0) {
                            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                                    new long[]{cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                        } else {
                            /* empty cursor, just return */
                            ToastWrapper.makeText(this, R.string.no_results_found, ToastWrapper.LENGTH_LONG).show();
                            this.finish();
                            shouldSelectItem = false;
                        }
                        cursor.close();
                    } else if (!screenLaunched) {
                        /* null cursor, just return */
                        ToastWrapper.makeText(this, R.string.no_results_found, ToastWrapper.LENGTH_LONG).show();
                        this.finish();
                        shouldSelectItem = false;
                    }
                } catch (FamiliarDbException e) {
                    e.printStackTrace();
                }
                DatabaseManager.getInstance(this, false).closeDatabase(false);
            }
            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
            if (shouldSelectItem) {
                selectItem(R.string.main_card_search, args, shouldClearFragmentStack, true);
            }
        } else if (ACTION_ROUND_TIMER.equals(intent.getAction())) {
            selectItem(R.string.main_timer, null, true, false);
        } else if (ACTION_CARD_SEARCH.equals(intent.getAction())) {
            selectItem(R.string.main_card_search, null, true, false);
        } else if (ACTION_LIFE.equals(intent.getAction())) {
            selectItem(R.string.main_life_counter, null, true, false);
        } else if (ACTION_DICE.equals(intent.getAction())) {
            selectItem(R.string.main_dice, null, true, false);
        } else if (ACTION_TRADE.equals(intent.getAction())) {
            selectItem(R.string.main_trade, null, true, false);
        } else if (ACTION_MANA.equals(intent.getAction())) {
            selectItem(R.string.main_mana_pool, null, true, false);
        } else if (ACTION_WISH.equals(intent.getAction())) {
            selectItem(R.string.main_wishlist, null, true, false);
        } else if (ACTION_RULES.equals(intent.getAction())) {
            selectItem(R.string.main_rules, null, true, false);
        } else if (ACTION_JUDGE.equals(intent.getAction())) {
            selectItem(R.string.main_judges_corner, null, true, false);
        } else if (ACTION_MOJHOSTO.equals(intent.getAction())) {
            selectItem(R.string.main_mojhosto, null, true, false);
        } else if (ACTION_PROFILE.equals(intent.getAction())) {
            selectItem(R.string.main_profile, null, true, false);
        } else if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            /* App launched as regular, show the default fragment if there isn't one already */
            if (getSupportFragmentManager().getFragments() == null) {
                launchHomeScreen();
            }
        } else {
            /* Some unknown intent, just finish */
            finish();
        }

        mDrawerList.setItemChecked(mCurrentFrag, true);
        return isDeepLink;
    }

    /**
     * Launch the home fragment, based on a preference
     */
    private void launchHomeScreen() {
        String defaultFragment = mPreferenceAdapter.getDefaultFragment();

        if (defaultFragment.equals(this.getString(R.string.main_card_search))) {
            selectItem(R.string.main_card_search, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_life_counter))) {
            selectItem(R.string.main_life_counter, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_mana_pool))) {
            selectItem(R.string.main_mana_pool, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_dice))) {
            selectItem(R.string.main_dice, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_trade))) {
            selectItem(R.string.main_trade, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_wishlist))) {
            selectItem(R.string.main_wishlist, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_timer))) {
            selectItem(R.string.main_timer, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_rules))) {
            selectItem(R.string.main_rules, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_judges_corner))) {
            selectItem(R.string.main_judges_corner, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_mojhosto))) {
            selectItem(R.string.main_mojhosto, null, true, false);
        } else if (defaultFragment.equals(this.getString(R.string.main_profile))) {
            selectItem(R.string.main_profile, null, true, false);
        } else {
            selectItem(R.string.main_card_search, null, true, false);
        }
    }

    /**
     * Instead of starting a new Activity, any intents to start a new Familiar Activity will be
     * received here, and this Activity should react properly
     *
     * @param intent The intent used to "start" this Activity
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    /**
     * Check to see if we should display the round timer in the actionbar, and start the inactivity timer
     */
    @Override
    protected void onResume() {
        super.onResume();
        PrefsFragment.checkOverrideSystemLanguage(this);
        if (mRoundEndTime != -1) {
            startUpdatingDisplay();
        }
        mInactivityHandler.postDelayed(userInactive, INACTIVITY_MS);
        mPagesAdapter.notifyDataSetChanged(); /* To properly color icons when popping activities */
    }

    /**
     * Select an item from the drawer menu. This will highlight the entry and manage fragment transactions.
     *
     * @param resId The string resource ID of the entry
     */
    private void selectItem(int resId, Bundle args, boolean shouldClearFragmentStack, boolean forceSelect) {

        int position = 0;
        for (DrawerEntry entry : mPageEntries) {
            if (resId == entry.mNameResource) {
                break;
            }
            position++;
        }

        mCurrentFrag = position;
        Fragment newFrag;
		/* Pick the new fragment */
        switch (resId) {
            case R.string.main_card_search: {
				/* If this is a quick search intent, launch either the card view or result list directly */
                if (args != null && args.containsKey(CardViewPagerFragment.CARD_ID_ARRAY)) {
                    newFrag = new CardViewPagerFragment();
                } else if (args != null && args.containsKey(SearchViewFragment.CRITERIA)) {
                    newFrag = new ResultListFragment();
                } else {
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
            case R.string.main_profile: {
                newFrag = new ProfileFragment();
                break;
            }
            default:
                return;
        }

        try {
            if (!forceSelect && ((Object) newFrag).getClass().equals(((Object) getSupportFragmentManager().findFragmentById(R.id.fragment_container)).getClass())) {
			    /* This is the same fragment, just close the menu */
                mDrawerLayout.closeDrawer(mDrawerList);
                return;
            }
        } catch (NullPointerException e) {
			/* no fragment to compare to */
        }

        if (args != null) {
            newFrag.setArguments(args);
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft;
        if (fm != null) {
            if (shouldClearFragmentStack) {
			    /* Remove any current fragments on the back stack */
                for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                    fm.popBackStack();
                }
            }

			/* Begin a new transaction */
            ft = fm.beginTransaction();

			/* Replace or add the fragment */
            ft.replace(R.id.fragment_container, newFrag, FamiliarActivity.FRAGMENT_TAG);
            if (!shouldClearFragmentStack) {
                ft.addToBackStack(null);
            }
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
        /* Dinky workaround for LG phones: https://code.google.com/p/android/issues/detail?id=78154 */
        else if ((keyCode == KeyEvent.KEYCODE_MENU) /* &&
                (Build.VERSION.SDK_INT == 16) &&
                (Build.MANUFACTURER.compareTo("LGE") == 0) */) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Called when a key was released and not handled by any of the views inside of the activity.
     * So, for example, key presses while the cursor is inside a TextView will not trigger the event
     * (unless it is a navigation to another object) because TextView handles its own key presses.
     * The default implementation handles KEYCODE_BACK to stop the activity and go back.
     * This has a dinky workaround for LG phones
     *
     * @param keyCode The value in event.getKeyCode().
     * @param event   Description of the key event.
     * @return If you handled the event, return true. If you want to allow the event to be handled
     * by the next receiver, return false.
     */
    @Override
    public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            if (toolbar != null) {
                if (toolbar.isOverflowMenuShowing()) {
                    toolbar.dismissPopupMenus();
                } else {
                    toolbar.showOverflowMenu();
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
     * If TTS couldn't init, show this dialog. It will only display once as to not annoy people
     */
    public void showTtsDialog() {
        if (mPreferenceAdapter.getTtsShowDialog()) {
            showDialogFragment(FamiliarActivity.DIALOG_TTS);
            mPreferenceAdapter.setTtsShowDialog();
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
    private void showDialogFragment(final int id) throws IllegalStateException {
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
            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);
                AlertDialogPro.Builder builder = new AlertDialogPro.Builder(this.getActivity());

                assert getPackageManager() != null;

                switch (id) {
                    case DIALOG_ABOUT: {

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
								/* Just close the dialog */
                            }
                        });

						/* Set the custom view, with some images below the text */
                        LayoutInflater inflater = this.getActivity().getLayoutInflater();
                        View dialogLayout = inflater.inflate(R.layout.activity_dialog_about, null, false);
                        assert dialogLayout != null;
                        TextView text = (TextView) dialogLayout.findViewById(R.id.aboutfield);
                        text.setText(ImageGetterHelper.formatHtmlString(getString(R.string.main_about_text)));
                        text.setMovementMethod(LinkMovementMethod.getInstance());
                        builder.setView(dialogLayout);

                        return builder.create();
                    }
                    case DIALOG_CHANGE_LOG: {
                        try {
                            builder.setTitle(getString(R.string.main_whats_new_in_title) + " " +
                                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                        } catch (PackageManager.NameNotFoundException e) {
                            builder.setTitle(R.string.main_whats_new_title);
                        }

                        builder.setNeutralButton(R.string.dialog_enjoy, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
								/* Just close the dialog */
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
                        builder.setView(dialogLayout);

                        return builder.create();
                    }
                    case DIALOG_DONATE: {
						/* Set the title */
                        builder.setTitle(R.string.main_donate_dialog_title);
						/* Set the buttons button */
                        builder.setNegativeButton(R.string.dialog_thanks_anyway, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
								/* Just close the dialog */
                            }
                        });
                        builder.setPositiveButton(R.string.main_donate_title, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL));
                                startActivity(myIntent);
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

                        builder.setView(dialogLayout);
                        return builder.create();
                    }
                    case DIALOG_TTS: {
						/* Then display a dialog informing them of TTS */

                        builder.setTitle(R.string.main_tts_warning_title)
                                .setMessage(R.string.main_tts_warning_text)
                                .setPositiveButton(R.string.main_install_tts, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
										/* TTS couldn't init, try installing TTS data */
                                        try {
                                            Intent installIntent = new Intent();
                                            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                            startActivity(installIntent);
                                        } catch (ActivityNotFoundException e) {
											/* TTS not even installed */
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.parse("market://details?id=com.google.android.tts"));
                                            startActivity(intent);
                                        }
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
                        return DontShowDialog();
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
                if (id == DIALOG_CHANGE_LOG) {
                    if (mPreferenceAdapter.getBounceDrawer()) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mDrawerLayout.openDrawer(mDrawerList);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDrawerLayout.closeDrawer(mDrawerList);
                                        mPreferenceAdapter.setBounceDrawer();
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
     * Show the timer in the action bar, set up the handler to update the displayed time every second.
     */
    public void startUpdatingDisplay() {
        mRoundEndTime = mPreferenceAdapter.getRoundTimerEnd();
        mUpdatingRoundTimer = true;

        mRoundTimerUpdateHandler.removeCallbacks(timerUpdate);
        mRoundTimerUpdateHandler.postDelayed(timerUpdate, 1);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    /**
     * Clear the timer from the action bar, stop the handler from updating it.
     */
    public void stopUpdatingDisplay() {
        mRoundEndTime = -1;
        mUpdatingRoundTimer = false;

        mRoundTimerUpdateHandler.removeCallbacks(timerUpdate);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
                } else {
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
        mInactivityHandler.postDelayed(userInactive, INACTIVITY_MS);
        mUserInactive = false;
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
                } else {
					/* This is a slight hack to handle the fact that it always rounds down. It will start the timer at
					   50:00 instead of 49:59, or whatever */
                    timeLeftSeconds++;
                    timeLeftStr = String.format("%02d:%02d:%02d",
                            timeLeftSeconds / 3600,
                            (timeLeftSeconds % 3600) / 60,
                            timeLeftSeconds % 60);
                }

                if (mUpdatingRoundTimer) {
                    assert getSupportActionBar() != null;
                    getSupportActionBar().setTitle(timeLeftStr);
                }
                mRoundTimerUpdateHandler.postDelayed(timerUpdate, 1000);
            } else {
                stopUpdatingDisplay();
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
                if (fragment instanceof RoundTimerFragment) {
                    ((RoundTimerFragment) fragment).timerEnded();
                }
            }
        }
    };

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

    /**
     * This helper function translates an attribute into a resource ID
     *
     * @param attr The attribute ID
     * @return the resource ID
     */
    public int getResourceIdFromAttr(int attr) {
        assert getTheme() != null;
        TypedArray ta = getTheme().obtainStyledAttributes(new int[]{attr});
        assert ta != null;
        int resId = ta.getResourceId(0, 0);
        ta.recycle();
        return resId;
    }

    /**
     * Checks the networks state
     *
     * @param shouldShowToast true, if you want a Toast to be shown indicating a lack of network
     * @return -1 if there is no network connection, or the type of network, like ConnectivityManager.TYPE_WIFI
     */
    public int getNetworkState(boolean shouldShowToast) {
        try {
            ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            for (NetworkInfo ni : conMan.getAllNetworkInfo()) {
                if (ni.isConnected()) {
                    return ni.getType();
                }
            }
            if (shouldShowToast) {
                ToastWrapper.makeText(FamiliarActivity.this, R.string.no_network, ToastWrapper.LENGTH_SHORT).show();
            }
            return -1;
        } catch (NullPointerException e) {
            if (shouldShowToast) {
                ToastWrapper.makeText(FamiliarActivity.this, R.string.no_network, ToastWrapper.LENGTH_SHORT).show();
            }
            return -1;
        }
    }

    private void addImageCache(FragmentManager fragmentManager,
                               ImageCache.ImageCacheParams cacheParams) {
        mImageCache = ImageCache.getInstance(fragmentManager, cacheParams);
        new CacheAsyncTask().execute(MESSAGE_INIT_DISK_CACHE);
    }

    private void initDiskCacheInternal() {
        if (mImageCache != null) {
            mImageCache.initDiskCache();
        }
    }

    private void clearCacheInternal() {
        if (mImageCache != null) {
            mImageCache.clearCache();
        }
    }

	/*
	 * Image Caching
	 */

    private void flushCacheInternal() {
        if (mImageCache != null) {
            mImageCache.flush();
        }
    }

    private void closeCacheInternal() {
        if (mImageCache != null) {
            mImageCache.close();
            mImageCache = null;
        }
    }

    /**
     * This nested class encapsulates the necessary information for an entry in the drawer menu
     */
    public class DrawerEntry {
        final int mNameResource;
        final int mIconResource;
        final boolean mIsDivider;

        public DrawerEntry(int nameResource, int iconResource, boolean isHeader) {
            mNameResource = nameResource;
            mIconResource = iconResource;
            mIsDivider = isHeader;
        }
    }

    /**
     * This nested class is the adapter which populates the listView in the drawer menu. It handles both entries and
     * headers
     */
    public class DrawerEntryArrayAdapter extends ArrayAdapter<DrawerEntry> {
        private final DrawerEntry[] values;
        private Drawable mHighlightedDrawable;

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
            if (values[position].mIsDivider) {
                layout = R.layout.drawer_list_divider;
            } else {
                layout = R.layout.drawer_list_item;
            }
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(layout, parent, false);
            }

            assert convertView != null;
            if (values[position].mIsDivider) {
				/* Make sure the recycled view is the right type, inflate a new one if necessary */
                if (convertView.findViewById(R.id.divider) == null) {
                    convertView = getLayoutInflater().inflate(layout, parent, false);
                }
                assert convertView != null;
                convertView.setFocusable(false);
                convertView.setFocusableInTouchMode(false);
            } else {
				/* Make sure the recycled view is the right type, inflate a new one if necessary */
                if (convertView.findViewById(R.id.drawer_entry_name) == null) {
                    convertView = getLayoutInflater().inflate(layout, parent, false);
                }
                assert convertView != null;
                ((TextView) convertView.findViewById(R.id.drawer_entry_name)).setText(values[position].mNameResource);
                ((TextView) convertView.findViewById(R.id.drawer_entry_name)).setCompoundDrawablesWithIntrinsicBounds(getResourceIdFromAttr(values[position].mIconResource), 0, 0, 0);
                /* Color the initial icon */
                if (mCurrentFrag == position) {
                    colorDrawerEntry(((TextView) convertView.findViewById(R.id.drawer_entry_name)));
                } else {
                    ((TextView) convertView.findViewById(R.id.drawer_entry_name)).getCompoundDrawables()[0].setColorFilter(null);
                }
            }

            return convertView;
        }

        /**
         * Applies the primary color to the selected icon in the drawer
         *
         * @param textView The TextView to color
         */
        void colorDrawerEntry(TextView textView) {
            if (mHighlightedDrawable != null) {
                mHighlightedDrawable.setColorFilter(null);
            }
            mHighlightedDrawable = textView.getCompoundDrawables()[0];
            mHighlightedDrawable.setColorFilter(getResources().getColor(getResourceIdFromAttr(R.attr.colorPrimary_attr)), PorterDuff.Mode.SRC_IN);
        }
    }

    private class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            switch ((Integer) params[0]) {
                case MESSAGE_CLEAR:
                    clearCacheInternal();
                    break;
                case MESSAGE_INIT_DISK_CACHE:
                    initDiskCacheInternal();
                    break;
                case MESSAGE_FLUSH:
                    flushCacheInternal();
                    break;
                case MESSAGE_CLOSE:
                    closeCacheInternal();
                    break;
            }
            return null;
        }
    }
}