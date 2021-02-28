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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Encapsulate all information about a magic card
 */
public class MtgCard extends Card {
    private static final String DELIMITER = "%";

    /* Wish and trade list fields */
    String mSetName;
    public int mNumberOf;
    public int mPrice; /* In cents */
    public String mMessage;
    public boolean mIsCustomPrice; /* default is false as all cards should first grab internet prices. */
    public boolean mIsFoil;
    public int mSide;
    public MarketPriceInfo mPriceInfo;
    private int mIndex;
    private boolean mIsSelected;
    private boolean mIsSideboard;

    /**
     * Default constructor, doesn't leave null fields
     */
    public MtgCard() {
        /* Database fields */
        mName = "";
        mExpansion = "";
        mScryfallSetCode = "";
        mType = "";
        mRarity = '\0';
        mManaCost = "";
        mCmc = 0;
        mPower = CardDbAdapter.NO_ONE_CARES;
        mToughness = CardDbAdapter.NO_ONE_CARES;
        mLoyalty = CardDbAdapter.NO_ONE_CARES;
        mText = "";
        mFlavor = "";
        mArtist = "";
        mNumber = "";
        mColor = "";
        mMultiverseId = 0;
        mColorIdentity = "";
        mWatermark = "";
        mForeignPrintings = new ArrayList<>();

        // From MtgCard
        this.mSetName = "";
        this.mNumberOf = 0;
        this.mPrice = 0; /* In cents */
        this.mMessage = "";
        this.mIsCustomPrice = false; /* default is false as all cards should first grab internet prices. */
        this.mIsFoil = false;
        this.mSide = 0;
        this.mPriceInfo = null;
        this.mIndex = 0;
        this.mIsSelected = false;
        this.mIsSideboard = false;
    }

    MtgCard(MtgCard card) {
        if (card != null) {
            // From Card
            this.mName = card.mName;
            this.mExpansion = card.mExpansion;
            this.mScryfallSetCode = card.mScryfallSetCode;
            this.mType = card.mType;
            this.mRarity = card.mRarity;
            this.mManaCost = card.mManaCost;
            this.mCmc = card.mCmc;
            this.mPower = card.mPower;
            this.mToughness = card.mToughness;
            this.mLoyalty = card.mLoyalty;
            this.mText = card.mText;
            this.mFlavor = card.mFlavor;
            this.mArtist = card.mArtist;
            this.mNumber = card.mNumber;
            this.mColor = card.mColor;
            this.mMultiverseId = card.mMultiverseId;
            this.mColorIdentity = card.mColorIdentity;
            this.mWatermark = card.mWatermark;
            this.mForeignPrintings = new ArrayList<>(card.mForeignPrintings.size());
            for (ForeignPrinting fp : card.mForeignPrintings) {
                this.mForeignPrintings.add(new ForeignPrinting(fp));
            }

            // From MtgCard
            this.mSetName = card.mSetName;
            this.mNumberOf = card.mNumberOf;
            this.mPrice = card.mPrice; /* In cents */
            this.mMessage = card.mMessage;
            this.mIsCustomPrice = card.mIsCustomPrice; /* default is false as all cards should first grab internet prices. */
            this.mIsFoil = card.mIsFoil;
            this.mSide = card.mSide;
            this.mPriceInfo = new MarketPriceInfo(card.mPriceInfo);
            this.mIndex = card.mIndex;
            this.mIsSelected = card.mIsSelected;
            this.mIsSideboard = card.mIsSideboard;
        }
    }

    /**
     * Construct a MtgCard based on the given parameters.
     *
     * @param activity activity the method is being called from
     * @param cardName name of the card to make
     * @param cardSet  set code of the card to make
     * @param isFoil   if the card is foil or not
     * @param numberOf how many copies of the card are needed
     */
    public MtgCard(
            Activity activity,
            String cardName,
            String cardSet,
            boolean isFoil,
            int numberOf) throws InstantiationException {

        Cursor cardCursor = null;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, handle);
            /* Construct a blank MTGCard */
            this.mIsFoil = isFoil;
            this.mNumberOf = numberOf;
            /* Note the card price is loading */
            this.mMessage = activity.getString(R.string.wishlist_loading);
            /* Get extra information from the database */
            if (cardSet == null) {
                cardCursor = CardDbAdapter.fetchCardByName(cardName, CardDbAdapter.ALL_CARD_DATA_KEYS, true, true, true, database);

                /* Make sure at least one card was found */
                if (cardCursor.getCount() == 0) {
                    SnackbarWrapper.makeAndShowText(activity, R.string.toast_no_card,
                            SnackbarWrapper.LENGTH_LONG);
                    throw new InstantiationException();
                }
                /* If we don't specify the set, and we are trying to find a foil card, choose the
                 * latest foil printing. If there are no eligible printings, select the latest */
                if (isFoil) {
                    while (!CardDbAdapter.canBeFoil(
                            cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET))
                            , database)) {
                        if (cardCursor.isLast()) {
                            cardCursor.moveToFirst();
                            break;
                        }
                        cardCursor.moveToNext();
                    }
                }
            } else {
                cardCursor = CardDbAdapter.fetchCardByNameAndSet(cardName, cardSet, CardDbAdapter.ALL_CARD_DATA_KEYS, database);
            }

            /* Make sure at least one card was found */
            if (cardCursor.getCount() == 0) {
                SnackbarWrapper.makeAndShowText(activity, R.string.toast_no_card,
                        SnackbarWrapper.LENGTH_LONG);
                throw new InstantiationException();
            }

            initializeCardFromCursor(database, cardCursor);

            /* Override choice is the card can't be foil */
            if (!CardDbAdapter.canBeFoil(this.mExpansion, database)) {
                this.mIsFoil = false;
            }
        } catch (SQLiteException | FamiliarDbException | NumberFormatException | CursorIndexOutOfBoundsException fde) {
            throw new InstantiationException();
        } finally {
            if (null != cardCursor) {
                cardCursor.close();
            }
            DatabaseManager.closeDatabase(activity, handle);
        }
    }

    /**
     * Construct a MtgCard based on the given parameters.
     *
     * @param activity activity the method is being called from
     * @param cardName name of the card to make
     * @param cardSet  set code of the card to make
     * @param isFoil   if the card is foil or not
     * @param numberOf how many copies of the card are needed
     * @param isSideboard Whether this card is in the sideboard
     */
    public MtgCard(
            Activity activity,
            String cardName,
            String cardSet,
            boolean isFoil,
            int numberOf,
            boolean isSideboard) throws InstantiationException {
        this(activity, cardName, cardSet, isFoil, numberOf);
        this.mIsSideboard = isSideboard;
    }
    /**
     * Initialize all the database variables for this MtgCard from a Cursor
     *
     * @param database   The database the Cursor comes from
     * @param cardCursor A cursor pointing to a row of card information
     * @throws FamiliarDbException If there's a database error
     */
    private void initializeCardFromCursor(SQLiteDatabase database, Cursor cardCursor) throws FamiliarDbException {
        /* Don't rely on the user's given name, get it from the DB just to be sure */
        this.mName = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
        this.mExpansion = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_SET));
        this.mSetName = CardDbAdapter.getSetNameFromCode(this.mExpansion, database);
        this.mScryfallSetCode = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_SCRYFALL_SET_CODE));
        this.mNumber = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_NUMBER));
        this.mCmc = cardCursor.getInt((cardCursor
                .getColumnIndex(CardDbAdapter.KEY_CMC)));
        this.mColor = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_COLOR));
        this.mType = CardDbAdapter.getTypeLine(cardCursor);
        this.mRarity = (char) cardCursor.getInt(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_RARITY));
        this.mManaCost = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_MANACOST));
        this.mPower = cardCursor.getInt(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_POWER));
        this.mToughness = cardCursor.getInt(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
        this.mLoyalty = cardCursor.getInt(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_LOYALTY));
        this.mText = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_ABILITY));
        this.mFlavor = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_FLAVOR));
        this.mMultiverseId = cardCursor.getInt(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

        // This card doesn't have a multiverseID, try to find an equivalent
        if (mMultiverseId < 1) {
            this.mMultiverseId = CardDbAdapter.getEquivalentMultiverseId(this.mName, database);
        }

        this.mArtist = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_ARTIST));
        this.mWatermark = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_WATERMARK));
        this.mColorIdentity = cardCursor.getString(cardCursor
                .getColumnIndex(CardDbAdapter.KEY_COLOR_IDENTITY));

        this.mPrice = 0; /* In cents */
        this.mIsCustomPrice = false; /* default is false as all cards should first grab internet prices. */
        this.mSide = 0;
        this.mPriceInfo = null;
        this.mIndex = 0;
        this.mIsSelected = false;
        this.mIsSideboard = false;
    }

    /**
     * Construct a MtgCard based on the given parameters. initFromCursor() really should be called
     * for this MtgCard later
     *
     * @param cardName name of the card to make
     * @param cardSet  set code of the card to make
     * @param isFoil   if the card is foil or not
     * @param numberOf how many copies of the card are needed
     */
    public MtgCard(
            String cardName,
            String cardSet,
            boolean isFoil,
            int numberOf,
            boolean isSideboard) {

        // Fill in safe empty values
        this();

        // Then add the parameters
        this.mName = cardName;
        this.mExpansion = cardSet;
        this.mIsFoil = isFoil;
        this.mNumberOf = numberOf;
        this.mIsSideboard = isSideboard;
    }

    /**
     * Constructor to initialize a card from the data in a Cursor. Also sets default values for
     * variables.
     *
     * @param database   The database the Cursor comes from
     * @param cardCursor A cursor pointing to a row of card information
     */
    public MtgCard(SQLiteDatabase database, Cursor cardCursor) throws FamiliarDbException {
        // Fill in safe empty values
        this();

        // Initialize from the cursor
        initializeCardFromCursor(database, cardCursor);
    }

    /**
     * Given a list of cards with partial data, perform a single database operation to retrieve
     * the missing data, then add it to the cards in the list
     *
     * @param mCtx  A context to do database operations with
     * @param cards A list of cards to fill in data for
     */
    public static void initCardListFromDb(Context mCtx, List<MtgCard> cards) throws FamiliarDbException {
        // First make sure there are cards to load data for
        if (cards.isEmpty()) {
            return;
        }

        // Then load it if necessary
        Cursor cardCursor = null;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(mCtx, false, handle);

            // Get everything
            cardCursor = CardDbAdapter.fetchCardByNamesAndSets(cards, database);

            // For each line database result
            while (!cardCursor.isAfterLast()) {

                // Get the name and set from the database
                String name = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_NAME));
                String set = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_SET));

                // Match that to a card in the initial list
                for (MtgCard card : cards) {
                    if (card.getName().equals(name) && card.getExpansion().equals(set)) {
                        try {
                            // Fill in the initial list with data from the cursor
                            card.initFromCursor(mCtx, cardCursor, database);
                        } catch (java.lang.InstantiationException e) {
                            // Eat it
                        }
                        // Keep looping if the same card is repeated (main deck + sideboard)
                    }
                }
                cardCursor.moveToNext();
            }
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException fde) {
            throw new FamiliarDbException(fde);
        } finally {
            if (null != cardCursor) {
                cardCursor.close();
            }
            DatabaseManager.closeDatabase(mCtx, handle);
        }
    }

    /**
     * This is a pseudo-constructor used to fill in missing data from a Cursor.
     *
     * @param context    A Context to get strings with
     * @param cardCursor A cursor pointing to this card's information from the database
     * @throws InstantiationException If this card can't be initialized
     */
    private void initFromCursor(Context context, Cursor cardCursor, SQLiteDatabase database) throws InstantiationException, FamiliarDbException {

        try {
            /* Note the card price is loading */
            this.mMessage = context.getString(R.string.wishlist_loading);

            /* Don't rely on the user's given name, get it from the DB just to be sure */
            this.mName = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_NAME));
            this.mExpansion = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_SET));
            this.mScryfallSetCode = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_SCRYFALL_SET_CODE));
            this.mNumber = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_NUMBER));
            this.mCmc = cardCursor.getInt((cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_CMC)));
            this.mColor = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_COLOR));

            this.mType = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_SUPERTYPE));
            String subtype = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_SUBTYPE));
            if (subtype.length() > 0) {
                this.mType += " - " + subtype;
            }

            this.mRarity = (char) cardCursor.getInt(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_RARITY));
            this.mManaCost = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_MANACOST));
            this.mPower = cardCursor.getInt(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_POWER));
            this.mToughness = cardCursor.getInt(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_TOUGHNESS));
            this.mLoyalty = cardCursor.getInt(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_LOYALTY));
            this.mText = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_ABILITY));
            this.mFlavor = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_FLAVOR));
            this.mMultiverseId = cardCursor.getInt(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_MULTIVERSEID));

            // This card doesn't have a multiverseID, try to find an equivalent
            if (mMultiverseId < 1) {
                this.mMultiverseId = CardDbAdapter.getEquivalentMultiverseId(this.mName, database);
            }

            this.mArtist = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_ARTIST));
            this.mWatermark = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_WATERMARK));
            this.mColorIdentity = cardCursor.getString(cardCursor.getColumnIndex("c_" + CardDbAdapter.KEY_COLOR_IDENTITY));

            this.mSetName = cardCursor.getString(cardCursor.getColumnIndex("s_" + CardDbAdapter.KEY_NAME));

            // Don't mess with any of the other MtgCard specific fields that may have been loaded fron files, like mIsCustomPrice

            /* Override choice is the card can't be foil */
            int canBeFoil = cardCursor.getInt(cardCursor.getColumnIndex("s_" + CardDbAdapter.KEY_CAN_BE_FOIL));
            if (0 == canBeFoil) {
                this.mIsFoil = false;
            }
        } catch (SQLiteException | NumberFormatException fde) {
            throw new InstantiationException();
        }
    }

    /**
     * @return true if this card has a price, false otherwise
     */
    public boolean hasPrice() {
        return (this.mPriceInfo != null || (this.mIsCustomPrice && this.mPrice != 0));
    }

    /**
     * @return Returns the current price of this card in string form
     */
    public String getPriceString() {
        return String.format(Locale.US, "$%d.%02d", this.mPrice / 100, this.mPrice % 100);
    }

    //////////////////////////////////////////////////
    //             MtgCards and Trading             //
    //////////////////////////////////////////////////

    /**
     * Returns the string representation of this MtgCard for saving trades
     *
     * @param side Whether this is on the LEFT or RIGHT side of the trade
     * @return A String representing this card
     */
    public String toTradeString(int side) {
        return side + DELIMITER +
                this.mName + DELIMITER +
                this.mExpansion + DELIMITER +
                this.mNumberOf + DELIMITER +
                this.mIsCustomPrice + DELIMITER +
                this.mPrice + DELIMITER +
                this.mIsFoil + DELIMITER +
                this.mCmc + DELIMITER +
                this.mColor + '\n';
    }

    /**
     * Static method to construct a MtgCard from a trade list line
     *
     * @param line     A String representation of a MtgCard
     * @param activity The activity for database access
     * @return An initialized MtgCard
     */
    public static @Nullable
    MtgCard fromTradeString(String line, Activity activity) {

        try {
            /* Parse these parts out of the string */
            String[] parts = line.split(DELIMITER);

            /* Correct the mExpansion code for Duel Deck Anthologies */
            if (parts[2].equals("DD3")) {
                FamiliarDbHandle handle = new FamiliarDbHandle();
                try {
                    SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, handle);
                    parts[2] = CardDbAdapter.getCorrectSetCode(parts[1], parts[2], database);
                } catch (SQLiteException | FamiliarDbException e) {
                    /* Oops, Expansion may be wrong */
                } finally {
                    DatabaseManager.closeDatabase(activity, handle);
                }
            }

            MtgCard card = new MtgCard(parts[1], parts[2], false, Integer.parseInt(parts[3]), false);
            card.mSide = Integer.parseInt(parts[0]);

            /* These parts may not exist */
            card.mIsCustomPrice = parts.length > 4 && Boolean.parseBoolean(parts[4]);
            if (parts.length > 5) {
                card.mPrice = Integer.parseInt(parts[5]);
            } else {
                card.mPrice = 0;
            }
            card.mIsFoil = parts.length > 6 && Boolean.parseBoolean(parts[6]);

            return card;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Build a string to share this card within a trade, in plaintext
     *
     * @param sb      The StringBuilder to append this string to
     * @param foilStr The localized string for "foil," since there is no context
     * @return The total price of this card object, in cents
     */
    public int toTradeShareString(StringBuilder sb, String foilStr) {
        int totalPrice = 0;
        sb.append(this.mNumberOf);
        sb.append(" ");
        sb.append(this.mName);
        sb.append(" [");
        sb.append(this.mSetName);
        sb.append("] ");
        if (this.mIsFoil) {
            sb.append("(");
            sb.append(foilStr);
            sb.append(") ");
        }
        if (this.hasPrice()) {
            sb.append(String.format(Locale.US, "$%d.%02d", this.mPrice / 100, this.mPrice % 100));
            totalPrice = (this.mPrice * this.mNumberOf);
        }

        sb.append("\n");
        return totalPrice;
    }

    //////////////////////////////////////////////////
    //             MtgCards and Wishlist            //
    //////////////////////////////////////////////////

    /**
     * Returns the string representation of this MtgCard for saving wishlists
     *
     * @return A String representing this card
     */
    public String toWishlistString() {
        return this.mName + DELIMITER +
                this.mExpansion + DELIMITER +
                this.mNumberOf + DELIMITER +
                this.mNumber + DELIMITER +
                ((int) this.mRarity) + DELIMITER +
                this.mIsFoil + '\n';
    }


    /**
     * Static method to construct MtgCard info from the result of a toWishlistString()
     *
     * @param line     Information about this card, in the form of what toWishlistString() prints
     * @param activity A context used for getting localized strings
     */
    public static @Nullable
    MtgCard fromWishlistString(String line, boolean isSideboard, Activity activity) {
        try {
            String[] parts = line.split(MtgCard.DELIMITER);

            /* Correct the mExpansion code for Duel Deck Anthologies */
            if (parts[1].equals("DD3")) {
                FamiliarDbHandle handle = new FamiliarDbHandle();
                try {
                    SQLiteDatabase database = DatabaseManager.openDatabase(activity, false, handle);
                    parts[1] = CardDbAdapter.getCorrectSetCode(parts[0], parts[1], database);
                } catch (SQLiteException | FamiliarDbException e) {
                    /* Eat it and use the old mExpansion code. */
                } finally {
                    DatabaseManager.closeDatabase(activity, handle);
                }
            }
            /* "foil" didn't exist in earlier versions, so it may not be part of the string */
            boolean foil = false;
            if (parts.length > 5) {
                foil = Boolean.parseBoolean(parts[5]);
            }

            return new MtgCard(parts[0], parts[1], foil, Integer.parseInt(parts[2]), isSideboard);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Check to see if two MtgCard objects are equivalent, or if this is equivalent to a CompressedWishlistInfo
     * object. The comparison is done on the MtgCard's mName
     *
     * @param o The object to compare to this one
     * @return true if the specified object is equal to this string, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof MtgCard && this.mName.equals(((MtgCard) o).mName) && this.mExpansion.equals(((MtgCard) o).mExpansion);
    }

    @Override
    public int hashCode() {
        // xor the name and expansion hash codes, used for equals()
        return this.mName.hashCode() ^ this.mExpansion.hashCode();
    }

    /**
     * Append the card's oracle text to the StringBuilder.
     * This is used when sharing a wishlist
     *
     * @param sb The StringBuilder to append data to
     */
    public void appendCardText(StringBuilder sb) {
        if (!mManaCost.isEmpty()) {
            sb.append(mManaCost);
            sb.append("\r\n");
        }
        if (!mType.isEmpty()) {
            sb.append(mType);
            sb.append("\r\n");
        }
        if (!mText.isEmpty()) {
            sb.append(mText.replace("<br>", "\r\n"));
            sb.append("\r\n");
        }

        if (mLoyalty != CardDbAdapter.NO_ONE_CARES) {
            sb.append(mLoyalty);
            sb.append("\r\n");
        } else if (mPower != CardDbAdapter.NO_ONE_CARES && mToughness != CardDbAdapter.NO_ONE_CARES) {
            if (mPower == (int) mPower) {
                sb.append((int) mPower);
            } else {
                sb.append(mPower);
            }
            sb.append("/");
            if (mToughness == (int) mToughness) {
                sb.append((int) mToughness);
            } else {
                sb.append(mToughness);
            }
            sb.append("\r\n");
        }
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        this.mIsSelected = selected;
    }

    /**
     * Gets an image URL for this card. Multiple image sources are used, so there may be multiple
     * attempts. Return the URL based on the attempt number
     *
     * @param attempt      The attempt number, should increment by one for each attempt
     * @param cardLanguage The language to get the image for
     * @return The URL for this attempt
     */
    public String getImageUrlString(int attempt, String cardLanguage) {
        // Try getting a URL
        try {
            // Try scryfall first
            URL url;
            if (null == (url = getScryfallImageUrl(cardLanguage, attempt))) {
                // If scryfall returns null, try gatherer
                url = getGathererImageUrl();
            }
            return url.toString();
        } catch (MalformedURLException | NullPointerException e) {
            // Return null, indicating a bad URL
            return null;
        }
    }

    /**
     * Easily gets the URL for the Scryfall image for a card by multiverseid.
     *
     * @param lang    The requested language for this card
     * @param attempt
     * @return URL of the card image
     * @throws MalformedURLException If we screw up building the URL
     */
    private URL getScryfallImageUrl(String lang, int attempt) throws MalformedURLException {
        // Only do one attempt for scryfall
        if (attempt > 0) {
            return null;
        }

        // Strip the last part of the number, if it is a letter
        String numberNoLetter = this.mNumber;
        if (Character.isAlphabetic(numberNoLetter.charAt(numberNoLetter.length() - 1))) {
            numberNoLetter = numberNoLetter.substring(0, numberNoLetter.length() - 1);
        }

        String code = this.mScryfallSetCode.toLowerCase();

        String urlStr = "https://api.scryfall.com/cards/" + code + "/" + numberNoLetter + "?format=image&version=normal&lang=" + lang;
        if (this.mNumber.endsWith("b")) {
            urlStr += "&face=back";
        }
        return new URL(urlStr);
    }

    /**
     * Easily gets the URL for the Gatherer image for a card by multiverse ID
     *
     * @return URL of the card image
     * @throws MalformedURLException If we screw up building the URL
     */
    private URL getGathererImageUrl() throws MalformedURLException {
        // Gatherer doesn't use HTTPS as of 1/6/2019
        return new URL("https://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=" + mMultiverseId + "&type=card");
    }

    public String getSetName() {
        return mSetName;
    }

    public boolean isSideboard() {
        return mIsSideboard;
    }
}