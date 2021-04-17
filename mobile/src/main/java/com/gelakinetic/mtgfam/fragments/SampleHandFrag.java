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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class SampleHandFrag extends FamiliarFragment{
    private final List<MtgCard> mDeck;
    private List<MtgCard> mHand;
    private int numOfMulls = 0;
    /* UI Elements */
    private ListView mListView;

    /**
     *
     * @param mDeck The deck to make sample hands from
     */
    public SampleHandFrag(List<MtgCard> mDeck) {
        this.mDeck = mDeck;
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
        mHand = SampleHandMaker.drawSampleHand(mDeck);
        mListView = myFragmentView.findViewById(R.id.hand_list);
        fillData();
        return myFragmentView;
    }

    /**
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.samplehand_menu, menu);
    }

    /**
     * Handle an ActionBar item click.
     *
     * @param item the item clicked
     * @return true if the click was acted on
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.new_hand) {
            mHand = SampleHandMaker.drawSampleHand(mDeck);
            numOfMulls = 0;
            fillData();
            return true;
        } else if (item.getItemId() == R.id.mulligan) {
            numOfMulls++;
            mHand = SampleHandMaker.drawSampleHand(mDeck, numOfMulls);
            fillData();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function fills mListView with the info in mCursor using a ResultListAdapter
     */
    private void fillData() {
        int handSize = mHand.size();
        if (handSize > 0) {
            long handId;
            Cursor mCursor = null;
            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, handle);
                Cursor[] handCursors = new Cursor[handSize];
                for (int i = 0; i < handSize; i++) {
                    handId = CardDbAdapter.fetchIdByName(mHand.get(i).getName(), database);
                    handCursors[i] = CardDbAdapter.fetchCards(new long[]{handId}, null, database);
                }
                mCursor = new MergeCursor(handCursors);
            } catch (FamiliarDbException e) {
                handleFamiliarDbException(false);
            }
            if (mCursor != null) {
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
            }
        } else {
            ResultListAdapter rla = new ResultListAdapter(getActivity(), null, null, null);
            mListView.setAdapter(rla);
        }
    }
}
