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

package com.gelakinetic.mtgfam.helpers.tcgp;

import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductDetails;
import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductMarketPrice;

public class MarketPriceInfo {

    public enum PriceType {
        /**
         * The order of the enum values must match the values in trade_option_entries and
         * trade_option_values
         */
        LOW,
        MID,
        HIGH,
        MARKET;

        /**
         * Convert an integer to a price type
         *
         * @param i The integer to convert
         * @return The PriceType representation of this integer
         */
        public static PriceType fromOrdinal(int i) {
            if (i < PriceType.values().length) {
                return PriceType.values()[i];
            }
            return MARKET;
        }
    }

    class Price {
        final double low;
        final double mid;
        final double high;
        final double market;

        /**
         * Create a simpler Price object from a more complicated MarketPrice
         *
         * @param price The MarketPrice to copy prices from
         */
        public Price(ProductMarketPrice.MarketPrice price) {
            low = price.lowPrice;
            mid = price.midPrice;
            high = price.highPrice;
            market = price.marketPrice;
        }
    }

    private final Price mNormalPrice;
    private final Price mFoilPrice;
    private final String mProductUrl;

    /**
     * Create a MarketPriceInfo object from data retrieved from the TCGPlayer.com API
     *
     * @param results The MarketPrice results retrieved from the API. This contains prices.
     * @param details The Details retrieved from the API. This contains the URL.
     */
    public MarketPriceInfo(ProductMarketPrice.MarketPrice[] results, ProductDetails.Details[] details) {
        ProductMarketPrice.MarketPrice foilPrice = null;
        ProductMarketPrice.MarketPrice normalPrice = null;
        for (ProductMarketPrice.MarketPrice marketPrice : results) {
            if (!(marketPrice.highPrice == 0 &&
                    marketPrice.midPrice == 0 &&
                    marketPrice.lowPrice == 0 &&
                    marketPrice.marketPrice == 0)) {
                if (marketPrice.subTypeName.equalsIgnoreCase("Foil")) {
                    foilPrice = marketPrice;
                } else if (marketPrice.subTypeName.equalsIgnoreCase("Normal")) {
                    normalPrice = marketPrice;
                }
            }
        }

        if (null != normalPrice) {
            mNormalPrice = new Price(normalPrice);
        } else {
            mNormalPrice = null;
        }

        if (null != foilPrice) {
            mFoilPrice = new Price(foilPrice);
        } else {
            mFoilPrice = null;
        }

        /* Set the URL from the details */
        mProductUrl = details[0].url + "?pk=MTGFAMILIA";
    }

    /**
     * Get a price from this object. If only foil or normal options are available, that price
     * will be returned regardless of the isFoil parameter
     *
     * @param isFoil    true to return the foil type, false to return the normal price (if those prices exist)
     * @param priceType LOW, MID, HIGH, or MARKET
     * @return The double price in dollars, or 0 of none was found
     */
    public double getPrice(boolean isFoil, PriceType priceType) {
        /* Protection if a card only has foil or normal price, or if it didn't load */
        if (null == mNormalPrice && null != mFoilPrice) {
            isFoil = true;
        } else if (null == mFoilPrice && null != mNormalPrice) {
            isFoil = false;
        } else if (null == mFoilPrice) {
            return 0;
        }

        Price toReturn;
        if (isFoil) {
            toReturn = mFoilPrice;
        } else {
            toReturn = mNormalPrice;
        }
        /* Return the requested price */
        switch (priceType) {
            case LOW: {
                return toReturn.low;
            }
            case MID: {
                return toReturn.mid;
            }
            case HIGH: {
                return toReturn.high;
            }
            default:
            case MARKET: {
                return toReturn.market;
            }
        }
    }

    /**
     * @return true if this card has a foil price, false otherwise
     */
    public boolean hasFoilPrice() {
        return mFoilPrice != null;
    }

    /**
     * @return true if this card has a normal price, false otherwise
     */
    public boolean hasNormalPrice() {
        return mNormalPrice != null;
    }

    /**
     * @return The URL to the TCGPlayer.com page for this MarketPriceInfo
     */
    public String getUrl() {
        return mProductUrl;
    }
}
