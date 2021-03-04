package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SampleHandMaker {

    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck) {
        ArrayList<MtgCard> sampleHand = new ArrayList<>();
        List<MtgCard> fullDeck = expandDeck(mDeck);
        Random rand = new Random();
        for (int i = 0; i < 7; i++) {
            int randCard = rand.nextInt(fullDeck.size());
            sampleHand.add(fullDeck.get(randCard));
            fullDeck.remove(randCard);
        }
        return sampleHand;
    }

    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck, int numOfMulls) {
        List<MtgCard> sampleHand = drawSampleHand(mDeck);
        for (int i = 0; i < numOfMulls; i++) {
            sampleHand.remove(6 - i);
        }
        return sampleHand;
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
