/*
 * Copyright 2018 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

@SuppressWarnings("FieldCanBeLocal")
public class ProductMarketPrice {

    private final boolean success;
    public final String[] errors;

    public final MarketPrice[] results;

    public ProductMarketPrice() {
        success = false;
        errors = new String[]{};
        results = new MarketPrice[]{};
    }

    public static class MarketPrice {
        final long productId;
        public final double lowPrice;
        public final double midPrice;
        public final double highPrice;
        public final double marketPrice;
        final double directLowPrice;
        public final String subTypeName;

        public MarketPrice() {
            productId = 0;
            lowPrice = 0;
            midPrice = 0;
            highPrice = 0;
            marketPrice = 0;
            directLowPrice = 0;
            subTypeName = null;
        }
    }
}
