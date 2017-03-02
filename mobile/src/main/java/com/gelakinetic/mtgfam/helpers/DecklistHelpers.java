package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by bmaurer on 2/28/2017.
 */

public class DecklistHelpers {

    private static final String DECKLIST_NAME = "card.deck";

    public static void WriteCompressedDecklist(Context mCtx, ArrayList<CompressedDecklistInfo> mCompressedDecklist) {
        try {
            FileOutputStream fos = mCtx.openFileOutput(DECKLIST_NAME, Context.MODE_PRIVATE);
            for (CompressedDecklistInfo cdi : mCompressedDecklist) {
                MtgCard card = cdi.mCard;
                for (IndividualSetInfo isi : cdi.mInfo) {
                    card.set = isi.mSet;
                    card.setCode = isi.mSetCode;
                    card.number = isi.mNumber;
                    card.foil = isi.mIsFoil;
                    card.numberOf = isi.mNumberOf;
                    String cardString = card.toWishlistString();
                    if (cdi.mIsSideboard) {
                        cardString += " sb";
                    }
                    fos.write(cardString.getBytes());
                }
            }
            fos.close();
        } catch (IOException ioe) {
            ToastWrapper.makeText(mCtx, ioe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        }
    }

    public static ArrayList<Pair<MtgCard, Boolean>> ReadDecklist(Context mCtx) {
        ArrayList<Pair<MtgCard, Boolean>> lDecklist = new ArrayList<>();
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(DECKLIST_NAME)));
            boolean isSideboard;
            while ((line = br.readLine()) != null) {
                isSideboard = false;
                String sideboard = line.substring(Math.max(line.length() - 3, 0));
                if (sideboard.equals(" sb")) {
                    isSideboard = true;
                }
                line = line.substring(0, Math.max(line.length() - 4, 0));
                lDecklist.add(new Pair<MtgCard, Boolean>(MtgCard.fromWishlistString(line, mCtx), isSideboard));
            }
        } catch (NumberFormatException nfe) {
            ToastWrapper.makeText(mCtx, nfe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        } catch (IOException ioe) {
            /* Catches file not found exception when decklist doesn't exist */
        }
        return lDecklist;
    }

    public static ArrayList<CompressedDecklistInfo> ReadDecklist2(Context mCtx) {
        ArrayList<CompressedDecklistInfo> lDecklist = new ArrayList<>();
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(DECKLIST_NAME)));
            boolean isSideboard;
            while ((line = br.readLine()) != null) {
                isSideboard = false;
                String sideboard = line.substring(Math.max(line.length() - 3, 0));
                if (sideboard.equals(" sb")) {
                    isSideboard = true;
                }
                line = line.substring(0, Math.max(line.length() - 4, 0));
                lDecklist.add(new CompressedDecklistInfo(MtgCard.fromWishlistString(line, mCtx), isSideboard));
            }
        } catch (NumberFormatException nfe) {
            ToastWrapper.makeText(mCtx, nfe.getLocalizedMessage(), ToastWrapper.LENGTH_LONG).show();
        } catch (IOException ioe) {
            /* Catches file not found exception when decklist doesn't exist */
        }
        return lDecklist;
    }

    public static class CompressedDecklistInfo implements CompressedCardInfo {

        public final MtgCard mCard;
        public final ArrayList<IndividualSetInfo> mInfo;
        public final boolean mIsSideboard;

        public CompressedDecklistInfo(MtgCard card, boolean isSideboard) {
            mInfo = new ArrayList<>();
            mCard = card;
            mIsSideboard = isSideboard;
            add(mCard);
        }

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

        public void clearCompressedInfo() {
            mInfo.clear();
        }

        public MtgCard getCard() {
            return mCard;
        }

        public ArrayList<IndividualSetInfo> getSetInfo() {
            return mInfo;
        }

        public int getTotalNumber() {
            int totalCopies = 0;
            for (IndividualSetInfo isi : mInfo) {
                totalCopies += isi.mNumberOf;
            }
            return totalCopies;
        }

    }

    public static class DecklistComparator implements Comparator<CompressedDecklistInfo> {
        @Override
        public int compare(CompressedDecklistInfo o1, CompressedDecklistInfo o2) {
            int cmcCompare;
            if (o1.mIsSideboard && !o2.mIsSideboard) {
                return 1;
            } else if (!o1.mIsSideboard && o2.mIsSideboard) {
                return -1;
            } else {
                if (o1.mCard.type.contains("Creature") && !o2.mCard.type.contains("Creature")) {
                    return -1;
                } else if (o1.mCard.type.contains("Creature") && o2.mCard.type.contains("Creature")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return compareName(o1.mCard.name.compareTo(o2.mCard.name));
                    }
                    return cmcCompare;
                } else if (o1.mCard.type.contains("Planeswalker") && !o2.mCard.type.contains("Planeswalker")) {
                    if (o2.mCard.type.contains("Creature")) {
                        return 1;
                    }
                    return -1;
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && !(o2.mCard.type.contains("Instant") && o2.mCard.type.contains("Sorcery"))) {
                    if (o2.mCard.type.contains("Creature") || o2.mCard.type.contains("Planeswalker")) {
                        return 1;
                    }
                    return -1;
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && (o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return compareName(o1.mCard.name.compareTo(o2.mCard.name));
                    }
                    return cmcCompare;
                } else if (o1.mCard.type.contains("Artifact") && !o2.mCard.type.contains("Artifact")) {
                    if (o2.mCard.type.contains("Creature") || o2.mCard.type.contains("Planeswalker") || o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery")) {
                        return 1;
                    }
                    return -1;
                } else if (o1.mCard.type.contains("Artifact") && o2.mCard.type.contains("Artifact")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return compareName(o1.mCard.name.compareTo(o2.mCard.name));
                    }
                } else if (o1.mCard.type.contains("Enchantment") && !o2.mCard.type.contains("Enchantment")) {
                    if (o2.mCard.type.contains("Creature") || o2.mCard.type.contains("Planeswalker") || o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery") || o2.mCard.type.contains("Artifact")) {
                        return 1;
                    }
                    return -1;
                } else if (o1.mCard.type.contains("Enchantment") && o2.mCard.type.contains("Enchantment")) {
                    cmcCompare = compareCMC(o1.mCard.cmc, o2.mCard.cmc);
                    if (cmcCompare == 0) {
                        return compareName(o1.mCard.name.compareTo(o2.mCard.name));
                    }
                    return cmcCompare;
                } else {
                    if (o2.mCard.type.contains("Creature") || o2.mCard.type.contains("Planeswalker") || o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery") || o2.mCard.type.contains("Artifact") || o2.mCard.type.contains("Enchantment")) {
                        return 1;
                    }
                    return compareName(o1.mCard.name.compareTo(o2.mCard.name));
                }
            }
            return 0;
        }

        private int compareCMC(int cmcValue1, int cmcValue2) {
            if (cmcValue1 < cmcValue2) {
                return -1;
            } else if (cmcValue1 > cmcValue2) {
                return 1;
            }
            return 0;
        }

        private int compareName(int nameValue) {
            if (nameValue > 0) {
                return 1;
            } else if (nameValue < 0) {
                return -1;
            }
            return 0;
        }
    };

}
