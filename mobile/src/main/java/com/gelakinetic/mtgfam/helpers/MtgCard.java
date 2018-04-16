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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Encapsulate all information about a magic card
 */
public class MtgCard extends Card {
    private static final String DELIMITER = "%";

    /* Wish and trade list fields */
    public String mSetName;
    public String mSetNameMtgi;
    public int mNumberOf;
    public int mPrice; /* In cents */
    public String mMessage;
    public boolean mIsCustomPrice = false; /* default is false as all cards should first grab internet prices. */
    public boolean mIsFoil = false;
    public int mSide;
    public MarketPriceInfo mPriceInfo;
    private int mIndex;
    private boolean mIsSelected = false;

    /**
     * Default constructor, doesn't leave null fields
     */
    public MtgCard() {
        /* Database fields */
        mName = "";
        mExpansion = "";
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
    }

    public MtgCard(Card card) {
        if (card != null) {
            this.mName = card.mName;
            this.mExpansion = card.mExpansion;
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
            this.mForeignPrintings = new ArrayList<>(card.mForeignPrintings);
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
     * @param line    A String representation of a MtgCard
     * @param context The context for database access
     * @return An initialized MtgCard
     */
    public static MtgCard fromTradeString(String line, Context context) {

        SQLiteDatabase database = null;
        try {
            database = DatabaseManager.getInstance(context, false).openDatabase(false);
        } catch (FamiliarDbException e) {
            /* Carry on without the database */
        }

        MtgCard card = new MtgCard();
        String[] parts = line.split(DELIMITER);

        /* Parse these parts out of the string */
        card.mSide = Integer.parseInt(parts[0]);
        card.mName = parts[1];
        card.mExpansion = parts[2];

        /* Correct the mExpansion code for Duel Deck Anthologies */
        if (card.mExpansion.equals("DD3")) {
            try {
                card.mExpansion = CardDbAdapter.getCorrectSetCode(card.mName, card.mExpansion, database);
            } catch (FamiliarDbException | NullPointerException e) {
                /* Eat it and use the old mExpansion code. */
            }
        }
        card.mNumberOf = Integer.parseInt(parts[3]);

        /* These parts may not exist */
        card.mIsCustomPrice = parts.length > 4 && Boolean.parseBoolean(parts[4]);
        if (parts.length > 5) {
            card.mPrice = Integer.parseInt(parts[5]);
        } else {
            card.mPrice = 0;
        }
        card.mIsFoil = parts.length > 6 && Boolean.parseBoolean(parts[6]);

        if (parts.length > 7) {
            card.mCmc = Integer.parseInt(parts[7]);
            card.mColor = parts[8];
        } else {
            /* Pull from db */
            try {
                Cursor cardCursor = CardDbAdapter.fetchCardByName(card.mName, Arrays.asList(
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_CMC,
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_COLOR), true, false, database);
                card.mCmc = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC));
                card.mColor = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
            } catch (FamiliarDbException | NullPointerException e) {
                card.mCmc = 0;
                card.mColor = "";
            }
        }

        /* Defaults regardless */
        try {
            card.mSetName = CardDbAdapter.getSetNameFromCode(card.mExpansion, database);
        } catch (FamiliarDbException | NullPointerException e) {
            card.mSetName = null;
        }
        DatabaseManager.getInstance(context, false).closeDatabase(false);
        card.mMessage = context.getString(R.string.wishlist_loading);
        return card;
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
     * @param line Information about this card, in the form of what toWishlistString() prints
     * @param mCtx A context used for getting localized strings
     */
    public static MtgCard fromWishlistString(String line, Context mCtx) {

        MtgCard newCard = new MtgCard();
        String[] parts = line.split(MtgCard.DELIMITER);

        newCard.mName = parts[0];
        newCard.mExpansion = parts[1];

        /* Correct the mExpansion code for Duel Deck Anthologies */
        if (newCard.mExpansion.equals("DD3")) {
            try {
                SQLiteDatabase database = DatabaseManager.getInstance(mCtx, false).openDatabase(false);
                newCard.mExpansion = CardDbAdapter.getCorrectSetCode(newCard.mName, newCard.mExpansion, database);
            } catch (FamiliarDbException e) {
                /* Eat it and use the old mExpansion code. */
            }
            DatabaseManager.getInstance(mCtx, false).closeDatabase(false);
        }
        newCard.mNumberOf = Integer.parseInt(parts[2]);

        /* "foil" didn't exist in earlier versions, so it may not be part of the string */
        if (parts.length > 3) {
            newCard.mNumber = parts[3];
            /* This is to fix a bug where the number was saved as the name. Clear it so it gets
             * fixed later
             */
            if (newCard.mNumber.equals(newCard.mName)) {
                newCard.mNumber = "";
            }
        }
        if (parts.length > 4) {
            newCard.mRarity = (char) Integer.parseInt(parts[4]);
        }
        boolean foil = false;
        if (parts.length > 5) {
            foil = Boolean.parseBoolean(parts[5]);
        }
        newCard.mIsFoil = foil;
        newCard.mMessage = mCtx.getString(R.string.wishlist_loading);

        return newCard;
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
        int hash = 23;
        hash = hash * 31 + mName.hashCode();
        return hash;
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
     * @param ctx          A context to use
     * @return The URL for this attempt
     */
    public String getImageUrlString(int attempt, String cardLanguage, Context ctx) {

        // Some trickery to figure out if we have a token
        boolean isToken = false;
        if (mType.contains("Token") || // try to take the easy way out
                (mCmc == 0 && // Tokens have a CMC of 0
                        // The only tokens in Gatherer are from Duel Decks
                        mSetName.contains("Duel Decks") &&
                        // The only tokens in Gatherer are creatures
                        mType.contains("Creature"))) {
            isToken = true;
        }

        // If the card is english or a token, skip over the foreign MTGI attempt
        if (cardLanguage.equalsIgnoreCase("en") || isToken) {
            attempt++;
        }

        // If the card is a token, skip over the other MTGI attempt too
        if (isToken && attempt >= 2) {
            attempt++;
        }

        // Try getting a URL
        try {
            switch (attempt) {
                case 0: {
                    // Try magiccards.info, foreign
                    return getMtgiPicUrl(cardLanguage, ctx).toString();
                }
                case 1: {
                    // Try scryfall
                    return getScryfallImageUrl().toString();
                }
                case 2: {
                    // Try magiccards.info, but english this time
                    return getMtgiPicUrl("en", ctx).toString();
                }
                case 3: {
                    // try gatherer
                    return getGathererImageUrl().toString();
                }
                default: {
                    // Return null, indicating we're out of attempts
                    return null;
                }
            }
        } catch (MalformedURLException | NullPointerException e) {
            // Return null, indicating a bad URL
            return null;
        }
    }

    /**
     * Jumps through hoops and returns a correctly formatted URL for magiccards.info's image.
     *
     * @param cardLanguage The language of the card
     * @param ctx          A context to get strings with
     * @return a URL to the card's image
     * @throws MalformedURLException If we screw up building the URL
     */
    private URL getMtgiPicUrl(String cardLanguage, Context ctx) throws MalformedURLException {

        final String mtgiExtras = "http://magiccards.info/extras/";
        String picURL;
        if (mType.toLowerCase().contains(ctx.getString(R.string.search_Ongoing).toLowerCase()) ||
                /* extra space to not confuse with planeswalker */
                mType.toLowerCase().contains(ctx.getString(R.string.search_Plane).toLowerCase() + " ") ||
                mType.toLowerCase().contains(ctx.getString(R.string.search_Phenomenon).toLowerCase()) ||
                mType.toLowerCase().contains(ctx.getString(R.string.search_Scheme).toLowerCase())) {
            switch (mExpansion) {
                case "PC2":
                    picURL = mtgiExtras + "plane/planechase-2012-edition/" + mName + ".jpg";
                    picURL = picURL.replace(" ", "-")
                            .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                    break;
                case "PCH":
                    String cardNameTmp = mName;
                    if (cardNameTmp.equalsIgnoreCase("tazeem")) {
                        cardNameTmp = "tazeem-release-promo";
                    } else if (cardNameTmp.equalsIgnoreCase("celestine reef")) {
                        cardNameTmp = "celestine-reef-pre-release-promo";
                    } else if (cardNameTmp.equalsIgnoreCase("horizon boughs")) {
                        cardNameTmp = "horizon-boughs-gateway-promo";
                    }
                    picURL = mtgiExtras + "plane/planechase/" + cardNameTmp + ".jpg";
                    picURL = picURL.replace(" ", "-")
                            .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                    break;
                case "ARC":
                    picURL = mtgiExtras + "scheme/archenemy/" + mName + ".jpg";
                    picURL = picURL.replace(" ", "-")
                            .replace("?", "").replace(",", "").replace("'", "").replace("!", "");
                    break;
                default:
                    picURL = "http://magiccards.info/scans/" + cardLanguage + "/" + mSetNameMtgi + "/" +
                            mNumber + ".jpg";
                    break;
            }
        } else {
            picURL = "http://magiccards.info/scans/" + cardLanguage + "/" + mSetNameMtgi + "/" + mNumber + ".jpg";
        }
        return new URL(picURL.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Easily gets the URL for the Scryfall image for a card by multiverseid.
     *
     * @return URL of the card image
     * @throws MalformedURLException If we screw up building the URL
     */
    private URL getScryfallImageUrl() throws MalformedURLException {
        return new URL("https://api.scryfall.com/cards/multiverse/" + mMultiverseId + "?format=image&version=normal");
    }

    /**
     * Easily gets the URL for the Gatherer image for a card by multiverse ID
     *
     * @return URL of the card image
     * @throws MalformedURLException If we screw up building the URL
     */
    private URL getGathererImageUrl() throws MalformedURLException {
        return new URL("http://gatherer.wizards.com/Handlers/Image.ashx?multiverseid=" + mMultiverseId + "&type=card");
    }
}