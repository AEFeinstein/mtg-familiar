package com.gelakinetic.mtgfam.helpers;

import java.util.Comparator;

/**
 * Created by bmaurer on 2/28/2017.
 */

public class DecklistHelpers {

    public static class CompressedDecklistInfo extends WishlistHelpers.CompressedWishlistInfo {

        public boolean mIsSideboard;

        public CompressedDecklistInfo(MtgCard card, boolean isSideboard) {
            super(card);
            this.mIsSideboard = isSideboard;
            add(card);
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
