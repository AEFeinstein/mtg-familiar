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

import com.gelakinetic.mtgfam.fragments.WishlistFragment;

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
	public int price;            // In cents
	public String message;
	public boolean customPrice = false; //default is false as all cards should first grab internet prices.
	public boolean foil = false;

	public static final String delimiter = "%";

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

	public MtgCard(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, String type, String cost,
				   String ability, float p, float t, int loyalty, int rarity) {
		this.name = name;
		this.number = number;
		this.setCode = setCode;
		this.tcgName = tcgName;
		this.numberOf = numberOf;
		this.price = price;
		this.message = message;
		this.type = type;
		this.manaCost = cost;
		this.ability = ability;
		this.power = p;
		this.toughness = t;
		this.loyalty = loyalty;
		this.rarity = (char) rarity;
	}

	public MtgCard(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity) {
		this.name = name;
		this.number = number;
		this.setCode = setCode;
		this.tcgName = tcgName;
		this.numberOf = numberOf;
		this.price = price;
		this.message = message;
		this.rarity = (char) rarity;
	}

	public MtgCard(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity, boolean customPrice, boolean foil) {
		this.name = name;
		this.number = number;
		this.setCode = setCode;
		this.tcgName = tcgName;
		this.numberOf = numberOf;
		this.price = price;
		this.message = message;
		this.rarity = (char) rarity;
		this.customPrice = customPrice;
		this.foil = foil;
	}

	public MtgCard(String cardName, String cardSet, int numberOf, String number, int rarity) {
		this.name = cardName;
		this.numberOf = numberOf;
		this.setCode = cardSet;
		this.number = number;
		this.rarity = (char) rarity;
	}

	public String getPriceString() {
		return String.format("%d.%02d", this.price / 100, this.price % 100);
	}

	public boolean hasPrice() {
		return this.message == null || this.message.length() == 0;
	}

	public String toString() {
		return this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.number + delimiter + ((int)this.rarity) + delimiter + this.foil + '\n';
	}

	public String toString(int side) {
		return side + delimiter + this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.customPrice + delimiter + this.price + delimiter + this.foil + '\n';
	}

	public String toReadableString(boolean includeTcgName) {
		return String.valueOf(this.numberOf) + ' ' + this.name + (this.foil ? " - Foil " : "") + (includeTcgName ? " (" + this.tcgName + ')' : "") + '\n';
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MtgCard) {
			return this.name.equals(((MtgCard) o).name);
		}
		else if(o instanceof WishlistFragment.CompressedWishlistInfo) {
			return this.name.equals(((WishlistFragment.CompressedWishlistInfo)o).mCard.name);
		}
		return false;
	}
}