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

package com.gelakinetic.GathererScraper.JsonTypes;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;

/*
 * This class contains all information about a scraped card
 *
 * @author AEFeinstein
 *
 */
@SuppressWarnings("CanBeFinal")
public class Card implements Comparable<Card> {

    // The card's name
    protected String mName = "";

    // The card's mana cost
    protected String mManaCost = "";

    // The card's converted mana cost
    protected int mCmc = 0;

    // The card's type, includes super and sub
    protected String mType = "";

    // The card's text text
    protected String mText = "";

    // The card's flavor text
    protected String mFlavor = "";

    // The card's expansion
    protected String mExpansion = "";

    // The card's expansion
    protected String mScryfallSetCode = "";

    // The card's rarity
    protected char mRarity = '\0';

    // The card's collector's number. Not an integer (i.e. 181a, 181b)
    protected String mNumber = "";

    // The card's artist
    protected String mArtist = "";

    // The card's colors
    protected String mColor = "";

    // The card's colors
    protected String mColorIdentity = "";

    // The card's multiverse id
    protected int mMultiverseId = 0;

    // The card's power. Not an integer (i.e. *+1, X)
    protected float mPower = CardDbAdapter.NO_ONE_CARES;

    // The card's toughness, see mPower
    protected float mToughness = CardDbAdapter.NO_ONE_CARES;

    // The card's loyalty. An integer in practice
    protected int mLoyalty = CardDbAdapter.NO_ONE_CARES;

    // All the card's foreign printings
    protected ArrayList<ForeignPrinting> mForeignPrintings = new ArrayList<>();

    // The card's watermark
    protected String mWatermark = "";

    public String getName() {
        return mName;
    }

    public String getManaCost() {
        return mManaCost;
    }

    public int getCmc() {
        return mCmc;
    }

    public String getType() {
        return mType;
    }

    public String getText() {
        return mText;
    }

    public String getFlavor() {
        return mFlavor;
    }

    public String getExpansion() {
        return mExpansion;
    }

    public String getScryfallSetCode() {
        return mScryfallSetCode;
    }

    public char getRarity() {
        return mRarity;
    }

    public String getNumber() {
        return mNumber;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getColor() {
        return mColor;
    }

    public String getColorIdentity() {
        return mColorIdentity;
    }

    public int getMultiverseId() {
        return mMultiverseId;
    }

    public float getPower() {
        return mPower;
    }

    public float getToughness() {
        return mToughness;
    }

    public int getLoyalty() {
        return mLoyalty;
    }

    public ArrayList<ForeignPrinting> getForeignPrintings() {
        return mForeignPrintings;
    }

    public String getWatermark() {
        return mWatermark;
    }

    // Private class for encapsulating foreign printing information
    @SuppressWarnings("CanBeFinal")
    public static class ForeignPrinting implements Comparable<ForeignPrinting> {
        private int mMultiverseId;
        private String mName;
        private String mLanguageCode;

        public ForeignPrinting(ForeignPrinting fp) {
            if (null != fp) {
                this.mMultiverseId = fp.mMultiverseId;
                this.mName = fp.mName;
                this.mLanguageCode = fp.mLanguageCode;
            } else {
                this.mName = "";
                this.mLanguageCode = "";
                this.mMultiverseId = 0;
            }
        }

        public ForeignPrinting(String name, String languageCode, int multiverseId) {
            this.mName = name;
            this.mLanguageCode = languageCode;
            this.mMultiverseId = multiverseId;
        }

        @Override
        public int compareTo(@NonNull ForeignPrinting o) {
            return Integer.compare(this.mMultiverseId, o.mMultiverseId);
        }

        @Override
        public boolean equals(Object arg0) {
            if (arg0 instanceof ForeignPrinting) {
                ForeignPrinting o = (ForeignPrinting) arg0;
                if (null == this.getLanguageCode() && null == o.getLanguageCode()) {
                    return true;
                } else if (null == this.getLanguageCode() || (null == o.getLanguageCode())) {
                    return false;
                } else {
                    return this.getLanguageCode().equals(o.mLanguageCode);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getLanguageCode().hashCode();
        }

        public int getMultiverseId() {
            return mMultiverseId;
        }

        public String getName() {
            return mName;
        }

        public String getLanguageCode() {
            return mLanguageCode;
        }
    }

    /**
     * This function usually sorts by collector's number. However, gatherer
     * doesn't have collector's number for expansions before collector's number
     * was printed, and magiccards.info uses a strange numbering scheme. This
     * function does it's best
     */
    @Override
    public int compareTo(@NonNull Card other) {

        /* Sort by collector's number */
        if (this.mNumber != null && other.mNumber != null && this.mNumber.length() > 0 && other.mNumber.length() > 0) {

            int this_num = this.getNumberInteger();
            int other_num = other.getNumberInteger();
            if (this_num > other_num) {
                return 1;
            } else if (this_num < other_num) {
                return -1;
            } else {
                return Character.compare(this.getNumberChar(), other.getNumberChar());
            }
        }

        /* Battle Royale is pure alphabetical, except for basics, why not */
        if (this.mExpansion.equals("BR")) {
            if (this.mType.contains("Basic Land") && !other.mType.contains("Basic Land")) {
                return 1;
            }
            if (!this.mType.contains("Basic Land") && other.mType.contains("Basic Land")) {
                return -1;
            }
            return this.mName.compareTo(other.mName);
        }

        /*
         * Or if that doesn't exist, sort by color order. Weird for
         * magiccards.info
         */
        if (this.getNumFromColor() > other.getNumFromColor()) {
            return 1;
        } else if (this.getNumFromColor() < other.getNumFromColor()) {
            return -1;
        }

        /* If the color matches, sort by name */
        return this.mName.compareTo(other.mName);
    }

    /**
     * Returns a number used for sorting by color. This is different for
     * Beatdown because magiccards.info is weird
     *
     * @return A number indicating how the card's color is sorted
     */
    private int getNumFromColor() {
        /* Because Beatdown properly sorts color */
        if (this.mExpansion.equals("BD")) {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'W': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'B': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'G': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        /* And magiccards.info has weird numbering for everything else */
        else {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'B': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'G': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'W': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        return 8;
    }

    private int getNumberInteger() {
        try {
            char c = this.mNumber.charAt(this.mNumber.length() - 1);
            if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
                return Integer.parseInt(this.mNumber.substring(0, this.mNumber.length() - 1));
            }
            return Integer.parseInt(this.mNumber);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private char getNumberChar() {
        char c = this.mNumber.charAt(this.mNumber.length() - 1);
        if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
            return c;
        }
        return 0;
    }
}