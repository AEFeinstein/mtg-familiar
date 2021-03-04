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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SampleHandMaker {

    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck) {
        ArrayList<MtgCard> sampleHand = new ArrayList<>();
        List<MtgCard> fullDeck = expandDeck(mDeck);
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < 7; i++) {
            int randCard = rand.nextInt(fullDeck.size());
            sampleHand.add(fullDeck.get(randCard));
            fullDeck.remove(randCard);
        }
        return sampleHand;
    }

    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck, int numOfMulls) {
        if (numOfMulls > 7) {
            return new ArrayList<>();
        }
        List<MtgCard> sampleHand = drawSampleHand(mDeck);
        for (int i = 0; i < numOfMulls; i++) {
            sampleHand.remove(6 - i);
        }
        return sampleHand;
    }

    private static List<MtgCard> expandDeck(List<MtgCard> mDeck) {
        ArrayList<MtgCard> expandedDeck = new ArrayList<>();
        for (MtgCard card : mDeck) {
            if (!card.isSideboard()) {
                for (int i = 0; i < card.mNumberOf; i++) {
                    expandedDeck.add(card);
                }
            }
        }
        return expandedDeck;
    }
}
