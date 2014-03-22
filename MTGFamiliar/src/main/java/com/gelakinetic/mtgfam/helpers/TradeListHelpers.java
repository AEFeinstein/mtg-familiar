package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.Arrays;

public class TradeListHelpers {

	public static final String card_not_found = "Card Not Found";
	public static final String database_busy = "Database Busy";

	public static MtgCard FetchMtgCard(Context ctx, String name, String set) throws FamiliarDbException {
		MtgCard data = new MtgCard();
		CardDbAdapter dbAdapter = new CardDbAdapter(ctx);
		try {
			Cursor card;

			if (set == null || set.equals("")) {
				card = dbAdapter.fetchCardByName(name, CardDbAdapter.allData);
			}
			else {
				card = dbAdapter.fetchCardByNameAndSet(name, set, CardDbAdapter.allData);
			}

			if (card.moveToFirst()) {
				data.name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
				data.setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
				data.tcgName = dbAdapter.getTCGname(data.setCode);
				data.type = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TYPE));
				data.manaCost = card.getString(card.getColumnIndex(CardDbAdapter.KEY_MANACOST));
				data.ability = card.getString(card.getColumnIndex(CardDbAdapter.KEY_ABILITY));
				data.power = card.getFloat(card.getColumnIndex(CardDbAdapter.KEY_POWER));
				data.toughness = card.getFloat(card.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
				data.loyalty = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
				data.rarity = (char) card.getInt(card.getColumnIndex(CardDbAdapter.KEY_RARITY));
				data.number = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			}
			card.close();
		} catch (SQLiteException e) {
			data.message = card_not_found;
		} catch (IllegalStateException e) {
			data.message = database_busy;
		}
		dbAdapter.close();
		return data;
	}

	public static boolean canBeFoil(String setCode, CardDbAdapter mDbHelper) throws FamiliarDbException {
		String[] extraSets = {"UNH", "US", "UL", "6E", "UD", "P3", "MM", "NE", "PY", "IN", "PS", "7E", "AP", "OD", "TO", "JU", "ON", "LE", "SC"};
		ArrayList<String> nonModernLegalSets = new ArrayList<String>(Arrays.asList(extraSets));
		for (String value : nonModernLegalSets) {
			if (value.equals(setCode)) {
				return true;
			}
		}

		return mDbHelper.isModernLegalSet(setCode);
	}
}
