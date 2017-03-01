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
            /*
            Creature
            Spells (Instant/Sorcery)
            Artifact
            Enchantment
            Land
             */
            int nameCompare = 0;
            if (o1.mIsSideboard && !o2.mIsSideboard) { // >
                return 1;
            } else if (!o1.mIsSideboard && o2.mIsSideboard) { // <
                return -1;
            } else {
                if (o1.mCard.type.contains("Creature") && !o2.mCard.type.contains("Creature")) {
                    return -1;
                } else if (o1.mCard.type.contains("Creature") && o2.mCard.type.contains("Creature")) {
                    if (o1.mCard.cmc < o2.mCard.cmc) {
                        return -1;
                    } else if (o1.mCard.cmc > o2.mCard.cmc) {
                        return 1;
                    } else {
                        nameCompare = o1.mCard.name.compareTo(o2.mCard.name);
                        if (nameCompare > 0) {
                            return 1;
                        } else if (nameCompare < 0) {
                            return -1;
                        }
                    }
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && !(o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    return -1;
                } else if ((o1.mCard.type.contains("Instant") || o1.mCard.type.contains("Sorcery")) && (o2.mCard.type.contains("Instant") || o2.mCard.type.contains("Sorcery"))) {
                    if (o1.mCard.cmc < o2.mCard.cmc) {
                        return -1;
                    } else if (o1.mCard.cmc > o2.mCard.cmc) {
                        return 1;
                    } else {
                        nameCompare = o1.mCard.name.compareTo(o2.mCard.name);
                        if (nameCompare > 0) {
                            return 1;
                        } else if (nameCompare < 0) {
                            return -1;
                        }
                    }
                } else if (o1.mCard.type.contains("Artifact") && !o2.mCard.type.contains("Artifact")) {
                    return -1;
                } else if (o1.mCard.type.contains("Artifact") && o2.mCard.type.contains("Artifact")) {
                    if (o1.mCard.cmc < o2.mCard.cmc) {
                        return -1;
                    } else if (o1.mCard.cmc > o2.mCard.cmc) {
                        return 1;
                    } else {
                        nameCompare = o1.mCard.name.compareTo(o2.mCard.name);
                        if (nameCompare > 0) {
                            return 1;
                        } else if (nameCompare < 0) {
                            return -1;
                        }
                    }
                } else if (o1.mCard.type.contains("Enchantment") && !o2.mCard.type.contains("Enchantment")) {
                    return -1;
                } else if (o1.mCard.type.contains("Enchantment") && o2.mCard.type.contains("Enchantment")) {
                    if (o1.mCard.cmc < o2.mCard.cmc) {
                        return -1;
                    } else if (o1.mCard.cmc > o2.mCard.cmc) {
                        return 1;
                    } else {
                        nameCompare = o1.mCard.name.compareTo(o2.mCard.name);
                        if (nameCompare > 0) {
                            return -1;
                        } else if (nameCompare < 0) {
                            return 1;
                        }
                    }
                } else {
                    nameCompare = o1.mCard.name.compareTo(o2.mCard.name);
                    if (nameCompare > 0) {
                        return 1;
                    } else if (nameCompare < 0) {
                        return -1;
                    }
                }
            }
            return 0;
        }
    };

}
