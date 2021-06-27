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

package com.gelakinetic.mtgfam.fragments;

import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;
import com.gelakinetic.mtgfam.helpers.SampleHandMaker;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.util.ArrayList;
import java.util.List;

public class SampleHandFrag extends FamiliarFragment {
    private List<MtgCard> mHand;
    private int numOfMulls = 0;
    private final SampleHandMaker handGen;
    /* UI Elements */
    private ListView mListView;
    private MergeCursor mCursor = null;
    private FamiliarDbHandle mDbHandle = null;

    /**
     * @param mDeck The deck to make sample hands from
     */
    public SampleHandFrag(List<MtgCard> mDeck) {
        handGen = new SampleHandMaker(mDeck);
    }

    /**
     * Create the view, pull out UI elements, and set up the listener for the "add cards" button.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in
     *                           the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should
     *                           be attached to. The fragment should not add the view itself, but
     *                           this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return The view to be displayed.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View myFragmentView =
                inflater.inflate(R.layout.samplehand_frag, container, false);
        assert myFragmentView != null;
        mHand = handGen.drawSampleHand();
        mListView = myFragmentView.findViewById(R.id.hand_list);
        fillData();
        Button drawCardButton = myFragmentView.findViewById(R.id.draw_card);
        Button newHandButton = myFragmentView.findViewById(R.id.new_hand);
        Button mullButton = myFragmentView.findViewById(R.id.mulligan);
        drawCardButton.setOnClickListener(v -> {
            mHand.addAll(handGen.drawCard());
            fillData();
        });
        newHandButton.setOnClickListener(v -> {
            mHand = handGen.drawSampleHand();
            numOfMulls = 0;
            fillData();
        });
        mullButton.setOnClickListener(v -> {
            numOfMulls++;
            mHand = handGen.drawSampleHand(numOfMulls);
            fillData();
        });
        return myFragmentView;
    }

    /**
     * Be clean with the cursor!
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mCursor != null) {
            mCursor.close();
        }
        try {
            DatabaseManager.closeDatabase(getActivity(), mDbHandle);
        } catch (NullPointerException e) {
            // Eat this exception
        }
    }

    /**
     * This function fills mListView with the info in mCursor using a ResultListAdapter
     */
    private void fillData() {
        int handSize = mHand.size();
        if (handSize > 0) {
            long handId;
            // Clean up if necessary
            if (null != mDbHandle) {
                if (mCursor != null) {
                    mCursor.close();
                }
                DatabaseManager.closeDatabase(getActivity(), mDbHandle);
            }
            mDbHandle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, mDbHandle);
                Cursor[] handCursors = new Cursor[handSize];
                for (int i = 0; i < handSize; i++) {
                    handId = CardDbAdapter.fetchIdByName(mHand.get(i).getName(), database);
                    handCursors[i] = CardDbAdapter.fetchCards(new long[]{handId}, null, database);
                }
                mCursor = new MergeCursor(handCursors);
                ArrayList<String> fromList = new ArrayList<>();
                ArrayList<Integer> toList = new ArrayList<>();
                // Always get name, set, and rarity. This is for the wishlist quick add
                fromList.add(CardDbAdapter.KEY_NAME);
                toList.add(R.id.card_name);
                fromList.add(CardDbAdapter.KEY_SET);
                toList.add(R.id.cardset);
                fromList.add(CardDbAdapter.KEY_RARITY);
                toList.add(R.id.rarity);
                if (PreferenceAdapter.getManaCostPref(getContext())) {
                    fromList.add(CardDbAdapter.KEY_MANACOST);
                    toList.add(R.id.cardcost);
                }
                if (PreferenceAdapter.getTypePref(getContext())) {
                    /* This will handle both sub and super type */
                    fromList.add(CardDbAdapter.KEY_SUPERTYPE);
                    toList.add(R.id.cardtype);
                }
                if (PreferenceAdapter.getAbilityPref(getContext())) {
                    fromList.add(CardDbAdapter.KEY_ABILITY);
                    toList.add(R.id.cardability);
                }
                if (PreferenceAdapter.getPTPref(getContext())) {
                    fromList.add(CardDbAdapter.KEY_POWER);
                    toList.add(R.id.cardp);
                    fromList.add(CardDbAdapter.KEY_TOUGHNESS);
                    toList.add(R.id.cardt);
                    fromList.add(CardDbAdapter.KEY_LOYALTY);
                    toList.add(R.id.cardt);
                }

                String[] from = new String[fromList.size()];
                fromList.toArray(from);

                int[] to = new int[toList.size()];
                for (int i = 0; i < to.length; i++) {
                    to[i] = toList.get(i);
                }

                ResultListAdapter rla = new ResultListAdapter(getActivity(), mCursor, from, to);
                mListView.setAdapter(rla);
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(false);
            }
        } else {
            ResultListAdapter rla = new ResultListAdapter(getActivity(), null, null, null);
            mListView.setAdapter(rla);
        }
    }
}
