package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;

import com.gelakinetic.mtgfam.R;

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

    private final Context context;
    private final SharedPreferences prefs;
    private final Editor edit;

    /**
     * Constructor
     *
     * @param context A context to get string keys and commit preferences with
     */
    public PreferenceAdapter(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.edit = this.prefs.edit();
    }

    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        this.prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        this.prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /* Last version */
    public synchronized int getLastVersion() {
        return this.prefs.getInt(context.getString(R.string.key_lastVersion), 0);
    }

    public synchronized void setLastVersion(int lastVersion) {
        this.edit.putInt(context.getString(R.string.key_lastVersion), lastVersion);
        this.edit.commit();
    }

    /* Last legality update */
    public synchronized int getLastLegalityUpdate() {
        return this.prefs.getInt(context.getString(R.string.key_lastLegalityUpdate), 0);
    }

    public synchronized void setLastLegalityUpdate(int lastLegalityUpdate) {
        this.edit.putInt(context.getString(R.string.key_lastLegalityUpdate), lastLegalityUpdate);
        this.edit.commit();
    }

    /* White mana */
    public synchronized int getWhiteMana() {
        return this.prefs.getInt(context.getString(R.string.key_whiteMana), 0);
    }

    public synchronized void setWhiteMana(int whiteMana) {
        this.edit.putInt(context.getString(R.string.key_whiteMana), whiteMana);
        this.edit.commit();
    }

    /* Blue mana */
    public synchronized int getBlueMana() {
        return this.prefs.getInt(context.getString(R.string.key_blueMana), 0);
    }

    public synchronized void setBlueMana(int blueMana) {
        this.edit.putInt(context.getString(R.string.key_blueMana), blueMana);
        this.edit.commit();
    }

    /* Black mana */
    public synchronized int getBlackMana() {
        return this.prefs.getInt(context.getString(R.string.key_blackMana), 0);
    }

    public synchronized void setBlackMana(int blackMana) {
        this.edit.putInt(context.getString(R.string.key_blackMana), blackMana);
        this.edit.commit();
    }

    /* Red mana */
    public synchronized int getRedMana() {
        return this.prefs.getInt(context.getString(R.string.key_redMana), 0);
    }

    public synchronized void setRedMana(int redMana) {
        this.edit.putInt(context.getString(R.string.key_redMana), redMana);
        this.edit.commit();
    }

    /* Green mana */
    public synchronized int getGreenMana() {
        return this.prefs.getInt(context.getString(R.string.key_greenMana), 0);
    }

    public synchronized void setGreenMana(int greenMana) {
        this.edit.putInt(context.getString(R.string.key_greenMana), greenMana);
        this.edit.commit();
    }

    /* Colorless mana */
    public synchronized int getColorlessMana() {
        return this.prefs.getInt(context.getString(R.string.key_colorlessMana), 0);
    }

    public synchronized void setColorlessMana(int colorlessMana) {
        this.edit.putInt(context.getString(R.string.key_colorlessMana), colorlessMana);
        this.edit.commit();
    }

    /* Spell count */
    public synchronized int getSpellCount() {
        return this.prefs.getInt(context.getString(R.string.key_spellCount), 0);
    }

    public synchronized void setSpellCount(int spellCount) {
        this.edit.putInt(context.getString(R.string.key_spellCount), spellCount);
        this.edit.commit();
    }

    /* Last rules update */
    public synchronized long getLastRulesUpdate() {
        return this.prefs.getLong(context.getString(R.string.key_lastRulesUpdate), BuildDate.get(this.context).getTime());
    }

    public synchronized void setLastRulesUpdate(long lastRulesUpdate) {
        this.edit.putLong(context.getString(R.string.key_lastRulesUpdate), lastRulesUpdate);
        this.edit.commit();
    }

    /* Last MTR update */
    public synchronized long getLastMTRUpdate() {
        return this.prefs.getLong(context.getString(R.string.key_lastMTRUpdate), 0);
    }

    public synchronized void setLastMTRUpdate(long lastMTRUpdate) {
        this.edit.putLong(context.getString(R.string.key_lastMTRUpdate), lastMTRUpdate);
        this.edit.commit();
    }

    /* Last IPG update */
    public synchronized long getLastIPGUpdate() {
        return this.prefs.getLong(context.getString(R.string.key_lastIPGUpdate), 0);
    }

    public synchronized void setLastIPGUpdate(long lastIPGUpdate) {
        this.edit.putLong(context.getString(R.string.key_lastIPGUpdate), lastIPGUpdate);
        this.edit.commit();
    }

    /* Last JAR update */
    public synchronized long getLastJARUpdate() {
        return this.prefs.getLong(context.getString(R.string.key_lastJARUpdate), 0);
    }

    public synchronized void setLastJARUpdate(long lastJARUpdate) {
        this.edit.putLong(context.getString(R.string.key_lastJARUpdate), lastJARUpdate);
        this.edit.commit();
    }

    /* TTS show dialog */
    public synchronized boolean getTtsShowDialog() {
        return this.prefs.getBoolean(context.getString(R.string.key_ttsShowDialog), true);
    }

    public synchronized void setTtsShowDialog() {
        this.edit.putBoolean(context.getString(R.string.key_ttsShowDialog), false);
        this.edit.commit();
    }

    /* Auto-update */
    public synchronized boolean getAutoUpdate() {
        return this.prefs.getBoolean(context.getString(R.string.key_autoupdate), true);
    }

    /* Consolidate search */
    public synchronized boolean getConsolidateSearch() {
        return this.prefs.getBoolean(context.getString(R.string.key_consolidateSearch), true);
    }

    /* Pic first */
    public synchronized boolean getPicFirst() {
        return this.prefs.getBoolean(context.getString(R.string.key_picFirst), false);
    }

    /* Keep Screen On */
    public synchronized boolean getKeepScreenOn() {
        return this.prefs.getBoolean(context.getString(R.string.key_wakelock), true);
    }

    /* Dim Screen */
    public synchronized boolean getDimScreen() {
        return this.prefs.getBoolean(context.getString(R.string.key_dimlock), true);
    }

    /* Percentage to dim screen */
    public synchronized int getDimLevel() {
        return this.prefs.getInt(context.getString(R.string.key_dimlevel), 1);
    }

    /* Set pref */
    public synchronized boolean getSetPref() {
        return this.prefs.getBoolean(context.getString(R.string.key_setPref), true);
    }

    /* Mana cost pref */
    public synchronized boolean getManaCostPref() {
        return this.prefs.getBoolean(context.getString(R.string.key_manacostPref), true);
    }

    /* Type pref */
    public synchronized boolean getTypePref() {
        return this.prefs.getBoolean(context.getString(R.string.key_typePref), true);
    }

    /* Ability pref */
    public synchronized boolean getAbilityPref() {
        return this.prefs.getBoolean(context.getString(R.string.key_abilityPref), true);
    }

    /* P/T pref */
    public synchronized boolean getPTPref() {
        return this.prefs.getBoolean(context.getString(R.string.key_ptPref), true);
    }

    /* 15-minute warning pref */
    public synchronized boolean getFifteenMinutePref() {
        return this.prefs.getBoolean(context.getString(R.string.key_fifteenMinutePref), false);
    }

    public synchronized void setFifteenMinutePref(boolean fifteenMinutePref) {
        this.edit.putBoolean(context.getString(R.string.key_fifteenMinutePref), fifteenMinutePref);
        this.edit.commit();
    }

    /* 10-minute warning pref */
    public synchronized boolean getTenMinutePref() {
        return this.prefs.getBoolean(context.getString(R.string.key_tenMinutePref), false);
    }

    public synchronized void setTenMinutePref(boolean tenMinutePref) {
        this.edit.putBoolean(context.getString(R.string.key_tenMinutePref), tenMinutePref);
        this.edit.commit();
    }

    /* 5-minute warning pref */
    public synchronized boolean getFiveMinutePref() {
        return this.prefs.getBoolean(context.getString(R.string.key_fiveMinutePref), false);
    }

    public synchronized void setFiveMinutePref(boolean fiveMinutePref) {
        this.edit.putBoolean(context.getString(R.string.key_fiveMinutePref), fiveMinutePref);
        this.edit.commit();
    }

    /* Show total wishlist price */
    public synchronized boolean getShowTotalWishlistPrice() {
        return this.prefs.getBoolean(context.getString(R.string.key_showTotalPriceWishlistPref), false);
    }

    /* Show individual wishlist prices */
    public synchronized boolean getShowIndividualWishlistPrices() {
        return this.prefs.getBoolean(context.getString(R.string.key_showIndividualPricesWishlistPref), true);
    }

    /* Verbose wishlist */
    public synchronized boolean getVerboseWishlist() {
        return this.prefs.getBoolean(context.getString(R.string.key_verboseWishlistPref), false);
    }

    /* MoJhoSto first time */
    public synchronized boolean getMojhostoFirstTime() {
        return this.prefs.getBoolean(context.getString(R.string.key_mojhostoFirstTime), true);
    }

    public synchronized void setMojhostoFirstTime() {
        this.edit.putBoolean(context.getString(R.string.key_mojhostoFirstTime), false);
        this.edit.commit();
    }


    /* String preferences */
    /* Update frequency */
    public synchronized String getUpdateFrequency() {
        return this.prefs.getString(context.getString(R.string.key_updatefrequency), "3");
    }

    /* Card language */
    public synchronized String getCardLanguage() {
        return this.prefs.getString(context.getString(R.string.key_cardlanguage), "en");
    }

    /* Default fragment */
    public synchronized String getDefaultFragment() {
        return this.prefs.getString(context.getString(R.string.key_defaultFragment), this.context.getString(R.string.main_card_search));
    }

    /* Display mode */
    public synchronized String getDisplayMode() {
        return this.prefs.getString(context.getString(R.string.key_displayMode), "0");
    }

    public synchronized void setDisplayMode(String displayMode) {
        this.edit.putString(context.getString(R.string.key_displayMode), displayMode);
        this.edit.commit();
    }

    /* Player data */
    public synchronized String getPlayerData() {
        return this.prefs.getString(context.getString(R.string.key_player_data), null);
    }

    public synchronized void setPlayerData(String playerData) {
        this.edit.putString(context.getString(R.string.key_player_data), playerData);
        this.edit.commit();
    }

    /* Timer sound */
    public synchronized String getTimerSound() {
        return this.prefs.getString(context.getString(R.string.key_timerSound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
    }

    public synchronized void setTimerSound(String timerSound) {
        this.edit.putString(context.getString(R.string.key_timerSound), timerSound);
        this.edit.commit();
    }

    /* Trade price */
    public synchronized String getTradePrice() {
        return this.prefs.getString(context.getString(R.string.key_tradePrice), "1");
    }

    public synchronized void setTradePrice(String tradePrice) {
        this.edit.putString(context.getString(R.string.key_tradePrice), tradePrice);
        this.edit.commit();
    }

    /* Date */
    public synchronized String getLegalityDate() {
        return this.prefs.getString(context.getString(R.string.key_date), null);
    }

    public synchronized void setLegalityDate(String date) {
        this.edit.putString(context.getString(R.string.key_date), date);
        this.edit.commit();
    }

    /* Last update */
    public synchronized String getLastUpdate() {
        return this.prefs.getString(context.getString(R.string.key_lastUpdate), "");
    }

    public synchronized void setLastUpdate(String lastUpdate) {
        this.edit.putString(context.getString(R.string.key_lastUpdate), lastUpdate);
        this.edit.commit();
    }

    /* Last TCG name update */
    public synchronized String getLastTCGNameUpdate() {
        return this.prefs.getString(context.getString(R.string.key_lastTCGNameUpdate), "");
    }

    public synchronized void setLastTCGNameUpdate(String lastTCGNameUpdate) {
        this.edit.putString(context.getString(R.string.key_lastTCGNameUpdate), lastTCGNameUpdate);
        this.edit.commit();
    }

    /* Life Counter Timer */
    public synchronized String getLifeTimer() {
        return this.prefs.getString(context.getString(R.string.key_lifeTimer), "1000");
    }

    /*  Round timer finish time */
    public synchronized long getRoundTimerEnd() {
        Long endTime = prefs.getLong(context.getString(R.string.key_currentRoundTimer), -1);
        /* If the timer has expired, set it as -1 */
        if (endTime < System.currentTimeMillis()) {
            endTime = -1l;
            setRoundTimerEnd(endTime);
        }
        return endTime;
    }

    public synchronized void setRoundTimerEnd(long milliseconds) {
        this.edit.putLong(context.getString(R.string.key_currentRoundTimer), milliseconds);
        this.edit.commit();
    }

    /* Bounce Drawer, default true */
    public synchronized boolean getBounceDrawer() {
        return this.prefs.getBoolean(context.getString(R.string.key_bounceDrawer), true);
    }

    public synchronized void setBounceDrawer() {
        this.edit.putBoolean(context.getString(R.string.key_bounceDrawer), false);
        this.edit.commit();
    }

    public synchronized Set<String> getWidgetButtons() {
        return this.prefs.getStringSet(context.getString(R.string.key_widgetButtons), new HashSet<>(
                Arrays.asList(context.getResources().getStringArray(R.array.default_widget_buttons_array_entries))));
    }

    public void setWidgetButtons(Set<String> widgetButtons) {
        this.edit.putStringSet(context.getString(R.string.key_widgetButtons), widgetButtons);
        this.edit.commit();
    }

    /* This is slightly different because we want to make sure to commit a theme if one doesn't
     * exist, not just return the default. asd is a nice tag, no?
     */
    public String getTheme() {
        String theme = this.prefs.getString(context.getString(R.string.key_theme), "asd");
        if (theme.equals("asd")) {
            theme = context.getResources().getString(R.string.pref_theme_light);
            setTheme(theme);
        }
        return theme;
    }

    private void setTheme(String theme) {
        this.edit.putString(context.getString(R.string.key_theme), theme);
        this.edit.commit();
    }

    public synchronized String getDCINumber() {
        return this.prefs.getString(context.getString(R.string.key_dci_number), "");
    }

    public void setDCINumber(String dciNumber) {
        this.edit.putString(context.getString(R.string.key_dci_number), dciNumber);
        this.edit.commit();
    }

    public int getImageCacheSize() {
        return this.prefs.getInt(context.getString(R.string.key_imageCacheSize), 12);
    }

}