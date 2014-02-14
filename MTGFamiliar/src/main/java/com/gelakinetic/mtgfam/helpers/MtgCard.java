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

	/**
	 * Default constructor, doesn't leave null fields
	 */
    public MtgCard() {
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
}