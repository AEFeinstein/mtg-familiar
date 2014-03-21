package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;
import java.util.Arrays;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;

public class TradeListHelpers {

	public static final String	card_not_found		= "Card Not Found";
	public static final String	mangled_url				= "Mangled URL";
	public static final String	database_busy			= "Database Busy";
	public static final String	fetch_failed			= "Fetch Failed";
	public static final String	familiarDbException		= "FamiliarDbException";
	
	public static CardData FetchCardData(CardData _data, CardDbAdapter mDbHelper) throws FamiliarDbException {
		CardData data = _data;
		try {
			Cursor card;
			boolean opened = false;
			if(!mDbHelper.mDb.isOpen()) {
				mDbHelper.openReadable();
				opened = true;
			}

			if (data.setCode == null || data.setCode.equals(""))
				card = mDbHelper.fetchCardByName(data.name, CardDbAdapter.allData);
			else
				card = mDbHelper.fetchCardByNameAndSet(data.name, data.setCode);

			if(opened){
				mDbHelper.close();
			}
			if (card.moveToFirst()) {
				data.name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
				data.setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
				data.tcgName = mDbHelper.getTCGname(data.setCode);
				data.type = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TYPE));
				data.cost = card.getString(card.getColumnIndex(CardDbAdapter.KEY_MANACOST));
				data.ability = card.getString(card.getColumnIndex(CardDbAdapter.KEY_ABILITY));
				data.power = card.getString(card.getColumnIndex(CardDbAdapter.KEY_POWER));
				data.toughness = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
				data.loyalty = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
				data.rarity = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_RARITY));
				data.cardNumber = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			}
			card.close();
		}
		catch (SQLiteException e) {
			data.message = card_not_found;
		}
		catch (IllegalStateException e) {
			data.message = database_busy;
		}
		return data;
	}
	
	public static boolean canBeFoil(String setCode, CardDbAdapter mDbHelper) throws FamiliarDbException {
		String[] extraSets = {"UNH", "US","UL","6E","UD","P3","MM","NE","PY","IN","PS","7E","AP","OD","TO","JU","ON","LE","SC"};
		ArrayList<String> nonModernLegalSets = new ArrayList<String>(Arrays.asList(extraSets));
		for (String value : nonModernLegalSets)
			if (value.equals(setCode))
				return true;
		
		if (mDbHelper.isModernLegalSet(setCode))
			return true;
		return false;
	}

	public class CardData implements Cloneable {

		public String	name;
		public String	cardNumber;
		public String	tcgName;
		public String	setCode;
		public int		numberOf;
		public int		price;			// In cents
		public String	message;
		public String	type;
		public String	cost;
		public String	ability;
		public String	power;
		public String	toughness;
		public int		loyalty;
		public int		rarity;
		public boolean  customPrice = false; //default is false as all cards should first grab internet prices.
		public boolean 	foil = false;

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, String type, String cost,
				String ability, String p, String t, int loyalty, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.type = type;
			this.cost = cost;
			this.ability = ability;
			this.power = p;
			this.toughness = t;
			this.loyalty = loyalty;
			this.rarity = rarity;
		}

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.rarity = rarity;
		}
		
		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity, boolean customPrice, boolean foil) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.rarity = rarity;
			this.customPrice = customPrice;
			this.foil = foil;
		}

		public CardData(String cardName, String cardSet, int numberOf, String number, int rarity) {
			this.name = cardName;
			this.numberOf = numberOf;
			this.setCode = cardSet;
			this.cardNumber = number;
			this.rarity = rarity;
		}

		public String getPriceString() {
			return "$" + String.valueOf(this.price / 100) + "." + String.format("%02d", this.price % 100);
		}

		public boolean hasPrice() {
			return this.message == null || this.message.length() == 0;
		}
		
		public void SetIsCustomPrice(){
			customPrice = true;
		}
		
		public void setIsFoil(boolean foil){
			this.foil = foil;
		}		

		public static final String	delimiter	= "%";

		public String toString() {
			return this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.cardNumber + delimiter + this.rarity + delimiter + this.foil + '\n';
		}

		public String toString(int side) {
			return side + delimiter + this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.customPrice + delimiter + this.price + delimiter + this.foil + '\n';
		}

		public String toReadableString(boolean includeTcgName) {
			return String.valueOf(this.numberOf) + ' ' + this.name + (this.foil ? " - Foil " : "") + (includeTcgName?" (" + this.tcgName + ')':"") + '\n';
		}

		public Object clone() { 
	        try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			} 
		} 
	}
}
