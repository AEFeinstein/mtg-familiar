package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.SampleHandMaker;

import java.util.List;

public class SampleHandFrag extends FamiliarFragment{
    private final List<MtgCard> mDeck;
    private List<MtgCard> mHand;
    private int numOfMulls = 0;

    public SampleHandFrag(List<MtgCard> mDeck) {
        this.mDeck = mDeck;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.samplehand_frag, container, false);
        assert myFragmentView != null;
        mHand = SampleHandMaker.drawSampleHand(mDeck);
        return myFragmentView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.samplehand_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.new_hand) {
            mHand = SampleHandMaker.drawSampleHand(mDeck);
            numOfMulls = 0;
            return true;
        } else if (item.getItemId() == R.id.mulligan) {
            numOfMulls++;
            mHand = SampleHandMaker.drawSampleHand(mDeck, numOfMulls);
            return true;
        }
        return false;
    }
}
