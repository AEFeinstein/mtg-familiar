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

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.model.Comparison;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;

/**
 * Encapsulate all information about a search query. It is serializable to save to a file easily
 */
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 4712329695735151965L;
    public String name = null;
    public String text = null;
    public List<String> superTypes = null;
    public List<String> subTypes = null;
    public String color = "wubrgl";
    public int colorLogic = 0;
    public List<String> sets = null;
    public float powChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String powLogic = null;
    public float touChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String touLogic = null;
    public int cmc = -1;
    public String cmcLogic = null;
    public String format = null;
    public String rarity = null;
    public String flavor = null;
    public String artist = null;
    public int typeLogic = 0;
    public int textLogic = 0;
    public int setLogic = CardDbAdapter.MOST_RECENT_PRINTING;
    public String collectorsNumber = null;
    public String colorIdentity = "wubrgl";
    public int colorIdentityLogic = 0;
    public List<String> manaCost = null;
    public Comparison manaCostLogic;
    public boolean moJhoStoFilter;
    public String watermark = null;
    public List<String> setTypes = null;
    public boolean isCommander;

    public String toJson() {
        return (new Gson()).toJson(this);
    }
}