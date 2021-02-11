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
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers.CompressedWishlistInfo;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class has helpers for reading, writing, and modifying the decklist from different fragments.
 */
public class DecklistHelpers {

    /**
     * Write the decklist passed as a parameter to the decklist file.
     *
     * @param activity  A context to open the file and pop toasts with
     * @param lDecklist The decklist to write to the file
     * @param fileName  The name of the file to write the decklist to
     */
    public static void WriteDecklist(
            Activity activity,
            ArrayList<MtgCard> lDecklist,
            String fileName) {

        try {
            fileName = sanitizeFilename(fileName);
            FileOutputStream fos = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
            for (MtgCard m : lDecklist) {
                String cardString = m.toWishlistString();
                /* If the card is a sideboard card, add the marking */
                if (m.isSideboard()) {
                    cardString = "SB:" + cardString;
                }
                fos.write(cardString.getBytes());
            }
            fos.close();
        } catch (IOException | IllegalArgumentException ioe) {
            SnackbarWrapper.makeAndShowText(activity, ioe.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        }

    }

    /**
     * Write the decklist passed as a parameter to the given filename.
     *
     * @param activity           A context to open the file and pop toasts with
     * @param compressedDecklist The decklist to write to the file
     * @param fileName           the filename for the decklist
     */
    public static void WriteCompressedDecklist(
            Activity activity,
            List<CompressedDecklistInfo> compressedDecklist,
            String fileName) {

        if (null == activity) {
            return;
        }
        try {

            final String newFileName = sanitizeFilename(fileName);
            FileOutputStream fos = activity.openFileOutput(newFileName, Context.MODE_PRIVATE);

            /* For each compressed card, make an MtgCard and write it to the default decklist */
            for (CompressedDecklistInfo cdi : compressedDecklist) {
                if (cdi.getName() != null && !cdi.getName().isEmpty()) {
                    for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                        cdi.applyIndividualInfo(isi);
                        String cardString = cdi.toWishlistString();
                        /* If the card is a sideboard card, add the sideboard marking */
                        if (cdi.mIsSideboard) {
                            cardString = "SB:" + cardString;
                        }
                        fos.write(cardString.getBytes());
                    }
                }
            }
            fos.close();
        } catch (IOException ioe) {
            SnackbarWrapper.makeAndShowText(activity, ioe.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        }

    }

    /**
     * Read the decklist from a file and return it as an ArrayList<Pair<MtgCard, Boolean>>.
     *
     * @param activity     A context to open the file and pop toasts with
     * @param deckName     the name of the deck to load
     * @param loadFullData true to load all card data from the database, false to to just read the file
     * @return The decklist as an ArrayList of MtgCards
     */
    public static ArrayList<MtgCard> ReadDecklist(Activity activity, String deckName, boolean loadFullData) throws FamiliarDbException {

        ArrayList<MtgCard> lDecklist = new ArrayList<>();

        try {
            String line;
            // Sanitize the deckname before loading in case it was saved improperly on an earlier version of Familiar
            deckName = sanitizeFilename(deckName);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(activity.openFileInput(deckName)))) {
                boolean isSideboard;
                /* Read each line as a card, and add them to the ArrayList */
                while ((line = br.readLine()) != null) {
                    isSideboard = false;
                    String sideboard = line.substring(0, 3);
                    // If the card has the markings of a sideboard card,
                    // mark it as such and remove the mark
                    if (sideboard.equals("SB:")) {
                        isSideboard = true;
                        line = line.substring(3);
                    }
                    MtgCard card = MtgCard.fromWishlistString(line, isSideboard, activity);
                    if (null != card) {
                        lDecklist.add(card);
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            SnackbarWrapper.makeAndShowText(activity, nfe.getLocalizedMessage(), SnackbarWrapper.LENGTH_LONG);
        } catch (IOException ioe) {
            /* Catches file not found exception when decklist doesn't exist */
        }

        if (loadFullData && !lDecklist.isEmpty()) {
            MtgCard.initCardListFromDb(activity, lDecklist);
        }
        return lDecklist;

    }

    private static String sanitizeFilename(String deckName) {
        return deckName.replaceAll("(\\s)", "_").replaceAll("[^\\w.-]", "_");
    }

    public static String getSharableDecklist(
            List<CompressedDecklistInfo> compressedDecklist,
            Context ctx) {

        StringBuilder readableDecklist = new StringBuilder();

        for (CompressedDecklistInfo cdi : compressedDecklist) {
            if (null != cdi.header) {
                readableDecklist.append(cdi.header).append("\r\n");
            } else {
                for (IndividualSetInfo isi : cdi.mInfo) {
                    if (cdi.mIsSideboard) {
                        readableDecklist.append("SB: ");
                    }
                    readableDecklist
                            .append(isi.mNumberOf)
                            .append(' ')
                            .append(cdi.getName());
                    if (isi.mIsFoil) {
                        readableDecklist
                                .append(" (")
                                .append(ctx.getString(R.string.wishlist_foil))
                                .append(")");
                    }
                    readableDecklist.append(" ").append(isi.mSetCode).append("\r\n");
                }
            }
        }
        return readableDecklist.toString();

    }

    public static Pair<Map<String, String>, Map<String, String>> getTargetNumberOfs(
            String mCardName,
            ArrayList<MtgCard> decklist,
            boolean isSideboard) {

        final Map<String, String> targetCardNumberOfs = new HashMap<>();
        final Map<String, String> targetFoilNumberOfs = new HashMap<>();

        for (MtgCard card : decklist) {
            if (card.getName().equals(mCardName) && card.isSideboard() == isSideboard) {
                if (card.mIsFoil) {
                    targetFoilNumberOfs.put(card.getExpansion(),
                            String.valueOf(card.mNumberOf));
                    continue;
                }
                targetCardNumberOfs.put(card.getExpansion(), String.valueOf(card.mNumberOf));
            }
        }
        return new Pair<>(targetCardNumberOfs, targetFoilNumberOfs);

    }

    /**
     * This class encapsulates a single MtgCard, an ArrayList of non-duplicated information for
     * different printings of that card, and if the card is part of the sideboard.
     */
    public static class CompressedDecklistInfo extends CardHelpers.CompressedCardInfo {

        public final boolean mIsSideboard;
        public String header;

        /**
         * Constructor.
         *
         * @param card        The MtgCard which will be the base for this object
         * @param isSideboard If the card is part of the sideboard or not
         */
        public CompressedDecklistInfo(MtgCard card, boolean isSideboard) {
            super(card);
            mIsSideboard = isSideboard;
        }

        public CompressedWishlistInfo convertToWishlist() {
            final CompressedWishlistInfo wishlist = new CompressedWishlistInfo(this, 0);
            wishlist.mInfo.clear();
            wishlist.mInfo.addAll(this.mInfo);
            return wishlist;
        }

        /**
         * Check to see if two CompressedDecklistInfo objects are equivalent, or if this is
         * equivalent to an MtgCard object. The comparison is done based on the MtgCard's name.
         *
         * @param o The object to compare to this one
         * @return true if the specified object is equal to this string, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof CompressedDecklistInfo) {
                final CompressedDecklistInfo cdi = (CompressedDecklistInfo) o;
                /* Are the headers equal? */
                return (header != null && !header.isEmpty() &&
                        header.equals(cdi.header)) ||
                        (mName != null && !mName.isEmpty() &&
                                mName.equals(cdi.mName) && (mIsSideboard == cdi.mIsSideboard));
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            int hash = 23;
            hash = hash * 31 + super.hashCode();
            return hash * 31 + ((Boolean) mIsSideboard).hashCode();
        }

    }

}
