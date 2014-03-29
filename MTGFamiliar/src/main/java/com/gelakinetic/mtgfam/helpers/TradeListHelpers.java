package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.Arrays;

public class TradeListHelpers {

	public static boolean canBeFoil(String setCode, CardDbAdapter mDbHelper) throws FamiliarDbException {
		String[] extraSets = {"UNH", "US", "UL", "UD", "P3", "MM", "NE", "PY", "IN", "PS", "7E", "AP", "OD", "TO", "JU", "ON", "LE", "SC"};
		ArrayList<String> nonModernLegalSets = new ArrayList<String>(Arrays.asList(extraSets));
		for (String value : nonModernLegalSets) {
			if (value.equals(setCode)) {
				return true;
			}
		}

		return mDbHelper.isModernLegalSet(setCode);
	}
}
