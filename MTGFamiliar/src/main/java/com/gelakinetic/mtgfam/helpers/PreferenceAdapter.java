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
 * 1. Choose a name for the preference and add it to the list of static final Strings
 * 2. Add getter/setter methods to this class for the new preference
 * 3. Channel all accesses to the preference through the new getter/setter methods
 * 4. ???
 * 5. Profit (or at least sanity)!
 */

public class PreferenceAdapter {

	private final Context context;
	private final SharedPreferences prefs;
	private final Editor edit;

	private static final String LAST_VERSION = "lastVersion"; //int, default 0
	private static final String LAST_LEGALITY_UPDATE = "lastLegalityUpdate"; //int, default 0
	private static final String WHITE_MANA = "whiteMana"; //int, default 0
	private static final String BLUE_MANA = "blueMana"; //int, default 0
	private static final String BLACK_MANA = "blackMana"; //int, default 0
	private static final String RED_MANA = "redMana"; //int, default 0
	private static final String GREEN_MANA = "greenMana"; //int, default 0
	private static final String COLORLESS_MANA = "colorlessMana"; //int, default 0
	private static final String SPELL_COUNT = "spellCount"; //int, default 0

	private static final String LAST_RULES_UPDATE = "lastRulesUpdate"; //long, default BuildDate.get(context).getTime()
	private static final String LAST_MTR_UPDATE = "lastMTRUpdate"; //long, default BuildDate.get(context).getTime()
	private static final String LAST_IPG_UPDATE = "lastIPGUpdate"; //long, default BuildDate.get(context).getTime()

	private static final String TTS_SHOW_DIALOG = "ttsShowDialog"; //boolean, default true
	private static final String AUTO_UPDATE = "autoupdate"; //boolean, default true
	private static final String CONSOLIDATE_SEARCH = "consolidateSearch"; //boolean, default true
	private static final String PIC_FIRST = "picFirst"; //boolean, default false
	private static final String SCROLL_RESULTS = "scrollresults"; //boolean, default false
	private static final String WAKELOCK = "wakelock"; //boolean, default true
	private static final String SET_PREF = "setPref"; //boolean, default true
	private static final String MANA_COST_PREF = "manacostPref"; //boolean, default true
	private static final String TYPE_PREF = "typePref"; //boolean, default true
	private static final String ABILITY_PREF = "abilityPref"; //boolean, default true
	private static final String PT_PREF = "ptPref"; //boolean, default true
	private static final String FIFTEEN_MINUTE_PREF = "fifteenMinutePref"; //boolean, default false
	private static final String TEN_MINUTE_PREF = "tenMinutePref"; //boolean, default false
	private static final String FIVE_MINUTE_PREF = "fiveMinutePref"; //boolean, default false
	private static final String SHOW_TOTAL_WISHLIST_PRICE = "showTotalPriceWishlistPref"; //boolean, default false
	private static final String SHOW_INDIVIDUAL_WISHLIST_PRICES = "showIndividualPricesWishlistPref"; //boolean, default true
	private static final String VERBOSE_WISHLIST = "verboseWishlistPref"; //boolean, default false
	private static final String MOJHOSTO_FIRST_TIME = "mojhostoFirstTime"; //boolean, default true

	private static final String UPDATE_FREQUENCY = "updatefrequency"; //String, default "3"
	private static final String DEFAULT_FRAGMENT = "defaultFragment"; //String, default R.string.main_card_search
	private static final String CARD_LANGUAGE = "cardlanguage"; //String, default "en"
	private static final String DISPLAY_MODE = "displayMode"; //String, default "0"
	private static final String PLAYER_DATA = "player_data"; //String, default null
	private static final String ROUND_LENGTH = "roundLength"; //String, default "50"
	private static final String TIMER_SOUND = "timerSound"; //String, default RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
	private static final String TRADE_PRICE = "tradePrice"; //String, default "1"
	private static final String LEGALITY_DATE = "date"; //String, default null
	private static final String LAST_UPDATE = "lastUpdate"; //String, default ""
	private static final String LAST_TCGNAME_UPDATE = "lastTCGNameUpdate"; //String, default ""
	private static final String LIFE_TIMER = "lifeTimer"; //String, default "1000"

	private static final String CURRENT_ROUND_TIMER = "currentRoundTimer"; //int, default "-1"
	private static final String BOUNCE_DRAWER = "bounceDrawer";
	private static final String WIDGET_BUTTONS = "widgetButtons";

	public PreferenceAdapter(Context context) {
		this.context = context;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.edit = this.prefs.edit();
	}
	
	/* Int preferences */
	//Last version

	public synchronized int getLastVersion() {
		return this.prefs.getInt(LAST_VERSION, 0);
	}

	public synchronized void setLastVersion(int lastVersion) {
		this.edit.putInt(LAST_VERSION, lastVersion);
		this.edit.commit();
	}

	//Last legality update
	public synchronized int getLastLegalityUpdate() {
		return this.prefs.getInt(LAST_LEGALITY_UPDATE, 0);
	}

	public synchronized void setLastLegalityUpdate(int lastLegalityUpdate) {
		this.edit.putInt(LAST_LEGALITY_UPDATE, lastLegalityUpdate);
		this.edit.commit();
	}

	//White mana
	public synchronized int getWhiteMana() {
		return this.prefs.getInt(WHITE_MANA, 0);
	}

	public synchronized void setWhiteMana(int whiteMana) {
		this.edit.putInt(WHITE_MANA, whiteMana);
		this.edit.commit();
	}

	//Blue mana
	public synchronized int getBlueMana() {
		return this.prefs.getInt(BLUE_MANA, 0);
	}

	public synchronized void setBlueMana(int blueMana) {
		this.edit.putInt(BLUE_MANA, blueMana);
		this.edit.commit();
	}

	//Black mana
	public synchronized int getBlackMana() {
		return this.prefs.getInt(BLACK_MANA, 0);
	}

	public synchronized void setBlackMana(int blackMana) {
		this.edit.putInt(BLACK_MANA, blackMana);
		this.edit.commit();
	}

	//Red mana
	public synchronized int getRedMana() {
		return this.prefs.getInt(RED_MANA, 0);
	}

	public synchronized void setRedMana(int redMana) {
		this.edit.putInt(RED_MANA, redMana);
		this.edit.commit();
	}

	//Green mana
	public synchronized int getGreenMana() {
		return this.prefs.getInt(GREEN_MANA, 0);
	}

	public synchronized void setGreenMana(int greenMana) {
		this.edit.putInt(GREEN_MANA, greenMana);
		this.edit.commit();
	}

	//Colorless mana
	public synchronized int getColorlessMana() {
		return this.prefs.getInt(COLORLESS_MANA, 0);
	}

	public synchronized void setColorlessMana(int colorlessMana) {
		this.edit.putInt(COLORLESS_MANA, colorlessMana);
		this.edit.commit();
	}

	//Spell count
	public synchronized int getSpellCount() {
		return this.prefs.getInt(SPELL_COUNT, 0);
	}

	public synchronized void setSpellCount(int spellCount) {
		this.edit.putInt(SPELL_COUNT, spellCount);
		this.edit.commit();
	}

	/* Long preferences */
	//Last rules update
	public synchronized long getLastRulesUpdate() {
		return this.prefs.getLong(LAST_RULES_UPDATE, BuildDate.get(this.context).getTime());
	}

	public synchronized void setLastRulesUpdate(long lastRulesUpdate) {
		this.edit.putLong(LAST_RULES_UPDATE, lastRulesUpdate);
		this.edit.commit();
	}

	//Last MTR update
	public synchronized long getLastMTRUpdate() {
		return this.prefs.getLong(LAST_MTR_UPDATE, 0);
	}

	public synchronized void setLastMTRUpdate(long lastMTRUpdate) {
		this.edit.putLong(LAST_MTR_UPDATE, lastMTRUpdate);
		this.edit.commit();
	}

	//Last IPG update
	public synchronized long getLastIPGUpdate() {
		return this.prefs.getLong(LAST_IPG_UPDATE, 0);
	}

	public synchronized void setLastIPGUpdate(long lastIPGUpdate) {
		this.edit.putLong(LAST_IPG_UPDATE, lastIPGUpdate);
		this.edit.commit();
	}


	/* Boolean preferences */
	//TTS show dialog
	public synchronized boolean getTtsShowDialog() {
		return this.prefs.getBoolean(TTS_SHOW_DIALOG, true);
	}

	public synchronized void setTtsShowDialog(boolean ttsShowDialog) {
		this.edit.putBoolean(TTS_SHOW_DIALOG, ttsShowDialog);
		this.edit.commit();
	}

	//Auto-update
	public synchronized boolean getAutoUpdate() {
		return this.prefs.getBoolean(AUTO_UPDATE, true);
	}

	public synchronized void setAutoUpdate(boolean autoUpdate) {
		this.edit.putBoolean(AUTO_UPDATE, autoUpdate);
		this.edit.commit();
	}

	//Consolidate search
	public synchronized boolean getConsolidateSearch() {
		return this.prefs.getBoolean(CONSOLIDATE_SEARCH, true);
	}

	public synchronized void setConsolidateSearch(boolean consolidateSearch) {
		this.edit.putBoolean(CONSOLIDATE_SEARCH, consolidateSearch);
		this.edit.commit();
	}

	//Pic first
	public synchronized boolean getPicFirst() {
		return this.prefs.getBoolean(PIC_FIRST, false);
	}

	public synchronized void setPicFirst(boolean picFirst) {
		this.edit.putBoolean(PIC_FIRST, picFirst);
		this.edit.commit();
	}

	//Scroll results
	public synchronized boolean getScrollResults() {
		return this.prefs.getBoolean(SCROLL_RESULTS, false);
	}

	public synchronized void setScrollResults(boolean scrollResults) {
		this.edit.putBoolean(SCROLL_RESULTS, scrollResults);
		this.edit.commit();
	}

	//Wakelock
	public synchronized boolean getWakelock() {
		return this.prefs.getBoolean(WAKELOCK, true);
	}

	public synchronized void setWakelock(boolean wakelock) {
		this.edit.putBoolean(WAKELOCK, wakelock);
		this.edit.commit();
	}

	//Set pref
	public synchronized boolean getSetPref() {
		return this.prefs.getBoolean(SET_PREF, true);
	}

	public synchronized void setSetPref(boolean setPref) {
		this.edit.putBoolean(SET_PREF, setPref);
		this.edit.commit();
	}

	//Mana cost pref
	public synchronized boolean getManaCostPref() {
		return this.prefs.getBoolean(MANA_COST_PREF, true);
	}

	public synchronized void setManaCostPref(boolean manaCostPref) {
		this.edit.putBoolean(MANA_COST_PREF, manaCostPref);
		this.edit.commit();
	}

	//Type pref
	public synchronized boolean getTypePref() {
		return this.prefs.getBoolean(TYPE_PREF, true);
	}

	public synchronized void setTypePref(boolean typePref) {
		this.edit.putBoolean(TYPE_PREF, typePref);
		this.edit.commit();
	}

	//Ability pref
	public synchronized boolean getAbilityPref() {
		return this.prefs.getBoolean(ABILITY_PREF, true);
	}

	public synchronized void setAbilityPref(boolean abilityPref) {
		this.edit.putBoolean(ABILITY_PREF, abilityPref);
		this.edit.commit();
	}

	//P/T pref
	public synchronized boolean getPTPref() {
		return this.prefs.getBoolean(PT_PREF, true);
	}

	public synchronized void setPTPref(boolean ptPref) {
		this.edit.putBoolean(PT_PREF, ptPref);
		this.edit.commit();
	}

	//15-minute warning pref
	public synchronized boolean getFifteenMinutePref() {
		return this.prefs.getBoolean(FIFTEEN_MINUTE_PREF, false);
	}

	public synchronized void setFifteenMinutePref(boolean fifteenMinutePref) {
		this.edit.putBoolean(FIFTEEN_MINUTE_PREF, fifteenMinutePref);
		this.edit.commit();
	}

	//10-minute warning pref
	public synchronized boolean getTenMinutePref() {
		return this.prefs.getBoolean(TEN_MINUTE_PREF, false);
	}

	public synchronized void setTenMinutePref(boolean tenMinutePref) {
		this.edit.putBoolean(TEN_MINUTE_PREF, tenMinutePref);
		this.edit.commit();
	}

	//5-minute warning pref
	public synchronized boolean getFiveMinutePref() {
		return this.prefs.getBoolean(FIVE_MINUTE_PREF, false);
	}

	public synchronized void setFiveMinutePref(boolean fiveMinutePref) {
		this.edit.putBoolean(FIVE_MINUTE_PREF, fiveMinutePref);
		this.edit.commit();
	}

	//Show total wishlist price
	public synchronized boolean getShowTotalWishlistPrice() {
		return this.prefs.getBoolean(SHOW_TOTAL_WISHLIST_PRICE, false);
	}

	public synchronized void setShowTotalWishlistPrice(boolean showTotalWishlistPrice) {
		this.edit.putBoolean(SHOW_TOTAL_WISHLIST_PRICE, showTotalWishlistPrice);
		this.edit.commit();
	}

	//Show individual wishlist prices
	public synchronized boolean getShowIndividualWishlistPrices() {
		return this.prefs.getBoolean(SHOW_INDIVIDUAL_WISHLIST_PRICES, true);
	}

	public synchronized void setShowIndividualWishlistPrices(boolean showIndividualWishlistPrices) {
		this.edit.putBoolean(SHOW_INDIVIDUAL_WISHLIST_PRICES, showIndividualWishlistPrices);
		this.edit.commit();
	}

	//Verbose wishlist
	public synchronized boolean getVerboseWishlist() {
		return this.prefs.getBoolean(VERBOSE_WISHLIST, false);
	}

	public synchronized void setVerboseWishlist(boolean verboseWishlist) {
		this.edit.putBoolean(VERBOSE_WISHLIST, verboseWishlist);
		this.edit.commit();
	}

	//MoJhoSto first time
	public synchronized boolean getMojhostoFirstTime() {
		return this.prefs.getBoolean(MOJHOSTO_FIRST_TIME, true);
	}

	public synchronized void setMojhostoFirstTime(boolean mojhostoFirstTime) {
		this.edit.putBoolean(MOJHOSTO_FIRST_TIME, mojhostoFirstTime);
		this.edit.commit();
	}


	/* String preferences */
	//Update frequency
	public synchronized String getUpdateFrequency() {
		return this.prefs.getString(UPDATE_FREQUENCY, "3");
	}

	public synchronized void setUpdateFrequency(String updateFrequency) {
		this.edit.putString(UPDATE_FREQUENCY, updateFrequency);
		this.edit.commit();
	}

	//Card language
	public synchronized String getCardLanguage() {
		return this.prefs.getString(CARD_LANGUAGE, "en");
	}

	public synchronized void setCardLanguage(String cardLanguage) {
		this.edit.putString(CARD_LANGUAGE, cardLanguage);
		this.edit.commit();
	}

	//Default fragment
	public synchronized String getDefaultFragment() {
		return this.prefs.getString(DEFAULT_FRAGMENT, this.context.getString(R.string.main_card_search));
	}

	public synchronized void setDefaultFragment(String defaultFragment) {
		this.edit.putString(DEFAULT_FRAGMENT, defaultFragment);
		this.edit.commit();
	}

	//Display mode
	public synchronized String getDisplayMode() {
		return this.prefs.getString(DISPLAY_MODE, "0");
	}

	public synchronized void setDisplayMode(String displayMode) {
		this.edit.putString(DISPLAY_MODE, displayMode);
		this.edit.commit();
	}

	//Player data
	public synchronized String getPlayerData() {
		return this.prefs.getString(PLAYER_DATA, null);
	}

	public synchronized void setPlayerData(String playerData) {
		this.edit.putString(PLAYER_DATA, playerData);
		this.edit.commit();
	}

	//Round length
	public synchronized String getRoundLength() {
		return this.prefs.getString(ROUND_LENGTH, "50");
	}

	public synchronized void setRoundLength(String roundLength) {
		this.edit.putString(ROUND_LENGTH, roundLength);
		this.edit.commit();
	}

	//Timer sound
	public synchronized String getTimerSound() {
		return this.prefs.getString(TIMER_SOUND, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
	}

	public synchronized void setTimerSound(String timerSound) {
		this.edit.putString(TIMER_SOUND, timerSound);
		this.edit.commit();
	}

	//Trade price
	public synchronized String getTradePrice() {
		return this.prefs.getString(TRADE_PRICE, "1");
	}

	public synchronized void setTradePrice(String tradePrice) {
		this.edit.putString(TRADE_PRICE, tradePrice);
		this.edit.commit();
	}

	//Date
	public synchronized String getLegalityDate() {
		return this.prefs.getString(LEGALITY_DATE, null);
	}

	public synchronized void setLegalityDate(String date) {
		this.edit.putString(LEGALITY_DATE, date);
		this.edit.commit();
	}

	//Last update
	public synchronized String getLastUpdate() {
		return this.prefs.getString(LAST_UPDATE, "");
	}

	public synchronized void setLastUpdate(String lastUpdate) {
		this.edit.putString(LAST_UPDATE, lastUpdate);
		this.edit.commit();
	}

	//Last TCG name update
	public synchronized String getLastTCGNameUpdate() {
		return this.prefs.getString(LAST_TCGNAME_UPDATE, "");
	}

	public synchronized void setLastTCGNameUpdate(String lastTCGNameUpdate) {
		this.edit.putString(LAST_TCGNAME_UPDATE, lastTCGNameUpdate);
		this.edit.commit();
	}

	//Life Counter Timer
	public synchronized String getLifeTimer() {
		return this.prefs.getString(LIFE_TIMER, "1000");
	}

	public synchronized void setLifeTimer(String milliseconds) {
		this.edit.putString(LIFE_TIMER, milliseconds);
		this.edit.commit();
	}

	// Round timer finish time
	public synchronized long getRoundTimerEnd() {
		Long endTime = prefs.getLong(CURRENT_ROUND_TIMER, -1);
		/* If the timer has expired, set it as -1 */
		if (endTime < System.currentTimeMillis()) {
			endTime = -1l;
			setRoundTimerEnd(endTime);
		}
		return endTime;
	}

	public synchronized void setRoundTimerEnd(long milliseconds) {
		this.edit.putLong(CURRENT_ROUND_TIMER, milliseconds);
		this.edit.commit();
	}

	//Bounce Drawer, default true
	public synchronized boolean getBounceDrawer() {
		return this.prefs.getBoolean(BOUNCE_DRAWER, true);
	}

	public synchronized void setBounceDrawer(boolean value) {
		this.edit.putBoolean(BOUNCE_DRAWER, value);
		this.edit.commit();
	}

	public synchronized Set<String> getWidgetButtons() {
		return this.prefs.getStringSet(WIDGET_BUTTONS, new HashSet<String>(
				Arrays.asList(context.getResources().getStringArray(R.array.default_widget_buttons_array_entries))));
	}

	public synchronized void setWidgetButtons(Set<String> buttons) {
		this.edit.putStringSet(WIDGET_BUTTONS, buttons);
		this.edit.commit();
	}

}