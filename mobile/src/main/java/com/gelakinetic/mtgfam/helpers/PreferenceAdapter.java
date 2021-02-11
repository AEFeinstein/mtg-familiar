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

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Brief guide to adding new preferences to this app:
 * 1. Choose a key for the preference and add it to strings-pref-keys.xml
 * 2. Add getter/setter methods to this class for the new preference, if they are necessary
 * 3. Channel all accesses to the preference through the new getter/setter methods
 * 4. ???
 * 5. Profit (or at least sanity)!
 */
@SuppressWarnings("SimplifiableIfStatement")
public class PreferenceAdapter {

    public static synchronized void registerOnSharedPreferenceChangeListener(
            @Nullable Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    public static synchronized void unregisterOnSharedPreferenceChangeListener(
            @Nullable Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(listener);
    }

    /* Last version */
    public static synchronized int getLastVersion(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_lastVersion), 0);
    }

    public static synchronized void setLastVersion(@Nullable Context context, int lastVersion) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_lastVersion), lastVersion);
        edit.apply();
    }

    /* Last legality update */
    public static synchronized int getLastLegalityUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_lastLegalityUpdate), 0);
    }

    public static synchronized void setLastLegalityUpdate(@Nullable Context context, int lastLegalityUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_lastLegalityUpdate), lastLegalityUpdate);
        edit.apply();
    }

    /* White mana */
    public static synchronized int getMana(@Nullable Context context, int key) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(key), 0);
    }

    public static synchronized void setMana(@Nullable Context context, int key, int mana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(key), mana);
        edit.apply();
    }

    /* Last rules update */
    public static synchronized long getLastRulesUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastRulesUpdate), BuildDate.get(context).getTime());
    }

    public static synchronized void setLastRulesUpdate(@Nullable Context context, long lastRulesUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastRulesUpdate), lastRulesUpdate);
        edit.apply();
    }

    /* Last MTR update */
    public static synchronized long getLastMTRUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastMTRUpdate), 0);
    }

    public static synchronized void setLastMTRUpdate(@Nullable Context context, long lastMTRUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastMTRUpdate), lastMTRUpdate);
        edit.apply();
    }

    /* Last IPG update */
    public static synchronized long getLastIPGUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastIPGUpdate), 0);
    }

    public static synchronized void setLastIPGUpdate(@Nullable Context context, long lastIPGUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastIPGUpdate), lastIPGUpdate);
        edit.apply();
    }

    /* Last JAR update */
    public static synchronized long getLastJARUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastJARUpdate), 0);
    }

    public static synchronized void setLastJARUpdate(@Nullable Context context, long lastJARUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastJARUpdate), lastJARUpdate);
        edit.apply();
    }

    /* Last DMTR update */
    public static synchronized long getLastDMTRUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastDMTRUpdate), 0);
    }

    public static synchronized void setLastDMTRUpdate(@Nullable Context context, long lastDMTRUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastDMTRUpdate), lastDMTRUpdate);
        edit.apply();
    }

    /* Last DIPG update */
    public static synchronized long getLastDIPGUpdate(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_lastDIPGUpdate), 0);
    }

    public static synchronized void setLastDIPGUpdate(@Nullable Context context, long lastDIPGUpdate) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_lastDIPGUpdate), lastDIPGUpdate);
        edit.apply();
    }
    
    /* TTS show dialog */
    public static synchronized boolean getTtsShowDialog(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_ttsShowDialog), true);
    }

    public static synchronized void setTtsShowDialog(@Nullable Context context) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_ttsShowDialog), false);
        edit.apply();
    }

    /* Auto-update */
    public static synchronized boolean getAutoUpdate(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_autoupdate), true);
    }

    /* Consolidate search */
    public static synchronized boolean getConsolidateSearch(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_consolidateSearch), true);
    }

    /* Pic first */
    public static synchronized boolean getPicFirst(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_picFirst), false);
    }

    /* Keep Screen On */
    public static synchronized boolean getKeepScreenOn(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_wakelock), true);
    }

    /* Dim Screen */
    public static synchronized boolean getDimScreen(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_dimlock), true);
    }

    /* Percentage to dim screen */
    public static synchronized int getDimLevel(@Nullable Context context) {
        if (null == context) {
            return 1;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_dimlevel), 1);
    }

    /* Set pref */
    public static synchronized boolean getSetPref(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_setPref), true);
    }

    /* Mana cost pref */
    public static synchronized boolean getManaCostPref(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_manacostPref), true);
    }

    /* Type pref */
    public static synchronized boolean getTypePref(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_typePref), true);
    }

    /* Ability pref */
    public static synchronized boolean getAbilityPref(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_abilityPref), true);
    }

    /* P/T pref */
    public static synchronized boolean getPTPref(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_ptPref), true);
    }

    /* 15-minute warning pref */
    public static synchronized boolean getFifteenMinutePref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_fifteenMinutePref), false);
    }

    public static synchronized void setFifteenMinutePref(@Nullable Context context, boolean fifteenMinutePref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_fifteenMinutePref), fifteenMinutePref);
        edit.apply();
    }

    /* 10-minute warning pref */
    public static synchronized boolean getTenMinutePref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_tenMinutePref), false);
    }

    public static synchronized void setTenMinutePref(@Nullable Context context, boolean tenMinutePref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_tenMinutePref), tenMinutePref);
        edit.apply();
    }

    /* 5-minute warning pref */
    public static synchronized boolean getFiveMinutePref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_fiveMinutePref), false);
    }

    public static synchronized void setFiveMinutePref(@Nullable Context context, boolean fiveMinutePref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_fiveMinutePref), fiveMinutePref);
        edit.apply();
    }

    /* 2-minute warning pref */
    public static synchronized boolean getTwoMinutePref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_twoMinutePref), false);
    }

    public static synchronized void setTwoMinutePref(@Nullable Context context, boolean twoMinutePref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_twoMinutePref), twoMinutePref);
        edit.apply();
    }

    /* Use sound instead of TTS pref */
    public static synchronized boolean getUseSoundInsteadOfTTSPref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_useSoundInsteadOfTTSPref), false);
    }

    public static synchronized void setUseSoundInsteadOfTTSPref(@Nullable Context context, boolean useSoundPref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_useSoundInsteadOfTTSPref), useSoundPref);
        edit.apply();
    }

    /* Show total wishlist price */
    public static synchronized boolean getShowTotalWishlistPrice(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showTotalPriceWishlistPref), false);
    }

    /* Show individual wishlist prices */
    public static synchronized boolean getShowIndividualWishlistPrices(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showIndividualPricesWishlistPref), true);
    }

    /* Verbose wishlist */
    public static synchronized boolean getVerboseWishlist(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_verboseWishlistPref), false);
    }

    /* MoJhoSto first time */
    public static synchronized boolean getMojhostoFirstTime(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_mojhostoFirstTime), true);
    }

    public static synchronized void setMojhostoFirstTime(@Nullable Context context) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_mojhostoFirstTime), false);
        edit.apply();
    }


    /* String preferences */
    /* Update frequency */
    public static synchronized String getUpdateFrequency(@Nullable Context context) {
        if (null == context) {
            return "3";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_updatefrequency), "3");
    }

    /* Card language */
    public static synchronized String getCardLanguage(@Nullable Context context) {
        if (null == context) {
            return "en";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_cardlanguage), "en");
    }

    /* Default fragment */
    public static synchronized String getDefaultFragment(@Nullable Context context) {
        if (null == context) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_defaultFragment), context.getString(R.string.main_card_search));
    }

    /* Display mode */
    public static synchronized String getDisplayMode(@Nullable Context context) {
        if (null == context) {
            return "0";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_displayMode), "0");
    }

    public static synchronized void setDisplayMode(@Nullable Context context, String displayMode) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_displayMode), displayMode);
        edit.apply();
    }

    /* Player data */
    public static synchronized String getPlayerData(@Nullable Context context) {
        if (null == context) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_player_data), null);
    }

    public static synchronized void setPlayerData(@Nullable Context context, String playerData) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_player_data), playerData);
        edit.apply();
    }

    /* Timer sound */
    public static synchronized String getTimerSound(@Nullable Context context) {
        if (null == context) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_timerSound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
    }

    public static synchronized void setTimerSound(@Nullable Context context, String timerSound) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_timerSound), timerSound);
        edit.apply();
    }

    /* Trade price */
    public static synchronized MarketPriceInfo.PriceType getTradePrice(@Nullable Context context) {
        if (null == context) {
            return MarketPriceInfo.PriceType.MARKET;
        }
        return MarketPriceInfo.PriceType.fromOrdinal(Integer.parseInt(
                Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_tradePrice), "3"))));
    }

    public static synchronized void setTradePrice(@Nullable Context context, MarketPriceInfo.PriceType tradePrice) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_tradePrice), Integer.toString(tradePrice.ordinal()));
        edit.apply();
    }

    /* Wishlist price */
    public static synchronized MarketPriceInfo.PriceType getWishlistPrice(@Nullable Context context) {
        if (null == context) {
            return MarketPriceInfo.PriceType.MARKET;
        }
        return MarketPriceInfo.PriceType.fromOrdinal(Integer.parseInt(
                Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_wishlistPrice), "3"))));
    }

    public static synchronized void setWishlistPrice(@Nullable Context context, MarketPriceInfo.PriceType wishlistPrice) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_wishlistPrice), Integer.toString(wishlistPrice.ordinal()));
        edit.apply();
    }

    /* Date, deprecated
    public static synchronized String getLegalityDate(@Nullable Context context) {
if(null == context){return  null;}
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_date), null);
    }

    public static synchronized void setLegalityDate(String date) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
edit.putString(context.getString(R.string.key_date), date);
        edit.apply();
    }
    */

    /* Date */
    public static synchronized long getLegalityTimestamp(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_legality_timestamp), 0);
    }

    public static synchronized void setLegalityTimestamp(@Nullable Context context, long timestamp) {
        if (null == context) {
            return;
        }

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

    public static synchronized long getLastUpdateTimestamp(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_last_update_timestamp), 0);
    }

    public static synchronized void setLastUpdateTimestamp(@Nullable Context context, long timestamp) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_last_update_timestamp), timestamp);
        edit.apply();
    }
    */

    /* Life Counter Timer */
    static synchronized String getLifeTimer(@Nullable Context context) {
        if (null == context) {
            return "1000";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_lifeTimer), "1000");
    }

    /*  Round timer finish time */
    public static synchronized long getRoundTimerEnd(@Nullable Context context) {
        if (null == context) {
            return -1;
        }
        long endTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_currentRoundTimer), -1);
        /* If the timer has expired, set it as -1 */
        if (endTime < System.currentTimeMillis()) {
            endTime = -1L;
            setRoundTimerEnd(context, endTime);
        }
        return endTime;
    }

    public static synchronized void setRoundTimerEnd(@Nullable Context context, long milliseconds) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_currentRoundTimer), milliseconds);
        edit.apply();
    }

    /* Bounce Drawer, default true */
    public static synchronized boolean getBounceDrawer(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_bounceDrawer), true);
    }

    public static synchronized void setBounceDrawer(@Nullable Context context) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_bounceDrawer), false);
        edit.apply();
    }

    public static synchronized Set<String> getWidgetButtons(@Nullable Context context) {
        if (null == context) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getString(R.string.key_widgetButtons), new HashSet<>(
                Arrays.asList(context.getResources().getStringArray(R.array.default_widget_buttons_array_entries))));
    }

    public static synchronized void setWidgetButtons(@Nullable Context context, Set<String> widgetButtons) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putStringSet(context.getString(R.string.key_widgetButtons), widgetButtons);
        edit.apply();
    }

    /* This is slightly different because we want to make sure to commit a theme if one doesn't
     * exist, not just return the default. asd is a nice tag, no?
     */
    public static synchronized String getTheme(@Nullable Context context) {
        if (null == context) {
            return "asd";
        }
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_theme), "asd");
        if (Objects.requireNonNull(theme).equals("asd")) {
            theme = context.getResources().getString(R.string.pref_theme_light);
            setTheme(context, theme);
        }
        return theme;
    }

    private static synchronized void setTheme(@Nullable Context context, String theme) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_theme), theme);
        edit.apply();
    }

    public static synchronized String getDCINumber(@Nullable Context context) {
        if (null == context) {
            return "";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_dci_number), "");
    }

    public static synchronized void setDCINumber(@Nullable Context context, String dciNumber) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_dci_number), dciNumber);
        edit.apply();
    }

    private static synchronized void setImageCacheSize(@Nullable Context context, int cacheSize) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_imageCacheSize), cacheSize);
        edit.apply();
    }

    public static synchronized int getImageCacheSize(@Nullable Context context) {
        final int MIN_CACHE_MB = 50;
        if (null == context) {
            return MIN_CACHE_MB;
        }
        int cacheSize = PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_imageCacheSize), MIN_CACHE_MB);

        /* Make sure the cache is at least 50 MB */
        if (cacheSize < MIN_CACHE_MB) {
            setImageCacheSize(context, MIN_CACHE_MB);
            return MIN_CACHE_MB;
        }
        return cacheSize;
    }

    public static synchronized int getDatabaseVersion(@Nullable Context context) {
        if (null == context) {
            return -1;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_database_version), -1);
    }

    public static synchronized void setDatabaseVersion(@Nullable Context context, int databaseVersion) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_database_version),
                databaseVersion);
        edit.apply();
    }

    public static synchronized String getLanguage(@Nullable Context context) {
        if (null == context) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_language),
                context.getResources().getConfiguration().locale.getLanguage());
    }

    static synchronized void setNumWidgetButtons(@Nullable Context context, int widgetID, int numButtons) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_widgetNumButtons) + widgetID,
                numButtons);
        edit.apply();
    }

    static synchronized int getNumWidgetButtons(@Nullable Context context, int widgetID) {
        if (null == context) {
            return 100;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_widgetNumButtons) + widgetID, 100);
    }

    public static synchronized void setSearchSortOrder(@Nullable Context context, String searchSortOrder) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_searchSortOrder), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getSearchSortOrder(@Nullable Context context) {
        String defaultOrder = CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC;
        if (null == context) {
            return defaultOrder;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_searchSortOrder), defaultOrder);
    }

    public static synchronized void setWishlistSortOrder(@Nullable Context context, String searchSortOrder) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_wishlist_sort_order_2), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getWishlistSortOrder(@Nullable Context context) {
        String defaultOrder = CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC + "," +
                SortOrderDialogFragment.KEY_PRICE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_DESC;
        if (null == context) {
            return defaultOrder;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_wishlist_sort_order_2), defaultOrder);
    }

    public static synchronized void setTradeSortOrder(@Nullable Context context, String searchSortOrder) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_trade_sort_order_2), searchSortOrder);
        edit.apply();
    }

    public static synchronized String getTradeSortOrder(@Nullable Context context) {
        String defaultOrder = CardDbAdapter.KEY_NAME + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_SET + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_COLOR + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_SUPERTYPE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_CMC + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_POWER + " " + SortOrderDialogFragment.SQL_ASC + "," +
                CardDbAdapter.KEY_TOUGHNESS + " " + SortOrderDialogFragment.SQL_ASC + "," +
                SortOrderDialogFragment.KEY_PRICE + " " + SortOrderDialogFragment.SQL_ASC + "," +
                SortOrderDialogFragment.KEY_ORDER + " " + SortOrderDialogFragment.SQL_DESC;
        if (context == null) {
            return defaultOrder;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_trade_sort_order_2), defaultOrder);
    }

    static synchronized
    @DrawableRes
    int getTapSymbol(@Nullable Context context) {
        if (null == context) {
            return R.drawable.glyph_tap;
        }
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

    static synchronized
    @DrawableRes
    int getWhiteSymbol(@Nullable Context context) {
        if (null == context) {
            return R.drawable.glyph_w;
        }
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
    public static synchronized int getUndoTimeout(@Nullable Context context) {
        if (null == context) {
            return 3 * 1000;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_undoTimeout), 3) * 1000;
    }

    public static synchronized boolean getShowTotalDecklistPrice(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_showTotalPriceDecklistPref), false);
    }

    /* Deck price */
    public static synchronized MarketPriceInfo.PriceType getDeckPrice(@Nullable Context context) {
        if (null == context) {
            return MarketPriceInfo.PriceType.MARKET;
        }
        return MarketPriceInfo.PriceType.fromOrdinal(Integer.parseInt(
                Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_deckPrice), "3"))));
    }

    public static synchronized void setDeckPrice(@Nullable Context context, MarketPriceInfo.PriceType tradePrice) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_deckPrice), Integer.toString(tradePrice.ordinal()));
        edit.apply();
    }

    public static synchronized String getTcgpApiToken(@Nullable Context context) {
        if (null == context) {
            return "1";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_TcgpToken), "");
    }

    public static synchronized void setTcgpApiToken(@Nullable Context context, String token) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_TcgpToken), token);
        edit.apply();
    }

    public static synchronized Date getTcgpApiTokenExpirationDate(@Nullable Context context) {
        if (null == context) {
            return new Date(0);
        }
        long time = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_TcgpTokenExpirationDate), 0);
        return new Date(time);
    }

    public static synchronized void setTcgpApiTokenExpirationDate(@Nullable Context context, Date date) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(context.getString(R.string.key_TcgpTokenExpirationDate), date.getTime());
        edit.apply();
    }

    public static synchronized String getLastLoadedDecklist(@Nullable Context context) {
        if (null == context) {
            return "";
        }

        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_LastDecklistLoaded), "");
    }

    public static synchronized void setLastLoadedDecklist(@Nullable Context context, String deckName) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_LastDecklistLoaded), deckName);
        edit.apply();
    }

    public static synchronized String getLastLoadedTrade(@Nullable Context context) {
        if (null == context) {
            return "";
        }

        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_LastTradeLoaded), "");
    }

    public static synchronized void setLastLoadedTrade(@Nullable Context context, String deckName) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_LastTradeLoaded), deckName);
        edit.apply();
    }

    public static void setSearchViewCriteria(@Nullable Context context, SearchCriteria searchCriteria) {
        if (null == context) {
            return;
        }
        saveCriteria(context, searchCriteria, context.getString(R.string.key_SearchCriteriaPerm));
    }

    public static SearchCriteria getSearchViewCriteria(@Nullable Context context) {
        if (null == context) {
            return new SearchCriteria();
        }
        return LoadCriteria(context, context.getString(R.string.key_SearchCriteriaPerm));
    }

    public static void setSearchCriteria(@Nullable Context context, SearchCriteria searchCriteria) {
        if (null == context) {
            return;
        }
        saveCriteria(context, searchCriteria, context.getString(R.string.key_SearchCriteria));
    }

    public static SearchCriteria getSearchCriteria(@Nullable Context context) {
        if (null == context) {
            return new SearchCriteria();
        }
        return LoadCriteria(context, context.getString(R.string.key_SearchCriteria));
    }

    private static void saveCriteria(@Nullable Context context, SearchCriteria searchCriteria, String key) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(key, searchCriteria.toJson());
        edit.apply();
    }

    private static SearchCriteria LoadCriteria(@Nullable Context context, String key) {
        if (null == context) {
            return new SearchCriteria();
        }

        return (new Gson()).fromJson(
                PreferenceManager.getDefaultSharedPreferences(context).getString(key, "{}"),
                SearchCriteria.class);
    }

    public static void setGroups(@Nullable Context context, LongSparseArray<String> groups) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_tcgpGroups), (new Gson()).toJson(groups));
        edit.apply();
    }

    public static LongSparseArray<String> getGroups(@Nullable Context context) {
        if (null == context) {
            return new LongSparseArray<>();
        }

        Type type = new TypeToken<LongSparseArray<String>>() {
        }.getType();
        return (new Gson()).fromJson(
                PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_tcgpGroups), "{}"),
                type);
    }

    /* Persist search options */
    public static synchronized boolean getPersistSearchOptions(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_persistSearch), true);
    }

    /* Persist search options */
    public static synchronized boolean getHideOnlineOnly(@Nullable Context context) {
        if (null == context) {
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_hideOnlineCards), false);
    }

    /* 15-minute warning pref */
    public static synchronized boolean getLoggingPref(@Nullable Context context) {
        if (null == context) {
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_loggingPref), false);
    }

    public static synchronized void setLoggingPref(@Nullable Context context, boolean loggingPref) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(context.getString(R.string.key_loggingPref), loggingPref);
        edit.apply();
    }
}