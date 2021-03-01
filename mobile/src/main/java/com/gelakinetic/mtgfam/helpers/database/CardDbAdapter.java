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

package com.gelakinetic.mtgfam.helpers.database;

import android.app.SearchManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.Language;
import com.gelakinetic.mtgfam.helpers.FamiliarLogger;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations and gives the
 * ability to list all Cards as well as retrieve or modify a Specific Card.
 */
public class CardDbAdapter {

    /* Database version. Must be incremented whenever datagz is updated */
    public static final int DATABASE_VERSION = 116;

    /* Database Tables */
    public static final String DATABASE_TABLE_CARDS = "cards";
    public static final String DATABASE_TABLE_SETS = "sets";
    private static final String DATABASE_TABLE_FORMATS = "formats";
    private static final String DATABASE_TABLE_LEGAL_SETS = "legal_sets";
    private static final String DATABASE_TABLE_BANNED_CARDS = "banned_cards";
    private static final String DATABASE_TABLE_RULES = "rules";
    private static final String DATABASE_TABLE_GLOSSARY = "glossary";

    /* Database Keys */
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_SET = "expansion";
    public static final String KEY_SCRYFALL_SET_CODE = "scryfall_set_code";
    public static final String KEY_SUPERTYPE = "supertype";
    public static final String KEY_SUBTYPE = "subtype";
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
    public static final String KEY_MULTIVERSEID = "multiverseID";
    public static final String KEY_CODE = "code";
    public static final String KEY_LEGALITY = "legality";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_SUBCATEGORY = "subcategory";
    public static final String KEY_ENTRY = "entry";
    public static final String KEY_RULE_TEXT = "rule_text";
    public static final String KEY_TERM = "term";
    public static final String KEY_DEFINITION = "definition";
    public static final String KEY_BANNED_LIST = "banned_list";
    public static final String KEY_LEGAL_SETS = "legal_sets";
    private static final String KEY_NAME_TCGPLAYER = "name_tcgplayer";
    private static final String KEY_ONLINE_ONLY = "online_only";
    private static final String KEY_BORDER_COLOR = "border_color";
    public static final String KEY_SET_TYPE = "set_type";
    private static final String KEY_FORMAT = "format";
    public static final String KEY_DIGEST = "digest";
    private static final String KEY_RULINGS = "rulings";
    public static final String KEY_CODE_MTGI = "code_mtgi";
    private static final String KEY_DATE = "date";
    private static final String KEY_POSITION = "position";
    public static final String KEY_COLOR_IDENTITY = "color_identity";
    public static final String KEY_CAN_BE_FOIL = "can_be_foil";
    private static final String KEY_NAME_NO_ACCENT = "name_no_accent";
    public static final String KEY_NAME_CHINESE_TRADITIONAL = "NAME_CHINESE_TRADITIONAL";
    public static final String KEY_MULTIVERSEID_CHINESE_TRADITIONAL = "MULTIVERSEID_CHINESE_TRADITIONAL";
    public static final String KEY_NAME_CHINESE_SIMPLIFIED = "NAME_CHINESE_SIMPLIFIED";
    public static final String KEY_MULTIVERSEID_CHINESE_SIMPLIFIED = "MULTIVERSEID_CHINESE_SIMPLIFIED";
    public static final String KEY_NAME_FRENCH = "NAME_FRENCH";
    public static final String KEY_NAME_NO_ACCENT_FRENCH = "NAME_NO_ACCENT_FRENCH";
    public static final String KEY_MULTIVERSEID_FRENCH = "MULTIVERSEID_FRENCH";
    public static final String KEY_NAME_GERMAN = "NAME_GERMAN";
    public static final String KEY_NAME_NO_ACCENT_GERMAN = "NAME_NO_ACCENT_GERMAN";
    public static final String KEY_MULTIVERSEID_GERMAN = "MULTIVERSEID_GERMAN";
    public static final String KEY_NAME_ITALIAN = "NAME_ITALIAN";
    public static final String KEY_NAME_NO_ACCENT_ITALIAN = "NAME_NO_ACCENT_ITALIAN";
    public static final String KEY_MULTIVERSEID_ITALIAN = "MULTIVERSEID_ITALIAN";
    public static final String KEY_NAME_JAPANESE = "NAME_JAPANESE";
    public static final String KEY_MULTIVERSEID_JAPANESE = "MULTIVERSEID_JAPANESE";
    public static final String KEY_NAME_PORTUGUESE_BRAZIL = "NAME_PORTUGUESE_BRAZIL";
    public static final String KEY_NAME_NO_ACCENT_PORTUGUESE_BRAZIL = "NAME_NO_ACCENT_PORTUGUESE_BRAZIL";
    public static final String KEY_MULTIVERSEID_PORTUGUESE_BRAZIL = "MULTIVERSEID_PORTUGUESE_BRAZIL";
    public static final String KEY_NAME_RUSSIAN = "NAME_RUSSIAN";
    public static final String KEY_MULTIVERSEID_RUSSIAN = "MULTIVERSEID_RUSSIAN";
    public static final String KEY_NAME_SPANISH = "NAME_SPANISH";
    public static final String KEY_NAME_NO_ACCENT_SPANISH = "NAME_NO_ACCENT_SPANISH";
    public static final String KEY_MULTIVERSEID_SPANISH = "MULTIVERSEID_SPANISH";
    public static final String KEY_NAME_KOREAN = "NAME_KOREAN";
    public static final String KEY_MULTIVERSEID_KOREAN = "MULTIVERSEID_KOREAN";
    public static final String KEY_WATERMARK = "WATERMARK";

    /* All the columns in DATABASE_TABLE_CARDS */
    public static final List<String> ALL_CARD_DATA_KEYS = Collections.unmodifiableList(Arrays.asList(
            DATABASE_TABLE_CARDS + "." + KEY_ID,
            DATABASE_TABLE_CARDS + "." + KEY_NAME,
            DATABASE_TABLE_CARDS + "." + KEY_SET,
            DATABASE_TABLE_CARDS + "." + KEY_SCRYFALL_SET_CODE,
            DATABASE_TABLE_CARDS + "." + KEY_NUMBER,
            DATABASE_TABLE_CARDS + "." + KEY_SUPERTYPE,
            DATABASE_TABLE_CARDS + "." + KEY_MANACOST,
            DATABASE_TABLE_CARDS + "." + KEY_ABILITY,
            DATABASE_TABLE_CARDS + "." + KEY_POWER,
            DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS,
            DATABASE_TABLE_CARDS + "." + KEY_LOYALTY,
            DATABASE_TABLE_CARDS + "." + KEY_RARITY,
            DATABASE_TABLE_CARDS + "." + KEY_FLAVOR,
            DATABASE_TABLE_CARDS + "." + KEY_CMC,
            DATABASE_TABLE_CARDS + "." + KEY_COLOR,
            DATABASE_TABLE_CARDS + "." + KEY_SUBTYPE,
            DATABASE_TABLE_CARDS + "." + KEY_ARTIST,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID,
            DATABASE_TABLE_CARDS + "." + KEY_RULINGS,
            DATABASE_TABLE_CARDS + "." + KEY_COLOR_IDENTITY,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_CHINESE_TRADITIONAL,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_CHINESE_TRADITIONAL,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_CHINESE_SIMPLIFIED,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_CHINESE_SIMPLIFIED,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_FRENCH,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_FRENCH,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_FRENCH,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_GERMAN,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_GERMAN,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_GERMAN,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_ITALIAN,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_ITALIAN,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_ITALIAN,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_JAPANESE,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_JAPANESE,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_PORTUGUESE_BRAZIL,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_PORTUGUESE_BRAZIL,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_PORTUGUESE_BRAZIL,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_RUSSIAN,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_RUSSIAN,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_SPANISH,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_SPANISH,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_SPANISH,
            DATABASE_TABLE_CARDS + "." + KEY_NAME_KOREAN,
            DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID_KOREAN,
            DATABASE_TABLE_CARDS + "." + KEY_WATERMARK
    ));

    /* All the columns in DATABASE_CREATE_SETS */
    private static final List<String> ALL_SET_DATA_KEYS = Collections.unmodifiableList(Arrays.asList(
            DATABASE_TABLE_SETS + "." + KEY_ID,
            DATABASE_TABLE_SETS + "." + KEY_NAME,
            DATABASE_TABLE_SETS + "." + KEY_CODE,
            DATABASE_TABLE_SETS + "." + KEY_CODE_MTGI,
            DATABASE_TABLE_SETS + "." + KEY_NAME_TCGPLAYER,
            DATABASE_TABLE_SETS + "." + KEY_DIGEST,
            DATABASE_TABLE_SETS + "." + KEY_DATE,
            DATABASE_TABLE_SETS + "." + KEY_CAN_BE_FOIL,
            DATABASE_TABLE_SETS + "." + KEY_ONLINE_ONLY,
            DATABASE_TABLE_SETS + "." + KEY_BORDER_COLOR,
            DATABASE_TABLE_SETS + "." + KEY_SET_TYPE));

    /* SQL Strings used to create the database tables */
    private static final String DATABASE_CREATE_FORMATS =
            "create table " + DATABASE_TABLE_FORMATS + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_NAME + " text not null);";

    private static final String DATABASE_CREATE_LEGAL_SETS =
            "create table " + DATABASE_TABLE_LEGAL_SETS + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_SET + " text not null, " +
                    KEY_FORMAT + " text not null);";

    private static final String DATABASE_CREATE_BANNED_CARDS =
            "create table " + DATABASE_TABLE_BANNED_CARDS + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_NAME + " text not null, " +
                    KEY_LEGALITY + " integer not null, " +
                    KEY_FORMAT + " text not null);";

    private static final String DATABASE_CREATE_GLOSSARY =
            "create table " + DATABASE_TABLE_GLOSSARY + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_TERM + " text not null, " +
                    KEY_DEFINITION + " text not null);";

    static final String DATABASE_CREATE_CARDS =
            "create table " + DATABASE_TABLE_CARDS + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_NAME + " text not null, " +
                    KEY_SET + " text not null, " +
                    KEY_SCRYFALL_SET_CODE + " text not null, " +
                    KEY_SUPERTYPE + " text not null, " +
                    KEY_SUBTYPE + " text not null, " +
                    KEY_RARITY + " integer, " +
                    KEY_MANACOST + " text, " +
                    KEY_CMC + " integer not null, " +
                    KEY_POWER + " real, " +
                    KEY_TOUGHNESS + " real, " +
                    KEY_LOYALTY + " integer, " +
                    KEY_ABILITY + " text, " +
                    KEY_FLAVOR + " text, " +
                    KEY_ARTIST + " text, " +
                    KEY_NUMBER + " text, " +
                    KEY_MULTIVERSEID + " integer not null, " +
                    KEY_COLOR + " text not null, " +
                    KEY_COLOR_IDENTITY + " text, " +
                    KEY_RULINGS + " text, " +
                    KEY_NAME_NO_ACCENT + " text not null, " +
                    KEY_WATERMARK + " text, " +
                    KEY_NAME_CHINESE_TRADITIONAL + " text, " +
                    KEY_MULTIVERSEID_CHINESE_TRADITIONAL + " integer, " +
                    KEY_NAME_CHINESE_SIMPLIFIED + " text, " +
                    KEY_MULTIVERSEID_CHINESE_SIMPLIFIED + " integer, " +
                    KEY_NAME_FRENCH + " text, " +
                    KEY_NAME_NO_ACCENT_FRENCH + " text, " +
                    KEY_MULTIVERSEID_FRENCH + " integer, " +
                    KEY_NAME_GERMAN + " text, " +
                    KEY_NAME_NO_ACCENT_GERMAN + " text, " +
                    KEY_MULTIVERSEID_GERMAN + " integer, " +
                    KEY_NAME_ITALIAN + " text, " +
                    KEY_NAME_NO_ACCENT_ITALIAN + " text, " +
                    KEY_MULTIVERSEID_ITALIAN + " integer, " +
                    KEY_NAME_JAPANESE + " text, " +
                    KEY_MULTIVERSEID_JAPANESE + " integer, " +
                    KEY_NAME_PORTUGUESE_BRAZIL + " text, " +
                    KEY_NAME_NO_ACCENT_PORTUGUESE_BRAZIL + " text, " +
                    KEY_MULTIVERSEID_PORTUGUESE_BRAZIL + " integer, " +
                    KEY_NAME_RUSSIAN + " text, " +
                    KEY_MULTIVERSEID_RUSSIAN + " integer, " +
                    KEY_NAME_SPANISH + " text, " +
                    KEY_NAME_NO_ACCENT_SPANISH + " text, " +
                    KEY_MULTIVERSEID_SPANISH + " integer, " +
                    KEY_NAME_KOREAN + " text, " +
                    KEY_MULTIVERSEID_KOREAN + " integer);";

    static final String DATABASE_CREATE_SETS =
            "create table " + DATABASE_TABLE_SETS + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_NAME + " text not null, " +
                    KEY_CODE + " text not null unique, " +
                    KEY_CODE_MTGI + " text not null, " +
                    KEY_NAME_TCGPLAYER + " text, " +
                    KEY_DIGEST + " text, " +
                    KEY_CAN_BE_FOIL + " integer, " +
                    KEY_ONLINE_ONLY + " integer, " +
                    KEY_BORDER_COLOR + " text, " +
                    KEY_SET_TYPE + " text, " +
                    KEY_DATE + " integer);";

    private static final String DATABASE_CREATE_RULES =
            "create table " + DATABASE_TABLE_RULES + "(" +
                    KEY_ID + " integer primary key autoincrement, " +
                    KEY_CATEGORY + " integer not null, " +
                    KEY_SUBCATEGORY + " integer not null, " +
                    KEY_ENTRY + " text, " +
                    KEY_RULE_TEXT + " text not null, " +
                    KEY_POSITION + " integer);";

    /* Special values for KEY_POWER and KEY_TOUGHNESS */
    public static final int STAR = -1000;
    public static final int ONE_PLUS_STAR = -1001;
    public static final int TWO_PLUS_STAR = -1002;
    public static final int SEVEN_MINUS_STAR = -1003;
    public static final int STAR_SQUARED = -1004;
    public static final int NO_ONE_CARES = -1005;
    public static final int X = -1006;
    public static final float QUESTION_MARK = -1007;
    public static final float INFINITY = 1000000000; // pronounce it like an astronaut would

    /* The options for printings for a query */
    public static final int MOST_RECENT_PRINTING = 0;
    public static final int FIRST_PRINTING = 1;
    public static final int ALL_PRINTINGS = 2;

    /* The options for format legality for a card */
    public static final int LEGAL = 0;
    public static final int BANNED = 1;
    public static final int RESTRICTED = 2;

    /* The various types of multi-cards */
    public enum MultiCardType {
        NOPE,
        TRANSFORM,
        FUSE,
        SPLIT,
    }

    /* Used to search for NOT a string, rather than that string */
    public static final String EXCLUDE_TOKEN = "!";
    private static final int EXCLUDE_TOKEN_START = 1;

    /* Use a hash map to increase performance for CardSearchProvider queries */
    private static final HashMap<String, String> mColumnMap = buildColumnMap();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                  Functions for all tables                                  //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Drop all of the tables, then create fresh copies of all of the tables.
     *
     * @param sqLiteDatabase The database to remake tables in
     * @throws FamiliarDbException If something goes wrong
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
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request columns
     * w/o the need to know real column names and create the alias itself.
     *
     * @return A column map
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                               DATABASE_TABLE_CARDS Functions                               //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return a String array of all the unique values in a given column in DATABASE_TABLE_CARDS.
     *
     * @param table       The database table to get a column from
     * @param colKey      The column to return unique values from
     * @param shouldSplit Whether or not each individual word from the column should be considered
     *                    unique, or whether the full String should be considered unique
     * @param database    The database to query
     * @return A String array of unique values from the given column
     * @throws FamiliarDbException If something goes wrong
     */
    public static String[] getUniqueColumnArray(String table, String colKey, boolean shouldSplit,
                                                SQLiteDatabase database) throws FamiliarDbException {
        Cursor cursor = null;
        try {
            String query =
                    "SELECT " + KEY_ID + ", " + colKey +
                            " FROM " + table +
                            " GROUP BY " + colKey +
                            " ORDER BY " + colKey;
            FamiliarLogger.logRawQuery(query, null, new Throwable().getStackTrace()[0].getMethodName());
            cursor = database.rawQuery(query, null);

            // If the cursor is null or has no results, return an empty array
            if (null == cursor || cursor.getCount() == 0) {
                return new String[]{};
            }

            /* Skip over any empty entries in the column */
            int colIndex = cursor.getColumnIndex(colKey);
            cursor.moveToFirst();
            while ("".equals(cursor.getString(colIndex))) {
                cursor.moveToNext();
            }

            /* HashSets contain unique values. Put each individual word in it */
            HashSet<String> words = new HashSet<>();
            while (!cursor.isAfterLast()) {
                if (shouldSplit) {
                    Collections.addAll(words, cursor.getString(colIndex).split("\\s+"));
                } else {
                    words.add(cursor.getString(colIndex));
                }
                cursor.moveToNext();
            }

            /* Turn the HashSet into an array, and sort it */
            String[] wordsArr = words.toArray(new String[0]);
            Arrays.sort(wordsArr);
            return wordsArr;

        } catch (SQLiteException | IllegalStateException | NullPointerException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Given a list of KEY_ID values, return a cursor with all of a cards' information.
     * <p>
     * TODO online only pref
     *
     * @param ids        A list of ids for cards to fetch
     * @param orderByStr A string of keys and directions to order this query by
     * @param database   The database to query
     * @return A cursor with all of the cards' information
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchCards(long[] ids, String orderByStr, SQLiteDatabase database)
            throws FamiliarDbException {
        try {
            boolean first = true;
            StringBuilder selectionStr = new StringBuilder();
            for (long id : ids) {
                if (!first) {
                    selectionStr.append(" OR ");
                } else {
                    first = false;
                }
                selectionStr.append(KEY_ID + "=").append(id);
            }
            String[] allCardDataKeys = new String[ALL_CARD_DATA_KEYS.size()];
            ALL_CARD_DATA_KEYS.toArray(allCardDataKeys);
            FamiliarLogger.logQuery(true, DATABASE_TABLE_CARDS, allCardDataKeys, selectionStr.toString(), null,
                    null, null, orderByStr, null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor cursor = database.query(true, DATABASE_TABLE_CARDS, allCardDataKeys, selectionStr.toString(), null,
                    null, null, orderByStr, null);

            if (cursor != null) {
                cursor.moveToFirst();
            }
            return cursor;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card's name, return a cursor with all of that card's requested information.
     * <p>
     * TODO online only pref
     *
     * @param name               The name of the card to query
     * @param fields             The requested information about the card
     * @param shouldGroup        true if the query should group by KEY_CODE, false otherwise
     * @param offlineOnly        true if the query should exclude online only cards, false otherwise
     * @param preferOptionalFoil true if the query should order cards that may or may not be foil first
     * @param mDb                The database to query
     * @return A cursor with the requested information about the card
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchCardByName(String name, List<String> fields, boolean shouldGroup,
                                         boolean offlineOnly, boolean preferOptionalFoil, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            /* Sanitize the string and remove accent marks */
            name = sanitizeString(name, true);
            StringBuilder sql = new StringBuilder("SELECT ");
            boolean first = true;
            for (String field : fields) {
                if (first) {
                    first = false;
                } else {
                    sql.append(", ");
                }
                sql.append(field);
            }
            sql.append(" FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + " = ").append(name);
            if (offlineOnly) {
                sql.append(" AND " + KEY_ONLINE_ONLY + " = 0");
            }
            sql.append(" COLLATE NOCASE");
            if (shouldGroup) {
                sql.append(" GROUP BY " + DATABASE_TABLE_SETS + "." + KEY_CODE);
            }
            if (preferOptionalFoil) {
                sql.append(" ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_CAN_BE_FOIL + " DESC, " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC");
            } else {
                sql.append(" ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC");
            }

            FamiliarLogger.logRawQuery(sql.toString(), null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor c = mDb.rawQuery(sql.toString(), null);
            if (c != null) {
                c.moveToFirst();
            }
            return c;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a multiverse ID, return a cursor with all of that card's requested information.
     * <p>
     * TODO online only pref
     *
     * @param multiverseId The card's multivers ID
     * @param fields       The requested information about the card
     * @param database     The database to query
     * @return A cursor with the requested information about the card
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchCardByMultiverseId(long multiverseId, String[] fields,
                                                 SQLiteDatabase database)
            throws FamiliarDbException {
        try {
            StringBuilder sql = new StringBuilder("SELECT ");
            boolean first = true;
            for (String field : fields) {
                if (first) {
                    first = false;
                } else {
                    sql.append(", ");
                }
                sql.append(field);
            }
            sql.append(" FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_MULTIVERSEID + " = ").append(multiverseId).append(" GROUP BY ").append(DATABASE_TABLE_SETS).append(".").append(KEY_CODE).append(" ORDER BY ").append(DATABASE_TABLE_SETS).append(".").append(KEY_DATE).append(" DESC");

            FamiliarLogger.logRawQuery(sql.toString(), null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor cursor = database.rawQuery(sql.toString(), null);
            if (cursor != null) {
                cursor.moveToFirst();
            }
            return cursor;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card name and set code, return a cursor with that card's requested data.
     * <p>
     * TODO online only pref
     *
     * @param name    The card's name
     * @param setCode The card's set code
     * @param fields  The requested data
     * @param mDb     The database to query
     * @return A Cursor with the requested information
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchCardByNameAndSet(String name, String setCode, List<String> fields,
                                               SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            /* Sanitize the string and remove accent marks */
            name = sanitizeString(name, true);
            setCode = sanitizeString(setCode, false);

            StringBuilder sql = new StringBuilder("SELECT ");
            boolean first = true;
            for (String field : fields) {
                if (first) {
                    first = false;
                } else {
                    sql.append(", ");
                }
                sql.append(field);
            }

            sql.append(" FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + " = ").append(name).append(" COLLATE NOCASE").append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SET).append(" = ").append(setCode).append(" ORDER BY ").append(DATABASE_TABLE_SETS).append(".").append(KEY_DATE).append(" DESC");

            FamiliarLogger.logRawQuery(sql.toString(), null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor c = mDb.rawQuery(sql.toString(), null);
            if (c != null) {
                c.moveToFirst();
            }
            return c;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a list of cards, fetch all the database info about them in a single query
     * <p>
     * TODO online only pref
     *
     * @param cards A list of cards to fetch info for
     * @param mDb   The database to query
     * @return A Cursor with the requested information
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchCardByNamesAndSets(List<MtgCard> cards, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            StringBuilder sql = new StringBuilder("SELECT ");

            // All the keys
            boolean first = true;
            for (String field : ALL_CARD_DATA_KEYS) {
                if (first) {
                    first = false;
                } else {
                    sql.append(", ");
                }
                sql.append(field).append(" as c_").append(field.split("\\.")[1]);
            }
            for (String field : ALL_SET_DATA_KEYS) {
                sql.append(", ");
                sql.append(field).append(" as s_").append(field.split("\\.")[1]);
            }

            // From a joined table
            sql.append(" FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET);

            sql.append(" WHERE ");

            first = true;
            for (MtgCard card : cards) {
                if (first) {
                    first = false;
                } else {
                    sql.append(" OR ");
                }
                sql.append("(c_").append(KEY_NAME_NO_ACCENT).append(" = ").append(sanitizeString(card.getName(), true)).append(" COLLATE NOCASE");
                if (sanitizeString(card.getExpansion(), false).length() > 0) {
                    sql.append(" AND ").append("c_").append(KEY_SET).append(" = ").append(sanitizeString(card.getExpansion(), false)).append(")");
                } else {
                    sql.append(")");
                }
            }
            sql.append(" ORDER BY s_").append(KEY_DATE).append(" DESC");

            FamiliarLogger.logRawQuery(sql.toString(), null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor c = mDb.rawQuery(sql.toString(), null);
            if (c != null) {
                c.moveToFirst();
            }
            return c;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card name, return the KEY_ID for that card.
     * <p>
     * TODO online only pref
     *
     * @param name The name of the card
     * @param mDb  The database to query
     * @return The KEY_ID value, or -1 if it isn't found
     * @throws FamiliarDbException If something goes wrong
     */
    public static long fetchIdByName(String name, SQLiteDatabase mDb) throws FamiliarDbException {
        /* replace lowercase ae with Ae */
        name = sanitizeString(name, true);

        String sql = "SELECT " +
                DATABASE_TABLE_CARDS + "." + KEY_ID + ", " +
                DATABASE_TABLE_CARDS + "." + KEY_SET + ", " +
                DATABASE_TABLE_SETS + "." + KEY_DATE +
                " FROM (" + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " +
                DATABASE_TABLE_CARDS + "." + KEY_SET + "=" +
                DATABASE_TABLE_SETS + "." + KEY_CODE + ")" +
                " WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + " = "
                + name + " COLLATE NOCASE ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC";

        try (Cursor cursor = mDb.rawQuery(sql, null)) {
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return cursor.getLong(cursor.getColumnIndex(CardDbAdapter.KEY_ID));
            }
            return -1;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card name, return the KEY_ID of matching cards, searching every language.
     * <p>
     * TODO online only pref
     *
     * @param name The name of the card
     * @param mDb  The database to query
     * @return The KEY_ID values
     * @throws FamiliarDbException If something goes wrong
     */
    public static long[] fetchIdsByLocalizedName(String name, SQLiteDatabase mDb) throws FamiliarDbException {
        /* replace lowercase ae with Ae */
        name = sanitizeString(name, true);

        String sql = "SELECT " +
                DATABASE_TABLE_CARDS + "." + KEY_ID +
                " FROM " + DATABASE_TABLE_CARDS +
                " JOIN " + DATABASE_TABLE_SETS +
                " ON " + DATABASE_TABLE_CARDS + "." + KEY_SET + "=" +
                DATABASE_TABLE_SETS + "." + KEY_CODE +
                " WHERE " + name + " COLLATE NOCASE IN (" +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_CHINESE_TRADITIONAL + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_CHINESE_SIMPLIFIED + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_FRENCH + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_GERMAN + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_ITALIAN + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_JAPANESE + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_PORTUGUESE_BRAZIL + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_RUSSIAN + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT_SPANISH + "," +
                DATABASE_TABLE_CARDS + "." + KEY_NAME_KOREAN +
                ") ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC";

        try (Cursor cursor = mDb.rawQuery(sql, null)) {
            if (cursor != null && cursor.getCount() > 0) {
                long[] ids = new long[cursor.getCount()];
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToNext();
                    ids[i] = cursor.getLong(cursor.getColumnIndex(CardDbAdapter.KEY_ID));
                }
                return ids;
            }
            return new long[0];
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * This function will query the database with the information in criteria and return a cursor
     * with the requested data.
     * <p>
     * TODO online only pref
     *
     * @param criteria    The criteria used to build the query
     * @param backface    Whether or not the results should include the 'b' side of multicards
     * @param returnTypes The columns which should be returned in the cursor
     * @param consolidate true to not include multiple printings of the same card, false otherwise
     * @param orderByStr  A string used to order the results
     * @param mDb         The database to query
     * @return A cursor with the requested information about the queried cards
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor Search(SearchCriteria criteria, boolean backface, String[] returnTypes,
                                boolean consolidate, String orderByStr, SQLiteDatabase mDb)
            throws FamiliarDbException {
        FamiliarLogger.appendToLogFile(new StringBuilder(criteria.toJson()), new Throwable().getStackTrace()[0].getMethodName());

        StringBuilder statement = new StringBuilder(" WHERE 1=1");

        if (criteria.name != null) {
            String[] nameParts = criteria.name.split(" ");
            for (String s : nameParts) {
                statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + " LIKE ").append(sanitizeString("%" + s + "%", true)).append(")");
            }
        }



        /* Check if the watermark matches exactly */
        if (criteria.watermark != null) {
            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_WATERMARK + " = ").append(sanitizeString(criteria.watermark, false)).append(")");
        }

        /*
         * Reuben's version Differences: Original code is verbose only, but mine allows for matching
         * exact text, all words, or just any one word.
         */
        if (criteria.text != null) {
            /* Separate each individual word or quoted phrase */
            Set<String> cardTextParts = new HashSet<>();

            /* The limit=-1 avoids split() to remove trailing empty strings, required for the algorithm */
            /* The negative lookbehind avoids matching escaped backslashes */
            String[] blocks = criteria.text.split("(?<!\\\\)\"");

            for (int i = 0; i < blocks.length; i++) {
                String block = blocks[i].replaceAll("\\\\\"", "\"");

                /* Even blocks are non-quoted blocks */
                if (i % 2 == 0) {
                    // Split the block by spaces
                    for (String word : block.split("\\s+")) {
                        if (!word.isEmpty()) {
                            cardTextParts.add(word);
                        }
                    }
                } else {
                    if (!block.isEmpty()) {
                        cardTextParts.add(block);
                    }
                }
            }

            /*
             * The following switch statement tests to see which text search logic was chosen by the
             * user. If they chose the first option (0), then look for cards with text that includes
             * all words, but not necessarily the exact phrase. The second option (1) finds cards
             * that have 1 or more of the chosen words in their text. The third option (2) searches
             * for the exact phrase as entered by the user. The 'default' option is impossible via
             * the way the code is written, but I believe it's also mandatory to include it in case
             * someone else is perhaps fussing with the code and breaks it. The if statement at the
             * end is theoretically unnecessary, because once we've entered the current if
             * statement, there is no way to NOT change the statement variable. However, you never
             * really know who's going to break open your code and fuss around with it, so it's
             * always good to leave some small safety measures.
             */
            switch (criteria.textLogic) {
                case 0:
                    for (String s : cardTextParts) {
                        if (s.contains(EXCLUDE_TOKEN))
                            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE ").append(sanitizeString("%" + s.substring(EXCLUDE_TOKEN_START) + "%", false)).append(")");
                        else
                            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE ").append(sanitizeString("%" + s + "%", false)).append(")");
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : cardTextParts) {
                        if (firstRun) {
                            firstRun = false;
                            if (s.contains(EXCLUDE_TOKEN))
                                statement.append(" AND ((" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE ").append(sanitizeString("%" + s.substring(EXCLUDE_TOKEN_START) + "%", false)).append(")");
                            else
                                statement.append(" AND ((" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE ").append(sanitizeString("%" + s + "%", false)).append(")");
                        } else {
                            if (s.contains(EXCLUDE_TOKEN))
                                statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE ").append(sanitizeString("%" + s.substring(EXCLUDE_TOKEN_START) + "%", false)).append(")");
                            else
                                statement.append(" OR (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE ").append(sanitizeString("%" + s + "%", false)).append(")");
                        }
                    }
                    statement.append(")");
                    break;
                default:
                    break;
            }
        }
        /*
         * End Reuben's version
         * Reuben's version Differences: Original version only allowed for
         * including all types, not any of the types or excluding the given
         * types.
         */

        List<String> supertypes = criteria.superTypes;
        List<String> subtypes = criteria.subTypes;

        if (criteria.isCommander) {
            // Don't check the backface for commanders
            backface = false;

            // Planeswalkers can be commanders
            statement.append(" AND ((").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" LIKE '%Planeswalker%'");
            // In brawl, every planeswalker is a commander! Otherwise it needs special text
            if (!("Brawl".equals(criteria.format) || "Historic".equals(criteria.format))) {
                statement.append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_ABILITY).append(" LIKE '%can be your commander%'");
            }

            // Legendary creatures can be commanders
            statement.append(") OR (").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" LIKE '%Legendary%Creature%'))");

            // Set the format to Commander if it isn't set already, so the banlist applies
            if (criteria.format == null) {
                criteria.format = "Commander";
            }
        }

        if (supertypes != null && !supertypes.isEmpty()) {
            /* Concat a leading and a trailing space to the supertype */
            final String supertypeInDb = "' ' || " + DATABASE_TABLE_CARDS + "." + KEY_SUPERTYPE + " || ' '";

            switch (criteria.typeLogic) {
                case 0:
                    for (String s : supertypes) {
                        if (s.contains(EXCLUDE_TOKEN)) {
                            statement.append(" AND (" + supertypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                        } else
                            statement.append(" AND (" + supertypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : supertypes) {
                        if (firstRun) {
                            firstRun = false;

                            if (s.contains(EXCLUDE_TOKEN))
                                statement.append(" AND ((" + supertypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                            else
                                statement.append(" AND ((" + supertypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                        } else if (s.contains(EXCLUDE_TOKEN))
                            statement.append(" AND (" + supertypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                        else
                            statement.append(" OR (" + supertypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                    }
                    statement.append(")");
                    break;
                case 2:
                    for (String s : supertypes) {
                        statement.append(" AND (" + supertypeInDb + " NOT LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                    }
                    break;
                default:
                    break;
            }
        }

        if (subtypes != null && !subtypes.isEmpty()) {
            /* Concat a leading and a trailing space to the subtype */
            final String subtypeInDb = "' ' || " + DATABASE_TABLE_CARDS + "." + KEY_SUBTYPE + " || ' '";

            switch (criteria.typeLogic) {
                case 0:
                    for (String s : subtypes) {
                        if (s.contains(EXCLUDE_TOKEN)) {
                            statement.append(" AND (" + subtypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                        } else {
                            statement.append(" AND (" + subtypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                        }
                    }
                    break;
                case 1:
                    boolean firstRun = true;
                    for (String s : subtypes) {
                        if (firstRun) {
                            firstRun = false;
                            if (s.contains(EXCLUDE_TOKEN))
                                statement.append(" AND ((" + subtypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                            else
                                statement.append(" AND ((" + subtypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                        } else if (s.contains(EXCLUDE_TOKEN))
                            statement.append(" AND (" + subtypeInDb + " NOT LIKE ").append(sanitizeString("% " + s.substring(1) + " %", false)).append(")");
                        else
                            statement.append(" OR (" + subtypeInDb + " LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                    }
                    statement.append(")");
                    break;
                case 2:
                    for (String s : subtypes) {
                        statement.append(" AND (" + subtypeInDb + " NOT LIKE ").append(sanitizeString("% " + s + " %", false)).append(")");
                    }
                    break;
                default:
                    break;
            }
        }
        /* End Reuben's version
         *************************************************************************************/

        if (criteria.flavor != null) {
            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_FLAVOR + " LIKE ").append(sanitizeString("%" + criteria.flavor + "%", false)).append(")");
        }

        if (criteria.artist != null) {
            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_ARTIST + " LIKE ").append(sanitizeString("%" + criteria.artist + "%", false)).append(")");
        }

        if (criteria.collectorsNumber != null) {
            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER + " = ").append(sanitizeString(criteria.collectorsNumber, false)).append(")");
        }

        /*
         * Code below added/modified by Reuben. Differences: Original version only had 'Any' and
         * 'All' options and lacked 'Exclusive' and 'Exact' matching. In addition, original
         * programming only provided exclusive results.
         */
        if (null != criteria.color &&
                !(criteria.color.equals("wubrgl") || (criteria.color.equals("WUBRGL") &&
                        criteria.colorLogic == 0))) {
            /* Can't contain these colors
             **
             * ...if the chosen color logic was exactly (2) or none (3) of the selected colors.
             */
            if (criteria.colorLogic > 1) {
                statement.append(" AND (( 1=1");
                for (byte b : criteria.color.getBytes()) {
                    char ch = (char) b;
                    switch (ch) {
                        case 'l': {
                            // This means colorless
                            statement.append(" AND ").append(DATABASE_TABLE_CARDS + "." + KEY_COLOR + " NOT LIKE ''");
                            break;
                        }
                        case 'w':
                        case 'u':
                        case 'b':
                        case 'r':
                        case 'g': {
                            statement.append(" AND ").append(DATABASE_TABLE_CARDS + "." + KEY_COLOR + " NOT LIKE '%").append(Character.toUpperCase(ch)).append("%'");
                            break;
                        }
                    }
                }
                statement.append(") AND ( ");
            }

            /* Might contain these colors */
            if (criteria.colorLogic < 2) {
                statement.append(" AND (");
            }

            /* Start the logic and pick the conjunction */
            String conjunction;
            if (criteria.colorLogic == 1 || criteria.colorLogic == 3) {
                conjunction = " AND ";
                statement.append(" 1=1 ");
            } else {
                conjunction = " OR ";
                statement.append(" 0=1 ");
            }

            for (byte b : criteria.color.getBytes()) {
                char ch = (char) b;
                switch (ch) {
                    case 'L': {
                        statement.append(conjunction).append(DATABASE_TABLE_CARDS + "." + KEY_COLOR + " LIKE ''");
                        break;
                    }
                    case 'W':
                    case 'U':
                    case 'B':
                    case 'R':
                    case 'G': {
                        statement.append(conjunction).append(DATABASE_TABLE_CARDS + "." + KEY_COLOR + " LIKE '%").append(ch).append("%'");
                        break;
                    }
                }
            }

            if (criteria.colorLogic > 1) {
                statement.append("))");
            } else {
                statement.append(")");
            }
        }
        /* End of addition
         *************************************************************************************/

        /*
         * Color Identity Filter
         * If a color is selected, it's upper case. Otherwise it's lower case.
         */
        if (null != criteria.colorIdentity && !(criteria.colorIdentity.equals("wubrgl"))) {
            switch (criteria.colorIdentityLogic) {
                case 0: {
                    // All colors selected and "may include any color" doesn't make sense
                    if (criteria.colorIdentity.equals("WUBRGL")) {
                        break;
                    }
                    /* search_May_include_any_colors */
                    statement.append(" AND ( 1=1");
                    for (int i = 0; i < criteria.colorIdentity.length(); i++) {
                        switch (criteria.colorIdentity.charAt(i)) {
                            case 'l': {
                                /* If colorless isn't selected, don't allow empty identities */
                                statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_COLOR_IDENTITY + " NOT LIKE \"\")");
                                break;
                            }
                            case 'w':
                            case 'u':
                            case 'b':
                            case 'r':
                            case 'g': {
                                statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_COLOR_IDENTITY + " NOT LIKE \"%").append(criteria.colorIdentity.toUpperCase().charAt(i)).append("%\")");
                                break;
                            }
                        }
                    }
                    statement.append(")");
                    break;
                }
                case 1: {
                    /* search_Exact_all_selected_and_no_others */
                    StringBuilder colorIdentity = new StringBuilder();
                    for (int i = 0; i < criteria.colorIdentity.length(); i++) {
                        switch (criteria.colorIdentity.charAt(i)) {
                            case 'L': {
                                /* Colorless identity is the empty string */
                                statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_COLOR_IDENTITY + " = \"\")");
                                break;
                            }
                            case 'W':
                            case 'U':
                            case 'B':
                            case 'R':
                            case 'G': {
                                colorIdentity.append(criteria.colorIdentity.charAt(i));
                                break;
                            }
                        }
                    }
                    statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_COLOR_IDENTITY + " = \"").append(colorIdentity).append("\")");
                    break;
                }
            }
        }

        if (criteria.sets != null && criteria.sets.size() > 0) {
            statement.append(" AND (");

            boolean first = true;

            for (String set : criteria.sets) {
                if (first) {
                    first = false;
                } else {
                    statement.append(" OR ");
                }
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_SET + " = '").append(set).append("'");
            }

            statement.append(")");
        }

        if (criteria.setTypes != null && !criteria.setTypes.isEmpty()) {
            statement.append(" AND (");

            boolean first = true;

            for (String setType : criteria.setTypes) {
                if (first) {
                    first = false;
                } else {
                    statement.append(" OR ");
                }
                statement.append(KEY_SET_TYPE + " = '").append(setType).append("'");
            }

            statement.append(")");
        }

        if (criteria.powChoice != NO_ONE_CARES) {
            statement.append(" AND (");

            if (criteria.powChoice > STAR) {
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_POWER + " ").append(criteria.powLogic).append(" ").append(criteria.powChoice);
                if (criteria.powLogic.equals("<")) {
                    statement.append(" AND " + DATABASE_TABLE_CARDS + "." + KEY_POWER + " > " + STAR);
                }
            } else if (criteria.powLogic.equals("=")) {
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_POWER + " ").append(criteria.powLogic).append(" ").append(criteria.powChoice);
            }
            statement.append(")");
        }

        if (criteria.touChoice != NO_ONE_CARES) {
            statement.append(" AND (");

            if (criteria.touChoice > STAR) {
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " ").append(criteria.touLogic).append(" ").append(criteria.touChoice);
                if (criteria.touLogic.equals("<")) {
                    statement.append(" AND " + DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " > " + STAR);
                }
            } else if (criteria.touLogic.equals("=")) {
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " ").append(criteria.touLogic).append(" ").append(criteria.touChoice);
            }
            statement.append(")");
        }

        if (null != criteria.manaCostLogic && null != criteria.manaCost) {
            StringBuilder manaCost = new StringBuilder();
            for (String mana : criteria.manaCost) {
                manaCost.append('{').append(mana).append('}');
            }
            statement = criteria.manaCostLogic.appendToSql(statement,
                    DATABASE_TABLE_CARDS + "." + KEY_MANACOST, manaCost.toString());
        }

        if (criteria.cmc != -1) {
            statement.append(" AND (");

            statement.append(DATABASE_TABLE_CARDS + "." + KEY_CMC + " ").append(criteria.cmcLogic).append(" ").append(criteria.cmc).append(")");
        }

        if (criteria.moJhoStoFilter) {
            /* Filter out tokens. */
            statement.append(" AND (" +
                    /* Cards without mana costs. */
                    "NOT " + DATABASE_TABLE_CARDS + "." + KEY_MANACOST + " = '' " +
                    /* Cards like 'Dryad Arbor'. */
                    "OR " + DATABASE_TABLE_CARDS + "." + KEY_SUPERTYPE + " LIKE '%Land Creature%')");
            /* Filter out 'UN-'sets*/
            statement.append(" AND NOT ").append(DATABASE_TABLE_SETS).append(".").append(KEY_BORDER_COLOR).append(" = \"Silver\"");
        }

        if (criteria.rarity != null) {
            statement.append(" AND (");

            boolean firstPrint = true;
            for (int i = 0; i < criteria.rarity.length(); i++) {
                if (firstPrint) {
                    firstPrint = false;
                } else {
                    statement.append(" OR ");
                }
                statement.append(DATABASE_TABLE_CARDS + "." + KEY_RARITY + " = ").append((int) criteria.rarity.toUpperCase().charAt(i));
            }
            statement.append(")");
        }

        if (criteria.format != null) {
            /* Check if the format is eternal or not, by the number of legal sets */
            try (Cursor numLegalSetCursor = mDb.rawQuery("SELECT * FROM " + DATABASE_TABLE_LEGAL_SETS + " WHERE " + KEY_FORMAT + " = \"" + criteria.format + "\"", null)) {
                /* If the format is not eternal, filter by set */
                if (numLegalSetCursor.getCount() > 0) {
                    statement
                            .append(" AND ")
                            .append(DATABASE_TABLE_CARDS).append(".").append(KEY_NAME)
                            .append(" IN ( SELECT ")
                            .append(DATABASE_TABLE_CARDS).append("_B.").append(KEY_NAME)
                            .append(" FROM ")
                            .append(DATABASE_TABLE_CARDS).append(" ").append(DATABASE_TABLE_CARDS).append("_B ")
                            .append(" WHERE ")
                            .append(DATABASE_TABLE_CARDS).append("_B.").append(KEY_SET)
                            .append(" IN ( SELECT ")
                            .append(DATABASE_TABLE_LEGAL_SETS).append(".").append(KEY_SET)
                            .append(" FROM ")
                            .append(DATABASE_TABLE_LEGAL_SETS)
                            .append(" WHERE ")
                            .append(DATABASE_TABLE_LEGAL_SETS).append(".").append(KEY_FORMAT).append("='").append(criteria.format).append("' ) )");
                } else {
                    /* Otherwise filter silver bordered cards, giant cards */
                    statement.append(" AND NOT ")
                            .append(DATABASE_TABLE_SETS).append(".").append(KEY_BORDER_COLOR).append(" = \"Silver\"");
                    statement
                            .append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" NOT LIKE 'Plane'")
                            .append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" NOT LIKE 'Conspiracy'")
                            .append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" NOT LIKE '%Scheme'")
                            .append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_SUPERTYPE).append(" NOT LIKE 'Vanguard'");
                }

                /* And make sure the name isn't in the list of banned cads */
                statement.append(" AND ")
                        .append(DATABASE_TABLE_CARDS).append(".").append(KEY_NAME)
                        .append(" NOT IN (SELECT ")
                        .append(DATABASE_TABLE_BANNED_CARDS).append(".").append(KEY_NAME)
                        .append(" FROM ")
                        .append(DATABASE_TABLE_BANNED_CARDS)
                        .append(" WHERE ")
                        .append(DATABASE_TABLE_BANNED_CARDS).append(".").append(KEY_FORMAT).append(" = '").append(criteria.format).append("'")
                        .append(" AND ")
                        .append(DATABASE_TABLE_BANNED_CARDS).append(".").append(KEY_LEGALITY).append(" = ").append(BANNED)
                        .append(")");

                // Ensure pauper only searches commons in valid Pauper sets
                if ("Pauper".equals(criteria.format)) {
                    statement.append(" AND ").append(DATABASE_TABLE_CARDS).append(".").append(KEY_RARITY).append(" = ").append(((int) 'C')).append(" ");
                }

            } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
                throw new FamiliarDbException(e);
            }
        }

        if (!backface) {
            statement.append(" AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER + " NOT LIKE '%b%')");
        }

        if (criteria.setLogic != MOST_RECENT_PRINTING && criteria.setLogic != ALL_PRINTINGS) {
            statement.insert(0, " JOIN (SELECT iT" + DATABASE_TABLE_CARDS + "."
                    + KEY_NAME + ", MIN(" + DATABASE_TABLE_SETS + "."
                    + KEY_DATE + ") AS " + KEY_DATE + " FROM "
                    + DATABASE_TABLE_CARDS + " AS iT" + DATABASE_TABLE_CARDS
                    + " JOIN " + DATABASE_TABLE_SETS + " ON iT"
                    + DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
                    + DATABASE_TABLE_SETS + "." + KEY_CODE + " GROUP BY iT"
                    + DATABASE_TABLE_CARDS + "." + KEY_NAME
                    + ") AS FirstPrints" + " ON " + DATABASE_TABLE_CARDS + "."
                    + KEY_NAME + " = FirstPrints." + KEY_NAME);
            if (criteria.setLogic == FIRST_PRINTING)
                statement.insert(0, " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + " = FirstPrints." + KEY_DATE);
            else
                statement.insert(0, " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
                        + " <> FirstPrints." + KEY_DATE);
        }

        if (statement.toString().equals(" WHERE 1=1")) {
            /* If the statement is just this, it means we added nothing */
            return null;
        }

        try {
            StringBuilder sel = new StringBuilder();
            for (String s : returnTypes) {
                if (sel.length() > 0) {
                    sel.append(", ");
                }
                sel.append(DATABASE_TABLE_CARDS + ".").append(s).append(" AS ").append(s);
            }
            sel.append(", " + DATABASE_TABLE_SETS + "." + KEY_DATE);
            sel.append(", " + DATABASE_TABLE_SETS + "." + KEY_SET_TYPE);

            String sql = "SELECT * FROM (SELECT " + sel + " FROM " + DATABASE_TABLE_CARDS
                    + " JOIN " + DATABASE_TABLE_SETS + " ON "
                    + DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
                    + DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

            if (null == orderByStr) {
                orderByStr = KEY_NAME + " COLLATE UNICODE";
            } else if (!(criteria.setLogic != MOST_RECENT_PRINTING && criteria.setLogic != ALL_PRINTINGS)) {
                // When sorting by set, sort by date first, then name
                orderByStr = orderByStr.replace(CardDbAdapter.KEY_SET + " asc", CardDbAdapter.KEY_DATE + " asc," + CardDbAdapter.KEY_SET + " asc");
                orderByStr = orderByStr.replace(CardDbAdapter.KEY_SET + " desc", CardDbAdapter.KEY_DATE + " desc," + CardDbAdapter.KEY_SET + " desc");
            }

            if (consolidate) {
                sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC, " + KEY_ID + " DESC"
                        + ") GROUP BY " + KEY_NAME + " ORDER BY " + orderByStr;
            } else {
                sql += " ORDER BY " + orderByStr
                        + ", " + DATABASE_TABLE_SETS + "." + KEY_DATE + " ASC, " + KEY_ID + " DESC)";
            }
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            Cursor cursor = mDb.rawQuery(sql, null);
            if (cursor != null) {
                cursor.moveToFirst();
            }
            return cursor;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a set and a card number, return the KEY_ID for that card.
     * <p>
     * TODO online only pref
     *
     * @param set    The set code
     * @param number The number to look up
     * @param mDb    The database to query
     * @return The KEY_ID value for the found card, or -1 if the card isn't found
     * @throws FamiliarDbException If something goes wrong
     */
    public static int getIdFromSetAndNumber(String set, String number, SQLiteDatabase mDb)
            throws FamiliarDbException {
        String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
                + KEY_SET + " = '" + set + "')";
        FamiliarLogger.logQuery(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_ID}, statement, null, null, null,
                KEY_ID, null, new Throwable().getStackTrace()[0].getMethodName());
        try (Cursor c = mDb.query(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_ID}, statement, null, null, null,
                KEY_ID, null)) {
            c.moveToFirst();
            return c.getInt(c.getColumnIndex(KEY_ID));
        } catch (SQLiteException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } catch (CursorIndexOutOfBoundsException e) {
            return -1; /* The other half doesn't exist... */
        }
    }

    /**
     * Returns a card name queried by set and collector's number.
     * <p>
     * TODO online only pref
     *
     * @param set    The set code
     * @param number The number to look up
     * @param mDb    The database to query
     * @return The KEY_NAME value for the found card, or null if the card isn't found
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getNameFromSetAndNumber(String set, String number, SQLiteDatabase mDb)
            throws FamiliarDbException {
        String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
                + KEY_SET + " = '" + set + "')";
        FamiliarLogger.logQuery(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_NAME}, statement, null, null, null,
                KEY_NAME, null, new Throwable().getStackTrace()[0].getMethodName());
        try (Cursor c = mDb.query(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_NAME}, statement, null, null, null,
                KEY_NAME, null)) {
            c.moveToFirst();
            return c.getString(c.getColumnIndex(KEY_NAME));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card name, find the ID for that card in the database
     * <p>
     * TODO online only pref
     *
     * @param name The name of the card to search for
     * @return The ID in the database
     */
    public static long getIdFromName(String name, SQLiteDatabase mDb) throws FamiliarDbException {
        String statement = "(" + KEY_NAME + " = " + sanitizeString(name, false) + ")";
        FamiliarLogger.logQuery(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_ID}, statement, null, null, null,
                KEY_NAME, null, new Throwable().getStackTrace()[0].getMethodName());
        try (Cursor c = mDb.query(true, DATABASE_TABLE_CARDS,
                new String[]{KEY_ID}, statement, null, null, null,
                KEY_NAME, null)) {
            c.moveToFirst();
            return c.getLong(c.getColumnIndex(KEY_ID));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Returns a Cursor positioned at the word specified by rowId.
     * <p>
     * TODO online only pref
     *
     * @param rowId   id of word to retrieve
     * @param columns The columns to include, if null then all are included
     * @param mDb     The database to query
     * @return Cursor positioned to matching word, or null if not found.
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getCardByRowId(String rowId, String[] columns, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            /*
             * The SQLiteBuilder provides a map for all possible columns requested to actual columns in
             * the database, creating a simple column alias mechanism by which the ContentProvider does
             * not need to know the real column names.
             */
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(DATABASE_TABLE_CARDS);
            builder.setProjectionMap(mColumnMap);

            /*
             * This builds a query that looks like: SELECT <columns> FROM <table>
             * WHERE rowid = <rowId>
             */
            FamiliarLogger.logBuiltQuery(columns, "rowid = ?", new String[]{rowId},
                    KEY_NAME, null, KEY_NAME, new Throwable().getStackTrace()[0].getMethodName());
            Cursor cursor = builder.query(mDb, columns, "rowid = ?", new String[]{rowId},
                    KEY_NAME, null, KEY_NAME);

            if (cursor != null && !cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            return cursor;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Returns a Cursor over all words that match the given query.
     * <p>
     * TODO online only pref
     *
     * @param query The string to search for
     * @param mDb   The database to query
     * @return Cursor over all words that match, or null if none found.
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getCardsByNamePrefix(String query, SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            query = sanitizeString(query + "%", true);

            if (query.length() < 2) {
                return null;
            }

            String sql =
                    "SELECT * FROM (" +
                            "SELECT " +
                            DATABASE_TABLE_CARDS + "." + KEY_NAME + " AS " + KEY_NAME + ", " +
                            DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + KEY_ID + ", " +
                            DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID +
                            " FROM " + DATABASE_TABLE_CARDS +
                            " JOIN " + DATABASE_TABLE_SETS +
                            " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
                            " WHERE " +
                            DATABASE_TABLE_CARDS + "." + KEY_NAME_NO_ACCENT + " LIKE " + query +
                            " ORDER BY " +
                            DATABASE_TABLE_CARDS + "." + KEY_NAME + " COLLATE UNICODE, " +
                            DATABASE_TABLE_SETS + "." + KEY_DATE + " ASC" +
                            " ) GROUP BY " + KEY_NAME;
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a collector's number and expansion, return the combined name for a multi-card
     *
     * @param expansion The expansion for this card
     * @param number    The card's number, with a letter suffix
     * @param mDb       The database to search
     * @return The whole, combined name for this multi-part card
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getCombinedName(String expansion, String number, SQLiteDatabase mDb)
            throws FamiliarDbException {

        // Strip the last part of the number, if it is a letter
        if (Character.isAlphabetic(number.charAt(number.length() - 1))) {
            number = number.substring(0, number.length() - 1);
        }

        // Select all rows from the database for cards with this number
        String statement = "SELECT " + KEY_NAME + ", " + KEY_NUMBER +
                " FROM " + DATABASE_TABLE_CARDS +
                " WHERE " + KEY_SET + " = '" + expansion + "' AND " + KEY_NUMBER + " LIKE '" + number + "%'" +
                " ORDER BY " + KEY_NUMBER + " ASC";

        // For every returned row
        try (Cursor c = mDb.rawQuery(statement, null)) {
            StringBuilder retVal = new StringBuilder();
            c.moveToFirst();
            while (!c.isAfterLast()) {
                // If we're not starting off, append the delimiter
                if (retVal.length() > 0) {
                    retVal.append(" // ");
                }
                // Append the card name
                retVal.append(c.getString(c.getColumnIndex(KEY_NAME)));
                c.moveToNext();
            }
            return retVal.toString();
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * For cards without a multiverse ID, find an equivalent for Gatherer lookups
     *
     * @param mName The name of this card
     * @param mDb   The database to query for another multiverse ID
     * @return A multiverse ID for a different card with the same name
     * @throws FamiliarDbException If something goes wrong
     */
    public static int getEquivalentMultiverseId(String mName, SQLiteDatabase mDb) throws FamiliarDbException {
        // Select all rows from the database for cards with this number
        String statement = "SELECT " + KEY_MULTIVERSEID + ", " + KEY_DATE +
                " FROM (" + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET + ")" +
                " WHERE (" + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + sanitizeString(mName, false) + ")" +
                " ORDER BY " + KEY_DATE + " DESC";

        // For every returned row
        try (Cursor c = mDb.rawQuery(statement, null)) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                int multiverseId = c.getInt(c.getColumnIndex(KEY_MULTIVERSEID));
                if (multiverseId > 0) {
                    return multiverseId;
                }
                c.moveToNext();
            }
            return 0;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Add a MtgCard to DATABASE_TABLE_CARDS.
     *
     * @param card The card to add to DATABASE_TABLE_CARDS
     * @param mDb  The database to add the card to
     */
    public static void createCard(Card card, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();

        String delimiter = " - ";
        initialValues.put(KEY_NAME, card.getName());
        initialValues.put(KEY_SET, card.getExpansion());
        initialValues.put(KEY_SCRYFALL_SET_CODE, card.getScryfallSetCode());
        String[] types = card.getType().split(delimiter);
        if (types.length > 0) {
            initialValues.put(KEY_SUPERTYPE, types[0]);
        } else {
            initialValues.put(KEY_SUPERTYPE, "");
        }
        if (types.length > 1) {
            /* Concatenate all strings after the first delimiter
             * in case there's a hyphen in the subtype
             */
            StringBuilder subtype = new StringBuilder();
            boolean first = true;
            for (int i = 1; i < types.length; i++) {
                if (!first) {
                    subtype.append(delimiter);
                }
                subtype.append(types[i]);
                first = false;
            }
            initialValues.put(KEY_SUBTYPE, subtype.toString());
        } else {
            initialValues.put(KEY_SUBTYPE, "");
        }
        initialValues.put(KEY_RARITY, (int) card.getRarity());
        initialValues.put(KEY_MANACOST, card.getManaCost());
        initialValues.put(KEY_CMC, card.getCmc());
        initialValues.put(KEY_POWER, card.getPower());
        initialValues.put(KEY_TOUGHNESS, card.getToughness());
        initialValues.put(KEY_LOYALTY, card.getLoyalty());
        initialValues.put(KEY_ABILITY, card.getText());
        initialValues.put(KEY_FLAVOR, card.getFlavor());
        initialValues.put(KEY_ARTIST, card.getArtist());
        initialValues.put(KEY_NUMBER, card.getNumber());
        initialValues.put(KEY_COLOR, card.getColor().replaceAll("[^WUBRGwubrg]", ""));
        initialValues.put(KEY_MULTIVERSEID, card.getMultiverseId());
        initialValues.put(KEY_COLOR_IDENTITY, card.getColorIdentity());
        initialValues.put(KEY_NAME_NO_ACCENT, removeAccentMarks(card.getName()));
        initialValues.put(KEY_WATERMARK, card.getWatermark());

        for (Card.ForeignPrinting fp : card.getForeignPrintings()) {
            switch (fp.getLanguageCode()) {
                case Language.Chinese_Traditional: {
                    initialValues.put(KEY_NAME_CHINESE_TRADITIONAL, fp.getName());
                    initialValues.put(KEY_MULTIVERSEID_CHINESE_TRADITIONAL, fp.getMultiverseId());
                    break;
                }
                case Language.Chinese_Simplified: {
                    initialValues.put(KEY_NAME_CHINESE_SIMPLIFIED, fp.getName());
                    initialValues.put(KEY_MULTIVERSEID_CHINESE_SIMPLIFIED, fp.getMultiverseId());
                    break;
                }
                case Language.French: {
                    initialValues.put(KEY_NAME_FRENCH, fp.getName());
                    initialValues.put(KEY_NAME_NO_ACCENT_FRENCH, removeAccentMarks(fp.getName()));
                    initialValues.put(KEY_MULTIVERSEID_FRENCH, fp.getMultiverseId());
                    break;
                }
                case Language.German: {
                    initialValues.put(KEY_NAME_GERMAN, fp.getName());
                    initialValues.put(KEY_NAME_NO_ACCENT_GERMAN, removeAccentMarks(fp.getName()));
                    initialValues.put(KEY_MULTIVERSEID_GERMAN, fp.getMultiverseId());
                    break;
                }
                case Language.Italian: {
                    initialValues.put(KEY_NAME_ITALIAN, fp.getName());
                    initialValues.put(KEY_NAME_NO_ACCENT_ITALIAN, removeAccentMarks(fp.getName()));
                    initialValues.put(KEY_MULTIVERSEID_ITALIAN, fp.getMultiverseId());
                    break;
                }
                case Language.Japanese: {
                    initialValues.put(KEY_NAME_JAPANESE, fp.getName());
                    initialValues.put(KEY_MULTIVERSEID_JAPANESE, fp.getMultiverseId());
                    break;
                }
                case Language.Portuguese_Brazil: {
                    initialValues.put(KEY_NAME_PORTUGUESE_BRAZIL, fp.getName());
                    initialValues.put(KEY_NAME_NO_ACCENT_PORTUGUESE_BRAZIL, removeAccentMarks(fp.getName()));
                    initialValues.put(KEY_MULTIVERSEID_PORTUGUESE_BRAZIL, fp.getMultiverseId());
                    break;
                }
                case Language.Russian: {
                    initialValues.put(KEY_NAME_RUSSIAN, fp.getName());
                    initialValues.put(KEY_MULTIVERSEID_RUSSIAN, fp.getMultiverseId());
                    break;
                }
                case Language.Spanish: {
                    initialValues.put(KEY_NAME_SPANISH, fp.getName());
                    initialValues.put(KEY_NAME_NO_ACCENT_SPANISH, removeAccentMarks(fp.getName()));
                    initialValues.put(KEY_MULTIVERSEID_SPANISH, fp.getMultiverseId());
                    break;
                }
                case Language.Korean: {
                    initialValues.put(KEY_NAME_KOREAN, fp.getName());
                    initialValues.put(KEY_MULTIVERSEID_KOREAN, fp.getMultiverseId());
                    break;
                }
            }
        }

        mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
    }

    /**
     * I messed up with Duel Deck Anthologies. Each deck should have had its own set code, rather
     * than grouping them all together. This function fixes any saved cards when loaded.
     * <p>
     * TODO online only pref
     *
     * @param name     The name of the card to get the correct set code for
     * @param setCode  The incorrect set code (i.e. DD3)
     * @param database A database to query
     * @return The correct set code (i.e. DD3EVG)
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getCorrectSetCode(String name, String setCode, SQLiteDatabase database)
            throws FamiliarDbException {

        Cursor cursor = null;
        try {
            String sql =
                    "SELECT " + KEY_SET +
                            " FROM " + DATABASE_TABLE_CARDS +
                            " WHERE (" + KEY_NAME + " = " + sanitizeString(name, false) +
                            " AND " + KEY_SET + " LIKE " + sanitizeString(setCode + "%", false) + ")";

            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            cursor = database.rawQuery(sql, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(KEY_SET));
            } else {
                return setCode;
            }
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    /**
     * Drop an expansion and all of its cards.
     *
     * @param setCode  The expansion to drop
     * @param database The database to drop from
     * @throws FamiliarDbException If something goes wrong
     */
    public static void dropSetAndCards(String setCode, SQLiteDatabase database)
            throws FamiliarDbException {

        try {
            database.delete(DATABASE_TABLE_CARDS, KEY_SET + " = " + sanitizeString(setCode, false), null);
            database.delete(DATABASE_TABLE_SETS, KEY_CODE + " = " + sanitizeString(setCode, false), null);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a Cursor pointed at a card, return the full type line (sub - super) for that card.
     * <p>
     * TODO online only pref
     *
     * @param cCardById The cursor pointing to a card
     * @return A String with the full type line
     */
    public static String getTypeLine(Cursor cCardById) {
        StringBuilder typeLine = new StringBuilder();
        String supertype = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SUPERTYPE));
        String subtype = cCardById.getString(cCardById.getColumnIndex(CardDbAdapter.KEY_SUBTYPE));

        typeLine.append(supertype);
        if (subtype.length() > 0) {
            typeLine.append(" - ");
            typeLine.append(subtype);
        }
        return typeLine.toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                           DATABASE_TABLE_BANNED_CARDS Functions                            //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add a card to the table of banned & restricted cards.
     *
     * @param card   The name of the card to add to the banned cards table
     * @param format The format the card is banned in
     * @param status LEGAL, BANNED, or RESTRICTED
     * @param mDb    The database to add a banned or restricted card to
     */
    public static void addLegalCard(String card, String format, int status, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, card);
        initialValues.put(KEY_LEGALITY, status);
        initialValues.put(KEY_FORMAT, format);
        mDb.insert(DATABASE_TABLE_BANNED_CARDS, null, initialValues);
    }

    /**
     * Given a format, return a cursor pointing to all the cards banned in that format.
     *
     * @param mDb    The database to query
     * @param format The format to return banned cards for
     * @return A Cursor pointing to all banned cards in a format
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getBannedCards(SQLiteDatabase mDb, String format) throws FamiliarDbException {
        try {
            String sql = "SELECT " +
                    KEY_LEGALITY + ", GROUP_CONCAT(" + KEY_NAME + ", '<br>') AS " + KEY_BANNED_LIST +
                    " FROM " + DATABASE_TABLE_BANNED_CARDS +
                    " WHERE " + KEY_FORMAT + " = '" + format + "'" +
                    " GROUP BY " + KEY_LEGALITY;
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a card and a format, check if that card is legal to play in that format.
     *
     * @param mCardName The card to check legality for
     * @param format    The format to check legality in
     * @param mDb       The database to query
     * @return LEGAL, BANNED, or RESTRICTED
     * @throws FamiliarDbException If something goes wrong
     */
    public static int checkLegality(String mCardName, String format, SQLiteDatabase mDb)
            throws FamiliarDbException {
        mCardName = sanitizeString(mCardName, false);
        format = sanitizeString(format, false);

        Cursor c = null;
        try {
            /* The new way (single query per type, should be much faster) - Alex */
            String sql = "SELECT COALESCE(CASE ";

            /* First coalesce logic, checks the card for a silver border */
            sql += "(SELECT 1 FROM " +
                    DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " +
                    DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
                    " WHERE " +
                    DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + mCardName +
                    " AND " +
                    DATABASE_TABLE_SETS + "." + KEY_BORDER_COLOR + " != " + "\"Silver\")";
            sql += " WHEN 1 THEN NULL ELSE 1 END,";

            /* Second coalesce logic, check card against legal sets */
            sql += "CASE (" +
                    " SELECT 1" +
                    " FROM " + DATABASE_TABLE_CARDS + " c INNER JOIN " + DATABASE_TABLE_LEGAL_SETS + " ls ON ls." + KEY_SET + " = c." + KEY_SET +
                    " WHERE ls." + KEY_FORMAT + " = " + format +
                    " AND c." + KEY_NAME + " = " + mCardName;
            sql += ")";
            sql += "  WHEN 1 THEN NULL ELSE CASE" +
                    " WHEN " + format + " = 'Legacy' THEN NULL" +
                    " WHEN " + format + " = 'Vintage' THEN NULL" +
                    " WHEN " + format + " = 'Commander' THEN NULL" +
                    " WHEN " + format + " = 'Pauper' THEN NULL" +
                    " WHEN " + format + " = 'Reserved List' THEN NULL" +
                    " ELSE 1";
            sql += " END END,";

            /* Third coalesce logic, check card against banned cards */
            sql += " (SELECT " + KEY_LEGALITY +
                    " FROM " + DATABASE_TABLE_BANNED_CARDS +
                    " WHERE " + KEY_NAME + " = " + mCardName +
                    " AND " + KEY_FORMAT + " = " + format + "),";

            if ("'Pauper'".equals(format)) {
                /* Fourth coalesce logic, check card's pauper legality*/
                sql += " CASE WHEN (" +
                        " SELECT " + KEY_SET +
                        " FROM " + DATABASE_TABLE_CARDS +
                        " WHERE " + KEY_NAME + " = " + mCardName +
                        " AND " + KEY_RARITY + " = " + ((int) 'C') +
                        " ) IS NULL THEN 1 ELSE NULL END,";
            }
            /* Finish the coalesce with a 0 */
            sql += "0) AS " + KEY_LEGALITY;

            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = mDb.rawQuery(sql, null);

            c.moveToFirst();
            return c.getInt(c.getColumnIndex(KEY_LEGALITY));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                               DATABASE_TABLE_SETS Functions                                //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add a MtgSet to DATABASE_TABLE_SETS.
     *
     * @param set The set to add to DATABASE_TABLE_SETS
     * @param mDb The database to add the set to
     */
    public static void createSet(Expansion set, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_CODE, set.mCode_gatherer);
        initialValues.put(KEY_NAME, set.mName_gatherer);
        initialValues.put(KEY_CODE_MTGI, set.mCode_mtgi);
        initialValues.put(KEY_DATE, set.mReleaseTimestamp);
        initialValues.put(KEY_DIGEST, set.mDigest);
        initialValues.put(KEY_CAN_BE_FOIL, set.mCanBeFoil);
        initialValues.put(KEY_NAME_TCGPLAYER, set.mName_tcgp);
        initialValues.put(KEY_ONLINE_ONLY, set.mIsOnlineOnly);
        initialValues.put(KEY_BORDER_COLOR, set.mBorderColor);
        initialValues.put(KEY_SET_TYPE, set.mType);

        mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
    }

    /**
     * Returns a cursor with all the information about all of the sets.
     *
     * @param sqLiteDatabase The database to query
     * @return a Cursor with all of the information about all of the sets
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchAllSets(SQLiteDatabase sqLiteDatabase) throws FamiliarDbException {

        try {
            String[] allSetDataKeys = new String[ALL_SET_DATA_KEYS.size()];
            ALL_SET_DATA_KEYS.toArray(allSetDataKeys);
            return sqLiteDatabase.query(DATABASE_TABLE_SETS, allSetDataKeys, null,
                    null, null, null, KEY_DATE + " DESC");
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException | NullPointerException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a standard set code, return the Magiccards.info set code.
     *
     * @param code The standard set code
     * @param mDb  The database to query
     * @return The Magiccards.info set code
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getCodeMtgi(String code, SQLiteDatabase mDb) throws FamiliarDbException {
        try (Cursor cursor = mDb.query(DATABASE_TABLE_SETS, new String[]{KEY_CODE_MTGI},
                KEY_CODE + "=\"" + code + "\"", null, null, null, null)) {
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndex(KEY_CODE_MTGI));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a set code, return the full set name.
     *
     * @param setCode  The set code to look up
     * @param database The database to query
     * @return The full set name
     * @throws FamiliarDbException If the database couldn't be accessed
     */
    public static String getSetNameFromCode(String setCode, SQLiteDatabase database)
            throws FamiliarDbException {

        String[] columns = new String[]{KEY_NAME};
        FamiliarLogger.logQuery(true, DATABASE_TABLE_SETS, columns, KEY_CODE
                + "=\"" + setCode + "\"", null, null, null, KEY_NAME, null, new Throwable().getStackTrace()[0].getMethodName());
        try (Cursor c = database.query(true, DATABASE_TABLE_SETS, columns, KEY_CODE
                + "=\"" + setCode + "\"", null, null, null, KEY_NAME, null)) {
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(KEY_NAME));
            } else return "";
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Returns whether or not a set is online only
     *
     * @param setCode  The set code to look up
     * @param database The database to query
     * @return true if the set is online only, false otherwise
     */
    public static boolean isOnlineOnly(String setCode, SQLiteDatabase database) throws FamiliarDbException {
        String[] columns = new String[]{KEY_ONLINE_ONLY};
        FamiliarLogger.logQuery(true, DATABASE_TABLE_SETS, columns, KEY_CODE
                        + "=\"" + setCode + "\"", null, null, null, null, null,
                new Throwable().getStackTrace()[0].getMethodName());
        try (Cursor c = database.query(true, DATABASE_TABLE_SETS, columns, KEY_CODE
                + "=\"" + setCode + "\"", null, null, null, null, null)) {

            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                return (1 == c.getInt(c.getColumnIndex(KEY_ONLINE_ONLY)));
            }
            return false;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a set code, return a String with the set name that TCGPlayer.com uses.
     *
     * @param setCode The set code to search for
     * @param mDb     The database to query
     * @return The TCGPlayer.com name string
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getTcgName(String setCode, SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c = null;
        try {
            String sql = "SELECT " + KEY_NAME_TCGPLAYER +
                    " FROM " + DATABASE_TABLE_SETS +
                    " WHERE " + KEY_CODE + " = " + sanitizeString(setCode, false) + ";";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = mDb.rawQuery(sql, null);
            c.moveToFirst();

            /* Some users had this cursor come up empty. I couldn't replicate. This is safe */
            if (c.getCount() == 0) {
                return "";
            }
            return c.getString(c.getColumnIndex(KEY_NAME_TCGPLAYER));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    /**
     * Helper function to determine what kind of multicard a card is based on set and number.
     * TODO add option for kamigawa flip? Determine type based on text search for supplemental sets?
     *
     * @param number  The card number
     * @param setCode The set the card is in
     * @return TRANSFORM, FUSE, SPLIT, or NOPE
     */
    public static MultiCardType isMultiCard(String number, String setCode) {
        if (number.contains("a") || number.contains("b")) {
            if (setCode.compareTo("ISD") == 0 ||
                    setCode.compareTo("DKA") == 0 ||
                    setCode.compareTo("SOI") == 0 ||
                    setCode.compareTo("EMN") == 0 ||
                    setCode.compareTo("ORI") == 0 ||
                    setCode.compareTo("XLN") == 0 ||
                    setCode.compareTo("RIX") == 0 ||
                    setCode.compareTo("V17") == 0) {
                return MultiCardType.TRANSFORM;
            } else if (setCode.compareTo("DGM") == 0) {
                return MultiCardType.FUSE;
            } else {
                return MultiCardType.SPLIT;
            }
        }
        return MultiCardType.NOPE;
    }


    /**
     * Return the text representation of numeric or non-numeric powers and toughnesses
     *
     * @param stat        The numeric representation of a power or toughness
     * @param displaySign True to display the sign, false otherwise
     * @return The string representation of the given stat
     */
    public static String getPrintedPTL(float stat, boolean displaySign) {
        if (stat == CardDbAdapter.STAR) {
            return "*";
        } else if (stat == CardDbAdapter.ONE_PLUS_STAR) {
            return "1+*";
        } else if (stat == CardDbAdapter.TWO_PLUS_STAR) {
            return "2+*";
        } else if (stat == CardDbAdapter.SEVEN_MINUS_STAR) {
            return "7-*";
        } else if (stat == CardDbAdapter.STAR_SQUARED) {
            return "*^2";
        } else if (stat == CardDbAdapter.X) {
            return "X";
        } else if (stat == CardDbAdapter.QUESTION_MARK) {
            return "?";
        } else if (stat == CardDbAdapter.INFINITY) {
            return "∞";
        } else if (stat == CardDbAdapter.NO_ONE_CARES) {
            return "";
        } else {
            if (stat == (int) stat) {
                if (displaySign) {
                    return String.format(Locale.US, "%+d", (int) stat);
                }
                return String.format(Locale.US, "%d", (int) stat);
            } else {
                if (displaySign) {
                    return String.format(Locale.US, "%+.1f", stat);
                }
                return String.format(Locale.US, "%.1f", stat);
            }
        }
    }

    /**
     * @param database The database to query with
     * @return A list of all the sets which do not have foils (or are only foil)
     * @throws FamiliarDbException If something goes terribly wrong
     */
    public static ArrayList<String> getNonFoilSets(SQLiteDatabase database) throws FamiliarDbException {
        Cursor c = null;
        ArrayList<String> nonFoilSets = new ArrayList<>();
        try {
            String sql = "SELECT " + KEY_CODE +
                    " FROM " + DATABASE_TABLE_SETS +
                    " WHERE " + KEY_CAN_BE_FOIL + " = 0;";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = database.rawQuery(sql, null);
            c.moveToFirst();

            /* Some users had this cursor come up empty. I couldn't replicate. This is safe */
            if (c.getCount() == 0) {
                return nonFoilSets;
            }

            c.moveToFirst();
            while (!c.isAfterLast()) {
                nonFoilSets.add(c.getString(c.getColumnIndex(KEY_CODE)));
                c.moveToNext();
            }

            return nonFoilSets;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                             DATABASE_TABLE_LEGAL_SETS Functions                            //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add a set and format pair to the table of legal sets.
     *
     * @param set    The set to add
     * @param format The format the set is legal in
     * @param mDb    The database to add legality data to
     */
    public static void addLegalSet(String set, String format, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SET, set);
        initialValues.put(KEY_FORMAT, format);
        mDb.insert(DATABASE_TABLE_LEGAL_SETS, null, initialValues);
    }

    /**
     * Helper function to determine if a set contains foil cards.
     *
     * @param setCode The set code
     * @param mDb     The database to query
     * @return true if the set has foils, false otherwise
     * @throws FamiliarDbException If something goes wrong
     */
    public static boolean canBeFoil(String setCode, SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c = null;
        try {
            String sql =
                    "SELECT " + KEY_CAN_BE_FOIL +
                            " FROM " + DATABASE_TABLE_SETS +
                            " WHERE " + KEY_CODE + " = \"" + setCode + "\"";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = mDb.rawQuery(sql, null);

            /* Check if the cursor returned any values first */
            if (c.getCount() == 0) {
                return false;
            }

            /* Then check if the set contains foils */
            c.moveToFirst();
            return 0 != c.getInt(c.getColumnIndex(KEY_CAN_BE_FOIL));
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    /**
     * Helper function to return all set codes which can have foil cards.
     *
     * @param mDb The database to query
     * @return A Set of set codes which can contain foil cards
     * @throws FamiliarDbException If something goes wrong
     */
    public static Set<String> getFoilSets(SQLiteDatabase mDb) throws FamiliarDbException {
        Cursor c = null;
        try {
            Set<String> foilSets = new HashSet<>();
            String sql =
                    "SELECT " + KEY_CODE +
                            " FROM " + DATABASE_TABLE_SETS +
                            " WHERE " + KEY_CAN_BE_FOIL + " = 1";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = mDb.rawQuery(sql, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                foilSets.add(c.getString(c.getColumnIndex(KEY_CODE)));
                c.moveToNext();
            }
            return foilSets;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    /**
     * Given a format, return a cursor pointing to all sets legal in that format.
     *
     * @param mDb    The database to query
     * @param format The format to return legal sets for
     * @return A Cursor pointing to all legal sets for the given format
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getLegalSets(SQLiteDatabase mDb, String format) throws FamiliarDbException {
        try {
            String sql = "SELECT GROUP_CONCAT" +
                    "(" + DATABASE_TABLE_SETS + "." + KEY_NAME + ", '<br>') AS " + KEY_LEGAL_SETS +
                    " FROM (" + DATABASE_TABLE_LEGAL_SETS + " JOIN " + DATABASE_TABLE_SETS +
                    " ON " + DATABASE_TABLE_LEGAL_SETS + "." + KEY_SET + " = " + DATABASE_TABLE_SETS + "." + KEY_CODE + ")" +
                    " WHERE " + DATABASE_TABLE_LEGAL_SETS + "." + KEY_FORMAT + " = '" + format + "'";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                              DATABASE_TABLE_FORMATS Functions                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * DATABASE_TABLE_FORMATS
     * <p>
     * Create a format in the database.
     *
     * @param name The name of the format to create
     * @param mDb  The database to create a format in
     */
    public static void createFormat(String name, SQLiteDatabase mDb) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        mDb.insert(DATABASE_TABLE_FORMATS, null, initialValues);
    }

    /**
     * Create all tables relating to card legality.
     *
     * @param mDb The database to create tables in
     * @throws FamiliarDbException If something goes wrong
     */
    public static void createLegalTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL(DATABASE_CREATE_FORMATS);
            mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
            mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Drop all tables relating to legality.
     *
     * @param mDb The database to drop tables from
     * @throws FamiliarDbException If something goes wrong
     */
    public static void dropLegalTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Fetch all formats that cards can be legal in.
     *
     * @param mDb The database to query
     * @return A cursor pointing to all formats
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor fetchAllFormats(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            return mDb.query(DATABASE_TABLE_FORMATS, new String[]{KEY_ID,
                    KEY_NAME,}, null, null, null, null, KEY_NAME);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                               DATABASE_TABLE_RULES Functions                               //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * TABLE DATABASE_CREATE_RULES
     * <p>
     * Drop the rules and glossary tables.
     *
     * @param mDb The database to drop tables from
     * @throws FamiliarDbException If something goes wrong
     */
    public static void dropRulesTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RULES);
            mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_GLOSSARY);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * TABLE DATABASE_CREATE_RULES
     * <p>
     * Create the rules and glossary tables.
     *
     * @param mDb The database to add tables to
     * @throws FamiliarDbException If something goes wrong
     */
    public static void createRulesTables(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            mDb.execSQL(DATABASE_CREATE_RULES);
            mDb.execSQL(DATABASE_CREATE_GLOSSARY);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a category and subcategory, return a Cursor pointing to all rules in that subcategory.
     *
     * @param category    The integer category, or -1 for the main categories
     * @param subcategory The integer subcategory, or -1 for no subcategory
     * @param mDb         The database to query
     * @return A Cursor pointing to all rules in that category & subcategory
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getRules(int category, int subcategory, SQLiteDatabase mDb)
            throws FamiliarDbException {
        try {
            if (category == -1) {
                /* No category specified; return the main categories */
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                        + " WHERE " + KEY_SUBCATEGORY + " = -1";
                FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                return mDb.rawQuery(sql, null);
            } else if (subcategory == -1) {
                /* No subcategory specified; return the subcategories under the given category */
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES +
                        " WHERE " + KEY_CATEGORY + " = " + category +
                        " AND " + KEY_SUBCATEGORY + " > -1" +
                        " AND " + KEY_ENTRY + " IS NULL";
                FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                return mDb.rawQuery(sql, null);
            } else {
                /* Both specified; return the rules under the given subcategory */
                String sql = "SELECT * FROM " + DATABASE_TABLE_RULES +
                        " WHERE " + KEY_CATEGORY + " = " + category +
                        " AND " + KEY_SUBCATEGORY + " = " + subcategory +
                        " AND " + KEY_ENTRY + " IS NOT NULL";
                FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                return mDb.rawQuery(sql, null);
            }
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a keyword, category, and subcategory, return a Cursor pointing to all rules which
     * match that keyword in that category & subcategory.
     *
     * @param keyword     A keyword to look for in the rule
     * @param category    The integer category, or -1 for the main categories
     * @param subcategory The integer subcategory, or -1 for no subcategory
     * @param mDb         The database to query
     * @return A Cursor pointing to all rules which match that keyword in that category &
     * subcategory
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getRulesByKeyword(String keyword, int category, int subcategory,
                                           SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            /* Don't let them pass in an empty string; it'll return ALL the rules */
            if (keyword != null && !keyword.trim().equals("")) {
                keyword = sanitizeString("%" + keyword + "%", false);

                if (category == -1) {
                    /* No category; we're searching from the main page, so no restrictions */
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL";
                    FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                    return mDb.rawQuery(sql, null);
                } else if (subcategory == -1) {
                    /* No subcategory; we're searching from a category page, so
                     * restrict within that */
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL"
                            + " AND " + KEY_CATEGORY + " = " + category;
                    FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                    return mDb.rawQuery(sql, null);
                } else {
                    /* We're searching within a subcategory, so restrict within
                     * that */
                    String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
                            + " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
                            + " AND " + KEY_ENTRY + " IS NOT NULL"
                            + " AND " + KEY_CATEGORY + " = " + category
                            + " AND " + KEY_SUBCATEGORY + " = " + subcategory;
                    FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                    return mDb.rawQuery(sql, null);
                }
            }
            return null;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Given a rule's category, subcategory, and entry, return that rule's position.
     *
     * @param category    The rule's category
     * @param subcategory The rule's subcategory
     * @param entry       The rule's entry
     * @param mDb         The database to query
     * @return The position of the rule, or 0 if not found
     * @throws FamiliarDbException If something goes wrong
     */
    public static int getRulePosition(int category, int subcategory, String entry, SQLiteDatabase mDb)
            throws FamiliarDbException {
        Cursor c = null;
        try {
            if (entry != null) {
                String sql = "SELECT " + KEY_POSITION +
                        " FROM " + DATABASE_TABLE_RULES +
                        " WHERE " + KEY_CATEGORY + " = " + category +
                        " AND " + KEY_SUBCATEGORY + " = " + subcategory +
                        " AND " + KEY_ENTRY + " = " + sanitizeString(entry, false);
                FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
                c = mDb.rawQuery(sql, null);
                if (c != null) {
                    c.moveToFirst();
                    return c.getInt(c.getColumnIndex(KEY_POSITION));
                }
            }
            return 0;
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    /**
     * Given a rule's category and subcategory, return the name of the category.
     *
     * @param category    The rule's category
     * @param subcategory The rule's subcategory
     * @param mDb         The database to query
     * @return A String with the rule's name, or ""
     * @throws FamiliarDbException If something goes wrong
     */
    public static String getCategoryName(int category, int subcategory, SQLiteDatabase mDb)
            throws FamiliarDbException {
        Cursor c = null;
        try {
            String sql = "SELECT " + KEY_RULE_TEXT +
                    " FROM " + DATABASE_TABLE_RULES +
                    " WHERE " + KEY_CATEGORY + " = " + category +
                    " AND " + KEY_SUBCATEGORY + " = " + subcategory +
                    " AND " + KEY_ENTRY + " IS NULL";
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            c = mDb.rawQuery(sql, null);
            if (c != null) {
                c.moveToFirst();
                return c.getString(c.getColumnIndex(KEY_RULE_TEXT));
            } else {
                return "";
            }
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        } finally {
            if (null != c) {
                c.close();
            }
        }
    }

    /**
     * Insert a rule's title & text into the database with it's category, subcategory, and position.
     *
     * @param category    The integer category
     * @param subcategory The integer subcategory
     * @param entry       The title of the rule
     * @param text        The text of the rule
     * @param position    The rule's position
     * @param mDb         The database to insert a rule into
     * @throws FamiliarDbException If something goes wrong
     */
    public static void insertRule(int category, int subcategory, String entry, String text,
                                  int position, SQLiteDatabase mDb) throws FamiliarDbException {
        if (entry == null) {
            entry = "NULL";
        } else {
            entry = sanitizeString(entry, false);
        }
        text = sanitizeString(text, false);
        String positionStr;
        if (position < 0) {
            positionStr = "NULL";
        } else {
            positionStr = String.valueOf(position);
        }
        String sql = "INSERT INTO " + DATABASE_TABLE_RULES + " ("
                + KEY_CATEGORY + ", " + KEY_SUBCATEGORY + ", " + KEY_ENTRY
                + ", " + KEY_RULE_TEXT + ", " + KEY_POSITION + ") VALUES ("
                + category + ", " + subcategory
                + ", " + entry + ", " + text + ", " + positionStr + ");";
        try {
            mDb.execSQL(sql);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                             DATABASE_TABLE_GLOSSARY Functions                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Insert a glossary term into DATABASE_TABLE_GLOSSARY.
     *
     * @param term       The term to insert
     * @param definition The definition for the glossary term
     * @param mDb        The database to add the term to
     * @throws FamiliarDbException If something goes wrong
     */
    public static void insertGlossaryTerm(String term, String definition, SQLiteDatabase mDb)
            throws FamiliarDbException {
        term = sanitizeString(term, false);
        definition = sanitizeString(definition, false);
        String sql = "INSERT INTO " + DATABASE_TABLE_GLOSSARY + " (" + KEY_TERM
                + ", " + KEY_DEFINITION + ") VALUES (" + term + ", "
                + definition + ");";
        try {
            mDb.execSQL(sql);
        } catch (SQLiteException | CursorIndexOutOfBoundsException e) {
            throw new FamiliarDbException(e);
        }
    }

    /**
     * Return a cursor to all glossary terms for the rules.
     *
     * @param mDb The database to query
     * @return A Cursor pointing to all glossary terms in the database
     * @throws FamiliarDbException If something goes wrong
     */
    public static Cursor getGlossaryTerms(SQLiteDatabase mDb) throws FamiliarDbException {
        try {
            String sql = "SELECT * FROM " + DATABASE_TABLE_GLOSSARY;
            FamiliarLogger.logRawQuery(sql, null, new Throwable().getStackTrace()[0].getMethodName());
            return mDb.rawQuery(sql, null);
        } catch (SQLiteException | CursorIndexOutOfBoundsException | IllegalStateException e) {
            throw new FamiliarDbException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                      Helper Functions                                      //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper function to sanitize a string for SQL queries, remove accent marks, and trim
     * whitespace.
     *
     * @param input A string to sanitize
     * @return The sanitized String
     */
    private static String sanitizeString(String input, boolean removeAccentMarks) {
        if (null == input) {
            return null;
        }
        input = input.trim();
        if (input.isEmpty()) {
            return input;
        }
        if ('[' == input.charAt(0)) {
            input = input.substring(1);
        }
        if (input.endsWith("]")) {
            input = input.substring(0, input.length() - 1);
        }
        if (removeAccentMarks) {
            return DatabaseUtils.sqlEscapeString(removeAccentMarks(input));
        }
        return DatabaseUtils.sqlEscapeString(input);
    }

    /**
     * Helper function to remove all non-ascii characters with accent marks from a String.
     *
     * @param str The String to remove accent marks from
     * @return The accent-less String
     */
    public static String removeAccentMarks(String str) {
        return StringUtils.stripAccents(str);

    }
}
