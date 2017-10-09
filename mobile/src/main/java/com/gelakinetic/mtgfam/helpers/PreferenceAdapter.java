package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

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
    public static synchronized int getWhiteMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_whiteMana), 0);
    }

    public static synchronized void setWhiteMana(@Nullable Context context, int whiteMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_whiteMana), whiteMana);
        edit.apply();
    }

    /* Blue mana */
    public static synchronized int getBlueMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_blueMana), 0);
    }

    public static synchronized void setBlueMana(@Nullable Context context, int blueMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_blueMana), blueMana);
        edit.apply();
    }

    /* Black mana */
    public static synchronized int getBlackMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_blackMana), 0);
    }

    public static synchronized void setBlackMana(@Nullable Context context, int blackMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_blackMana), blackMana);
        edit.apply();
    }

    /* Red mana */
    public static synchronized int getRedMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_redMana), 0);
    }

    public static synchronized void setRedMana(@Nullable Context context, int redMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_redMana), redMana);
        edit.apply();
    }

    /* Green mana */
    public static synchronized int getGreenMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_greenMana), 0);
    }

    public static synchronized void setGreenMana(@Nullable Context context, int greenMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_greenMana), greenMana);
        edit.apply();
    }

    /* Colorless mana */
    public static synchronized int getColorlessMana(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_colorlessMana), 0);
    }

    public static synchronized void setColorlessMana(@Nullable Context context, int colorlessMana) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_colorlessMana), colorlessMana);
        edit.apply();
    }

    /* Spell count */
    public static synchronized int getSpellCount(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_spellCount), 0);
    }

    public static synchronized void setSpellCount(@Nullable Context context, int spellCount) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_spellCount), spellCount);
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
    public static synchronized String getTradePrice(@Nullable Context context) {
        if (null == context) {
            return "1";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_tradePrice), "1");
    }

    public static synchronized void setTradePrice(@Nullable Context context, String tradePrice) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(context.getString(R.string.key_tradePrice), tradePrice);
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
        Long endTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.key_currentRoundTimer), -1);
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
        if (theme.equals("asd")) {
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

    public static synchronized int getImageCacheSize(@Nullable Context context) {
        if (null == context) {
            return 12;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_imageCacheSize), 12);
    }

    static synchronized int getNumTutorCardsSearches(@Nullable Context context) {
        if (null == context) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.key_num_tutor_cards_searches), 0);
    }

    static synchronized void setNumTutorCardsSearches(@Nullable Context context, int NumTutorCardsSearches) {
        if (null == context) {
            return;
        }

        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(context.getString(R.string.key_num_tutor_cards_searches),
                NumTutorCardsSearches);
        edit.apply();
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

    public static synchronized String getDeckPrice(@Nullable Context context) {
        if (null == context) {
            return "1";
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_deckPrice), "1");
    }

}