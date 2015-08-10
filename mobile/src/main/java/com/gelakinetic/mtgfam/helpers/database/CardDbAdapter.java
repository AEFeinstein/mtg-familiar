/**
 * Copyright 2011 Adam Feinstein
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.database;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.MtgSet;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all Cards as well as retrieve or modify a
 * specific Card.
 */
@SuppressWarnings("JavaDoc")
public class CardDbAdapter {

    public static final int DATABASE_VERSION = 61;

    public static final int STAR = -1000;
    public static final int ONE_PLUS_STAR = -1001;
    public static final int TWO_PLUS_STAR = -1002;
    public static final int SEVEN_MINUS_STAR = -1003;
    public static final int STAR_SQUARED = -1004;
    public static final int NO_ONE_CARES = -1005;

    public static final int MOST_RECENT_PRINTING = 0;
    public static final int FIRST_PRINTING = 1;
    public static final int ALL_PRINTINGS = 2;

    public static final String DATABASE_NAME = "data";
    public static final String DATABASE_TABLE_CARDS = "cards";
    public static final String DATABASE_TABLE_SETS = "sets";
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1; // "name";
    public static final String KEY_SET = "expansion";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ABILITY = "cardtext";
    public static final String KEY_COLOR = "color";
    public static final String KEY_MANACOST = "manacost";
    public static final String KEY_CMC = "cmc";
    public static final String KEY_POWER = "power";
    public static final String KEY_TOUGHNESS = "toughness";
    public static final String KEY_RARITY = "rarity";
    public static final String KEY_LOYALTY = "loyalty";
    public static final String KEY_FLAVOR = "flavor";
    public static final String KEY_ARTIST = "artist";
    public static final String KEY_NUMBER = "number";
    public static final String[] allData = {DATABASE_TABLE_CARDS + "." + KEY_ID,
            DATABASE_TABLE_CARDS + "." + KEY_NAME, DATABASE_TABLE_CARDS + "." + KEY_SET,
            DATABASE_TABLE_CARDS + "." + KEY_NUMBER, DATABASE_TABLE_CARDS + "." + KEY_TYPE,
            DATABASE_TABLE_CARDS + "." + KEY_MANACOST, DATABASE_TABLE_CARDS + "." + KEY_ABILITY,
            DATABASE_TABLE_CARDS + "." + KEY_POWER, DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS,
            DATABASE_TABLE_CARDS + "." + KEY_LOYALTY, DATABASE_TABLE_CARDS + "." + KEY_RARITY,
            DATABASE_TABLE_CARDS + "." + KEY_FLAVOR, DATABASE_TABLE_CARDS + "." + KEY_CMC,
            DATABASE_TABLE_CARDS + "." + KEY_COLOR
    };
    public static final String KEY_MULTIVERSEID = "multiverseID";
    public static final String KEY_CODE = "code";
    private static final String KEY_NAME_TCGPLAYER = "name_tcgplayer";
    private static final String KEY_FORMAT = "format";
    public static final String KEY_LEGALITY = "legality";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_SUBCATEGORY = "subcategory";
    public static final String KEY_ENTRY = "entry";
    public static final String KEY_RULE_TEXT = "rule_text";
    public static final String KEY_TERM = "term";
    public static final String KEY_DEFINITION = "definition";
    public static final String KEY_BANNED_LIST = "banned_list";
    public static final String KEY_LEGAL_SETS = "legal_sets";
    public static final int LEGAL = 0;
    public static final int BANNED = 1;
    public static final int RESTRICTED = 2;
    public static final int NOPE = 0;
    public static final int TRANSFORM = 1;
    public static final int FUSE = 2;
    public static final int SPLIT = 3;
    private static final String DATABASE_TABLE_FORMATS = "formats";
    private static final String DATABASE_CREATE_FORMATS = "create table "
            + DATABASE_TABLE_FORMATS + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_NAME
            + " text not null);";
    private static final String DATABASE_TABLE_LEGAL_SETS = "legal_sets";
    private static final String DATABASE_CREATE_LEGAL_SETS = "create table "
            + DATABASE_TABLE_LEGAL_SETS + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_SET
            + " text not null, " + KEY_FORMAT + " text not null);";
    private static final String DATABASE_TABLE_BANNED_CARDS = "banned_cards";
    private static final String DATABASE_CREATE_BANNED_CARDS = "create table "
            + DATABASE_TABLE_BANNED_CARDS + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_NAME
            + " text not null, " + KEY_LEGALITY + " integer not null, "
            + KEY_FORMAT + " text not null);";
    private static final String DATABASE_TABLE_RULES = "rules";
    private static final String DATABASE_TABLE_GLOSSARY = "glossary";
    private static final String DATABASE_CREATE_GLOSSARY = "create table "
            + DATABASE_TABLE_GLOSSARY + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_TERM
            + " text not null, " + KEY_DEFINITION + " text not null);";
    private static final String KEY_RULINGS = "rulings";
    public static final String DATABASE_CREATE_CARDS = "create table "
            + DATABASE_TABLE_CARDS + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_NAME
            + " text not null, " + KEY_SET + " text not null, " + KEY_TYPE
            + " text not null, " + KEY_RARITY + " integer, " + KEY_MANACOST
            + " text, " + KEY_CMC + " integer not null, " + KEY_POWER
            + " real, " + KEY_TOUGHNESS + " real, " + KEY_LOYALTY
            + " integer, " + KEY_ABILITY + " text, " + KEY_FLAVOR + " text, "
            + KEY_ARTIST + " text, " + KEY_NUMBER + " text, "
            + KEY_MULTIVERSEID + " integer not null, " + KEY_COLOR
            + " text not null, " + KEY_RULINGS + " text);";
    private static final String KEY_CODE_MTGI = "code_mtgi";
    private static final String KEY_DATE = "date";
    public static final String DATABASE_CREATE_SETS = "create table "
            + DATABASE_TABLE_SETS + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_NAME
            + " text not null, " + KEY_CODE + " text not null unique, "
            + KEY_CODE_MTGI + " text not null, " + KEY_NAME_TCGPLAYER
            + " text, " + KEY_DATE + " integer);";
    private static final String KEY_POSITION = "position";
    private static final String DATABASE_CREATE_RULES = "create table "
            + DATABASE_TABLE_RULES + "(" + KEY_ID
            + " integer primary key autoincrement, " + KEY_CATEGORY
            + " integer not null, " + KEY_SUBCATEGORY + " integer not null, "
            + KEY_ENTRY + " text null, " + KEY_RULE_TEXT + " text not null, "
            + KEY_POSITION + " integer null);";
    private static final String EXCLUDE_TOKEN = "!";
    private static final int EXCLUDE_TOKEN_START = 1;
    // use a hash map for performance
    private static final HashMap<String, String> mColumnMap = buildColumnMap();
    private static final String DB_NAME = "data";

    /**
     * @param sqLiteDatabase
     * @throws FamiliarDbException
     */
    public static void dropCreateDB(SQLiteDatabase sqLiteDatabase) throws FamiliarDbException {
        try {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDS);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SETS);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RULES);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_GLOSSARY);

            sqLiteDatabase.execSQL(DATABASE_CREATE_CARDS);
            sqLiteDatabase.execSQL(DATABASE_CREATE_SETS);
            sqLiteDatabase.execSQL(DATABASE_CREATE_FORMATS);
            sqLiteDatabase.execSQL(DATABASE_CREATE_LEGAL_SETS);
            sqLiteDatabase.execSQL(DATABASE_CREATE_BANNED_CARDS);
            sqLiteDatabase.execSQL(DATABASE_CREATE_RULES);
            sqLiteDatabase.execSQL(DATABASE_CREATE_GLOSSARY);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param c
     * @param mDb
     * @return
     */
    public static void createCard(MtgCard c, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_NAME, c.name);
        initialValues.put(KEY_SET, c.set);
        initialValues.put(KEY_TYPE, c.type);
        initialValues.put(KEY_RARITY, (int) c.rarity);
        initialValues.put(KEY_MANACOST, c.manaCost);
        initialValues.put(KEY_CMC, c.cmc);
        initialValues.put(KEY_POWER, c.power);
        initialValues.put(KEY_TOUGHNESS, c.toughness);
        initialValues.put(KEY_LOYALTY, c.loyalty);
        initialValues.put(KEY_ABILITY, c.ability);
        initialValues.put(KEY_FLAVOR, c.flavor);
        initialValues.put(KEY_ARTIST, c.artist);
        initialValues.put(KEY_NUMBER, c.number);
        initialValues.put(KEY_COLOR, c.color);
        initialValues.put(KEY_MULTIVERSEID, c.multiverseId);

        mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
    }

    /**
     * @param set
     * @param mDb
     * @return
     */
    public static void createSet(MtgSet set, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_CODE, set.code);
        initialValues.put(KEY_NAME, set.name);
        initialValues.put(KEY_CODE_MTGI, set.codeMagicCards);
        initialValues.put(KEY_DATE, set.date);

        mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
    }

    /**
     * @param name
     * @param code
     * @param mDb
     * @return
     */
    public static void addTcgName(String name, String code, SQLiteDatabase mDb) {
        ContentValues args = new ContentValues();

        args.put(KEY_NAME_TCGPLAYER, name);

        mDb.update(DATABASE_TABLE_SETS, args, KEY_CODE + " = '" + code + "'", null);
    }

    /**
     * @param sqLiteDatabase
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchAllSets(SQLiteDatabase sqLiteDatabase) throws FamiliarDbException {

        Cursor c;
        try {
            c = sqLiteDatabase.query(DATABASE_TABLE_SETS, new String[]{KEY_ID, KEY_NAME, KEY_CODE, KEY_CODE_MTGI}, null,
                    null, null, null, KEY_DATE + " DESC");
        } catch (SQLiteException | IllegalStateException | NullPointerException e) {
            throw new FamiliarDbException(e);
        }

        return c;
    }

    /**
     * @param code
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static boolean doesSetExist(String code, SQLiteDatabase mDb) throws FamiliarDbException {

        String statement = "(" + KEY_CODE + " = '" + code + "')";

        Cursor c;
        int count;
        try {
            c = mDb.query(true, DATABASE_TABLE_SETS, new String[]{KEY_ID}, statement, null, null, null, KEY_NAME, null);
            count = c.getCount();
            c.close();
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        return count > 0;
    }

    /**
     * @param code
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static String getCodeMtgi(String code, SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c;
        try {
            c = mDb.query(DATABASE_TABLE_SETS, new String[]{KEY_CODE_MTGI}, KEY_CODE + "=\"" + code + "\"", null, null,
                    null, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        c.moveToFirst();
        String returnVal = c.getString(c.getColumnIndex(KEY_CODE_MTGI));
        c.close();
        return returnVal;
    }

    /**
     * @param id
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchCard(long id, SQLiteDatabase mDb)
            throws FamiliarDbException {

        String columns[] = new String[]{KEY_ID, KEY_NAME, KEY_SET, KEY_TYPE,
                KEY_RARITY, KEY_MANACOST, KEY_CMC, KEY_POWER,
                KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR,
                KEY_ARTIST, KEY_NUMBER, KEY_COLOR, KEY_MULTIVERSEID};
        Cursor c;
        try {
            c = mDb.query(true, DATABASE_TABLE_CARDS, columns, KEY_ID
                    + "=" + id, null, null, null, KEY_NAME, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (c != null) {
            c.moveToFirst();
        }
        return c;

    }

    /**
     * @param name
     * @param fields
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchCardByName(String name, String[] fields, SQLiteDatabase mDb)
            throws FamiliarDbException {
        // replace lowercase ae with Ae
        name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
        String sql = "SELECT ";
        boolean first = true;
        for (String field : fields) {
            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
            sql += field;
        }
        sql += " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
                + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
                + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
                + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name)
                + " GROUP BY " + DATABASE_TABLE_SETS + "." + KEY_CODE
                + " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
                + " DESC";
        Cursor c;

        try {
            c = mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /**
     * @param l
     * @param fields
     * @param database
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchCardByMultiverseId(long l, String[] fields, SQLiteDatabase database) throws FamiliarDbException {
        String sql = "SELECT ";
        boolean first = true;
        for (String field : fields) {
            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
            sql += field;
        }
        sql += " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
                + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
                + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
                + DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID + " = " + l
                + " GROUP BY " + DATABASE_TABLE_SETS + "." + KEY_CODE
                + " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
                + " DESC";
        Cursor c;

        try {
            c = database.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /**
     * @param mCompressedWishlist
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void fillExtraWishlistData(ArrayList<CompressedWishlistInfo> mCompressedWishlist,
                                             SQLiteDatabase mDb) throws FamiliarDbException {
        String sql = "SELECT ";

        boolean first = true;
        for (String field : CardDbAdapter.allData) {
            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
            sql += field;
        }

        sql += " FROM " + DATABASE_TABLE_CARDS +
                " JOIN " + DATABASE_TABLE_SETS +
                " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
                " WHERE (";

        first = true;
        boolean doSql = false;
        for (CompressedWishlistInfo cwi : mCompressedWishlist) {
            if (cwi.mCard.type == null || cwi.mCard.type.equals("")) {
                doSql = true;
                if (first) {
                    first = false;
                } else {
                    sql += " OR ";
                }
                if (cwi.mCard.setCode != null && !cwi.mCard.setCode.equals("")) {
                    sql += "(" + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(cwi.mCard.name) +
                            " AND " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = '" + cwi.mCard.setCode + "')";
                } else {
                    sql += "(" + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(cwi.mCard.name) + ")";
                }
            }
        }

        if (!doSql) {
            return;
        }

        sql += ")"; /*  ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC */

        Cursor cursor;

        try {
            cursor = mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (cursor != null) {
            cursor.moveToFirst();
        } else {
            return;
        }

        while (!cursor.isAfterLast()) {
            /* Do stuff */
            String name = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                if (name != null && name.equals(cwi.mCard.name)) {
                    cwi.mCard.type = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_TYPE));
                    cwi.mCard.rarity = (char) cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_RARITY));
                    cwi.mCard.manaCost = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_MANACOST));
                    cwi.mCard.power = cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_POWER));
                    cwi.mCard.toughness = cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
                    cwi.mCard.loyalty = cursor.getInt(cursor.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
                    cwi.mCard.ability = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_ABILITY));
                    cwi.mCard.flavor = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
                    cwi.mCard.number = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NUMBER));
                    cwi.mCard.cmc = cursor.getInt((cursor.getColumnIndex(CardDbAdapter.KEY_CMC)));
                    cwi.mCard.color = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
                }
            }
            /* NEXT! */
            cursor.moveToNext();
        }

		/* Use the cursor to populate stuff */
        cursor.close();
    }

    /**
     * @param name
     * @param setCode
     * @param fields
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchCardByNameAndSet(String name, String setCode, String[] fields, SQLiteDatabase mDb)
            throws FamiliarDbException {
        // replace lowercase ae with Ae
        name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);

        String sql = "SELECT ";
        boolean first = true;
        for (String field : fields) {
            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
            sql += field;
        }

        sql += " FROM "
                + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
                + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
                + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
                + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name)
                + " AND " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = '"
                + setCode + "' ORDER BY " + DATABASE_TABLE_SETS + "."
                + KEY_DATE + " DESC";
        Cursor c;

        try {
            c = mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /**
     * @param name
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static long fetchIdByName(String name, SQLiteDatabase mDb) throws FamiliarDbException {
        // replace lowercase ae with Ae
        name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);

        String sql = "SELECT " + DATABASE_TABLE_CARDS + "." + KEY_ID + ", " + DATABASE_TABLE_CARDS + "." + KEY_SET + ", " + DATABASE_TABLE_SETS + "." + KEY_DATE +
                " FROM (" + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_CARDS + "." + KEY_SET + "=" + DATABASE_TABLE_SETS + "." + KEY_CODE + ")" +
                " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name) + " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC";

        Cursor c;
        try {
            c = mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (c != null) {
            c.moveToFirst();
            long id = c.getLong(c
                    .getColumnIndex(CardDbAdapter.KEY_ID));
            c.close();
            return id;
        }
        return -1;
    }

    /**
     * @param cardname
     * @param cardtext
     * @param cardtype
     * @param color
     * @param colorlogic
     * @param sets
     * @param pow_choice
     * @param pow_logic
     * @param tou_choice
     * @param tou_logic
     * @param cmc
     * @param cmcLogic
     * @param format
     * @param rarity
     * @param flavor
     * @param artist
     * @param type_logic
     * @param text_logic
     * @param set_logic
     * @param backface
     * @param returnTypes
     * @param consolidate
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor Search(String cardname, String cardtext, String cardtype,
                                String color, int colorlogic, String sets, float pow_choice,
                                String pow_logic, float tou_choice, String tou_logic, int cmc,
                                String cmcLogic, String format, String rarity, String flavor,
                                String artist, int type_logic, int text_logic, int set_logic, String collectorsNumber,
                                boolean backface, String[] returnTypes, boolean consolidate, SQLiteDatabase mDb)
            throws FamiliarDbException {
        Cursor c;

        if (cardname != null)
            cardname = cardname.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
        if (cardtext != null)
            cardtext = cardtext.replace("'", "''").trim();
        if (cardtype != null)
            cardtype = cardtype.replace("'", "''").trim();
        if (flavor != null)
            flavor = flavor.replace("'", "''").trim();
        if (artist != null)
            artist = artist.replace("'", "''").trim();

        String statement = " WHERE 1=1";

        if (cardname != null) {
            String[] nameParts = cardname.split(" ");
            for (String s : nameParts) {
                statement += " AND (" +
                        DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '%" + s + "%' OR " +
                        DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '%" + s.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0])) + "%')";
            }
        }

        /*************************************************************************************/
        /**
         * Reuben's version Differences: Original code is verbose only, but mine
         * allows for matching exact text, all words, or just any one word.
         */
        if (cardtext != null) {
            String[] cardTextParts = cardtext.split(" "); // Separate each
            // individual

            /**
             * The following switch statement tests to see which text search
             * logic was chosen by the user. If they chose the first option (0),
             * then look for cards with text that includes all words, but not
             * necessarily the exact phrase. The second option (1) finds cards
             * that have 1 or more of the chosen words in their text. The third
             * option (2) searches for the exact phrase as entered by the user.
             * The 'default' option is impossible via the way the code is
             * written, but I believe it's also mandatory to include it in case
             * someone else is perhaps fussing with the code and breaks it. The
             * if statement at the end is theoretically unnecessary, because
             * once we've entered the current if statement, there is no way to
             * NOT change the statement variable. However, you never really know
             * who's going to break open your code and fuss around with it, so
             * it's always good to leave some small safety measures.
             */
            switch (text_logic) {
                case 0:
                    for (String s : cardTextParts) {
                        if (s.contains(EXCLUDE_TOKEN))
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_ABILITY + " NOT LIKE '%"
                                    + s.substring(EXCLUDE_TOKEN_START) + "%')";
                        else
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_ABILITY + " LIKE '%" + s + "%')";
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : cardTextParts) {
                        if (firstRun) {
                            firstRun = false;
                            if (s.contains(EXCLUDE_TOKEN))
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_ABILITY + " NOT LIKE '%"
                                        + s.substring(EXCLUDE_TOKEN_START) + "%')";
                            else
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_ABILITY + " LIKE '%" + s + "%')";
                        } else {
                            if (s.contains(EXCLUDE_TOKEN))
                                statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                        + KEY_ABILITY + " NOT LIKE '%"
                                        + s.substring(EXCLUDE_TOKEN_START) + "%')";
                            else
                                statement += " OR (" + DATABASE_TABLE_CARDS + "."
                                        + KEY_ABILITY + " LIKE '%" + s + "%')";
                        }
                    }
                    statement += ")";
                    break;
                case 2:
                    statement += " AND (" + DATABASE_TABLE_CARDS + "."
                            + KEY_ABILITY + " LIKE '%" + cardtext + "%')";
                    break;
                default:
                    break;
            }
        }
        /** End Reuben's version */

        /**
         * Reuben's version Differences: Original version only allowed for
         * including all types, not any of the types or excluding the given
         * types.
         */

        String supertypes = null;
        String subtypes = null;

        if (cardtype != null && !cardtype.equals("-")) {
            boolean containsSupertype = true;
            if (cardtype.substring(0, 2).equals("- ")) {
                containsSupertype = false;
            }
            String[] split = cardtype.split(" - ");
            if (split.length >= 2) {
                supertypes = split[0].replace(" -", "");
                subtypes = split[1].replace(" -", "");
            } else if (containsSupertype) {
                supertypes = cardtype.replace(" -", "");
            } else {
                subtypes = cardtype.replace("- ", "");
            }
        }

        if (supertypes != null) {
            String[] supertypesParts = supertypes.split(" "); // Separate each
            // individual

            switch (type_logic) {
                case 0:
                    for (String s : supertypesParts) {
                        if (s.contains(EXCLUDE_TOKEN))
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
                                    + "%')";
                        else
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " LIKE '%" + s + "%')";
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : supertypesParts) {
                        if (firstRun) {
                            firstRun = false;

                            if (s.contains(EXCLUDE_TOKEN))
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_TYPE + " NOT LIKE '%"
                                        + s.substring(1) + "%')";
                            else
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_TYPE + " LIKE '%" + s + "%')";
                        } else if (s.contains(EXCLUDE_TOKEN))
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
                                    + "%')";
                        else
                            statement += " OR (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " LIKE '%" + s + "%')";
                    }
                    statement += ")";
                    break;
                case 2:
                    for (String s : supertypesParts) {
                        statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                + KEY_TYPE + " NOT LIKE '%" + s + "%')";
                    }
                    break;
                default:
                    break;
            }
        }

        if (subtypes != null) {
            String[] subtypesParts = subtypes.split(" "); // Separate each
            // individual

            switch (type_logic) {
                case 0:
                    for (String s : subtypesParts) {
                        if (s.contains(EXCLUDE_TOKEN))
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
                                    + "%')";
                        else
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " LIKE '%" + s + "%')";
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : subtypesParts) {
                        if (firstRun) {
                            firstRun = false;
                            if (s.contains(EXCLUDE_TOKEN))
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_TYPE + " NOT LIKE '%"
                                        + s.substring(1) + "%')";
                            else
                                statement += " AND ((" + DATABASE_TABLE_CARDS + "."
                                        + KEY_TYPE + " LIKE '%" + s + "%')";
                        } else if (s.contains(EXCLUDE_TOKEN))
                            statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
                                    + "%')";
                        else
                            statement += " OR (" + DATABASE_TABLE_CARDS + "."
                                    + KEY_TYPE + " LIKE '%" + s + "%')";
                    }
                    statement += ")";
                    break;
                case 2:
                    for (String s : subtypesParts) {
                        statement += " AND (" + DATABASE_TABLE_CARDS + "."
                                + KEY_TYPE + " NOT LIKE '%" + s + "%')";
                    }
                    break;
                default:
                    break;
            }
        }
        /** End Reuben's version */
        /*************************************************************************************/

        if (flavor != null) {
            statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_FLAVOR
                    + " LIKE '%" + flavor + "%')";
        }

        if (artist != null) {
            statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ARTIST
                    + " LIKE '%" + artist + "%')";
        }

        if (collectorsNumber != null) {
            statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER
                    + " = '" + collectorsNumber + "')";
        }

        /*************************************************************************************/
        /**
         * Code below added/modified by Reuben. Differences: Original version
         * only had 'Any' and 'All' options and lacked 'Exclusive' and 'Exact'
         * matching. In addition, original programming only provided exclusive
         * results.
         */
        if (!(color.equals("wubrgl") || (color.equals("WUBRGL") && colorlogic == 0))) {
            boolean firstPrint = true;

            // Can't contain these colors
            /**
             * ...if the chosen color logic was exactly (2) or none (3) of the
             * selected colors
             */
            if (colorlogic > 1) // if colorlogic is 2 or 3 it will be greater
            // than 1
            {
                statement += " AND ((";
                for (byte b : color.getBytes()) {
                    char ch = (char) b;

                    if (ch > 'a') {
                        if (firstPrint)
                            firstPrint = false;
                        else
                            statement += " AND ";

                        if (ch == 'l' || ch == 'L')
                            statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
                                    + " NOT GLOB '[CLA]'";
                        else
                            statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
                                    + " NOT LIKE '%" + Character.toUpperCase(ch)
                                    + "%'";
                    }
                }
                statement += ") AND (";
            }

            firstPrint = true;

            // Might contain these colors
            if (colorlogic < 2)
                statement += " AND (";

            for (byte b : color.getBytes()) {
                char ch = (char) b;
                if (ch < 'a') {
                    if (firstPrint)
                        firstPrint = false;
                    else {
                        if (colorlogic == 1 || colorlogic == 3)
                            statement += " AND ";
                        else
                            statement += " OR ";
                    }

                    if (ch == 'l' || ch == 'L')
                        statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
                                + " GLOB '[CLA]'";
                    else
                        statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
                                + " LIKE '%" + ch + "%'";
                }
            }
            if (colorlogic > 1)
                statement += "))";
            else
                statement += ")";
        }
        /** End of addition */
        /*************************************************************************************/

        if (sets != null) {
            statement += " AND (";

            boolean first = true;

            for (String s : sets.split("-")) {
                if (first) {
                    first = false;
                } else {
                    statement += " OR ";
                }
                statement += DATABASE_TABLE_CARDS + "." + KEY_SET + " = '" + s
                        + "'";
            }

            statement += ")";
        }

        if (pow_choice != NO_ONE_CARES) {
            statement += " AND (";

            if (pow_choice > STAR) {
                statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " "
                        + pow_logic + " " + pow_choice;
                if (pow_logic.equals("<")) {
                    statement += " AND " + DATABASE_TABLE_CARDS + "."
                            + KEY_POWER + " > " + STAR;
                }
            } else if (pow_logic.equals("=")) {
                statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " "
                        + pow_logic + " " + pow_choice;
            }
            statement += ")";
        }

        if (tou_choice != NO_ONE_CARES) {
            statement += " AND (";

            if (tou_choice > STAR) {
                statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " "
                        + tou_logic + " " + tou_choice;
                if (tou_logic.equals("<")) {
                    statement += " AND " + DATABASE_TABLE_CARDS + "."
                            + KEY_TOUGHNESS + " > " + STAR;
                }
            } else if (tou_logic.equals("=")) {
                statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " "
                        + tou_logic + " " + tou_choice;
            }
            statement += ")";
        }

        if (cmc != -1) {
            statement += " AND (";

            statement += DATABASE_TABLE_CARDS + "." + KEY_CMC + " " + cmcLogic
                    + " " + cmc + ")";
        }

        if (rarity != null) {
            statement += " AND (";

            boolean firstPrint = true;
            for (int i = 0; i < rarity.length(); i++) {
                if (firstPrint) {
                    firstPrint = false;
                } else {
                    statement += " OR ";
                }
                statement += DATABASE_TABLE_CARDS + "." + KEY_RARITY + " = "
                        + (int) rarity.toUpperCase().charAt(i) + "";
            }
            statement += ")";
        }

        String tbl = DATABASE_TABLE_CARDS;
        if (format != null) {

			/* Check if the format is eternal or not, by the number of legal sets */
            String numLegalSetsSql = "SELECT * FROM " + DATABASE_TABLE_LEGAL_SETS + " WHERE " + KEY_FORMAT + " = \"" + format + "\"";
            Cursor numLegalSetCursor = mDb.rawQuery(numLegalSetsSql, null);

			/* If the format is not eternal, filter by set */
            if (numLegalSetCursor.getCount() > 0) {
                tbl = "(" + DATABASE_TABLE_CARDS + " JOIN "
                        + DATABASE_TABLE_LEGAL_SETS + " ON "
                        + DATABASE_TABLE_CARDS + "." + KEY_SET + "="
                        + DATABASE_TABLE_LEGAL_SETS + "." + KEY_SET + " AND "
                        + DATABASE_TABLE_LEGAL_SETS + "." + KEY_FORMAT + "='"
                        + format + "')";
            } else {
                /* Otherwise filter silver bordered cards, giant cards */
                statement += " AND NOT " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = 'UNH'" +
                        " AND NOT " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = 'UG'" +
                        " AND " + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE 'Plane %'" +
                        " AND " + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE 'Conspiracy%'" +
                        " AND " + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%Scheme%'" +
                        " AND " + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE 'Vanguard%'";
            }

            numLegalSetCursor.close();

            statement += " AND NOT EXISTS (SELECT * FROM "
                    + DATABASE_TABLE_BANNED_CARDS + " WHERE "
                    + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = "
                    + DATABASE_TABLE_BANNED_CARDS + "." + KEY_NAME + " AND "
                    + DATABASE_TABLE_BANNED_CARDS + "." + KEY_FORMAT + " = '"
                    + format + "' AND " + DATABASE_TABLE_BANNED_CARDS + "."
                    + KEY_LEGALITY + " = " + BANNED + ")";
        }

        if (!backface) {
            statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER
                    + " NOT LIKE '%b%')";
        }

        if (set_logic != MOST_RECENT_PRINTING && set_logic != ALL_PRINTINGS) {
            statement = " JOIN (SELECT iT" + DATABASE_TABLE_CARDS + "."
                    + KEY_NAME + ", MIN(" + DATABASE_TABLE_SETS + "."
                    + KEY_DATE + ") AS " + KEY_DATE + " FROM "
                    + DATABASE_TABLE_CARDS + " AS iT" + DATABASE_TABLE_CARDS
                    + " JOIN " + DATABASE_TABLE_SETS + " ON iT"
                    + DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
                    + DATABASE_TABLE_SETS + "." + KEY_CODE + " GROUP BY iT"
                    + DATABASE_TABLE_CARDS + "." + KEY_NAME
                    + ") AS FirstPrints" + " ON " + DATABASE_TABLE_CARDS + "."
                    + KEY_NAME + " = FirstPrints." + KEY_NAME + statement;
            if (set_logic == FIRST_PRINTING)
                statement = " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + " = FirstPrints." + KEY_DATE + statement;
            else
                statement = " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + " <> FirstPrints." + KEY_DATE + statement;
        }

        if (statement.equals(" WHERE 1=1")) {
            // If the statement is just this, it means we added nothing
            return null;
        }

        try {
            String sel = null;
            for (String s : returnTypes) {
                if (sel == null) {
                    sel = DATABASE_TABLE_CARDS + "." + s + " AS " + s;
                } else {
                    sel += ", " + DATABASE_TABLE_CARDS + "." + s + " AS " + s;
                }
            }
            sel += ", " + DATABASE_TABLE_SETS + "." + KEY_DATE;

            String sql = "SELECT * FROM (SELECT " + sel + " FROM " + tbl
                    + " JOIN " + DATABASE_TABLE_SETS + " ON "
                    + DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
                    + DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

            if (consolidate) {
                sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + ") GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME + " COLLATE UNICODE";
            } else {
                sql += " ORDER BY " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " COLLATE UNICODE"
                        + ", " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + " DESC)";
            }
            c = mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /**
     * @param set
     * @param number
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static int getTransform(String set, String number, SQLiteDatabase mDb)
            throws FamiliarDbException {
        Cursor c;
        String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
                + KEY_SET + " = '" + set + "')";
        try {
            c = mDb.query(true, DATABASE_TABLE_CARDS,
                    new String[]{KEY_ID}, statement, null, null, null,
                    KEY_ID, null);
            c.moveToFirst();
            int ID = c.getInt(c.getColumnIndex(KEY_ID));
            c.close();
            return ID;
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } catch (CursorIndexOutOfBoundsException e) {
            return -1; /* The other half doesn't exist... */
        }
    }

    /**
     * Returns a card queried by set and collector's number
     *
     * @param set
     * @param number
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static String getTransformName(String set, String number, SQLiteDatabase mDb)
            throws FamiliarDbException {
        Cursor c;
        String name;
        String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
                + KEY_SET + " = '" + set + "')";
        try {
            c = mDb.query(true, DATABASE_TABLE_CARDS,
                    new String[]{KEY_NAME}, statement, null, null, null,
                    KEY_NAME, null);
            c.moveToFirst();
            name = c.getString(c.getColumnIndex(KEY_NAME));
            c.close();
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        return name;
    }

    /**
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void createLegalTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL(DATABASE_CREATE_FORMATS);
            mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
            mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void dropLegalTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param name
     * @param mDb
     * @return
     */
    public static void createFormat(String name, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        mDb.insert(DATABASE_TABLE_FORMATS, null, initialValues);
    }

    /**
     * @param set
     * @param format
     * @param mDb
     * @return
     */
    public static void addLegalSet(String set, String format, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SET, set);
        initialValues.put(KEY_FORMAT, format);
        mDb.insert(DATABASE_TABLE_LEGAL_SETS, null, initialValues);
    }

    /**
     * @param card
     * @param format
     * @param status
     * @param mDb
     * @return
     */
    public static void addLegalCard(String card, String format, int status, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, card);
        initialValues.put(KEY_LEGALITY, status);
        initialValues.put(KEY_FORMAT, format);
        mDb.insert(DATABASE_TABLE_BANNED_CARDS, null, initialValues);
    }

    /**
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor fetchAllFormats(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            return mDb.query(DATABASE_TABLE_FORMATS, new String[]{KEY_ID,
                    KEY_NAME,}, null, null, null, null, KEY_NAME);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mCardName
     * @param format
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static int checkLegality(String mCardName, String format, SQLiteDatabase mDb)
            throws FamiliarDbException {
        mCardName = mCardName.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
        format = format.replace("'", "''"); // Just to be safe; remember Bobby
        // Tables
        try {
            // The new way (single query per type, should be much faster) - Alex
            String sql = "SELECT COALESCE(CASE (SELECT "
                    + KEY_SET
                    + " FROM "
                    + DATABASE_TABLE_CARDS
                    + " WHERE "
                    + KEY_NAME
                    + " = '"
                    + mCardName
                    + "') WHEN 'UG' THEN 1 WHEN 'UNH' THEN 1 WHEN 'ARS' THEN 1 WHEN 'PCP' THEN 1 "
                    + "WHEN 'PP2' THEN 1 ELSE NULL END, "
                    + "CASE (SELECT 1 FROM " + DATABASE_TABLE_CARDS
                    + " c INNER JOIN " + DATABASE_TABLE_LEGAL_SETS
                    + " ls ON ls." + KEY_SET + " = c." + KEY_SET + " WHERE ls."
                    + KEY_FORMAT + " = '" + format + "' AND c." + KEY_NAME
                    + " = '" + mCardName
                    + "') WHEN 1 THEN NULL ELSE CASE WHEN '" + format
                    + "' = 'Legacy' " + "THEN NULL WHEN '" + format
                    + "' = 'Vintage' THEN NULL WHEN '" + format
                    + "' = 'Commander' THEN NULL ELSE 1 END END, (SELECT "
                    + KEY_LEGALITY + " from " + DATABASE_TABLE_BANNED_CARDS
                    + " WHERE " + KEY_NAME + " = '" + mCardName + "' AND "
                    + KEY_FORMAT + " = '" + format + "'), 0) AS "
                    + KEY_LEGALITY;

            Cursor c = mDb.rawQuery(sql, null);

            c.moveToFirst();
            int legality = c.getInt(c.getColumnIndex(KEY_LEGALITY));
            c.close();
            return legality;
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param setCode
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static String getTcgName(String setCode, SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            String sql = "SELECT " + KEY_NAME_TCGPLAYER + " FROM " + DATABASE_TABLE_SETS + " WHERE " + KEY_CODE + " = '" + setCode.replace("'", "''") + "';";
            Cursor c = mDb.rawQuery(sql, null);
            c.moveToFirst();

			/* Some users had this cursor come up empty. I couldn't replicate. This is safe */
            if (c.getCount() == 0) {
                c.close();
                return "";
            }
            String tcgName = c.getString(c.getColumnIndex(KEY_NAME_TCGPLAYER));
            c.close();
            return tcgName;
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param setName
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    private static boolean isModernLegalSet(String setName, SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            String sql = "SELECT " + KEY_SET + " FROM " + DATABASE_TABLE_LEGAL_SETS + " WHERE " + KEY_SET + " = '" + setName.replace("'", "''") + "';";
            Cursor c = mDb.rawQuery(sql, null);
            boolean isModernLegal = (c.getCount() >= 1);
            c.close();
            return isModernLegal;
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param category
     * @param subcategory
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor getRules(int category, int subcategory, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            if (category == -1) {
                // No category specified; return the main categories
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                        + " WHERE " + KEY_SUBCATEGORY + " = -1";
                return mDb.rawQuery(sql, null);
            } else if (subcategory == -1) {
                // No subcategory specified; return the subcategories under the given category
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                        + " WHERE " + KEY_CATEGORY + " = "
                        + String.valueOf(category) + " AND " + KEY_SUBCATEGORY
                        + " > -1 AND " + KEY_ENTRY + " IS NULL";
                return mDb.rawQuery(sql, null);
            } else {
                // Both specified; return the rules under the given subcategory
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                        + " WHERE " + KEY_CATEGORY + " = "
                        + String.valueOf(category) + " AND " + KEY_SUBCATEGORY
                        + " = " + String.valueOf(subcategory) + " AND "
                        + KEY_ENTRY + " IS NOT NULL";
                return mDb.rawQuery(sql, null);
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param keyword
     * @param category
     * @param subcategory
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor getRulesByKeyword(String keyword, int category,
                                           int subcategory, SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            // Don't let them pass in an empty string; it'll return ALL the
            // rules
            if (keyword != null && !keyword.trim().equals("")) {
                keyword = "'%" + keyword.replace("'", "''") + "%'";

                if (category == -1) {
                    // No category; we're searching from the main page, so no
                    // restrictions
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL";
                    return mDb.rawQuery(sql, null);
                } else if (subcategory == -1) {
                    // No subcategory; we're searching from a category page, so
                    // restrict
                    // within that
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL AND "
                            + KEY_CATEGORY + " = " + String.valueOf(category);
                    return mDb.rawQuery(sql, null);
                } else {
                    // We're searching within a subcategory, so restrict within
                    // that
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL AND "
                            + KEY_CATEGORY + " = " + String.valueOf(category)
                            + " AND " + KEY_SUBCATEGORY + " = "
                            + String.valueOf(subcategory);
                    return mDb.rawQuery(sql, null);
                }
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
        return null;
    }

    /**
     * @param category
     * @param subcategory
     * @param entry
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static int getRulePosition(int category, int subcategory, String entry, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            if (entry != null) {
                String sql = "SELECT " + KEY_POSITION + " FROM "
                        + DATABASE_TABLE_RULES + " WHERE " + KEY_CATEGORY
                        + " = " + String.valueOf(category) + " AND "
                        + KEY_SUBCATEGORY + " = " + String.valueOf(subcategory)
                        + " AND " + KEY_ENTRY + " = '"
                        + entry.replace("'", "''") + "'";
                Cursor c = mDb.rawQuery(sql, null);
                if (c != null) {
                    c.moveToFirst();
                    int result = c.getInt(c.getColumnIndex(KEY_POSITION));
                    c.close();
                    return result;
                }
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
        return 0;
    }

    /**
     * @param category
     * @param subcategory
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static String getCategoryName(int category, int subcategory, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            String sql = "SELECT " + KEY_RULE_TEXT + " FROM "
                    + DATABASE_TABLE_RULES + " WHERE " + KEY_CATEGORY + " = "
                    + String.valueOf(category) + " AND " + KEY_SUBCATEGORY
                    + " = " + String.valueOf(subcategory) + " AND " + KEY_ENTRY
                    + " IS NULL";
            Cursor c = mDb.rawQuery(sql, null);
            if (c != null) {
                c.moveToFirst();
                String result = c.getString(c.getColumnIndex(KEY_RULE_TEXT));
                c.close();
                return result;
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
        return "";
    }

    /**
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor getGlossaryTerms(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            String sql = "SELECT * FROM " + DATABASE_TABLE_GLOSSARY;
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mDb
     * @param format
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor getBannedCards(SQLiteDatabase mDb, String format) throws FamiliarDbException {
        try {
            String sql = "SELECT " + KEY_LEGALITY + ", GROUP_CONCAT(" +
                    KEY_NAME + ", '<br>') AS " + KEY_BANNED_LIST + " FROM " + DATABASE_TABLE_BANNED_CARDS +
                    " WHERE " + KEY_FORMAT + " = '" + format + "'" + " GROUP BY " + KEY_LEGALITY;
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mDb
     * @param format
     * @return
     * @throws FamiliarDbException
     */
    public static Cursor getLegalSets(SQLiteDatabase mDb, String format) throws FamiliarDbException {
        try {
            String sql = "SELECT GROUP_CONCAT(" + DATABASE_TABLE_SETS + "." + KEY_NAME + ", '<br>') AS " + KEY_LEGAL_SETS +
                    " FROM (" + DATABASE_TABLE_LEGAL_SETS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_LEGAL_SETS + "." + KEY_SET + " = " + DATABASE_TABLE_SETS + "." + KEY_CODE + ")" +
                    " WHERE " + DATABASE_TABLE_LEGAL_SETS + "." + KEY_FORMAT + " = '" + format + "'";
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void dropRulesTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RULES);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_GLOSSARY);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void createRulesTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL(DATABASE_CREATE_RULES);
            mDb.execSQL(DATABASE_CREATE_GLOSSARY);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param category
     * @param subcategory
     * @param entry
     * @param text
     * @param position
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void insertRule(int category, int subcategory, String entry,
                                  String text, int position, SQLiteDatabase mDb) throws FamiliarDbException {
        if (entry == null) {
            entry = "NULL";
        } else {
            entry = "'" + entry.replace("'", "''") + "'";
        }
        text = "'" + text.replace("'", "''") + "'";
        String positionStr;
        if (position < 0) {
            positionStr = "NULL";
        } else {
            positionStr = String.valueOf(position);
        }
        String sql = "INSERT INTO " + DATABASE_TABLE_RULES + " ("
                + KEY_CATEGORY + ", " + KEY_SUBCATEGORY + ", " + KEY_ENTRY
                + ", " + KEY_RULE_TEXT + ", " + KEY_POSITION + ") VALUES ("
                + String.valueOf(category) + ", " + String.valueOf(subcategory)
                + ", " + entry + ", " + text + ", " + positionStr + ");";
        try {
            mDb.execSQL(sql);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param term
     * @param definition
     * @param mDb
     * @throws FamiliarDbException
     */
    public static void insertGlossaryTerm(String term, String definition, SQLiteDatabase mDb) throws FamiliarDbException {
        term = "'" + term.replace("'", "''") + "'";
        definition = "'" + definition.replace("'", "''") + "'";
        String sql = "INSERT INTO " + DATABASE_TABLE_GLOSSARY + " (" + KEY_TERM
                + ", " + KEY_DEFINITION + ") VALUES (" + term + ", "
                + definition + ");";
        try {
            mDb.execSQL(sql);
        } catch (SQLiteException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Builds a map for all columns that may be requested, which will be given
     * to the SQLiteQueryBuilder. This is a good way to define aliases for
     * column names, but must include all columns, even if the value is the key.
     * This allows the ContentProvider to request columns w/o the need to know
     * real column names and create the alias itself.
     *
     * @return
     */
    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(KEY_NAME, KEY_NAME);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS "
                + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS "
                + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    /**
     * @param selection     The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns       The columns to return
     * @param mDb
     * @return A Cursor over all rows matching the query
     * @throws FamiliarDbException
     */
    private static Cursor query(String selection, String[] selectionArgs,
                                String[] columns, SQLiteDatabase mDb) throws FamiliarDbException {
        /*
		 * The SQLiteBuilder provides a map for all possible columns requested
		 * to actual columns in the database, creating a simple column alias
		 * mechanism by which the ContentProvider does not need to know the real
		 * column names
		 */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DATABASE_TABLE_CARDS);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor;
        try {
            cursor = builder.query(mDb, columns, selection, selectionArgs,
                    KEY_NAME, null, KEY_NAME);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        if (cursor != null && !cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        return cursor;
    }

    /**
     * Returns a Cursor positioned at the word specified by rowId
     *
     * @param rowId   id of word to retrieve
     * @param columns The columns to include, if null then all are included
     * @param mDb
     * @return Cursor positioned to matching word, or null if not found.
     * @throws FamiliarDbException
     */
    public static Cursor getCardByRowId(String rowId, String[] columns, SQLiteDatabase mDb)
            throws FamiliarDbException {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[]{rowId};

        return query(selection, selectionArgs, columns, mDb);

		/*
		 * This builds a query that looks like: SELECT <columns> FROM <table>
		 * WHERE rowid = <rowId>
		 */
    }

    /**
     * Returns a Cursor over all words that match the given query
     *
     * @param query The string to search for
     * @param mDb
     * @return Cursor over all words that match, or null if none found.
     * @throws FamiliarDbException
     */
    public static Cursor getCardsByNamePrefix(String query, SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            query = query.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
            String convert = query.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0]));

            if (query.length() < 2) {
                return null;
            }

            String sql =
                    "SELECT * FROM (" +
                            "SELECT " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " AS " + KEY_NAME + ", " + DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + KEY_ID + ", " + DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID +
                            " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
                            " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '" + query + "%'"
                            + " OR " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '" + convert + "%'" +
                            " ORDER BY " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " COLLATE UNICODE, " + DATABASE_TABLE_SETS + "." + KEY_DATE + " ASC " +
                            ") GROUP BY " + KEY_NAME;
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param ctx
     * @return
     */
    public static boolean isDbOutOfDate(Context ctx) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String dbPath = ctx.getFilesDir().getPath();
        dbPath = dbPath.substring(0, dbPath.lastIndexOf("/")) + "/databases";
        File f = new File(dbPath, DB_NAME);
        int dbVersion = preferences.getInt("databaseVersion", -1);
        return (!f.exists() || f.length() < 1048576 || dbVersion < CardDbAdapter.DATABASE_VERSION);
    }

    /**
     * @param ctx
     */
    public static void copyDB(Context ctx) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = preferences.edit();

        try {

            String dbPath = ctx.getFilesDir().getPath();
            dbPath = dbPath.substring(0, dbPath.lastIndexOf("/")) + "/databases";

            File folder = new File(dbPath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            File dbFile = new File(folder, DB_NAME);
            if (dbFile.exists()) {
                dbFile.delete();
                editor.putString("lastUpdate", "");
                editor.putInt("databaseVersion", -1);
                editor.apply();
            }
            if (!dbFile.exists()) {

                GZIPInputStream gis = new GZIPInputStream(ctx.getResources().openRawResource(R.raw.datagz));
                FileOutputStream fos = new FileOutputStream(dbFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                editor.putInt("databaseVersion", CardDbAdapter.DATABASE_VERSION);
                editor.apply();

                // Close the streams
                fos.flush();
                fos.close();
                gis.close();
            }
        } catch (NotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param name
     * @param setCode
     * @param mDb     @return
     * @throws FamiliarDbException
     */
    public static int getSplitMultiverseID(String name, String setCode, SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c;
        String statement = "SELECT " + KEY_MULTIVERSEID + " from "
                + DATABASE_TABLE_CARDS + " WHERE " + KEY_NAME + " = '"
                + name + "' AND " + KEY_SET + " = '" + setCode + "'";

        try {
            c = mDb.rawQuery(statement, null);

            if (c.getCount() > 0) {
                c.moveToFirst();
                int retVal = c.getInt(c.getColumnIndex(KEY_MULTIVERSEID));
                c.close();
                return retVal;
            } else {
                c.close();
                return -1;
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param multiverseId  The multiverse id to search for
     * @param isAscending   Whether the query should be sorted in ascending or descending order
     * @param mDb           The database to search
     * @return              A String name
     * @throws FamiliarDbException
     */
    public static String getSplitName(int multiverseId, boolean isAscending, SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c;
        String statement = "SELECT " + KEY_NAME + ", " + KEY_NUMBER + " from "
                + DATABASE_TABLE_CARDS + " WHERE " + KEY_MULTIVERSEID + " = "
                + multiverseId + " ORDER BY " + KEY_NUMBER;

        if (isAscending) {
            statement += " ASC";
        } else {
            statement += " DESC";
        }

        try {
            c = mDb.rawQuery(statement, null);

            if (c.getCount() == 2) {
                c.moveToFirst();
                String retVal;
                retVal = c.getString(c.getColumnIndex(KEY_NAME));
                retVal += " // ";
                c.moveToNext();
                retVal += c.getString(c.getColumnIndex(KEY_NAME));
                c.close();
                return retVal;
            } else {
                c.close();
                return null;
            }
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * @param s
     * @return
     */
    public static String removeAccentMarks(String s) {
        return s.replace(Character.toChars(0xC0)[0] + "", "A")
                .replace(Character.toChars(0xC1)[0] + "", "A")
                .replace(Character.toChars(0xC2)[0] + "", "A")
                .replace(Character.toChars(0xC3)[0] + "", "A")
                .replace(Character.toChars(0xC4)[0] + "", "A")
                .replace(Character.toChars(0xC5)[0] + "", "A")
                .replace(Character.toChars(0xC6)[0] + "", "Ae")
                .replace(Character.toChars(0xC7)[0] + "", "C")
                .replace(Character.toChars(0xC8)[0] + "", "E")
                .replace(Character.toChars(0xC9)[0] + "", "E")
                .replace(Character.toChars(0xCA)[0] + "", "E")
                .replace(Character.toChars(0xCB)[0] + "", "E")
                .replace(Character.toChars(0xCC)[0] + "", "I")
                .replace(Character.toChars(0xCD)[0] + "", "I")
                .replace(Character.toChars(0xCE)[0] + "", "I")
                .replace(Character.toChars(0xCF)[0] + "", "I")
                .replace(Character.toChars(0xD0)[0] + "", "D")
                .replace(Character.toChars(0xD1)[0] + "", "N")
                .replace(Character.toChars(0xD2)[0] + "", "O")
                .replace(Character.toChars(0xD3)[0] + "", "O")
                .replace(Character.toChars(0xD4)[0] + "", "O")
                .replace(Character.toChars(0xD5)[0] + "", "O")
                .replace(Character.toChars(0xD6)[0] + "", "O")
                .replace(Character.toChars(0xD7)[0] + "", "x")
                .replace(Character.toChars(0xD8)[0] + "", "O")
                .replace(Character.toChars(0xD9)[0] + "", "U")
                .replace(Character.toChars(0xDA)[0] + "", "U")
                .replace(Character.toChars(0xDB)[0] + "", "U")
                .replace(Character.toChars(0xDC)[0] + "", "U")
                .replace(Character.toChars(0xDD)[0] + "", "Y")
                .replace(Character.toChars(0xE0)[0] + "", "a")
                .replace(Character.toChars(0xE1)[0] + "", "a")
                .replace(Character.toChars(0xE2)[0] + "", "a")
                .replace(Character.toChars(0xE3)[0] + "", "a")
                .replace(Character.toChars(0xE4)[0] + "", "a")
                .replace(Character.toChars(0xE5)[0] + "", "a")
                .replace(Character.toChars(0xE6)[0] + "", "ae")
                .replace(Character.toChars(0xE7)[0] + "", "c")
                .replace(Character.toChars(0xE8)[0] + "", "e")
                .replace(Character.toChars(0xE9)[0] + "", "e")
                .replace(Character.toChars(0xEA)[0] + "", "e")
                .replace(Character.toChars(0xEB)[0] + "", "e")
                .replace(Character.toChars(0xEC)[0] + "", "i")
                .replace(Character.toChars(0xED)[0] + "", "i")
                .replace(Character.toChars(0xEE)[0] + "", "i")
                .replace(Character.toChars(0xEF)[0] + "", "i")
                .replace(Character.toChars(0xF1)[0] + "", "n")
                .replace(Character.toChars(0xF2)[0] + "", "o")
                .replace(Character.toChars(0xF3)[0] + "", "o")
                .replace(Character.toChars(0xF4)[0] + "", "o")
                .replace(Character.toChars(0xF5)[0] + "", "o")
                .replace(Character.toChars(0xF6)[0] + "", "o")
                .replace(Character.toChars(0xF8)[0] + "", "o")
                .replace(Character.toChars(0xF9)[0] + "", "u")
                .replace(Character.toChars(0xFA)[0] + "", "u")
                .replace(Character.toChars(0xFB)[0] + "", "u")
                .replace(Character.toChars(0xFC)[0] + "", "u")
                .replace(Character.toChars(0xFD)[0] + "", "y")
                .replace(Character.toChars(0xFF)[0] + "", "y");
    }

    /**
     * @param number
     * @param setCode
     * @return
     */
    public static int isMultiCard(String number, String setCode) {
        if (number.contains("a") || number.contains("b")) {
            if (setCode.compareTo("ISD") == 0 || setCode.compareTo("DKA") == 0|| setCode.compareTo("ORI") == 0) {
                return TRANSFORM;
            } else if (setCode.compareTo("DGM") == 0) {
                return FUSE;
            } else {
                return SPLIT;
            }
        }
        return NOPE;
    }

    /**
     * @param setCode
     * @param mDb
     * @return
     * @throws FamiliarDbException
     */
    public static boolean canBeFoil(String setCode, SQLiteDatabase mDb) throws FamiliarDbException {
        String[] extraSets = {"UNH", "UL", "UD", "MM", "NE", "PY", "IN", "PS", "7E", "AP", "OD", "TO", "JU", "ON", "LE", "SC", "CNS", "CNSC"};
        ArrayList<String> nonModernLegalSets = new ArrayList<>(Arrays.asList(extraSets));
        for (String value : nonModernLegalSets) {
            if (value.equals(setCode)) {
                return true;
            }
        }

        return isModernLegalSet(setCode, mDb);
    }

    public static String getSetNameFromCode(String setCode, SQLiteDatabase database) throws FamiliarDbException {

        String columns[] = new String[]{KEY_NAME};
        Cursor c;
        try {
            c = database.query(true, DATABASE_TABLE_SETS, columns, KEY_CODE
                    + "=\"" + setCode + "\"", null, null, null, KEY_NAME, null);
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }

        String returnString = null;
        if (c != null) {
            c.moveToFirst();
            returnString = c.getString(c.getColumnIndex(KEY_NAME));
            c.close();
        }
        return returnString;
    }
}
