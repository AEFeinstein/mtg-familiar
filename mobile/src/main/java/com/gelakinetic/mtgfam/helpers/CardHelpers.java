package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;

import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by bmaurer on 3/6/2017.
 */

public class CardHelpers {

    /**
     * This class encapsulates all non-duplicated information for two cards in different sets
     */
    public static class IndividualSetInfo {
        public String mSet;
        public String mSetCode;
        public String mNumber;

        public Boolean mIsFoil;
        public PriceInfo mPrice;
        public String mMessage;
        public Integer mNumberOf;
        public Character mRarity;
    }

    /*
     * Parent class of CompressedDecklistInfo and CompressedWishlistInfo
     * It contains the common code shared between the two
     */
    public static class CompressedCardInfo {

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

    public static ComparatorChain<CompressedCardInfo> getComparatorWithName(Comparator<CompressedCardInfo> comparator) {
        ComparatorChain<CompressedCardInfo> chain = new ComparatorChain<>();
        chain.addComparator(comparator);
        chain.addComparator(new CardComparatorName());
        return chain;
    }

    /* Comparator based on name */
    public static class CardComparatorName implements Comparator<CompressedCardInfo> {
        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            return card1.mCard.name.compareTo(card2.mCard.name);
        }
    }

    /* Comparator based on CMC */
    public static class CardComparatorCMC implements Comparator<CompressedCardInfo> {
        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            if (card1.mCard.cmc == card2.mCard.cmc) {
                return 0;
            } else if (card1.mCard.cmc > card2.mCard.cmc) {
                return 1;
            }
            return -1;
        }
    }

    /* Comparator based on color */
    public static class CardComparatorColor implements Comparator<CompressedCardInfo> {

        private static final String colors = "WUBRG";
        private static final String nonColors = "LAC";

        /**
         * Gets what colors are in the given string
         * @param c the string of colors
         * @return valid colors from the string
         */
        private String getColors(String c) {
            String validColors = "";
            //1. catch null/empty string
            if (c == null || c.isEmpty()) {
                return "";
            }
            //2. For each char, if a valid color, add to return String
            for (int i = 0; i < c.length(); i++) {
                if (colors.indexOf(c.charAt(i)) > -1) {
                    validColors += c.charAt(i);
                }
            }
            return validColors;
        }

        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            String cardColors1 = getColors(card1.mCard.color);
            String cardColors2 = getColors(card2.mCard.color);
            int priority1;
            int priority2;
            //1. If colorless, perform colorless comparison
            if (cardColors1.length() + cardColors2.length() == 0) {
                cardColors1 = card1.mCard.color;
                cardColors2 = card2.mCard.color;
                for (int i = 0; i < Math.min(cardColors1.length(), cardColors2.length()); i++) {
                    priority1 = nonColors.indexOf(cardColors1.charAt(i));
                    priority2 = nonColors.indexOf(cardColors2.charAt(i));
                    if (priority1 != priority2) {
                        return priority1 < priority2 ? -1 : 1;
                    }
                }
                return 0;
            }
            if (cardColors1.length() < cardColors2.length()) {
                return -1;
            } else if (cardColors1.length() > cardColors2.length()) {
                return 1;
            }
            //3. Else if the same number of colors exist, compare based on WUBRG-ness
            else {
                for (int i = 0; i < Math.min(cardColors1.length(), cardColors2.length()); i++) {
                    priority1 = colors.indexOf(cardColors1.charAt(i));
                    priority2 = colors.indexOf(cardColors2.charAt(i));
                    if (priority1 != priority2) {
                        return priority1 < priority2 ? -1 : 1;
                    }
                }
                return 0;
            }
        }
    }

    /* Comparator based on sideboard */
    public static class CardComparatorSideboard implements Comparator<CompressedDecklistInfo> {
        @Override
        public int compare(CompressedDecklistInfo card1, CompressedDecklistInfo card2) {
            if (card1.mIsSideboard == card2.mIsSideboard) {
                return 0;
            } else if (card1.mIsSideboard && !card2.mIsSideboard) {
                return 1;
            }
            return -1;
        }
    }

    /* Comparator based on card supertype */
    public static class CardComparatorSupertype implements Comparator<CompressedCardInfo> {
        String[] mTypes = new String[]{"Creature", "Planeswalker", "Instant", "Sorcery", "Artifact", "Enchantment", "Land"};
        boolean mSeparateSpells;
        public CardComparatorSupertype(boolean separateSpells) {
            mSeparateSpells = separateSpells;
        }
        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            String card1Type = card1.mCard.type;
            String card2Type = card2.mCard.type;
            if (!mSeparateSpells && ((card1Type.contains(mTypes[2]) || card1Type.contains(mTypes[3]))
                    && (card2Type.contains(mTypes[2]) || card2Type.contains(mTypes[3])))) {
                return 0;
            }
            for (String type : mTypes) {
                if (card1Type.contains(type) && card2Type.contains(type)) {
                    return 0;
                } else if (card1Type.contains(type) && !card2Type.contains(type)) {
                    return -1;
                } else if (!card1Type.contains(type) && card2Type.contains(type)) {
                    return 1;
                }
            }
            return 0;
        }
    }

    /* Comparator based on price */
    public static class CardComparatorPrice implements Comparator<CompressedCardInfo> {
        /* Price setting constants */
        private static final int LOW_PRICE = 0;
        private static final int AVG_PRICE = 1;
        private static final int HIGH_PRICE = 2;

        private final int mPriceSetting;

        public CardComparatorPrice(int mPriceSetting) {
            this.mPriceSetting = mPriceSetting;
        }

        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            double sumWish1 = 0;
            double sumWish2 = 0;

            for (IndividualSetInfo isi : card1.mInfo) {
                try {
                    if (isi.mIsFoil) {
                        sumWish1 += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                    } else {
                        switch (mPriceSetting) {
                            case LOW_PRICE:
                                sumWish1 += (isi.mPrice.mLow * isi.mNumberOf);
                                break;
                            case AVG_PRICE:
                                sumWish1 += (isi.mPrice.mAverage * isi.mNumberOf);
                                break;
                            case HIGH_PRICE:
                                sumWish1 += (isi.mPrice.mHigh * isi.mNumberOf);
                                break;
                        }
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }

            for (IndividualSetInfo isi : card2.mInfo) {
                try {
                    if (isi.mIsFoil) {
                        sumWish2 += (isi.mPrice.mFoilAverage * isi.mNumberOf);
                    } else {
                        switch (mPriceSetting) {
                            case LOW_PRICE:
                                sumWish2 += (isi.mPrice.mLow * isi.mNumberOf);
                                break;
                            case AVG_PRICE:
                                sumWish2 += (isi.mPrice.mAverage * isi.mNumberOf);
                                break;
                            case HIGH_PRICE:
                                sumWish2 += (isi.mPrice.mHigh * isi.mNumberOf);
                                break;
                        }
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }

            if (sumWish1 == sumWish2) {
                return card1.mCard.name.compareTo(card2.mCard.name);
            } else if (sumWish1 > sumWish2) {
                return 1;
            }
            return -1;
        }
    }

    /* Comparator based on first set of a card */
    public static class CardComparatorSet implements Comparator<CompressedCardInfo> {
        @Override
        public int compare(CompressedCardInfo card1, CompressedCardInfo card2) {
            return card1.mInfo.get(0).mSet.compareTo(card2.mInfo.get(0).mSet);
        }
    }

}
