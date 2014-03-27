/**
 Copyright 2011 Adam Feinstein

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;

/**
 * Encapsulate all information about a magic card
 */
public class MtgCard {
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
	public String tcgName;
	public String setCode;
	public int numberOf;
	public int price; /* In cents */
	public String message;
	/* public boolean customPrice = false; default is false as all cards should first grab internet prices. */
	public boolean foil = false;

	public static final String DELIMITER = "%";

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
		power = CardDbAdapter.NOONECARES;
		toughness = CardDbAdapter.NOONECARES;
		loyalty = CardDbAdapter.NOONECARES;
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

	/* Prints a bunch of information about this card, predominantly to save it in a plaintext file */
	public String toString() {
		return this.name + DELIMITER + this.setCode + DELIMITER + this.numberOf + DELIMITER + this.number + DELIMITER +
				((int) this.rarity) + DELIMITER + this.foil + '\n';
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
		}
		else if (o instanceof CompressedWishlistInfo) {
			return this.name.equals(((CompressedWishlistInfo) o).mCard.name);
		}
		return false;
	}
}