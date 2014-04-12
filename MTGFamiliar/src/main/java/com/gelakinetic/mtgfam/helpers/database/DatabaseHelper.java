package com.gelakinetic.mtgfam.helpers.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

/**
 * Created by Adam Feinstein on 4/12/2014.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, CardDbAdapter.DATABASE_NAME, null, CardDbAdapter.DATABASE_VERSION);
		if (CardDbAdapter.isDbOutOfDate(context)) {
			CardDbAdapter.copyDB(context);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CardDbAdapter.DATABASE_CREATE_CARDS);
		db.execSQL(CardDbAdapter.DATABASE_CREATE_SETS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}