package com.gelakinetic.mtgfam.helpers;

import android.os.Build;
import android.util.Pair;
import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class DeckStatsGenerator {
    private List<MtgCard> mDeckToStat;
    private Map<String, Integer> typeStats;
    private Map<String, Integer> colorStats;
    private Map<Integer, Integer> cmcStats;

    @RequiresApi(api = Build.VERSION_CODES.N)
    DeckStatsGenerator(List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        runStats();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void runStats() {
        typeStats = null;
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
        for (MtgCard card : mDeckToStat) {
            colorStats.computeIfPresent(card.getColor(), (k, v) -> (v++));
            cmcStats.computeIfPresent(Math.min(card.getCmc(), 7), (k, v) -> (v++));
        }
    }
}
