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

import com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects.ProductMarketPrice;

/**
 * Created by Adam on 1/29/2018.
 */

public class MarketPriceInfo {

    public enum CardType {
        NORMAL,
        FOIL
    }

    public enum PriceType {
        LOW,
        MID,
        HIGH,
        MARKET
    }

    class Price {
        double low = 0;
        double mid = 0;
        double high = 0;
        double market = 0;
    }

    Price mNormalPrice = null;
    Price mFoilPrice = null;
    String mProductUrl = null;

    public void setUrl(String url) {
        mProductUrl = url + "?pk=MTGFAMILIA";
    }

    public void setNormalPrice(ProductMarketPrice.MarketPrice marketPrice) {
        mNormalPrice = new Price();
        setPrice(mNormalPrice, marketPrice);
    }

    public void setFoilPrice(ProductMarketPrice.MarketPrice marketPrice) {
        mFoilPrice = new Price();
        setPrice(mFoilPrice, marketPrice);
    }

    private void setPrice(Price price, ProductMarketPrice.MarketPrice marketPrice) {
        price.low = marketPrice.lowPrice;
        price.mid = marketPrice.midPrice;
        price.high = marketPrice.highPrice;
        price.market = marketPrice.marketPrice;
    }

    public double getPrice(CardType cardType, PriceType priceType) {
        /* Protection if a card only has foil or normal price, or if it didn't load */
        if (null == mNormalPrice && null != mFoilPrice) {
            cardType = CardType.FOIL;
        } else if (null == mFoilPrice && null != mNormalPrice) {
            cardType = CardType.NORMAL;
        } else if (null == mFoilPrice && null == mNormalPrice) {
            return 0;
        }
        /* Return the requested price */
        switch (cardType) {
            case NORMAL: {
                switch (priceType) {
                    case LOW: {
                        return mNormalPrice.low;
                    }
                    case MID: {
                        return mNormalPrice.mid;
                    }
                    case HIGH: {
                        return mNormalPrice.high;
                    }
                    case MARKET: {
                        return mNormalPrice.market;
                    }
                }
            }
            case FOIL: {
                switch (priceType) {
                    case LOW: {
                        return mFoilPrice.low;
                    }
                    case MID: {
                        return mFoilPrice.mid;
                    }
                    case HIGH: {
                        return mFoilPrice.high;
                    }
                    case MARKET: {
                        return mFoilPrice.market;
                    }
                }
            }
        }
        return 0;
    }

    public boolean hasFoilPrice() {
        return mFoilPrice != null;
    }

    public boolean hasNormalPrice() {
        return mNormalPrice != null;
    }


    public String getUrl() {
        return mProductUrl;
    }
}
