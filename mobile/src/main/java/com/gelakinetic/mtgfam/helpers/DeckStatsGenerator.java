package com.gelakinetic.mtgfam.helpers;

import android.os.Build;
import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DeckStatsGenerator {
    private List<MtgCard> mDeckToStat;
    private float mDeckSize; //Defined as a Float to avoid integer division
    private Map<String, Float> typeStats;
    private Map<String, Float> colorStats;
    private Map<Integer, Integer> cmcStats;
    private static final Pattern mTypePattern = Pattern.compile("(Land|Creature|Planeswalker|Instant|Sorcery|Artifact|Enchantment)");

    public DeckStatsGenerator(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        mDeckSize = (float) mDeckToStat.size();
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
            if (!card.getColorIdentity().isEmpty()) {
                for (String color : card.getColorIdentity().split("")) {
                    colorStats.computeIfPresent(color, (k, v) -> (v + 1));
                }
            } else {
                //Catch colorless
                colorStats.computeIfPresent(card.getColorIdentity(), (k, v) -> (v + 1));
            }
            cmcStats.computeIfPresent(Math.min(card.getCmc(), 7), (k, v) -> (v + 1));
            Matcher match = mTypePattern.matcher(card.getType());
            //Must have at least 1 type
            match.find();
            do {
                typeStats.computeIfPresent(match.group(0), (k, v) -> (v + 1));
            } //Can have more than 1 type
            while (match.find());
        }

        for (String type : typeStats.keySet()) {
            typeStats.computeIfPresent(type, (k, v) -> (v / mDeckSize));
        }
        for (String color : colorStats.keySet()) {
            typeStats.computeIfPresent(color, (k, v) -> (v / mDeckSize));
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
}
