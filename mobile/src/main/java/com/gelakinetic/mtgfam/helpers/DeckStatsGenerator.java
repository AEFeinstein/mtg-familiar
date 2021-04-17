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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckStatsGenerator {
    private List<MtgCard> mDeckToStat;
    private float mDeckSize; //Defined as a Float to avoid integer division
    private Map<String, Float> typeStats;
    private Map<String, Float> colorStats;
    private Map<Integer, Integer> cmcStats;
    private static final Pattern mTypePattern = Pattern.compile("(Land|Creature|Planeswalker|Instant|Sorcery|Artifact|Enchantment)");
    private static final Pattern mColorPattern = Pattern.compile("\\{([WUBRGC\\d])+?[^WUBRGC]*([WUBRGC])*\\}(?![^(]*\\))"); //Escape is not redundant, will break stuff if removed

    public DeckStatsGenerator(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        mDeckSize = (float) 0; //calculated as total physical cards
        runStats();
    }

    /**
     * Clears any previous statistics contained by this DeckStatsGenerator
     */
    private void resetStats() {
        typeStats = new HashMap<>();
        typeStats.put("Creature", (float) 0);
        typeStats.put("Planeswalker", (float) 0);
        typeStats.put("Instant", (float) 0);
        typeStats.put("Sorcery", (float) 0);
        typeStats.put("Artifact", (float) 0);
        typeStats.put("Enchantment", (float) 0);
        typeStats.put("Land", (float) 0);
        colorStats = new HashMap<>();
        colorStats.put("W", (float) 0);
        colorStats.put("U", (float) 0);
        colorStats.put("B", (float) 0);
        colorStats.put("R", (float) 0);
        colorStats.put("G", (float) 0);
        colorStats.put("C", (float) 0);
        colorStats.put("", (float) 0);
        cmcStats = new HashMap<>();
        cmcStats.put(0, 0);
        cmcStats.put(1, 0);
        cmcStats.put(2, 0);
        cmcStats.put(3, 0);
        cmcStats.put(4, 0);
        cmcStats.put(5, 0);
        cmcStats.put(6, 0);
        cmcStats.put(7, 0);
    }

    /**
     * Calculates statistics for card colors, types, and cmcs for the provided List<MtgCard>
     */
    private void runStats() {
        resetStats();
        for (MtgCard card : mDeckToStat) {
            boolean isLand = false;
            if (!card.isSideboard()) {
                Matcher typeMatcher = mTypePattern.matcher(card.getType());
                //Must have at least 1 type
                typeMatcher.find();
                do {
                    mapAddIfPresent(typeStats, typeMatcher.group(0), card.mNumberOf);
                    if (Objects.equals(typeMatcher.group(0), "Land")) {
                        isLand = true;
                    }
                } //Can have more than 1 type
                while (typeMatcher.find());

                if (!isLand) {
                    Matcher manaCostMatcher = mColorPattern.matcher(card.getManaCost());
                    Matcher rulesColorMatcher = mColorPattern.matcher(card.getText());
                    boolean hasColor = true;
                    if (manaCostMatcher.find()) {
                        do {
                            for (int i = 1; i <= manaCostMatcher.groupCount(); i++) {
                                mapAddIfPresent(colorStats, manaCostMatcher.group(i), card.mNumberOf);
                            }
                        } while (manaCostMatcher.find());
                    } else {
                        hasColor = false;
                    }
                    if (rulesColorMatcher.find()) {
                        do {
                            for (int i = 1; i <= rulesColorMatcher.groupCount(); i++) {
                                mapAddIfPresent(colorStats, rulesColorMatcher.group(i), card.mNumberOf);
                            }
                        } while (rulesColorMatcher.find());
                    } else if (!hasColor){
                        //Catch colorless
                        mapAddIfPresent(colorStats, card.getColor(), card.mNumberOf);
                    }
                    mapAddIfPresent(cmcStats, Math.min(card.getCmc(), 7), card.mNumberOf);
                    mDeckSize += card.mNumberOf;
                }
            }
        }

        for (String type : typeStats.keySet()) {
            mapDivideIfPresent(typeStats, type, mDeckSize);
        }
        for (String color : colorStats.keySet()) {
            mapDivideIfPresent(colorStats, color, mDeckSize);
        }
    }

    /**
     * Calls runStats() if no typeStats exist then returns typeStats
     * @return typeStats
     */
    public Map<String, Float> getTypeStats() {
        if (typeStats == null) {
            runStats();
        }
        return typeStats;
    }

    /**
     * Calls runStats() if no colorStats exist then returns colorStats
     * @return colorStats
     */
    public Map<String, Float> getColorStats() {
        if (colorStats == null) {
            runStats();
        }
        return colorStats;
    }

    /**
     * Calls runStats() if no cmcStats exist then returns cmcStats
     * @return cmcStats
     */
    public Map<Integer, Integer> getCmcStats() {
        if (cmcStats == null) {
            runStats();
        }
        return cmcStats;
    }

    /**
     * Replace computeIfPresent for division to keep minApi at 21
     * @param map Map to divide values from
     * @param key Key for which value to divide
     * @param divisor What to divide by
     * @param <K> Key type
     */
    private <K> void mapDivideIfPresent(Map<K, Float> map, K key, float divisor) {
        if (map.get(key) != null) {
            Float oldValue = map.get(key);
            Float newValue = oldValue / divisor;
            map.put(key, newValue);
        }
    }

    /**
     * Replace computeIfPresent for addition of Floats to keep minApi at 21
     * @param map Map to add values from
     * @param key Key for which value to add
     * @param difference What to add
     * @param <K> Key type
     */
    private <K> void mapAddIfPresent(Map<K, Float> map, K key, float difference) {
        if (map.get(key) != null) {
            Float oldValue = map.get(key);
            Float newValue = oldValue + difference;
            map.put(key, newValue);
        }
    }

    /**
     * Replace computeIfPresent for addition of Integers to keep minApi at 21
     * @param map Map to add values from
     * @param key Key for which value to add
     * @param difference What to add
     * @param <K> Key type
     */
    private <K> void mapAddIfPresent(Map<K, Integer> map, K key, int difference) {
        if (map.get(key) != null) {
            Integer oldValue = map.get(key);
            Integer newValue = oldValue + difference;
            map.put(key, newValue);
        }
    }
}
