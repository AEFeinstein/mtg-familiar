package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;
import java.util.List;

public class SampleHandMaker {

    public static List<MtgCard> drawSampleHand(List<MtgCard> mFullDeck) {
        return null;
    }

    public static List<MtgCard> drawMulligan(List<MtgCard> mFullDeck, int numOfMulls) {
        return null;
    }

    private static List<MtgCard> expandDeck(List<MtgCard> mDeck) {
        ArrayList<MtgCard> expandedDeck = new ArrayList<>();
        for (MtgCard card : mDeck) {
            for (int i = 0; i < card.mNumberOf; i++) {
                expandedDeck.add(card);
            }
        }
        return expandedDeck;
    }
}
