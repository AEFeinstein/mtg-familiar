package com.gelakinetic.mtgfam.helpers;

import android.os.Build;
import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckStatsGenerator {
    private List<MtgCard> mDeckToStat;
    private Float mDeckSize;
    private Map<String, Float> typeStats;
    private Map<String, Float> colorStats;
    private Map<Integer, Float> cmcStats;
    private static final Pattern mTypePattern = Pattern.compile("(Land|Creature|Planeswalker|Instant|Sorcery|Artifact|Enchantment)");

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeckStatsGenerator(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        mDeckSize = (float) mDeckToStat.size();
        runStats();
    }
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
        cmcStats.put(0, (float) 0);
        cmcStats.put(1, (float) 0);
        cmcStats.put(2, (float) 0);
        cmcStats.put(3, (float) 0);
        cmcStats.put(4, (float) 0);
        cmcStats.put(5, (float) 0);
        cmcStats.put(6, (float) 0);
        cmcStats.put(7, (float) 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void runStats() {
        resetStats();
        for (MtgCard card : mDeckToStat) {
            if (!card.getColor().isEmpty()) {
                for (String color : card.getColor().split("//B", 5)) {
                    colorStats.computeIfPresent(color, (k, v) -> (v + 1));
                }
            } else {
                //Catch colorless
                colorStats.computeIfPresent(card.getColor(), (k, v) -> (v + 1));
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
        for (Integer cmc : cmcStats.keySet()) {
            cmcStats.computeIfPresent(cmc, (k, v) -> (v / mDeckSize));
        }
    }

    public Map<String, Float> getTypeStats() {
        return typeStats;
    }
    public Map<String, Float> getColorStats() {
        return colorStats;
    }
    public Map<Integer, Float> getCmcStats() {
        return cmcStats;
    }
}
