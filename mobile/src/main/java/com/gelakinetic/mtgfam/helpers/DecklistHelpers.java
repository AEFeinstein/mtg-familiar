package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.util.Pair;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers.IndividualSetInfo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class has helpers for reading, writing, and modifying the decklist from different fragments
 */
public class DecklistHelpers {

    /**
     * Write the decklist passed as a parameter to the decklist file
     * @param mCtx                A context to open the file and pop toasts with
     * @param lDecklist The decklist to write to the file
     * @param fileName
     */
    public static void WriteDecklist(Context mCtx, ArrayList<Pair<MtgCard, Boolean>> lDecklist, String fileName) {
        try {
            FileOutputStream fos = mCtx.openFileOutput(fileName, Context.MODE_PRIVATE);
            for (Pair<MtgCard, Boolean> m : lDecklist) {
                String cardString = m.first.toWishlistString();
                /* If the card is a sideboard card, add the marking */
                if (m.second) {
                    cardString = "SB:" + cardString;
                }
                fos.write(cardString.getBytes());
            }
            fos.close();
        } catch (IOException ioe) {
            ToastWrapper.makeText(mCtx, ioe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * Write the decklist passed as a parameter to the given filename
     * @param mCtx                A context to open the file and pop toasts with
     * @param mCompressedDecklist The decklist to write to the file
     * @param fileName the filename for the decklist
     */
    public static void WriteCompressedDecklist(Context mCtx, ArrayList<CompressedDecklistInfo> mCompressedDecklist, String fileName) {
        try {
            final String newFileName = fileName.replaceAll("(\\s)", "_"); // Unix doesn't like spaces in the file name
            FileOutputStream fos = mCtx.openFileOutput(newFileName, Context.MODE_PRIVATE);
            /* For each compressed card, make an MtgCard and write it to the default decklist */
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                if (cdi.mCard != null) {
                    MtgCard card = cdi.mCard;
                    for (CardHelpers.IndividualSetInfo isi : cdi.mInfo) {
                        card.mExpansion = isi.mSet;
                        card.setCode = isi.mSetCode;
                        card.mNumber = isi.mNumber;
                        card.foil = isi.mIsFoil;
                        card.numberOf = isi.mNumberOf;
                        String cardString = card.toWishlistString();
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
            ToastWrapper.makeText(mCtx, ioe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }

    /**
     * Write the decklist passed as a parameter to the autosave file
     * @param mCtx
     * @param mCompressedDecklist
     */
    public static void WriteCompressedDecklist(Context mCtx, ArrayList<CompressedDecklistInfo> mCompressedDecklist) {
        WriteCompressedDecklist(mCtx, mCompressedDecklist, DecklistFragment.AUTOSAVE_NAME + DecklistFragment.DECK_EXTENSION);
    }

    /**
     * Read the decklist from a file and return it as an ArrayList<Pair<MtgCard, Boolean>>
     * @param mCtx A context to open the file and pop toasts with
     * @return     The decklist in ArrayList<Pair> form
     */
    public static ArrayList<Pair<MtgCard, Boolean>> ReadDecklist(Context mCtx, String deckName) {
        ArrayList<Pair<MtgCard, Boolean>> lDecklist = new ArrayList<>();
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(deckName)));
            boolean isSideboard;
            /* Read each line as a card, and add them to the ArrayList */
            while ((line = br.readLine()) != null) {
                isSideboard = false;
                String sideboard = line.substring(0, 3);
                // If the card has the markings of a sideboard card, mark it as such and remove the mark
                if (sideboard.equals("SB:")) {
                    isSideboard = true;
                    line = line.substring(3);
                }
                lDecklist.add(new Pair<>(MtgCard.fromWishlistString(line, mCtx), isSideboard));
            }
        } catch (NumberFormatException nfe) {
            ToastWrapper.makeText(mCtx, nfe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        } catch (IOException ioe) {
            /* Catches file not found exception when decklist doesn't exist */
        }
        return lDecklist;
    }

    public static String GetSharableDecklist(ArrayList<CompressedDecklistInfo> mCompressedDecklist,
                                             Context ctx) {
        StringBuilder readableDecklist = new StringBuilder();
        for (CompressedDecklistInfo cdi : mCompressedDecklist) {
            for (IndividualSetInfo isi : cdi.mInfo) {
                if (cdi.mIsSideboard) {
                    readableDecklist.append("SB: ");
                }
                readableDecklist
                        .append(isi.mNumberOf)
                        .append(' ')
                        .append(cdi.mCard.mName);
                if (isi.mIsFoil) {
                    readableDecklist
                            .append(" (")
                            .append(ctx.getString(R.string.wishlist_foil))
                            .append(")");
                }
                readableDecklist.append(" ").append(isi.mSetCode).append("\r\n");
            }
        }
        return readableDecklist.toString();
    }

    public static Pair<Map<String,String>,Map<String,String>> getTargetNumberOfs(String mCardName, ArrayList<Pair<MtgCard, Boolean>> decklist) {
        final Map<String, String> targetCardNumberOfs = new HashMap<>();
        final Map<String, String> targetFoilNumberOfs = new HashMap<>();
        for (Pair<MtgCard, Boolean> card : decklist) {
            if (card.first.mName.equals(mCardName)) {
                if (card.first.foil) {
                    targetFoilNumberOfs.put(card.first.setCode, String.valueOf(card.first.numberOf));
                    continue;
                }
                targetCardNumberOfs.put(card.first.setCode, String.valueOf(card.first.numberOf));
            }
        }
        return new Pair<>(targetCardNumberOfs, targetFoilNumberOfs);
    }

    /**
     * This class encapsulates a single MtgCard, an ArrayList of non-duplicated information for
     * different printings of that card, and if the card is part of the sideboard
     */
    public static class CompressedDecklistInfo extends CardHelpers.CompressedCardInfo {

        public final boolean mIsSideboard;
        public String header;

        /**
         * Constructor
         * @param card The MtgCard which will be the base for this object
         * @param isSideboard If the card is part of the sideboard or not
         */
        public CompressedDecklistInfo(MtgCard card, boolean isSideboard) {
            super(card);
            mIsSideboard = isSideboard;
        }

        /**
         * Check to see if two CompressedDecklistInfo objects are equivalent, or if this is
         * equivalent to an MtgCard object. The comparison is done based on the MtgCard's name
         * @param o The object to compare to this one
         * @return  true if the specified object is equal to this string, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof CompressedDecklistInfo) {
                CompressedDecklistInfo cdi = (CompressedDecklistInfo) o;
                if (cdi.mCard == null || mCard == null) {
                    if (cdi.header != null && cdi.header.equals(header)) {
                        return true;
                    }
                } else {
                    return (mCard.mName.equals(cdi.mCard.mName) && mIsSideboard == cdi.mIsSideboard);
                }
            } else if (o instanceof MtgCard
                    && mCard != null) {
                return mCard.mName.equals(((MtgCard) o).mName);
            }
            return false;
        }

    }

}
