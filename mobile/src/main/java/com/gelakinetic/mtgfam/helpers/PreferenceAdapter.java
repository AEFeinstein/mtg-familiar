package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Brief guide to adding new preferences to this app:
 * 1. Choose a key for the preference and add it to strings-pref-keys.xml
 * 2. Add getter/setter methods to this class for the new preference, if they are necessary
 * 3. Channel all accesses to the preference through the new getter/setter methods
 * 4. ???
 * 5. Profit (or at least sanity)!
 */
public class PreferenceAdapter {

    public static synchronized void registerOnSharedPreferenceChangeListener(
            Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    public static synchronized void unregisterOnSharedPreferenceChangeListener(
            Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(listener);
    }

    /* Last version */
    public static synchronized int getLastVersion(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_lastVersion), 0);
    }

    public static synchronized void setLastVersion(Context context, int lastVersion) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_lastVersion), lastVersion);
        edit.apply();
    }

    /* Last legality update */
    public static synchronized int getLastLegalityUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_lastLegalityUpdate), 0);
    }

    public static synchronized void setLastLegalityUpdate(Context context, int lastLegalityUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_lastLegalityUpdate), lastLegalityUpdate);
        edit.apply();
    }

    /* White mana */
    public static synchronized int getWhiteMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_whiteMana), 0);
    }

    public static synchronized void setWhiteMana(Context context, int whiteMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_whiteMana), whiteMana);
        edit.apply();
    }

    /* Blue mana */
    public static synchronized int getBlueMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_blueMana), 0);
    }

    public static synchronized void setBlueMana(Context context, int blueMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_blueMana), blueMana);
        edit.apply();
    }

    /* Black mana */
    public static synchronized int getBlackMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_blackMana), 0);
    }

    public static synchronized void setBlackMana(Context context, int blackMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_blackMana), blackMana);
        edit.apply();
    }

    /* Red mana */
    public static synchronized int getRedMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_redMana), 0);
    }

    public static synchronized void setRedMana(Context context, int redMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_redMana), redMana);
        edit.apply();
    }

    /* Green mana */
    public static synchronized int getGreenMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_greenMana), 0);
    }

    public static synchronized void setGreenMana(Context context, int greenMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_greenMana), greenMana);
        edit.apply();
    }

    /* Colorless mana */
    public static synchronized int getColorlessMana(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_colorlessMana), 0);
    }

    public static synchronized void setColorlessMana(Context context, int colorlessMana) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_colorlessMana), colorlessMana);
        edit.apply();
    }

    /* Spell count */
    public static synchronized int getSpellCount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_spellCount), 0);
    }

    public static synchronized void setSpellCount(Context context, int spellCount) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_spellCount), spellCount);
        edit.apply();
    }

    /* Last rules update */
    public static synchronized long getLastRulesUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastRulesUpdate), BuildDate.get(context).getTime());
    }

    public static synchronized void setLastRulesUpdate(Context context, long lastRulesUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastRulesUpdate), lastRulesUpdate);
        edit.apply();
    }

    /* Last MTR update */
    public static synchronized long getLastMTRUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastMTRUpdate), 0);
    }

    public static synchronized void setLastMTRUpdate(Context context, long lastMTRUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastMTRUpdate), lastMTRUpdate);
        edit.apply();
    }

    /* Last IPG update */
    public static synchronized long getLastIPGUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastIPGUpdate), 0);
    }

    public static synchronized void setLastIPGUpdate(Context context, long lastIPGUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastIPGUpdate), lastIPGUpdate);
        edit.apply();
    }

    /* Last JAR update */
    public static synchronized long getLastJARUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastJARUpdate), 0);
    }

    public static synchronized void setLastJARUpdate(Context context, long lastJARUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastJARUpdate), lastJARUpdate);
        edit.apply();
    }

    /* TTS show dialog */
    public static synchronized boolean getTtsShowDialog(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_ttsShowDialog), true);
    }

    public static synchronized void setTtsShowDialog(Context context) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_ttsShowDialog), false);
        edit.apply();
    }

    /* Auto-update */
    public static synchronized boolean getAutoUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_autoupdate), true);
    }

    /* Consolidate search */
    public static synchronized boolean getConsolidateSearch(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_consolidateSearch), true);
    }

    /* Pic first */
    public static synchronized boolean getPicFirst(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_picFirst), false);
    }

    /* Keep Screen On */
    public static synchronized boolean getKeepScreenOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_wakelock), true);
    }

    /* Dim Screen */
    public static synchronized boolean getDimScreen(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_dimlock), true);
    }

    /* Percentage to dim screen */
    public static synchronized int getDimLevel(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_dimlevel), 1);
    }

    /* Set pref */
    public static synchronized boolean getSetPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_setPref), true);
    }

    /* Mana cost pref */
    public static synchronized boolean getManaCostPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_manacostPref), true);
    }

    /* Type pref */
    public static synchronized boolean getTypePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_typePref), true);
    }

    /* Ability pref */
    public static synchronized boolean getAbilityPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_abilityPref), true);
    }

    /* P/T pref */
    public static synchronized boolean getPTPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_ptPref), true);
    }

    /* 15-minute warning pref */
    public static synchronized boolean getFifteenMinutePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_fifteenMinutePref), false);
    }

    public static synchronized void setFifteenMinutePref(Context context, boolean fifteenMinutePref) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_fifteenMinutePref), fifteenMinutePref);
        edit.apply();
    }

    /* 10-minute warning pref */
    public static synchronized boolean getTenMinutePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_tenMinutePref), false);
    }

    public static synchronized void setTenMinutePref(Context context, boolean tenMinutePref) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_tenMinutePref), tenMinutePref);
        edit.apply();
    }

    /* 5-minute warning pref */
    public static synchronized boolean getFiveMinutePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_fiveMinutePref), false);
    }

    public static synchronized void setFiveMinutePref(Context context, boolean fiveMinutePref) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_fiveMinutePref), fiveMinutePref);
        edit.apply();
    }

    /* 2-minute warning pref */
    public static synchronized boolean getTwoMinutePref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_twoMinutePref), false);
    }

    public static synchronized void setTwoMinutePref(Context context, boolean twoMinutePref) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_twoMinutePref), twoMinutePref);
        edit.apply();
    }

    /* Use sound instead of TTS pref */
    public static synchronized boolean getUseSoundInsteadOfTTSPref(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_useSoundInsteadOfTTSPref), false);
    }

    public static synchronized void setUseSoundInsteadOfTTSPref(Context context, boolean useSoundPref) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_useSoundInsteadOfTTSPref), useSoundPref);
        edit.apply();
    }

    /* Show total wishlist price */
    public static synchronized boolean getShowTotalWishlistPrice(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showTotalPriceWishlistPref), false);
    }

    /* Show individual wishlist prices */
    public static synchronized boolean getShowIndividualWishlistPrices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showIndividualPricesWishlistPref), true);
    }

    /* Verbose wishlist */
    public static synchronized boolean getVerboseWishlist(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_verboseWishlistPref), false);
    }

    /* MoJhoSto first time */
    public static synchronized boolean getMojhostoFirstTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_mojhostoFirstTime), true);
    }

    public static synchronized void setMojhostoFirstTime(Context context) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_mojhostoFirstTime), false);
        edit.apply();
    }


    /* String preferences */
    /* Update frequency */
    public static synchronized String getUpdateFrequency(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_updatefrequency), "3");
    }

    /* Card language */
    public static synchronized String getCardLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_cardlanguage), "en");
    }

    /* Default fragment */
    public static synchronized String getDefaultFragment(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_defaultFragment), context.getString(R.string.main_card_search));
    }

    /* Display mode */
    public static synchronized String getDisplayMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_displayMode), "0");
    }

    public static synchronized void setDisplayMode(Context context, String displayMode) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_displayMode), displayMode);
        edit.apply();
    }

    /* Player data */
    public static synchronized String getPlayerData(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_player_data), null);
    }

    public static synchronized void setPlayerData(Context context, String playerData) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_player_data), playerData);
        edit.apply();
    }

    /* Timer sound */
    public static synchronized String getTimerSound(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_timerSound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
    }

    public static synchronized void setTimerSound(Context context, String timerSound) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_timerSound), timerSound);
        edit.apply();
    }

    /* Trade price */
    public static synchronized String getTradePrice(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_tradePrice), "1");
    }

    public static synchronized void setTradePrice(Context context, String tradePrice) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_tradePrice), tradePrice);
        edit.apply();
    }

    /* Date, deprecated
    public static synchronized String getLegalityDate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_date), null);
    }

    public static synchronized void setLegalityDate(String date) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
edit.putString(context.getString(R.string.key_date), date);
        edit.apply();
    }
    */

    /* Date */
    public static synchronized long getLegalityTimestamp(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_legality_timestamp), 0);
    }

    public static synchronized void setLegalityTimestamp(Context context, long timestamp) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_legality_timestamp), timestamp);
        edit.apply();
    }

    /* Deprecated
    public static synchronized void setLastUpdate(String lastUpdate) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
edit.putString(context.getString(R.string.key_lastUpdate), lastUpdate);
        edit.apply();
    }
    */

    public static synchronized long getLastUpdateTimestamp(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_last_update_timestamp), 0);
    }

    public static synchronized void setLastUpdateTimestamp(Context context, long timestamp) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_last_update_timestamp), timestamp);
        edit.apply();
    }


    /* Life Counter Timer */
    public static synchronized String getLifeTimer(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_lifeTimer), "1000");
    }

    /*  Round timer finish time */
    public static synchronized long getRoundTimerEnd(Context context) {
        Long endTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_currentRoundTimer), -1);
        /* If the timer has expired, set it as -1 */
        if (endTime < System.currentTimeMillis()) {
            endTime = -1L;
            setRoundTimerEnd(context, endTime);
        }
        return endTime;
    }

    public static synchronized void setRoundTimerEnd(Context context, long milliseconds) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_currentRoundTimer), milliseconds);
        edit.apply();
    }

    /* Bounce Drawer, default true */
    public static synchronized boolean getBounceDrawer(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_bounceDrawer), true);
    }

    public static synchronized void setBounceDrawer(Context context) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_bounceDrawer), false);
        edit.apply();
    }

    public static synchronized Set<String> getWidgetButtons(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getString(R.string.key_widgetButtons), new HashSet<>(
                Arrays.asList(context.getResources().getStringArray(R.array.default_widget_buttons_array_entries))));
    }

    public static synchronized void setWidgetButtons(Context context, Set<String> widgetButtons) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putStringSet(context.getString(R.string.key_widgetButtons), widgetButtons);
        edit.apply();
    }

    /* This is slightly different because we want to make sure to commit a theme if one doesn't
     * exist, not just return the default. asd is a nice tag, no?
     */
    public static synchronized String getTheme(Context context) {
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_theme), "asd");
        if (theme.equals("asd")) {
            theme = context.getResources().getString(R.string.pref_theme_light);
            setTheme(context, theme);
        }
        return theme;
    }

    public static synchronized void setTheme(Context context, String theme) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_theme), theme);
        edit.apply();
    }

    public static synchronized String getDCINumber(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_dci_number), "");
    }

    public static synchronized void setDCINumber(Context context, String dciNumber) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_dci_number), dciNumber);
        edit.apply();
    }

    public static synchronized int getImageCacheSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_imageCacheSize), 12);
    }

    public static synchronized int getNumTutorCardsSearches(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_num_tutor_cards_searches), 0);
    }

    public static synchronized void setNumTutorCardsSearches(Context context, int NumTutorCardsSearches) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_num_tutor_cards_searches),
                NumTutorCardsSearches);
        edit.apply();
    }

    public static synchronized int getDatabaseVersion(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_database_version), -1);
    }

    public static synchronized void setDatabaseVersion(Context context, int databaseVersion) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_database_version),
                databaseVersion);
        edit.apply();
    }

    public static synchronized String getLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_language),
                context.getResources().getConfiguration().locale.getLanguage());
    }

    public static synchronized void setNumWidgetButtons(Context context, int widgetID, int numButtons) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_widgetNumButtons) + widgetID,
                numButtons);
        edit.apply();
    }

    public static synchronized int getNumWidgetButtons(Context context, int widgetID) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_widgetNumButtons) + widgetID, 100);
    }

    public static synchronized void setSearchSortOrder(Context context, String searchSortOrder) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_searchSortOrder), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getSearchSortOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_searchSortOrder),
                CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC
        );
    }

    public static synchronized void setWishlistSortOrder(Context context, String searchSortOrder) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_wishlist_sort_order_2), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getWishlistSortOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_wishlist_sort_order_2),
                CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        SortOrderDialogFragment.KEY_PRICE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_DESC
        );
    }

    public static synchronized void setTradeSortOrder(Context context, String searchSortOrder) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_trade_sort_order_2), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getTradeSortOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_trade_sort_order_2),
                CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_SET + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        SortOrderDialogFragment.KEY_PRICE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                        SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_DESC
        );
    }

    public static synchronized
    @DrawableRes
    int getTapSymbol(Context context) {
        String symbolResName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_tap_symbol), null);
        if (symbolResName == null) {
            return R.drawable.glyph_tap;
        } else {
            symbolResName = symbolResName.substring(symbolResName.lastIndexOf('/') + 1, symbolResName.lastIndexOf('.'));
            return context.getResources().getIdentifier(
                    symbolResName,
                    "drawable",
                    context.getPackageName());
        }
    }

    public static synchronized
    @DrawableRes
    int getWhiteSymbol(Context context) {
        String symbolResName = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_white_symbol), null);
        if (symbolResName == null) {
            return R.drawable.glyph_w;
        } else {
            symbolResName = symbolResName.substring(symbolResName.lastIndexOf('/') + 1, symbolResName.lastIndexOf('.'));
            return context.getResources().getIdentifier(
                    symbolResName,
                    "drawable",
                    context.getPackageName());
        }
    }

    /* General list settings (trades, wishlist, decklist) */
    public static synchronized int getUndoTimeout(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_undoTimeout), 3) * 1000;
    }

    public static synchronized boolean getShowTotalDecklistPrice(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showTotalPriceDecklistPref), false);
    }

    public static synchronized String getDeckPrice(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_deckPrice), "1");
    }

}