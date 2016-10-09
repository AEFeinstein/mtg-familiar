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

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulate all information about a magic card
 */
public class MtgCard {
    private static final String DELIMITER = "%";
    public String name;
    public String set;
    public String type;
    public char rarity;
    public String manaCost;
    public int cmc;
    public float power;
    public float toughness;
    public int loyalty;
    public String ability;
    public String flavor;
    public String artist;
    public String number;
    public String color;
    public int multiverseId;
    public String colorIdentity;
    /* Wish and trade list fields */
    public String setName;
    public String setCode;
    public int numberOf;
    public int price; /* In cents */
    public String message;
    public boolean customPrice = false; /* default is false as all cards should first grab internet prices. */
    public boolean foil = false;
    public int mSide;
    public PriceInfo priceInfo;

    /**
     * Default constructor, doesn't leave null fields
     */
    public MtgCard() {
        /* Database fields */
        name = "";
        set = "";
        type = "";
        rarity = '\0';
        manaCost = "";
        cmc = 0;
        power = CardDbAdapter.NO_ONE_CARES;
        toughness = CardDbAdapter.NO_ONE_CARES;
        loyalty = CardDbAdapter.NO_ONE_CARES;
        ability = "";
        flavor = "";
        artist = "";
        number = "";
        color = "";
        multiverseId = 0;
        colorIdentity = "";
    }

    /**
     * @return true if this card has a price, false otherwise
     */
    public boolean hasPrice() {
        return (this.priceInfo != null || (this.customPrice && this.price != 0));
    }

    /**
     * @return Returns the current price of this card in string form
     */
    public String getPriceString() {
        return String.format(Locale.US, "$%d.%02d", this.price / 100, this.price % 100);
    }

    /************************************************
     *             MtgCards and Trading             *
     ************************************************/

    /**
     * Constructor used when creating a new card for the trade list. Only populates relevant information
     *
     * @param name     This card's name
     * @param tcgName  The tcgplayer.com set name
     * @param setCode  The set code
     * @param numberOf The number of these cards being traded
     * @param message  A message associated with this card
     * @param number   This card's number within the set
     * @param foil     Whether or not this card is foil
     * @param color    This card's color
     * @param cmc      This card's converted mana cost
     */
    public MtgCard(String name, String tcgName, String setCode, int numberOf, String message, String number, boolean foil, String color, int cmc) {
        this.name = name;
        this.number = number;
        this.setCode = setCode;
        this.setName = tcgName;
        this.numberOf = numberOf;
        this.price = 0;
        this.message = message;
        this.rarity = (char) (int) '-';
        this.customPrice = false;
        this.foil = foil;
        this.color = color;
        this.cmc = cmc;
    }

    /**
     * Returns the string representation of this MtgCard for saving trades
     *
     * @param side Whether this is on the LEFT or RIGHT side of the trade
     * @return A String representing this card
     */
    public String toTradeString(int side) {
        return side + DELIMITER +
                this.name + DELIMITER +
                this.setCode + DELIMITER +
                this.numberOf + DELIMITER +
                this.customPrice + DELIMITER +
                this.price + DELIMITER +
                this.foil + DELIMITER +
                this.cmc + DELIMITER +
                this.color + '\n';
    }

    /**
     * Static method to construct a MtgCard from a trade list line
     *
     * @param line    A String representation of a MtgCard
     * @param context The context for database access
     * @return An initialized MtgCard
     */
    public static MtgCard fromTradeString(String line, Context context) {

        SQLiteDatabase database = DatabaseManager.getInstance(context, false).openDatabase(false);

        MtgCard card = new MtgCard();
        String[] parts = line.split(DELIMITER);

        /* Parse these parts out of the string */
        card.mSide = Integer.parseInt(parts[0]);
        card.name = parts[1];
        card.setCode = parts[2];

        /* Correct the set code for Duel Deck Anthologies */
        if (card.setCode.equals("DD3")) {
            try {
                card.setCode = CardDbAdapter.getCorrectSetCode(card.name, card.setCode, database);
            } catch (FamiliarDbException e) {
                /* Eat it and use the old set code. */
            }
        }
        card.numberOf = Integer.parseInt(parts[3]);

        /* These parts may not exist */
        card.customPrice = parts.length > 4 && Boolean.parseBoolean(parts[4]);
        if (parts.length > 5) {
            card.price = Integer.parseInt(parts[5]);
        } else {
            card.price = 0;
        }
        card.foil = parts.length > 6 && Boolean.parseBoolean(parts[6]);

        if (parts.length > 7) {
            card.cmc = Integer.parseInt(parts[7]);
            card.color = parts[8];
        } else {
            /* Pull from db */
            try {
                Cursor cardCursor = CardDbAdapter.fetchCardByName(card.name, new String[]{
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_CMC,
                        CardDbAdapter.DATABASE_TABLE_CARDS + "." + CardDbAdapter.KEY_COLOR}, true, database);
                card.cmc = cardCursor.getInt(cardCursor.getColumnIndex(CardDbAdapter.KEY_CMC));
                card.color = cardCursor.getString(cardCursor.getColumnIndex(CardDbAdapter.KEY_COLOR));
            } catch (FamiliarDbException e) {
                card.cmc = 0;
                card.color = "";
            }
        }

        /* Defaults regardless */
        try {
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
        } catch (FamiliarDbException e) {
            card.setName = null;
        }
        DatabaseManager.getInstance(context, false).closeDatabase(false);
        card.message = "loading";
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
        sb.append(this.numberOf);
        sb.append(" ");
        sb.append(this.name);
        sb.append(" [");
        sb.append(this.setName);
        sb.append("] ");
        if (this.foil) {
            sb.append("(");
            sb.append(foilStr);
            sb.append(") ");
        }
        if (this.hasPrice()) {
            sb.append(String.format(Locale.US, "$%d.%02d", this.price / 100, this.price % 100));
            totalPrice = (this.price * this.numberOf);
        }

        sb.append("\n");
        return totalPrice;
    }

    /************************************************
     *             MtgCards and Wishlist            *
     ************************************************/

    /**
     * Returns the string representation of this MtgCard for saving wishlists
     *
     * @return A String representing this card
     */
    public String toWishlistString() {
        return this.name + DELIMITER + this.setCode + DELIMITER + this.numberOf + DELIMITER + this.number + DELIMITER +
                ((int) this.rarity) + DELIMITER + this.foil + '\n';
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

        newCard.name = parts[0];
        newCard.setCode = parts[1];

        /* Correct the set code for Duel Deck Anthologies */
        if (newCard.setCode.equals("DD3")) {
            SQLiteDatabase database = DatabaseManager.getInstance(mCtx, false).openDatabase(false);
            try {
                newCard.setCode = CardDbAdapter.getCorrectSetCode(newCard.name, newCard.setCode, database);
            } catch (FamiliarDbException e) {
                /* Eat it and use the old set code. */
            }
            DatabaseManager.getInstance(mCtx, false).closeDatabase(false);
        }
        newCard.numberOf = Integer.parseInt(parts[2]);

        /* "foil" didn't exist in earlier versions, so it may not be part of the string */
        if (parts.length > 3) {
            newCard.number = parts[3];
        }
        if (parts.length > 4) {
            newCard.rarity = (char) Integer.parseInt(parts[4]);
        }
        boolean foil = false;
        if (parts.length > 5) {
            foil = Boolean.parseBoolean(parts[5]);
        }
        newCard.foil = foil;
        newCard.message = mCtx.getString(R.string.wishlist_loading);

        return newCard;
    }

    /**
     * Check to see if two MtgCard objects are equivalent, or if this is equivalent to a CompressedWishlistInfo
     * object. The comparison is done on the MtgCard's name
     *
     * @param o The object to compare to this one
     * @return true if the specified object is equal to this string, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof MtgCard) {
            return this.name.equals(((MtgCard) o).name);
        } else if (o instanceof CompressedWishlistInfo) {
            return this.name.equals(((CompressedWishlistInfo) o).mCard.name);
        }
        return false;
    }

    /**
     * Append the card's oracle text to the StringBuilder.
     * This is used when sharing a wishlist
     *
     * @param sb The StringBuilder to append data to
     */
    public void appendCardText(StringBuilder sb) {
        if (!manaCost.isEmpty()) {
            sb.append(manaCost);
            sb.append("\r\n");
        }
        if (!type.isEmpty()) {
            sb.append(type);
            sb.append("\r\n");
        }
        if (!ability.isEmpty()) {
            sb.append(ability.replace("<br>", "\r\n"));
            sb.append("\r\n");
        }

        if (loyalty != CardDbAdapter.NO_ONE_CARES) {
            sb.append(loyalty);
            sb.append("\r\n");
        } else if (power != CardDbAdapter.NO_ONE_CARES && toughness != CardDbAdapter.NO_ONE_CARES) {
            if (power == (int) power) {
                sb.append((int) power);
            } else {
                sb.append(power);
            }
            sb.append("/");
            if (toughness == (int) toughness) {
                sb.append((int) toughness);
            } else {
                sb.append(toughness);
            }
            sb.append("\r\n");
        }
    }

    /************************************************
     *          MtgCards and Color Identity         *
     ************************************************/

    /**
     * Calculates the color identity for this card, not counting any parts of a
     * multicard
     *
     * @param card The card to find a color identity for, excluding multicard
     * @return A color identity string for the given card consisting of "WUBRG"
     */
    private static String getColorIdentity(MtgCard card) {
        boolean colors[] = {false, false, false, false, false};
        String colorLetters[] = {"W", "U", "B", "R", "G"};
        String basicLandTypes[] = {"Plains", "Island", "Swamp", "Mountain",
                "Forest"};

		/* Search for colors in the cost & color */
        for (int i = 0; i < colors.length; i++) {
            if (card.color.contains(colorLetters[i])) {
                colors[i] = true;
            }
            if (card.manaCost.contains(colorLetters[i])) {
                colors[i] = true;
            }
        }

		/* Remove reminder text */
        String noReminderText = card.ability.replaceAll("\\([^\\(\\)]+\\)", "");
        /* Find mana symbols in the rest of the text */
        Pattern manaPattern = Pattern.compile("\\{[^\\{\\}]+\\}");
        Matcher m = manaPattern.matcher(noReminderText);
        while (m.find()) {
            /* Search for colors in the mana symbols in the non-reminder text */
            for (int i = 0; i < colors.length; i++) {
                if (m.group(0).contains(colorLetters[i])) {
                    colors[i] = true;
                }
            }
        }

		/* For typed lands, add color identity */
        if (card.type.toLowerCase().contains("land")) {
            for (int i = 0; i < colors.length; i++) {
                if (card.type.contains(basicLandTypes[i])) {
                    colors[i] = true;
                }
            }
        }

		/* Write the color identity */
        String colorIdentity = "";
        for (int i = 0; i < colors.length; i++) {
            if (colors[i]) {
                colorIdentity += colorLetters[i];
            }
        }
        return colorIdentity;
    }

    /**
     * Calculates the full color identity for this card, and stores it in
     * mColorIdentity
     *
     * @param otherCards A list of other cards, used to find the second part if this is
     *                   a multi-card
     */
    public void calculateColorIdentity(ArrayList<MtgCard> otherCards) {
        String colorLetters[] = {"W", "U", "B", "R", "G"};

		/* Get the color identity for the first part of the card */
        String firstPartIdentity = getColorIdentity(this);

		/* Find the color identity for multicards */
        String secondPartIdentity = "";
        String newNumber = null;
        if (number.contains("a")) {
            newNumber = number.replace("a", "b");
        } else if (number.contains("b")) {
            newNumber = number.replace("b", "a");
        }
        if (newNumber != null) {
            for (MtgCard c : otherCards) {
                if (c.number.equals(newNumber)) {
                    secondPartIdentity = getColorIdentity(c);
                    break;
                }
            }
        }

		/* Combine the two color identity parts into one */
        this.colorIdentity = "";
        for (String colorLetter : colorLetters) {
            if (firstPartIdentity.contains(colorLetter)
                    || secondPartIdentity.contains(colorLetter)) {
                colorIdentity += colorLetter;
            }
        }
    }
}