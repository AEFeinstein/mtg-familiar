package com.gelakinetic.mtgfam.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.gelakinetic.mtgfam.helpers.DeckStatsGenerator;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DeckStatsFragment extends FamiliarFragment {
    private DeckStatsGenerator mStatGenerator;
    private List<MtgCard> mDeckToStat;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeckStatsFragment (List<MtgCard> mDeckToStat) {
        this.mDeckToStat = mDeckToStat;
        mStatGenerator = new DeckStatsGenerator(mDeckToStat);
    }

    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }
}
