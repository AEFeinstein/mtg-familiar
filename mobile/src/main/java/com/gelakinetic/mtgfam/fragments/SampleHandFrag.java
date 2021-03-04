package com.gelakinetic.mtgfam.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.R;

public class SampleHandFrag extends FamiliarFragment{
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.samplehand_frag, container, false);
        assert myFragmentView != null;
        return myFragmentView;
    }
}
