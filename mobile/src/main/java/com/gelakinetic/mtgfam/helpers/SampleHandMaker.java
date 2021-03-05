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

    /**
     * Draws a sample hand from the provided deck with no mulligans taken
     * @param mDeck The deck to draw a sample hand from
     * @return The drawn sample hand
     */
    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck) {
        ArrayList<MtgCard> sampleHand = new ArrayList<>();
        List<MtgCard> fullDeck = expandDeck(mDeck);
        Random rand = new Random(System.currentTimeMillis());
        int fullSize = fullDeck.size();
        for (int i = 0; i < Math.min(7, fullSize); i++) {
            int randCard = rand.nextInt(fullDeck.size());
            sampleHand.add(fullDeck.get(randCard));
            fullDeck.remove(randCard);
        }
        return sampleHand;
    }

    /**
     * Draws a sample hand from the provided deck given how many mulligans have been taken
     * @param mDeck The deck to draw a sample hand from
     * @param numOfMulls The number of mulligans that have been taken
     * @return The drawn sample hand
     */
    public static List<MtgCard> drawSampleHand(List<MtgCard> mDeck, int numOfMulls) {
        List<MtgCard> sampleHand = drawSampleHand(mDeck);
        if (numOfMulls >= sampleHand.size()) {
            return new ArrayList<>();
        }

        for (int i = 0; i < numOfMulls; i++) {
            sampleHand.remove(sampleHand.size() - 1);
        }
        return sampleHand;
    }

    /**
     * Expands a deck to include a separate entry for each copy of a card
     * @param mDeck The deck to expand
     * @return The expanded deck
     */
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
