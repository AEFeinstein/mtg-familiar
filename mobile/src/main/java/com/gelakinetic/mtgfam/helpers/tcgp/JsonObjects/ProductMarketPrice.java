package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class ProductMarketPrice {

    public final boolean success;
    public final String[] errors;

    public final MarketPrice results[];

    public ProductMarketPrice() {
        success = false;
        errors = null;
        results = null;
    }

    public static class MarketPrice {
        public final long productId;
        public final double lowPrice;
        public final double midPrice;
        public final double highPrice;
        public final double marketPrice;
        public final double directLowPrice;
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
