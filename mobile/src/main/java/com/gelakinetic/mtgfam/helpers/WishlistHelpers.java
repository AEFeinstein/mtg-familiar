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

import android.app.Activity;
import android.content.Context;
import android.util.Pair;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.SortOrderDialogFragment.SortOption;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.tcgp.MarketPriceInfo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class has helpers used for reading, writing, and modifying the wishlist from different fragments
 */
public class WishlistHelpers {

    /* The name of the wishlist file */
    private static final String WISHLIST_NAME = "card.wishlist";

    /**
     * Write the wishlist passed as a parameter to the wishlist file
     *
     * @param activity  A context to open the file and pop toasts with
     * @param lWishlist The wishlist to write to the file
     */
    public static void WriteWishlist(Activity activity, ArrayList<MtgCard> lWishlist) {
        try {
            FileOutputStream fos = activity.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

            for (MtgCard m : lWishlist) {
                fos.write(m.toWishlistString().getBytes());
            }

            fos.close();
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(activity, e.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        }
    }

    /**
     * Write the wishlist passed as a parameter to the wishlist file
     *
     * @param activity            A context to open the file and pop toasts with
     * @param mCompressedWishlist The wishlist to write to the file
     */
    public static void WriteCompressedWishlist(Activity activity, List<CompressedWishlistInfo> mCompressedWishlist) {
        if (null == activity) {
            // Context is null, don't try to write the wishlist
            return;
        }
        try {
            FileOutputStream fos = activity.openFileOutput(WISHLIST_NAME, Context.MODE_PRIVATE);

            /* For each compressed card, make an MtgCard and write it to the wishlist */
            for (CompressedWishlistInfo cwi : mCompressedWishlist) {
                for (IndividualSetInfo isi : cwi.mInfo) {
                    cwi.applyIndividualInfo(isi);
                    fos.write(cwi.toWishlistString().getBytes());
                }
            }

            fos.close();
        } catch (IOException e) {
            SnackbarWrapper.makeAndShowText(activity, e.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        }
    }

    /**
     * Adds an item as a CompressedWishlistInfo to the wishlist
     *
     * @param activity     activity that this is being called from
     * @param wishlistInfo the CompressedWishlistInfo to add to the wishlist
     */
    public static void addItemToWishlist(final Activity activity, final CompressedWishlistInfo wishlistInfo) {
        try {
            final ArrayList<MtgCard> currentWishlist = ReadWishlist(activity, false);
            for (IndividualSetInfo isi : wishlistInfo.mInfo) {
                wishlistInfo.applyIndividualInfo(isi);
                if (currentWishlist.contains(wishlistInfo)) {
                    final int existingIndex = currentWishlist.indexOf(wishlistInfo);
                    currentWishlist.get(existingIndex).mNumberOf += wishlistInfo.mNumberOf;
                } else {
                    currentWishlist.add(wishlistInfo);
                }
            }
            WriteWishlist(activity, currentWishlist);
        } catch (FamiliarDbException e) {
            // eat it
        }
    }

    /**
     * Delete the wishlist
     *
     * @param mCtx A context to open the file wish
     */
    public static void ResetCards(Context mCtx) {

        String[] files = mCtx.fileList();
        for (String fileName : files) {
            if (fileName.equals(WISHLIST_NAME)) {
                mCtx.deleteFile(fileName);
            }
        }
    }

    /**
     * Read the wishlist from a file and return it as an ArrayList<MtgCard>
     *
     * @param activity     A context to open the file and pop toasts with
     * @param loadFullData true to load all card data from the database, false to just read the file
     * @return The wishlist in ArrayList form
     */
    public static ArrayList<MtgCard> ReadWishlist(Activity activity, boolean loadFullData) throws FamiliarDbException {

        ArrayList<MtgCard> lWishlist = new ArrayList<>();
        int orderAddedIdx = 0;

        try {
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(activity.openFileInput(WISHLIST_NAME)))) {
                /* Read each line as a card, and add them to the ArrayList */
                while ((line = br.readLine()) != null) {
                    MtgCard card = MtgCard.fromWishlistString(line, false, activity);
                    if (null != card) {
                        card.setIndex(orderAddedIdx++);
                        lWishlist.add(card);
                    }
                }
            }
        } catch (NumberFormatException e) {
            SnackbarWrapper.makeAndShowText(activity, e.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        } catch (IOException e) {
            /* Catches file not found exception when wishlist doesn't exist */
        }

        if (loadFullData && !lWishlist.isEmpty()) {
            MtgCard.initCardListFromDb(activity, lWishlist);
        }
        return lWishlist;
    }

    /**
     * Take a wishlist and turn it into plaintext so that it can be shared via email or whatever,
     * with the choice of including the set in the wishlist export
     *
     * @param mCompressedWishlist The wishlist to share
     * @param ctx                 The context to get localized strings with
     * @param shareText           Whether or not the full card text should be exported
     * @param sharePrice          Whether or not the card price should be exported
     * @return A string containing all the wishlist data
     */
    public static String GetSharableWishlist(List<CompressedWishlistInfo> mCompressedWishlist,
                                             Context ctx, boolean shareText, boolean sharePrice,
                                             MarketPriceInfo.PriceType priceOption) {
        StringBuilder readableWishlist = new StringBuilder();

        /* For each wishlist entry */
        for (CompressedWishlistInfo cwi : mCompressedWishlist) {
            /* Append the card name, always */
            readableWishlist.append(cwi.getName());
            readableWishlist.append("\r\n");

            /* Append the full text, if the user wants it */
            if (shareText) {
                cwi.appendCardText(readableWishlist);
            }

            /* For each set info in the wishlist */
            for (IndividualSetInfo isi : cwi.mInfo) {
                /* Append the number of the card, per-set */
                readableWishlist
                        .append(isi.mNumberOf)
                        .append(' ')
                        .append(isi.mSet);
                /* Append whether it is foil or not */
                if (isi.mIsFoil) {
                    readableWishlist
                            .append(" (")
                            .append(ctx.getString(R.string.wishlist_foil))
                            .append(")");
                }
                /* Attempt to append the price */
                if (sharePrice && isi.mPrice != null) {
                    double price;
                    price = isi.mPrice.getPrice(isi.mIsFoil, priceOption).price;
                    if (price != 0) {
                        readableWishlist
                                .append(", $")
                                .append(String.format(Locale.US, "%d.%02d", (int) price, (int) ((price - ((int) price)) * 100)));
                    }
                }
                readableWishlist.append("\r\n");
            }
            readableWishlist.append("\r\n");
        }
        return readableWishlist.toString();
    }

    public static Pair<Map<String, String>, Map<String, String>> getTargetNumberOfs(String cardName, ArrayList<MtgCard> wishlist) {
        final Map<String, String> targetCardNumberOfs = new HashMap<>();
        final Map<String, String> targetFoilNumberOfs = new HashMap<>();
        for (MtgCard card : wishlist) {
            if (card.getName().equals(cardName)) {
                if (card.mIsFoil) {
                    targetFoilNumberOfs.put(card.getExpansion(), String.valueOf(card.mNumberOf));
                    continue;
                }
                targetCardNumberOfs.put(card.getExpansion(), String.valueOf(card.mNumberOf));
            }
        }
        return new Pair<>(targetCardNumberOfs, targetFoilNumberOfs);
    }

    /**
     * This class encapsulates a single MtgCard and an ArrayList of non-duplicated information for
     * different printings of that card.
     */
    public static class CompressedWishlistInfo extends CardHelpers.CompressedCardInfo {

        final int mIndex;

        /**
         * Constructor
         *
         * @param card The MtgCard which will be the base for this object
         */
        public CompressedWishlistInfo(MtgCard card, int index) {
            super(card);
            mIndex = index;
        }

        /**
         * Check to see if two CompressedWishlistInfo objects are equivalent, or if this is
         * equivalent to a MtgCard object. The comparison is done on the MtgCard's name.
         *
         * @param o The object to compare to this one
         * @return true if the specified object is equal to this string, false otherwise.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof CompressedWishlistInfo) {
                return mName.equals(((CompressedWishlistInfo) o).mName);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            int hash = 23;
            hash = hash * 31 + super.hashCode();
            return hash * 31 + mIndex;
        }

        /**
         * Clear all the different printings for this object
         */
        public void clearCompressedInfo() {
            mInfo.clear();
        }

        /**
         * Return the total price of all cards in this object
         *
         * @param priceSetting LOW_PRICE, AVG_PRICE, or HIGH_PRICE
         * @return The sum price of all cards in this object
         */
        double getTotalPrice(MarketPriceInfo.PriceType priceSetting) {
            double sumWish = 0;

            for (IndividualSetInfo isi : mInfo) {
                try {
                    if (null != isi.mPrice) {
                        sumWish += (isi.mPrice.getPrice(isi.mIsFoil, priceSetting).price * isi.mNumberOf);
                    }
                } catch (NullPointerException e) {
                    /* eat it, no price is loaded */
                }
            }
            return sumWish;
        }

        public int getIndex() {
            return mIndex;
        }
    }

    public static class WishlistComparator implements Comparator<CompressedWishlistInfo> {

        final ArrayList<SortOption> options = new ArrayList<>();
        final MarketPriceInfo.PriceType mPriceSetting;

        /**
         * Constructor. It parses an "order by" string into search options. The first options have
         * higher priority
         *
         * @param orderByStr   The string to parse. It uses SQLite syntax: "KEY asc,KEY2 desc" etc
         * @param priceSetting The current price setting (LO/AVG/HIGH) used to sort by prices
         */
        public WishlistComparator(String orderByStr, MarketPriceInfo.PriceType priceSetting) {
            int idx = 0;
            for (String option : orderByStr.split(",")) {
                String key = option.split(" ")[0];
                boolean ascending = option.split(" ")[1].equalsIgnoreCase(SortOrderDialogFragment.SQL_ASC);
                options.add(new SortOption(null, ascending, key, idx++));
            }
            mPriceSetting = priceSetting;
        }

        /**
         * Compare two CompressedWishlistInfo objects based on all the search options in descending priority
         *
         * @param wish1 One card to compare
         * @param wish2 The other card to compare
         * @return an integer < 0 if wish1 is less than wish2, 0 if they are equal, and > 0 if wish1 is greater than wish2.
         */
        @Override
        @SuppressFBWarnings(value = "DM_BOXED_PRIMITIVE_FOR_COMPARE", justification = "Minimum API Level not high enough")
        public int compare(CompressedWishlistInfo wish1, CompressedWishlistInfo wish2) {

            int retVal = 0;
            /* Iterate over all the sort options, starting with the high priority ones */
            for (SortOption option : options) {
                /* Compare the entries based on the key */
                try {
                    switch (option.getKey()) {
                        case CardDbAdapter.KEY_NAME: {
                            retVal = wish1.getName().compareTo(wish2.getName());
                            break;
                        }
                        case CardDbAdapter.KEY_COLOR: {
                            retVal = wish1.getColor().compareTo(wish2.getColor());
                            break;
                        }
                        case CardDbAdapter.KEY_SUPERTYPE: {
                            retVal = wish1.getType().compareTo(wish2.getType());
                            break;
                        }
                        case CardDbAdapter.KEY_CMC: {
                            retVal = Integer.compare(wish1.getCmc(), wish2.getCmc());
                            break;
                        }
                        case CardDbAdapter.KEY_POWER: {
                            retVal = Float.compare(wish1.getPower(), wish2.getPower());
                            break;
                        }
                        case CardDbAdapter.KEY_TOUGHNESS: {
                            retVal = Float.compare(wish1.getToughness(), wish2.getToughness());
                            break;
                        }
                        case CardDbAdapter.KEY_SET: {
                            retVal = wish1.getFirstExpansion().compareTo(wish2.getFirstExpansion());
                            break;
                        }
                        case SortOrderDialogFragment.KEY_PRICE: {
                            retVal = Double.compare(wish1.getTotalPrice(mPriceSetting), wish2.getTotalPrice(mPriceSetting));
                            break;
                        }
                        case SortOrderDialogFragment.KEY_ORDER: {
                            retVal = Integer.compare(wish1.getIndex(), wish2.getIndex());
                            break;
                        }
                        case CardDbAdapter.KEY_RARITY: {
                            retVal = Character.compare(wish1.getHighestRarity(), wish2.getHighestRarity());
                            break;
                        }
                    }
                } catch (NullPointerException e) {
                    retVal = 0;
                }

                /* Adjust for ascending / descending */
                if (!option.getAscending()) {
                    retVal = -retVal;
                }

                /* If these two entries aren't equal, return. Otherwise continue and compare the
                 * next value
                 */
                if (retVal != 0) {
                    return retVal;
                }
            }

            /* Guess they're totally equal */
            return retVal;
        }
    }

}
