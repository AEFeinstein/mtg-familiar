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
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.helpers.util.ComparatorChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SampleHandMaker {

    private final List<MtgCard> mDeck;
    private List<MtgCard> mDeckWithoutHand;

    public SampleHandMaker(List<MtgCard> mDeck) {
        this.mDeck = expandDeck(mDeck);
    }

    /**
     * Draws a sample hand from the provided deck with no mulligans taken
     *
     * @return The drawn sample hand
     */
    public List<MtgCard> drawSampleHand() {
        ComparatorChain<MtgCard> compareChain = new ComparatorChain<>();
        compareChain.addComparator(new CardHelpers.CardComparatorCMC());
        compareChain.addComparator(new CardHelpers.CardComparatorColor());
        compareChain.addComparator(new CardHelpers.CardComparatorName());
        ArrayList<MtgCard> sampleHand = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        int fullSize = mDeck.size();
        mDeckWithoutHand = new ArrayList<>(mDeck);
        for (int i = 0; i < Math.min(7, fullSize); i++) {
            int randCard = rand.nextInt(mDeckWithoutHand.size());
            sampleHand.add(mDeckWithoutHand.get(randCard));
            mDeckWithoutHand.remove(randCard);
        }
        Collections.sort(sampleHand, compareChain);
        return sampleHand;
    }

    /**
     * Draws a sample hand from the provided deck given how many mulligans have been taken
     *
     * @param numOfMulls The number of mulligans that have been taken
     * @return The drawn sample hand
     */
    public List<MtgCard> drawSampleHand(int numOfMulls) {
        List<MtgCard> sampleHand = drawSampleHand();
        if (numOfMulls >= sampleHand.size()) {
            return new ArrayList<>();
        }
        for (int i = 0; i < numOfMulls; i++) {
            mDeckWithoutHand.add(sampleHand.get(sampleHand.size() - 1));
            sampleHand.remove(sampleHand.size() - 1);
        }
        return sampleHand;
    }

    /**
     * Expands a deck to include a separate entry for each copy of a card
     *
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

    /**
     * Returns a new card from the deck. Returning as list makes 0 deck size case easier
     *
     * @return List with new card or empty list if deck is empty
     */
    public List<MtgCard> drawCard() {
        Random rand = new Random(System.currentTimeMillis());
        if (mDeckWithoutHand.size() > 0) {
            int randCard = rand.nextInt(mDeckWithoutHand.size());
            List<MtgCard> toReturn = new ArrayList<>();
            toReturn.add(mDeckWithoutHand.get(randCard));
            mDeckWithoutHand.remove(randCard);
            return toReturn;
        } else {
            return new ArrayList<>();
        }
    }
}
