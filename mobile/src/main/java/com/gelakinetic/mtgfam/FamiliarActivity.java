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

package com.gelakinetic.mtgfam;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.CardViewPagerFragment;
import com.gelakinetic.mtgfam.fragments.DeckCounterFragment;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.GatheringsFragment;
import com.gelakinetic.mtgfam.fragments.HtmlDocFragment;
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
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarActivityDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.helpers.FamiliarLogger;
import com.gelakinetic.mtgfam.helpers.MTGFamiliarAppWidgetProvider;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.ZipUtils;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceFetcher;
import com.gelakinetic.mtgfam.helpers.updaters.DbUpdaterService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

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
    public static final String ACTION_DECKLIST = "android.intent.action.DECKLIST";

    /* Used to request permissions */
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_IMAGE = 77;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_BACKUP = 78;
    public static final int REQUEST_READ_EXTERNAL_STORAGE_BACKUP = 79;

    /* Constants used for saving state */
    private static final String CURRENT_FRAG = "CURRENT_FRAG";
    private static final String IS_REFRESHING = "IS_REFRESHING";
    //    /* PayPal URL */
//    @SuppressWarnings("SpellCheckingInspection")
//    public static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
//            "&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD" +
//            "&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
    /* Timer to determine user inactivity for screen dimming in the life counter */
    private static final long INACTIVITY_MS = 30000;
    /* Spice setup */
    public final MarketPriceFetcher mMarketPriceStore = new MarketPriceFetcher(this);

    /* What the drawer menu will be */
    private final DrawerEntry[] mPageEntries = {
            new DrawerEntry(R.string.main_card_search, R.attr.ic_drawer_search, false, new Class[]{SearchViewFragment.class, ResultListFragment.class, CardViewPagerFragment.class, CardViewFragment.class}),
            new DrawerEntry(R.string.main_life_counter, R.attr.ic_drawer_life, false, new Class[]{LifeCounterFragment.class, GatheringsFragment.class}),
            new DrawerEntry(R.string.main_mana_pool, R.attr.ic_drawer_mana, false, new Class[]{ManaPoolFragment.class}),
            new DrawerEntry(R.string.main_dice, R.attr.ic_drawer_dice, false, new Class[]{DiceFragment.class}),
            new DrawerEntry(R.string.main_trade, R.attr.ic_drawer_trade, false, new Class[]{TradeFragment.class}),
            new DrawerEntry(R.string.main_wishlist, R.attr.ic_drawer_wishlist, false, new Class[]{WishlistFragment.class}),
            new DrawerEntry(R.string.main_decklist, R.attr.ic_drawer_deck, false, new Class[]{DecklistFragment.class}),
            new DrawerEntry(R.string.main_timer, R.attr.ic_drawer_timer, false, new Class[]{RoundTimerFragment.class}),
            new DrawerEntry(R.string.main_rules, R.attr.ic_drawer_rules, false, new Class[]{RulesFragment.class}),
            new DrawerEntry(R.string.main_judges_corner, R.attr.ic_drawer_judge, false, new Class[]{JudgesCornerFragment.class, DeckCounterFragment.class, HtmlDocFragment.class}),
            new DrawerEntry(R.string.main_mojhosto, R.attr.ic_drawer_mojhosto, false, new Class[]{MoJhoStoFragment.class}),
            new DrawerEntry(0, 0, true, null),
            new DrawerEntry(R.string.main_settings_title, R.attr.ic_drawer_settings, false, null),
            new DrawerEntry(R.string.main_force_update_title, R.attr.ic_drawer_download, false, null),
//            new DrawerEntry(R.string.main_donate_title, R.attr.ic_drawer_good, false, null),
            new DrawerEntry(R.string.main_about, R.attr.ic_drawer_about, false, null),
            new DrawerEntry(R.string.main_whats_new_title, R.attr.ic_drawer_help, false, null),
            new DrawerEntry(R.string.main_export_data_title, R.attr.ic_drawer_save, false, null),
            new DrawerEntry(R.string.main_import_data_title, R.attr.ic_drawer_load, false, null),
    };
    private final Handler mInactivityHandler = new Handler();
    /* Drawer elements */
    public DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    public boolean mIsMenuVisible;

    /* Listen for changes to preferences */
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = (sharedPreferences, s) -> {
        if (s.equals(getString(R.string.key_widgetButtons))) {
            Intent intent = new Intent(FamiliarActivity.this, MTGFamiliarAppWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            assert AppWidgetManager.getInstance(getApplication()) != null;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
            assert appWidgetManager != null;
            int[] ids = appWidgetManager.getAppWidgetIds(
                    new ComponentName(getApplication(), MTGFamiliarAppWidgetProvider.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        } else if (s.equals(getString(R.string.key_theme)) || s.equals(getString(R.string.key_language))) {
            /* Restart the activity for theme & language changes */
            FamiliarActivity.this.finish();
            startActivity(new Intent(FamiliarActivity.this, FamiliarActivity.class).setAction(Intent.ACTION_MAIN));
        }
    };
    private ActionBarDrawerToggle mDrawerToggle;
    /* UI elements */
    private SmoothProgressBar mSmoothProgressBar;
    private boolean mIsLoading = false;
    /* Used to pass results between fragments */
    private Bundle mFragResults;
    /* Timer setup */
    private boolean mUpdatingRoundTimer;
    private long mRoundEndTime;
    private Handler mRoundTimerUpdateHandler;

    /*
     * We need this to allow TextView drawables for any API under 21.
     */
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    /**
     * This runnable is posted with a handler every second. It displays the time left in the action
     * bar as the title. If the time runs out, it will stop updating the display and notify the
     * fragment, if it is a RoundTimerFragment.
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
                    /* This is a slight hack to handle the fact that it always rounds down. It will
                       start the timer at 50:00 instead of 49:59, or whatever */
                    timeLeftSeconds++;
                    timeLeftStr = String.format(Locale.US, "%02d:%02d:%02d",
                            timeLeftSeconds / 3600,
                            (timeLeftSeconds % 3600) / 60,
                            timeLeftSeconds % 60);
                }

                if (mUpdatingRoundTimer) {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setTitle(timeLeftStr);
                    }
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
    private int mCurrentFrag;
    private boolean mUserInactive = false;
    private final Runnable userInactive = () -> {
        mUserInactive = true;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment instanceof FamiliarFragment) {
            ((FamiliarFragment) fragment).onUserInactive();
        }
    };
    private DrawerEntryArrayAdapter mPagesAdapter;

    /**
     * Open an inputStream to the HTML content at the given URL.
     *
     * @param stringUrl The URL to open a stream to, in String form
     * @param logWriter A PrintWriter to log debug info to. Can be null
     * @param ctx       A context to build the User Agent with
     * @return An InputStream to the content at the URL, or null
     * @throws IOException Thrown if something goes terribly wrong
     */
    public static
    @Nullable
    InputStream getHttpInputStream(String stringUrl, PrintWriter logWriter, Context ctx) throws IOException {
        return getHttpInputStream(new URL(stringUrl), logWriter, ctx, 0);
    }

    /**
     * Open an inputStream to the HTML content at the given URL.
     *
     * @param url       The URL to open a stream to
     * @param logWriter A PrintWriter to log debug info to. Can be null
     * @param ctx       A context to build the User Agent with
     * @return An InputStream to the content at the URL, or null
     * @throws IOException Thrown if something goes terribly wrong
     */
    public static
    @Nullable
    InputStream getHttpInputStream(URL url, PrintWriter logWriter, Context ctx) throws IOException {
        return getHttpInputStream(url, logWriter, ctx, 0);
    }

    /**
     * Open an inputStream to the HTML content at the given URL, making recursive calls for
     * redirection (HTTP 301, 302).
     *
     * @param url            The URL to open a stream to
     * @param logWriter      A PrintWriter to log debug info to. Can be null
     * @param ctx            A context to build the User Agent with
     * @param recursionLevel The redirect recursion level. Starts at 0, doesn't go past 10
     * @return An InputStream to the content at the URL, or null
     * @throws IOException Thrown if something goes terribly wrong
     */
    private static
    @Nullable
    InputStream getHttpInputStream(URL url, @Nullable PrintWriter logWriter, Context ctx,
                                   int recursionLevel) throws IOException {

        /* Don't allow infinite recursion */
        if (recursionLevel > 10) {
            return null;
        }

        /* Make the URL & connection objects, follow redirects, timeout after 5s */
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection = (HttpURLConnection) (url).openConnection();
        String version = "";
        try {
            version = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        connection.setRequestProperty("User-Agent", ctx.getString(R.string.app_name) + "/" + version);
        connection.setConnectTimeout(5000);
        connection.setInstanceFollowRedirects(true);

        /* If the connection is not OK, debug print the response */
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            /* Log the URL and response code */
            if (logWriter != null) {
                logWriter.write("URL : " + url.toString() + '\n');
                logWriter.write("RESP: " + connection.getResponseCode() + '\n');
            }

            /* Comb through header fields for a redirect location */
            URL nextUrl = null;
            for (String key : connection.getHeaderFields().keySet()) {
                /* Log the header */
                if (logWriter != null) {
                    logWriter.write("HDR : [" + key + "] " + connection.getHeaderField(key) + '\n');
                }

                /* Found the URL to try next */
                if (key != null && key.equalsIgnoreCase("location")) {
                    nextUrl = new URL(connection.getHeaderField(key));
                }
            }

            /* If the next location is still null, comb through the HTML
             * This is kind of a hack for when sites.google.com is serving up malformed 302
             * redirects and all the header fields end up being in this input stream
             */
            if (nextUrl == null) {
                /* Open the stream */
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line;
                int linesRead = 0;
                /* Read one line at a time */
                while ((line = br.readLine()) != null) {
                    /* Log the line */
                    if (logWriter != null) {
                        logWriter.write("HTML:" + line + '\n');
                    }
                    /* Check for a location */
                    if (line.toLowerCase().contains("location")) {
                        nextUrl = new URL(line.split("\\s+")[1]);
                        break;
                    }
                    /* Count the line, make sure to quit after 1000 */
                    linesRead++;
                    if (linesRead > 1000) {
                        break;
                    }
                }
            }

            if (nextUrl != null) {
                /* If there is a URL to follow, follow it */
                return getHttpInputStream(nextUrl, logWriter, ctx, recursionLevel + 1);
            } else {
                /* Otherwise return null */
                return null;
            }

        } else {
            /* HTTP response is A-OK. Return the inputStream */
            return connection.getInputStream();
        }
    }

    /**
     * Stop the Spice Manager when the activity stops.
     */
    @Override
    protected void onStop() {
        super.onStop();
        SnackbarWrapper.cancelSnackbar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceAdapter.unregisterOnSharedPreferenceChangeListener(this, mPreferenceChangeListener);
    }

    /**
     * Called whenever we call invalidateOptionsMenu(). This hides action bar items when the
     * drawer is open.
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
     * Set up drawer menu.
     * Run updater service.
     * Check for, and potentially start, round timer.
     * Check for TTS support.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied
     *                           in onSaveInstanceState(Bundle).
     *                           Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save this for static access by loggers
        FamiliarLogger.initLogger(this);

        PrefsFragment.checkOverrideSystemLanguage(this);

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
        if (!themeString.equals(PreferenceAdapter.getTheme(this))) {
            this.setTheme(otherTheme);
        }

        /* Set the system bar color programatically, for lollipop+ */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, getResourceIdFromAttr(R.attr.colorPrimaryDark_attr)));
        }

        setContentView(R.layout.activity_main);

        DatabaseManager.initializeInstances(getApplicationContext());

        mSmoothProgressBar = findViewById(R.id.smooth_progress_bar);
        mSmoothProgressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(this)
                .colors(new int[]{
                        ContextCompat.getColor(this, getResourceIdFromAttr(R.attr.color_common)),
                        ContextCompat.getColor(this, getResourceIdFromAttr(R.attr.color_uncommon)),
                        ContextCompat.getColor(this, getResourceIdFromAttr(R.attr.color_rare)),
                        ContextCompat.getColor(this, getResourceIdFromAttr(R.attr.color_mythic))})
                .interpolator(new AccelerateDecelerateInterpolator())
                .sectionsCount(4)
                .separatorLength(0)
                .progressiveStartSpeed(1.5f)
                .progressiveStopSpeed(1.5f)
                .progressiveStart(true)
                .strokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()))
                .build());

        mSmoothProgressBar.setSmoothProgressDrawableCallbacks(new SmoothProgressDrawable.Callbacks() {
            @Override
            public void onStop() {
                mSmoothProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onStart() {
                mSmoothProgressBar.setVisibility(View.VISIBLE);
            }
        });
        mSmoothProgressBar.setVisibility(View.GONE);
        clearLoading();

        /* Set default preferences manually so that the listener doesn't do weird things on init */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        /* Set up a listener to update the home screen widget whenever the user changes the
           preference */
        PreferenceAdapter.registerOnSharedPreferenceChangeListener(this, mPreferenceChangeListener);

        /* Create the handler to update the timer in the action bar */
        mRoundTimerUpdateHandler = new Handler();

        /* Check if we should make the timer notification */
        mRoundEndTime = PreferenceAdapter.getRoundTimerEnd(this);
        if (mRoundEndTime != -1) {
            RoundTimerFragment.showTimerRunningNotification(this, mRoundEndTime);
            RoundTimerFragment.setOrCancelAlarms(this, mRoundEndTime, true);
        }
        mUpdatingRoundTimer = false;

        /* Get the drawer layout and list */
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);

        /* set a custom shadow that overlays the main content when the drawer opens */
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        /* set up the drawer's list view with items and click listener */
        mPagesAdapter = new DrawerEntryArrayAdapter(this);

        mDrawerList.setAdapter(mPagesAdapter);
        mDrawerList.setOnItemLongClickListener((adapterView, view, i, l) -> {
            boolean shouldCloseDrawer = false;
            if (mPageEntries[i].mNameResource == R.string.main_about) {
                showDialogFragment(FamiliarActivityDialogFragment.DIALOG_LOGGING);
                shouldCloseDrawer = true;
            } else if (mPageEntries[i].mNameResource == R.string.main_force_update_title) {
                if (getNetworkState(FamiliarActivity.this, true) != -1) {
                    FamiliarDbHandle handle = new FamiliarDbHandle();
                    try {
                        SQLiteDatabase database = DatabaseManager.openDatabase(FamiliarActivity.this, true, handle);
                        CardDbAdapter.dropCreateDB(database);
                        PreferenceAdapter.setLastLegalityUpdate(FamiliarActivity.this, 0);
                        PreferenceAdapter.setLastIPGUpdate(FamiliarActivity.this, 0);
                        PreferenceAdapter.setLastMTRUpdate(FamiliarActivity.this, 0);
                        PreferenceAdapter.setLastJARUpdate(FamiliarActivity.this, 0);
                        PreferenceAdapter.setLastRulesUpdate(FamiliarActivity.this, 0);
                        PreferenceAdapter.setLegalityTimestamp(FamiliarActivity.this, 0);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                        } else {
                            startService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                        }
                    } catch (SQLiteException | FamiliarDbException | IllegalStateException e) {
                        e.printStackTrace();
                    } finally {
                        DatabaseManager.closeDatabase(FamiliarActivity.this, handle);
                    }
                }
                shouldCloseDrawer = true;
            }

            mDrawerList.setItemChecked(mCurrentFrag, true);
            if (shouldCloseDrawer) {
                (new Handler()).postDelayed(() -> mDrawerLayout.closeDrawer(mDrawerList), 50);
                return true;
            }
            return false;
        });
        mDrawerList.setOnItemClickListener((adapterView, view, i, l) -> {
            /* FamiliarFragments will automatically close the drawer when they hit onResume().
               It's more precise than a delayed handler. Other options have to close the drawer
               themselves */
            boolean shouldCloseDrawer = false;
            //noinspection StatementWithEmptyBody
            if ((mPageEntries[i].mNameResource == R.string.main_extras) ||
                    (mPageEntries[i].mNameResource == R.string.main_pages)) {
                /* It's a header */
                /* don't close the drawer or change a selection */
            } else if ((mPageEntries[i].mNameResource == R.string.main_mana_pool)
                    || (mPageEntries[i].mNameResource == R.string.main_dice)
                    || (mPageEntries[i].mNameResource == R.string.main_trade)
                    || (mPageEntries[i].mNameResource == R.string.main_wishlist)
                    || (mPageEntries[i].mNameResource == R.string.main_decklist)
                    || (mPageEntries[i].mNameResource == R.string.main_timer)
                    || (mPageEntries[i].mNameResource == R.string.main_rules)
                    || (mPageEntries[i].mNameResource == R.string.main_judges_corner)
                    || (mPageEntries[i].mNameResource == R.string.main_mojhosto)
                    || (mPageEntries[i].mNameResource == R.string.main_card_search)
                    || (mPageEntries[i].mNameResource == R.string.main_life_counter)) {
                selectItem(mPageEntries[i].mNameResource, null, true, false);
            } else if (mPageEntries[i].mNameResource == R.string.main_settings_title) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                ft.replace(R.id.fragment_container, new PrefsFragment(), FamiliarActivity.FRAGMENT_TAG);
                ft.commitAllowingStateLoss();
                shouldCloseDrawer = true;
            } else if (mPageEntries[i].mNameResource == R.string.main_force_update_title) {
                if (getNetworkState(FamiliarActivity.this, true) != -1) {
                    PreferenceAdapter.setLastLegalityUpdate(FamiliarActivity.this, 0);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                        } else {
                            startService(new Intent(FamiliarActivity.this, DbUpdaterService.class));
                        }
                    } catch (IllegalStateException e) {
                        // Ignore it
                    }
                }
                shouldCloseDrawer = true;
            }
//                else if (mPageEntries[i].mNameResource == R.string.main_donate_title) {
//                    showDialogFragment(FamiliarActivityDialogFragment.DIALOG_DONATE);
//                    shouldCloseDrawer = true;
//                    break;
//                }
            else if (mPageEntries[i].mNameResource == R.string.main_about) {
                showDialogFragment(FamiliarActivityDialogFragment.DIALOG_ABOUT);
                shouldCloseDrawer = true;
            } else if (mPageEntries[i].mNameResource == R.string.main_whats_new_title) {
                showDialogFragment(FamiliarActivityDialogFragment.DIALOG_CHANGE_LOG);
                shouldCloseDrawer = true;
            } else if (mPageEntries[i].mNameResource == R.string.main_export_data_title) {
                ZipUtils.exportData(FamiliarActivity.this);
                shouldCloseDrawer = true;
            } else if (mPageEntries[i].mNameResource == R.string.main_import_data_title) {
                ZipUtils.importData(FamiliarActivity.this);
                shouldCloseDrawer = true;
            }

            mDrawerList.setItemChecked(mCurrentFrag, true);
            if (shouldCloseDrawer) {
                (new Handler()).postDelayed(() -> mDrawerLayout.closeDrawer(mDrawerList), 50);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            //toolbar.setCollapsible(true);
            /* I don't like styling in java, but I can't get it to work other ways */
            if (PreferenceAdapter.getTheme(this).equals(getString(R.string.pref_theme_light))) {
                toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
            } else {
                toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat);
            }
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, android.R.color.white));
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
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
             * If the menu is visible and the menu is open(ing), or the menu is not visible and the
             * drawer is closed, invalidate the options menu to either hide or show the action bar
             * icons.
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
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("");

        boolean isDeepLink = false;

        /* The activity can be launched a few different ways. Check the intent and show the
         * appropriate fragment */
        /* Only launch a fragment if the app isn't being recreated, i.e. savedInstanceState is
         * null */
        if (savedInstanceState == null && getIntent() != null) {
            isDeepLink = processIntent(getIntent());
        }

        /* Check to see if the change log should be shown */
        if (!isDeepLink) {
            PackageInfo pInfo;
            try {
                assert getPackageManager() != null;
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

                int lastVersion = PreferenceAdapter.getLastVersion(this);
                if (pInfo.versionCode != lastVersion) {

                    // Show the changelog dialog, sometimes
                    if (lastVersion != 0) {
                        showDialogFragment(FamiliarActivityDialogFragment.DIALOG_CHANGE_LOG);
                    }

                    /* Clear the mtr and ipg on update, to replace them with the newly colored
                     *  versions, but only if we're updating to 3.0.1 (v24) */
                    if (lastVersion < 24) {
                        File mtr = new File(getFilesDir(), JudgesCornerFragment.MTR_LOCAL_FILE);
                        File ipg = new File(getFilesDir(), JudgesCornerFragment.IPG_LOCAL_FILE);
                        File jar = new File(getFilesDir(), JudgesCornerFragment.JAR_LOCAL_FILE);
                        if (mtr.exists()) {
                            if (!mtr.delete()) {
                                SnackbarWrapper.makeAndShowText(this, mtr.getName() + " " + getString(R.string.not_deleted),
                                        SnackbarWrapper.LENGTH_LONG);
                            }
                            if (!ipg.delete()) {
                                SnackbarWrapper.makeAndShowText(this, ipg.getName() + " " + getString(R.string.not_deleted),
                                        SnackbarWrapper.LENGTH_LONG);
                            }
                            if (!jar.delete()) {
                                SnackbarWrapper.makeAndShowText(this, jar.getName() + " " + getString(R.string.not_deleted),
                                        SnackbarWrapper.LENGTH_LONG);
                            }
                        }
                    }

                    // When upgrading from 53 or below, clear out both the internal and external cache
                    // Version 54 added in new caching for images and prices
                    if (lastVersion < 54) {
                        File cacheDir = getCacheDir();
                        if (null != cacheDir && cacheDir.exists()) {
                            File[] listFiles = cacheDir.listFiles();
                            if (null != listFiles) {
                                for (File cachedFile : listFiles) {
                                    //noinspection ResultOfMethodCallIgnored
                                    cachedFile.delete();
                                }
                            }
                        }

                        cacheDir = getExternalCacheDir();
                        if (null != cacheDir && cacheDir.exists()) {
                            File[] listFiles = cacheDir.listFiles();
                            if (null != listFiles) {
                                for (File cachedFile : listFiles) {
                                    //noinspection ResultOfMethodCallIgnored
                                    cachedFile.delete();
                                }
                            }
                        }
                    }

                    PreferenceAdapter.setLastVersion(this, pInfo.versionCode);
                }
            } catch (PackageManager.NameNotFoundException e) {
                /* Eat it, don't show change log */
            }
        }

        /* Run the updater service if there is a network connection */
        if (getNetworkState(FamiliarActivity.this, false) != -1 && PreferenceAdapter.getAutoUpdate(this)) {
            /* Only update the banning list if it hasn't been updated recently */
            long curTime = System.currentTimeMillis();
            int updateFrequency = Integer.parseInt(PreferenceAdapter.getUpdateFrequency(this));
            int lastLegalityUpdate = PreferenceAdapter.getLastLegalityUpdate(this);
            /* days to ms */
            if (((curTime / 1000) - lastLegalityUpdate) > (updateFrequency * 24 * 60 * 60)) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(this, DbUpdaterService.class));
                    } else {
                        startService(new Intent(this, DbUpdaterService.class));
                    }
                } catch (IllegalStateException e) {
                    // Ignore it
                }
            }
        }

        // Uncomment this to run a test to lookup all prices for all cards
        // (new LookupAllPricesTest()).execute(this);
    }

    private boolean processIntent(@NonNull Intent intent) {
        boolean isDeepLink = false;

        String action;
        try {
            action = intent.getAction();
            if (null == action) {
                action = Intent.ACTION_MAIN;
            }
        } catch (NullPointerException e) {
            action = Intent.ACTION_MAIN;
        }
        switch (action) {
            case Intent.ACTION_SEARCH: {
                /* Do a search by name, launched from the quick search */
                String query = intent.getStringExtra(SearchManager.QUERY);
                Bundle args = new Bundle();
                SearchCriteria sc = new SearchCriteria();
                sc.name = query;
                args.putBoolean(SearchViewFragment.CRITERIA_FLAG, true);
                PreferenceAdapter.setSearchCriteria(this, sc);
                selectItem(R.string.main_card_search, args, false, true); /* Don't clear backstack, do force the intent */

                break;
            }
            case Intent.ACTION_VIEW: {

                boolean shouldSelectItem = true;

                Uri data = intent.getData();
                Bundle args = new Bundle();

                if (null == data || null == data.getAuthority()) {
                    SnackbarWrapper.makeAndShowText(this, R.string.no_results_found, SnackbarWrapper.LENGTH_LONG);
                    this.finish();
                    return false;
                }

                boolean shouldClearFragmentStack = true; /* Clear backstack for deep links */
                if (data.getAuthority().toLowerCase().contains(".wizards")) {
                    Cursor cursor = null;
                    FamiliarDbHandle fromUrlHandle = new FamiliarDbHandle();
                    try {
                        SQLiteDatabase database = DatabaseManager.openDatabase(this, false, fromUrlHandle);
                        String queryParam;
                        if ((queryParam = data.getQueryParameter("multiverseid")) != null) {
                            cursor = CardDbAdapter.fetchCardByMultiverseId(Long.parseLong(queryParam),
                                    new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);
                            if (cursor.getCount() != 0) {
                                isDeepLink = true;
                                args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                                        new long[]{cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                            }
                            if (args.size() == 0) {
                                throw new Exception("Not Found");
                            }
                        } else if ((queryParam = data.getQueryParameter("name")) != null) {
                            String cardName = queryParam;
                            if (queryParam.matches("\\+\\[.+\\]")) { // See #458
                                cardName = queryParam.substring(2, queryParam.length() - 1);
                            }
                            long[] cardIds = CardDbAdapter.fetchIdsByLocalizedName(cardName, database);
                            if (cardIds.length != 0) {
                                isDeepLink = true;
                                args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, cardIds);
                            }
                            if (args.size() == 0) {
                                throw new Exception("Not Found");
                            }
                        } else {
                            throw new Exception("Not Found");
                        }
                    } catch (Exception e) {
                        /* empty cursor, just return */
                        SnackbarWrapper.makeAndShowText(this, R.string.no_results_found, SnackbarWrapper.LENGTH_LONG);
                        this.finish();
                        shouldSelectItem = false;
                    } finally {
                        if (null != cursor) {
                            cursor.close();
                        }
                        DatabaseManager.closeDatabase(this, fromUrlHandle);
                    }
                } else if (data.getAuthority().contains("CardSearchProvider")) {
                    /* User clicked a card in the quick search autocomplete, jump right to it */
                    args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                            new long[]{Long.parseLong(Objects.requireNonNull(data.getLastPathSegment()))});
                    shouldClearFragmentStack = false; /* Don't clear backstack for search intents */
                } else {
                    /* User clicked a deep link, jump to the card(s) */
                    isDeepLink = true;

                    Cursor cursor = null;
                    FamiliarDbHandle deepLinkHandle = new FamiliarDbHandle();
                    try {
                        SQLiteDatabase database = DatabaseManager.openDatabase(this, false, deepLinkHandle);
                        boolean screenLaunched = false;
                        if (Objects.requireNonNull(data.getScheme()).toLowerCase().equals("card") &&
                                data.getAuthority().toLowerCase().equals("multiverseid")) {
                            if (data.getLastPathSegment() == null) {
                                /* Home screen deep link */
                                launchHomeScreen();
                                screenLaunched = true;
                                shouldSelectItem = false;
                            } else {
                                try {
                                    /* Don't clear the fragment stack for internal links (thanks Meld
                                     * cards) */
                                    if (data.getPathSegments().contains("internal")) {
                                        shouldClearFragmentStack = false;
                                    }
                                    cursor = CardDbAdapter.fetchCardByMultiverseId(Long.parseLong(data.getLastPathSegment()),
                                            new String[]{CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_ID}, database);
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }

                        if (cursor != null) {
                            if (cursor.getCount() != 0) {
                                args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY,
                                        new long[]{cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_ID))});
                            } else {
                                /* empty cursor, just return */
                                SnackbarWrapper.makeAndShowText(this, R.string.no_results_found, SnackbarWrapper.LENGTH_LONG);
                                this.finish();
                                shouldSelectItem = false;
                            }
                        } else if (!screenLaunched) {
                            /* null cursor, just return */
                            SnackbarWrapper.makeAndShowText(this, R.string.no_results_found, SnackbarWrapper.LENGTH_LONG);
                            this.finish();
                            shouldSelectItem = false;
                        }
                    } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != cursor) {
                            cursor.close();
                        }
                        DatabaseManager.closeDatabase(this, deepLinkHandle);
                    }
                }
                args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
                if (shouldSelectItem) {
                    selectItem(R.string.main_card_search, args, shouldClearFragmentStack, true);
                }
                break;
            }
            case ACTION_ROUND_TIMER:
                selectItem(R.string.main_timer, null, true, false);
                break;
            case ACTION_CARD_SEARCH:
                selectItem(R.string.main_card_search, null, true, false);
                break;
            case ACTION_LIFE:
                selectItem(R.string.main_life_counter, null, true, false);
                break;
            case ACTION_DICE:
                selectItem(R.string.main_dice, null, true, false);
                break;
            case ACTION_TRADE:
                selectItem(R.string.main_trade, null, true, false);
                break;
            case ACTION_MANA:
                selectItem(R.string.main_mana_pool, null, true, false);
                break;
            case ACTION_WISH:
                selectItem(R.string.main_wishlist, null, true, false);
                break;
            case ACTION_RULES:
                selectItem(R.string.main_rules, null, true, false);
                break;
            case ACTION_JUDGE:
                selectItem(R.string.main_judges_corner, null, true, false);
                break;
            case ACTION_MOJHOSTO:
                selectItem(R.string.main_mojhosto, null, true, false);
                break;
            case ACTION_DECKLIST:
                selectItem(R.string.main_decklist, null, true, false);
                break;
            case Intent.ACTION_MAIN:
                /* App launched as regular, show the default fragment if there isn't one already */
                getSupportFragmentManager().getFragments();
                if (getSupportFragmentManager().getFragments().isEmpty()) {
                    launchHomeScreen();
                }
                break;
            default:
                /* Some unknown intent, just finish */
                finish();
                break;
        }

        mDrawerList.setItemChecked(mCurrentFrag, true);
        return isDeepLink;
    }

    /**
     * Launch the home fragment, based on a preference.
     */
    private void launchHomeScreen() {
        String defaultFragment = PreferenceAdapter.getDefaultFragment(this);

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
        } else if (defaultFragment.equals(this.getString(R.string.main_decklist))) {
            selectItem(R.string.main_decklist, null, true, false);
        } else {
            selectItem(R.string.main_card_search, null, true, false);
        }
    }

    /**
     * Instead of starting a new Activity, any intents to start a new Familiar Activity will be
     * received here, and this Activity should react properly.
     *
     * @param intent The intent used to "start" this Activity
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    /**
     * Check to see if we should display the round timer in the actionbar, and start the inactivity
     * timer.
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
     * Select an item from the drawer menu. This will highlight the entry and manage fragment
     * transactions.
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
        FamiliarFragment newFrag = null;
        /* Pick the new fragment */
        if (resId == R.string.main_card_search) {
            /* If this is a quick search intent, launch either the card view or result list
             * directly */
            if (args != null && args.containsKey(CardViewPagerFragment.CARD_ID_ARRAY)) {
                newFrag = new CardViewPagerFragment();
            } else if (args != null && args.containsKey(SearchViewFragment.CRITERIA_FLAG)) {
                newFrag = new ResultListFragment();
            } else {
                newFrag = new SearchViewFragment();
            }
        } else if (resId == R.string.main_life_counter) {
            newFrag = new LifeCounterFragment();
        } else if (resId == R.string.main_mana_pool) {
            newFrag = new ManaPoolFragment();
        } else if (resId == R.string.main_dice) {
            newFrag = new DiceFragment();
        } else if (resId == R.string.main_trade) {
            newFrag = new TradeFragment();
        } else if (resId == R.string.main_wishlist) {
            newFrag = new WishlistFragment();
        } else if (resId == R.string.main_decklist) {
            newFrag = new DecklistFragment();
        } else if (resId == R.string.main_timer) {
            newFrag = new RoundTimerFragment();
        } else if (resId == R.string.main_rules) {
            newFrag = new RulesFragment();
        } else if (resId == R.string.main_judges_corner) {
            newFrag = new JudgesCornerFragment();
        } else if (resId == R.string.main_mojhosto) {
            newFrag = new MoJhoStoFragment();
        }

        try {
            if (!forceSelect && null != newFrag && ((Object) newFrag).getClass().equals(((Object) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment_container))).getClass())) {
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
            if (shouldClearFragmentStack && !fm.isStateSaved()) {
                /* Remove any current fragments on the back stack */
                while (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStackImmediate();
                }
            }

            /* Begin a new transaction */
            ft = fm.beginTransaction();

            /* Replace or add the fragment */
            ft.replace(R.id.fragment_container, newFrag, FamiliarActivity.FRAGMENT_TAG);
            if (!shouldClearFragmentStack) {
                ft.addToBackStack(null);
            }
            ft.commitAllowingStateLoss();

            /* Color the icon when the fragment changes */
            mPagesAdapter.colorDrawerEntry(mPageEntries[position].getTextView());
        }
    }

    /**
     * When a FamiliarFragment resumes, if it was below another FamiliarFragment on the backstack,
     * call this to reselect the drawer entry based on the class
     *
     * @param aClass The class that resumed and should have the associated entry selected
     */
    public void selectDrawerEntry(Class<? extends FamiliarFragment> aClass) {
        // Look through all the entries
        for (int position = 0; position < mPageEntries.length; position++) {
            // Check if the entry expects the current class
            if (mPageEntries[position].isClass(aClass)) {
                // If it does, set the position and select the entry
                mCurrentFrag = position;
                mPagesAdapter.colorDrawerEntry(mPageEntries[mCurrentFrag].getTextView());
                mDrawerList.setItemChecked(mCurrentFrag, true);
                return;
            }
        }
    }

    /**
     * mDrawerToggle.syncState() must be called during onPostCreate().
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied
     *                           in onSaveInstanceState(Bundle).
     *                           Note: Otherwise it is null.
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        /* Sync the toggle state after onRestoreInstanceState has occurred. */
        mDrawerToggle.syncState();
    }

    /**
     * The drawer toggle should be notified of any configuration changes.
     *
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /* Pass any configuration change to the drawer toggles */
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * If a fragment has a result when it pops off the back stack, store it in the activity.
     * It can be checked by the next FamiliarFragment to be displayed with getFragmentResults.
     * The Bundle is cleared in the fragment's onResume() if it isn't accessed.
     *
     * @param result The bundle of results to save
     */
    public void setFragmentResult(Bundle result) {
        mFragResults = result;
    }

    /**
     * This will return a Bundle which a fragment has stored in the activity, and then null the
     * Bundle immediately.
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
     * Called when a key down event has occurred. Checks if the fragment should handle the key event
     * or if the activity should.
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
            return ((FamiliarFragment) Objects.requireNonNull(f)).onInterceptSearchKey() || super.onKeyDown(keyCode, event);
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
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Toolbar toolbar = findViewById(R.id.toolbar);
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
     * This function hides the soft keyboard if it is being displayed. It's nice for switching
     * fragments.
     */
    public void hideKeyboard() {
        try {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (null != inputManager && null != getCurrentFocus()) {
                inputManager
                        .hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (NullPointerException e) {
            /* eat it */
        }
    }

    /**
     * A friendly reminder to not use the regular FragmentManager, ever.
     *
     * @return Return? How about an exception!
     */
    @Override
    @NonNull
    public android.app.FragmentManager getFragmentManager() {
        FamiliarActivity.DebugLog(Log.WARN, "Suggestion", "Use .getSupportFragmentManager()");
        return super.getFragmentManager();
    }

    /**
     * If TTS couldn't init, show this dialog. It will only display once as to not annoy people.
     */
    public void showTtsDialog() {
        if (PreferenceAdapter.getTtsShowDialog(this)) {
            showDialogFragment(FamiliarActivityDialogFragment.DIALOG_TTS);
            PreferenceAdapter.setTtsShowDialog(this);
        }
    }

    /**
     * Removes a fragment with the DIALOG_TAG. The FragmentManager is a parameter so that it plays
     * nice with nested fragments and getChildFragmentManager().
     *
     * @param fragmentManager The FragmentManager to use for this transaction
     */
    public void removeDialogFragment(FragmentManager fragmentManager) {
        if (fragmentManager != null) {
            Fragment prev = fragmentManager.findFragmentByTag(FamiliarActivity.DIALOG_TAG);
            if (prev != null) {
                if (prev instanceof DialogFragment) {
                    try {
                        ((DialogFragment) prev).dismissAllowingStateLoss();
                    } catch (IllegalStateException e) {
                        // Don't remove the dialog I guess
                        return;
                    }
                }
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.remove(prev);
                ft.commitAllowingStateLoss();
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
        FamiliarActivityDialogFragment newFragment = new FamiliarActivityDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
        newFragment.show(getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Show the timer in the action bar, set up the handler to update the displayed time every
     * second.
     */
    public void startUpdatingDisplay() {
        mRoundEndTime = PreferenceAdapter.getRoundTimerEnd(this);
        mUpdatingRoundTimer = true;

        mRoundTimerUpdateHandler.removeCallbacks(timerUpdate);
        mRoundTimerUpdateHandler.postDelayed(timerUpdate, 1);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        findViewById(R.id.toolbar).setOnClickListener(v -> selectItem(R.string.main_timer, null, false, false));
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
        findViewById(R.id.toolbar).setOnClickListener(null);
    }

    /**
     * Called when another activity returns and this one is displayed. Since the app is fragment
     * based, this is only called when querying for TTS support, or when returning from a ringtone
     * picker. The preference fragment does not handle the ringtone result correctly, so it must be
     * caught here.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its
     *                    setResult().
     * @param data        An Intent, which can return result data to the caller (various data can be
     *                    attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* The ringtone picker in the preference fragment and RoundTimerFragment will send a result
         * here */
        if (data != null && data.getExtras() != null) {
            if (data.getExtras().keySet().contains(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    PreferenceAdapter.setTimerSound(this, uri.toString());
                }
            }
        }
    }

    /**
     * Handle options item presses. In this case, the home button opens and closes the drawer.
     *
     * @param item The item selected
     * @return True if the click was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                mDrawerLayout.openDrawer(mDrawerList);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called whenever the user does anything. On an interaction, reset the inactivity timer and
     * notifies the FamiliarFragment if it was inactive. The inactivity timer will notify the
     * FamiliarFragment of inactivity
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
     * Save the current fragment.
     *
     * @param outState a Bundle in which to save the state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_FRAG, mCurrentFrag);
        outState.putBoolean(IS_REFRESHING, mIsLoading);
        clearLoading();
        super.onSaveInstanceState(outState);

        FamiliarActivity.logBundleSize("OSSI " + this.getClass().getName(), outState);
    }

    /**
     * Restore the current fragment, and highlight it.
     *
     * @param savedInstanceState a Bundle which contains the saved state
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(CURRENT_FRAG)) {
            mCurrentFrag = savedInstanceState.getInt(CURRENT_FRAG);
            mDrawerList.setItemChecked(mCurrentFrag, true);

            if (savedInstanceState.getBoolean(IS_REFRESHING)) {
                setLoading();
            } else {
                clearLoading();
            }
        }
    }

    /**
     * Show the indeterminate loading bar.
     */
    public void setLoading() {
        runOnUiThread(() -> {
            if (!mIsLoading) {
                mSmoothProgressBar.progressiveStart();
                mIsLoading = true;
            }
        });
    }

    /**
     * Hide the indeterminate loading bar.
     */
    public void clearLoading() {
        runOnUiThread(() -> {
            if (mIsLoading) {
                mSmoothProgressBar.progressiveStop();
                mIsLoading = false;
            }
        });
    }

    /**
     * This helper function translates an attribute into a resource ID.
     *
     * @param attr The attribute ID
     * @return the resource ID
     */
    public int getResourceIdFromAttr(int attr) {
        assert getTheme() != null;
        TypedArray ta = getTheme().obtainStyledAttributes(new int[]{attr});
        int resId = ta.getResourceId(0, 0);
        ta.recycle();
        return resId;
    }

    /**
     * Checks the networks state.
     *
     * @param activity        the activity to show the Snackbar in
     * @param shouldShowToast true, if you want a Toast to be shown indicating a lack of network
     * @return -1 if there is no network connection, or the type of network, like
     * ConnectivityManager.TYPE_WIFI
     */
    public static int getNetworkState(Activity activity, boolean shouldShowToast) {
        try {
            ConnectivityManager conMan = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null == conMan) {
                if (shouldShowToast) {
                    SnackbarWrapper.makeAndShowText(activity, R.string.no_network, SnackbarWrapper.LENGTH_SHORT);
                }
                return -1;
            }
            for (NetworkInfo ni : conMan.getAllNetworkInfo()) {
                if (ni.isConnected()) {
                    return ni.getType();
                }
            }
            if (shouldShowToast) {
                SnackbarWrapper.makeAndShowText(activity, R.string.no_network, SnackbarWrapper.LENGTH_SHORT);
            }
            return -1;
        } catch (NullPointerException e) {
            if (shouldShowToast) {
                SnackbarWrapper.makeAndShowText(activity, R.string.no_network, SnackbarWrapper.LENGTH_SHORT);
            }
            return -1;
        }
    }

    /**
     * Callback for when a permission is requested.
     *
     * @param requestCode  The request code passed in requestPermissions(String[], int).
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     android.content.pm.PackageManager.PERMISSION_GRANTED or
     *                     android.content.pm.PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE_BACKUP: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    ZipUtils.importData(this);
                }
                break;
            }
            case REQUEST_WRITE_EXTERNAL_STORAGE_BACKUP:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ZipUtils.exportData(this);
                }
                break;
            default:
                Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                        .onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    /**
     * This nested class encapsulates the necessary information for an entry in the drawer menu.
     */
    static class DrawerEntry {
        final int mNameResource;
        final int mIconAttr;
        final boolean mIsDivider;
        private final Class[] mFragClasses;
        TextView textView;

        DrawerEntry(int nameResource, int iconResource, boolean isHeader, Class[] fragments) {
            mNameResource = nameResource;
            mIconAttr = iconResource;
            mIsDivider = isHeader;
            mFragClasses = fragments;
        }

        void setTextView(TextView textView) {
            this.textView = textView;
        }

        TextView getTextView() {
            return textView;
        }

        boolean isClass(Class<? extends FamiliarFragment> aClass) {
            if (null == mFragClasses) {
                return false;
            }
            for (Class fragClass : mFragClasses) {
                if (aClass.equals(fragClass)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This nested class is the adapter which populates the listView in the drawer menu. It handles
     * both entries and headers.
     */
    class DrawerEntryArrayAdapter extends ArrayAdapter<DrawerEntry> {
        private Drawable mHighlightedDrawable;

        /**
         * Constructor. The context will be used to inflate views later. The array of values will be
         * used to populate the views.
         *
         * @param context The application's context, used to inflate views later.
         */
        DrawerEntryArrayAdapter(Context context) {
            super(context, R.layout.drawer_list_item, mPageEntries);
        }

        /**
         * Called to get a view for an entry in the listView.
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible. Since the layouts for entries and
         *                    headers are different, this will be ignored
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            int layout;
            if (mPageEntries[position].mIsDivider) {
                layout = R.layout.drawer_list_divider;
            } else {
                layout = R.layout.drawer_list_item;
            }
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(layout, parent, false);
            }

            assert convertView != null;
            if (mPageEntries[position].mIsDivider) {
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
                TextView textView = convertView.findViewById(R.id.drawer_entry_name);
                mPageEntries[position].setTextView(textView);
                textView.setText(mPageEntries[position].mNameResource);
                textView.setCompoundDrawablesWithIntrinsicBounds(getResourceIdFromAttr(mPageEntries[position].mIconAttr), 0, 0, 0);
                /* Color the initial icon */
                if (mCurrentFrag == position) {
                    colorDrawerEntry(textView);
                } else {
                    ((TextView) convertView.findViewById(R.id.drawer_entry_name)).getCompoundDrawables()[0].setColorFilter(null);
                }
            }

            return convertView;
        }

        /**
         * Applies the primary color to the selected icon in the drawer.
         *
         * @param textView The TextView to color
         */
        void colorDrawerEntry(TextView textView) {
            if (mHighlightedDrawable != null) {
                mHighlightedDrawable.setColorFilter(null);
            }
            if (textView != null) {
                mHighlightedDrawable = textView.getCompoundDrawables()[0];
                mHighlightedDrawable.setColorFilter(ContextCompat.getColor(FamiliarActivity.this, getResourceIdFromAttr(R.attr.colorPrimary_attr)), PorterDuff.Mode.SRC_IN);
            }
        }
    }

    /**
     * Debug wrapper for Log.x
     *
     * @param level The Log level, VERBOSE, DEBUG, INFO, WARN, ERROR, or ASSERT
     * @param tag   The tag for this message
     * @param msg   The message to log
     */
    private static void DebugLog(int level, String tag, String msg) {
        if (BuildConfig.DEBUG) {
            switch (level) {
                case Log.VERBOSE:
                    Log.v(tag, msg);
                    break;
                case Log.DEBUG:
                    Log.d(tag, msg);
                    break;
                case Log.INFO:
                    Log.i(tag, msg);
                    break;
                case Log.WARN:
                    Log.w(tag, msg);
                    break;
                case Log.ERROR:
                    Log.e(tag, msg);
                    break;
                case Log.ASSERT:
                    Log.wtf(tag, msg);
                    break;
            }
        }
    }

    /**
     * Helper to log a bundle's size
     *
     * @param name     A label for the log
     * @param outState The bundle to log the size of
     */
    public static void logBundleSize(String name, Bundle outState) {
        if (BuildConfig.DEBUG) {
            Parcel parcel = Parcel.obtain();
            parcel.writeBundle(outState);
            int size = parcel.dataSize();
            parcel.recycle();
            FamiliarActivity.DebugLog(Log.VERBOSE, "logBundleSize", name + " saving " + size + " bytes");

            StringBuilder toPrint = new StringBuilder();
            toPrint.append("\r\n\r\n");
            printBundleContents(toPrint, outState, 0);
            FamiliarActivity.DebugLog(Log.VERBOSE, "logBundleContents", toPrint.toString());
        }
    }

    private static void printBundleContents(StringBuilder toPrint, Bundle outState, int recursionLevel) {
        try {
            for (String key : outState.keySet()) {
                for (int i = 0; i < recursionLevel; i++) {
                    toPrint.append("  ");
                }
                toPrint.append(key).append(" :: ").append(Objects.requireNonNull(outState.get(key)).getClass().getName()).append("\r\n");
                if (outState.get(key) instanceof Bundle) {
                    printBundleContents(toPrint, (Bundle) outState.get(key), recursionLevel + 1);
                }
            }
        } catch (NullPointerException e) {
            // eat it
        }
    }

    /**
     * Helper to log a parcelable's size
     *
     * @param name     A label for the log
     * @param outState THe parcelable to log the size of
     */
    public static void logBundleSize(String name, Parcelable outState) {
        if (BuildConfig.DEBUG) {
            Parcel parcel = Parcel.obtain();
            parcel.writeParcelable(outState, 0);
            int size = parcel.dataSize();
            parcel.recycle();
            FamiliarActivity.DebugLog(Log.VERBOSE, "logBundleSize", name + " saving " + size + " bytes");
        }
    }
}
