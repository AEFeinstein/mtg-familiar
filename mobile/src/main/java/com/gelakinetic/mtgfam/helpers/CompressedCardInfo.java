package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;

/**
 * Created by brian on 3/1/2017.
 */

public class CompressedCardInfo {

    public final MtgCard mCard;
    public final ArrayList<IndividualSetInfo> mInfo;

    /**
     * Constructor
     *
     * @param card The MtgCard which will be the base for this object
     */
    public CompressedCardInfo(MtgCard card) {
        mInfo = new ArrayList<>();
        mCard = card;
        add(card);
    }

    /**
     * Add a new printing of a MtgCard to this object
     *
     * @param card The new printing to add to this object
     */
    public void add(MtgCard card) {
        IndividualSetInfo isi = new IndividualSetInfo();

        isi.mSet = card.setName;
        isi.mSetCode = card.setCode;
        isi.mNumber = card.number;
        isi.mIsFoil = card.foil;
        isi.mPrice = null;
        isi.mMessage = card.message;
        isi.mNumberOf = card.numberOf;
        isi.mRarity = card.rarity;

        mInfo.add(isi);
    }

    /**
     * Clear all the different printings for this object
     */
    public void clearCompressedInfo() {
        mInfo.clear();
    }

    /**
     *
     * @return The total number cards this object contains
     */
    public int getTotalNumber() {
        int totalCopies = 0;
        for (IndividualSetInfo isi : mInfo) {
            totalCopies += isi.mNumberOf;
        }
        return totalCopies;
    }

}
