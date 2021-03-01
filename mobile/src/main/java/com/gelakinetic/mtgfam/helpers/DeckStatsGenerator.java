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
    private Map<String, Integer> typeStats;
    private Map<String, Integer> colorStats;
    private Map<Integer, Integer> cmcStats;
    private static final Pattern mTypePattern = Pattern.compile("(Land|Creature|Planeswalker|Instant|Sorcery|Artifact|Enchantment)");

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeckStatsGenerator(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        runStats();
    }
    private void resetStats() {
        typeStats = new HashMap<>();
        typeStats.put("Creature", 0);
        typeStats.put("Planeswalker", 0);
        typeStats.put("Instant", 0);
        typeStats.put("Sorcery", 0);
        typeStats.put("Artifact", 0);
        typeStats.put("Enchantment", 0);
        typeStats.put("Land", 0);
        colorStats = new HashMap<>();
        colorStats.put("W", 0);
        colorStats.put("U", 0);
        colorStats.put("B", 0);
        colorStats.put("R", 0);
        colorStats.put("G", 0);
        colorStats.put("", 0);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Map[] runStats() {
        resetStats();
        for (MtgCard card : mDeckToStat) {
            colorStats.computeIfPresent(card.getColor(), (k, v) -> (v++));
            cmcStats.computeIfPresent(Math.min(card.getCmc(), 7), (k, v) -> (v++));
            Matcher match = mTypePattern.matcher(card.getType());
            //Must have at least 1 type
            match.find();
            do {
                typeStats.computeIfPresent(match.group(0), (k, v) -> (v++));
            } //Can have more than 1 type
            while (match.find());
        }
        return new Map[]{typeStats, colorStats, cmcStats};
    }
}
