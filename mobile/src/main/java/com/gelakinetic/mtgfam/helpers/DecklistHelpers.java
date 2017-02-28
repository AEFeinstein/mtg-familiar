package com.gelakinetic.mtgfam.helpers;

/**
 * Created by bmaurer on 2/28/2017.
 */

public class DecklistHelpers {

    public static class CompressedDecklistInfo extends WishlistHelpers.CompressedWishlistInfo {

        public boolean mIsSideboard;

        public CompressedDecklistInfo(MtgCard card, boolean isSideboard) {
            super(card);
            this.mIsSideboard = isSideboard;
            add(card);
        }

    }

}
