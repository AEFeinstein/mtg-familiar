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
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.MoJhoStoDialogFragment;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.util.Collections;
import java.util.Random;

/**
 * This fragment helps user play Momir Stonehewer Jhoira basic, a variant format usually found online. It contains
 * the rules, as well as ways to pick random cards
 */
public class MoJhoStoFragment extends FamiliarFragment {

    /* Type constants */
    private static final String EQUIPMENT = "equipment";
    private static final String CREATURE = "creature";
    private static final String INSTANT = "instant";
    private static final String SORCERY = "sorcery";

    /* UI Elements */
    private Spinner mMomirCmcChoice;
    private Spinner mStonehewerCmcChoice;

    /* It has to be random somehow, right? */
    private Random mRandom;

    /**
     * Create the view, set up the ImageView clicks to see the full Vanguards, set up the buttons to display random
     * cards, save the spinners to figure out CMCs later.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The created view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRandom = new Random(System.currentTimeMillis());

        View myFragmentView = inflater.inflate(R.layout.mojhosto_frag, container, false);

        assert myFragmentView != null;

        /* Add listeners to the portraits to show the full Vanguards */
        myFragmentView.findViewById(R.id.imageViewMo).setOnClickListener(v -> showDialog(MoJhoStoDialogFragment.DIALOG_MOMIR));

        myFragmentView.findViewById(R.id.imageViewSto).setOnClickListener(v -> showDialog(MoJhoStoDialogFragment.DIALOG_STONEHEWER));

        myFragmentView.findViewById(R.id.imageViewJho).setOnClickListener(v -> showDialog(MoJhoStoDialogFragment.DIALOG_JHOIRA));

        /* Add the listeners to the buttons to display random cards */
        myFragmentView.findViewById(R.id.momir_button).setOnClickListener(v -> {
            try {
                int cmc = Integer.parseInt((String) mMomirCmcChoice.getSelectedItem());
                getOneSpell(CREATURE, cmc);
            } catch (NumberFormatException e) {
                /* eat it */
            }
        });

        myFragmentView.findViewById(R.id.stonehewer_button).setOnClickListener(v -> {
            try {
                int cmc = Integer.parseInt((String) mStonehewerCmcChoice.getSelectedItem());
                getOneSpell(EQUIPMENT, cmc);
            } catch (NumberFormatException e) {
                /* eat it */
            }
        });

        myFragmentView.findViewById(R.id.jhorira_instant_button).setOnClickListener(v -> getThreeSpells(INSTANT));

        myFragmentView.findViewById(R.id.jhorira_sorcery_button).setOnClickListener(v -> getThreeSpells(SORCERY));

        /* Save the spinners to pull out the CMCs later */
        mMomirCmcChoice = myFragmentView.findViewById(R.id.momir_spinner);
        mStonehewerCmcChoice = myFragmentView.findViewById(R.id.stonehewer_spinner);

        /* Return the view */
        return myFragmentView;
    }

    /**
     * Check if the rules should be automatically displayed. Must be done in onResume(), otherwise the dialog won't show
     * because the fragment is not yet visible
     */
    @Override
    public void onResume() {
        super.onResume();
        if (PreferenceAdapter.getMojhostoFirstTime(getContext())) {
            showDialog(MoJhoStoDialogFragment.DIALOG_RULES);
            PreferenceAdapter.setMojhostoFirstTime(getContext());
        }
    }

    /**
     * Inflate the menu, has an option to display the rules
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mojhosto_menu, menu);
    }

    /**
     * Handle a menu item click, in this case, it's only to show the rules dialog
     *
     * @param item The item selected
     * @return True if the click was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle item selection */
        if (item.getItemId() == R.id.random_rules) {
            showDialog(MoJhoStoDialogFragment.DIALOG_RULES);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Remove any showing dialogs, and show the requested one
     */
    private void showDialog(int id) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        MoJhoStoDialogFragment newFragment = new MoJhoStoDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Convenience method to fetch a random card of the given supertype and cmc, and start a CardViewPagerFragment to
     * display said card
     *
     * @param type The supertype of the card to randomly fetch
     * @param cmc  The converted mana cost of the card to randomly fetch
     */
    private void getOneSpell(String type, int cmc) {
        Cursor permanents = null;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SearchCriteria criteria = new SearchCriteria();
            SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, handle);
            if (type.equals(EQUIPMENT)) {
                criteria.cmcLogic = "<=";
                criteria.subTypes = Collections.singletonList(type);
            } else {
                criteria.cmcLogic = "=";
                criteria.superTypes = Collections.singletonList(type);
            }
            String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME};
            criteria.cmc = cmc;
            criteria.moJhoStoFilter = true;
            permanents = CardDbAdapter.Search(criteria, false, returnTypes, true, null, database);

            if (permanents == null) {
                throw new FamiliarDbException(new Exception("permanents failure"));
            }

            if (permanents.getCount() == 0) {
                return;
            }
            int pos = mRandom.nextInt(permanents.getCount());
            permanents.moveToPosition(pos);

            /* add a fragment */
            Bundle args = new Bundle();
            args.putLongArray(CardViewPagerFragment.CARD_ID_ARRAY, new long[]{permanents.getLong(permanents.getColumnIndex(CardDbAdapter.KEY_ID))});
            args.putInt(CardViewPagerFragment.STARTING_CARD_POSITION, 0);
            CardViewPagerFragment cvpFrag = new CardViewPagerFragment();
            startNewFragment(cvpFrag, args);
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
            handleFamiliarDbException(true);
        } finally {
            if (null != permanents) {
                permanents.close();
            }
            DatabaseManager.closeDatabase(getActivity(), handle);
        }
    }

    /**
     * Convenience method to fetch three random cards of the given supertype, and start a ResultListFragment to
     * display said cards
     *
     * @param type The supertype of the card to randomly fetch
     */
    private void getThreeSpells(String type) {
        Cursor spells = null;
        FamiliarDbHandle handle = new FamiliarDbHandle();
        try {
            SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, handle);
            String[] returnTypes = new String[]{CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME};
            SearchCriteria criteria = new SearchCriteria();
            criteria.superTypes = Collections.singletonList(type);
            spells = CardDbAdapter.Search(criteria, false, returnTypes, true, null, database);

            if (spells == null) {
                throw new FamiliarDbException(new Exception("three spell failure"));
            }
            /* Get 3 random, distinct numbers */
            int[] pos = new int[3];
            pos[0] = mRandom.nextInt(spells.getCount());
            pos[1] = mRandom.nextInt(spells.getCount());
            while (pos[0] == pos[1]) {
                pos[1] = mRandom.nextInt(spells.getCount());
            }
            pos[2] = mRandom.nextInt(spells.getCount());
            while (pos[0] == pos[2] || pos[1] == pos[2]) {
                pos[2] = mRandom.nextInt(spells.getCount());
            }

            Bundle args = new Bundle();

            spells.moveToPosition(pos[0]);
            args.putLong(ResultListFragment.CARD_ID_0, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));
            spells.moveToPosition(pos[1]);
            args.putLong(ResultListFragment.CARD_ID_1, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));
            spells.moveToPosition(pos[2]);
            args.putLong(ResultListFragment.CARD_ID_2, spells.getLong(spells.getColumnIndex(CardDbAdapter.KEY_ID)));

            /* add a fragment */
            ResultListFragment rlFrag = new ResultListFragment();
            startNewFragment(rlFrag, args);
        } catch (SQLiteException | FamiliarDbException | CursorIndexOutOfBoundsException e) {
            handleFamiliarDbException(true);
        } finally {
            if (null != spells) {
                spells.close();
            }
            DatabaseManager.closeDatabase(getActivity(), handle);
        }
    }
}