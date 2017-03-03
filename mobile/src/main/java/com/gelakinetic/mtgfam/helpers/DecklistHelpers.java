package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.util.Pair;

import com.gelakinetic.mtgfam.R;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * This class has helpers for reading, writing, and modifying the decklist from different fragments
 */
public class DecklistHelpers {

    public static final String DECKLIST_NAME = "autosave.fDeck";

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
            fileName = fileName.replaceAll("(\\s)", "_"); // Unix doesn't like spaces in the file name
            FileOutputStream fos = mCtx.openFileOutput(fileName, Context.MODE_PRIVATE);
            /* For each compressed card, make an MtgCard and write it to the default decklist */
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                MtgCard card = cdi.mCard;
                for (IndividualSetInfo isi : cdi.mInfo) {
                    card.set = isi.mSet;
                    card.setCode = isi.mSetCode;
                    card.number = isi.mNumber;
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
        WriteCompressedDecklist(mCtx, mCompressedDecklist, DECKLIST_NAME);
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
                        .append(cdi.mCard.name);
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

    /**
     * This class encapsulates a single MtgCard, an ArrayList of non-duplicated information for
     * different printings of that card, and if the card is part of the sideboard
     */
    public static class CompressedDecklistInfo extends CompressedCardInfo {

        public final boolean mIsSideboard;

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
                return (mCard.name.equals(cdi.mCard.name) && mIsSideboard == cdi.mIsSideboard);
            } else if (o instanceof MtgCard) {
                return mCard.name.equals(((MtgCard) o).name);
            }
            return false;
        }

    }

    /**
     * Comparator based on all factors. First it checks if the card is in the sideboard, if so it is
     * it is greater. If it isn't, it checks the type of the card in the following order: Creature,
     * Planeswalker, Instant or Sorcery, Artifact, Enchantment, and finally land. If both objects
     * have the same type, it then compares the CMC. If the CMC is the same, finally it compares the
     * card name. If both cards are in the sideboard, it simply compares the CMC, then the names.
     */
    public static class DecklistComparator implements Comparator<CompressedDecklistInfo> {
        @Override
        public int compare(CompressedDecklistInfo o1, CompressedDecklistInfo o2) {
            int cmcCompare;
            if (o1.mIsSideboard && !o2.mIsSideboard) {
                return 1;
            } else if (!o1.mIsSideboard && o2.mIsSideboard) {
                return -1;
            } else if (!o1.mIsSideboard && !o2.mIsSideboard) { // I'm keeping the second value for clarity's sake
                // Are we creatures?
                if (o1.mCard.type.contains("Creature") && !o2.mCard.type.contains("Creature")) {
                    return -1;
                } else if (!o1.mCard.type.contains("Creature") && o2.mCard.type.contains("Creature")) {
                    return 1;
                } else if (o1.mCard.type.contains("Creature") && o2.mCard.type.contains("Creature")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return o1.mCard.name.compareTo(o2.mCard.name);
                    }
                    return cmcCompare;
                // Are we Planeswalkers?
                } else if (o1.mCard.type.contains("Planeswalker") && !o2.mCard.type.contains("Planeswalker")) {
                    return -1;
                } else if (!o1.mCard.type.contains("Planeswalker") && o2.mCard.type.contains("Planeswalker")) {
                    return 1;
                } else if (o1.mCard.type.contains("Planeswalker") && o2.mCard.type.contains("Planeswalker")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return o1.mCard.name.compareTo(o2.mCard.name);
                    }
                    return cmcCompare;
                // Are we an instant or a sorcery?
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && !(o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    return -1;
                } else if (!(o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && (o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    return 1;
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && (o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return o1.mCard.name.compareTo(o2.mCard.name);
                    }
                    return cmcCompare;
                // Are we an artifact?
                } else if (o1.mCard.type.contains("Artifact") && !o2.mCard.type.contains("Artifact")) {
                    return -1;
                } else if (!o1.mCard.type.contains("Artifact") && o2.mCard.type.contains("Artifact")) {
                    return 1;
                } else if (o1.mCard.type.contains("Artifact") && o2.mCard.type.contains("Artifact")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return o1.mCard.name.compareTo(o2.mCard.name);
                    }
                    return cmcCompare;
                // Are we an enchantment?
                } else if (o1.mCard.type.contains("Enchantment") && !o2.mCard.type.contains("Enchantment")) {
                    return -1;
                } else if (!o1.mCard.type.contains("Enchantment") && o2.mCard.type.contains("Enchantment")) {
                    return 1;
                } else if (o1.mCard.type.contains("Enchantment") && o2.mCard.type.contains("Enchantment")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return o1.mCard.name.compareTo(o2.mCard.name);
                    }
                    return cmcCompare;
                // If not, we must be a land, and lands are all CMC 0.
                } else {
                    return o1.mCard.name.compareTo(o2.mCard.name);
                }
            } else {
                cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                if (cmcCompare == 0) {
                    return o1.mCard.name.compareTo(o2.mCard.name);
                }
                return cmcCompare;
            }
        }

        /**
         * Compare the CMC of two cards
         * @param cmcValue1 the CMC of the first card
         * @param cmcValue2 the CMC of the second card
         * @return -1 if cmcValue2 is greater, 1 if cmcValue1 is greater, otherwise 0
         */
        private int compareCMC(int cmcValue1, int cmcValue2) {
            if (cmcValue1 < cmcValue2) {
                return -1;
            } else if (cmcValue1 > cmcValue2) {
                return 1;
            }
            return 0;
        }

    };

}
