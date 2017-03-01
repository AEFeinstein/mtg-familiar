package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;

/**
 * Created by brian on 3/1/2017.
 */

public interface CompressedCardInfo {

    void add(MtgCard card);
    void clearCompressedInfo();
    MtgCard getCard();
    ArrayList<IndividualSetInfo> getSetInfo();

}
