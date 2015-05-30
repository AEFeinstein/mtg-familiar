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
import android.database.sqlite.SQLiteDatabase;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

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
    }

    /**
     * Constructor when building a MtgCard info from the result of a toString()
     *
     * @param line Information about this card, in the form of what toString() prints
     * @param mCtx A context used for getting localized strings
     */
    public MtgCard(String line, Context mCtx) {

        String[] parts = line.split(MtgCard.DELIMITER);

        this.name = parts[0];
        this.setCode = parts[1];
        this.numberOf = Integer.parseInt(parts[2]);

		/* "foil" didn't exist in earlier versions, so it may not be part of the string */
        if (parts.length > 3) {
            this.number = parts[3];
        }
        if (parts.length > 4) {
            this.rarity = (char) Integer.parseInt(parts[4]);
        }
        boolean foil = false;
        if (parts.length > 5) {
            foil = Boolean.parseBoolean(parts[5]);
        }
        this.foil = foil;
        this.message = mCtx.getString(R.string.wishlist_loading);

    }

    public MtgCard(String name, String tcgName, String setCode, int numberOf, String message, String number, boolean foil) {
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
    }

    public static MtgCard MtgCardFromTradeString(String line, Context context) {

        MtgCard card = new MtgCard();
        String[] parts = line.split(DELIMITER);

		/* Parse these parts out of the string */
        card.mSide = Integer.parseInt(parts[0]);
        card.name = parts[1];
        card.setCode = parts[2];
        card.numberOf = Integer.parseInt(parts[3]);

		/* These parts may not exist */
        card.customPrice = parts.length > 4 && Boolean.parseBoolean(parts[4]);
        if (parts.length > 5) {
            card.price = Integer.parseInt(parts[5]);
        } else {
            card.price = 0;
        }
        card.foil = parts.length > 6 && Boolean.parseBoolean(parts[6]);

		/* Defaults regardless */
        SQLiteDatabase database = DatabaseManager.getInstance(context, false).openDatabase(false);
        try {
            card.setName = CardDbAdapter.getSetNameFromCode(card.setCode, database);
        } catch (FamiliarDbException e) {
            card.setName = null;
        }
        DatabaseManager.getInstance(context, false).closeDatabase(false);
        card.message = "loading";
        return card;
    }

    /* Prints a bunch of information about this card, predominantly to save it in a plaintext file */
    public String toWishlistString() {
        return this.name + DELIMITER + this.setCode + DELIMITER + this.numberOf + DELIMITER + this.number + DELIMITER +
                ((int) this.rarity) + DELIMITER + this.foil + '\n';
    }

    public String toTradeString(int side) {
        return side + DELIMITER + this.name + DELIMITER + this.setCode + DELIMITER + this.numberOf + DELIMITER + this.customPrice + DELIMITER + this.price + DELIMITER + this.foil + '\n';
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

    public String getPriceString() {
        return String.format("$%d.%02d", this.price / 100, this.price % 100);
    }

    public boolean hasPrice() {
        return (this.priceInfo != null || (this.customPrice && this.price != 0));
    }
}