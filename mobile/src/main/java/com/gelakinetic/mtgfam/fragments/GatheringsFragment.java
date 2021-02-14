/*
 * Copyright 2012 Jonathan Bettger
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.GatheringsDialogFragment;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This fragment handles the creation, loading, and saving of Gatherings (default sets of players, lives, and view modes
 * for the life counter
 */
public class GatheringsFragment extends FamiliarFragment {

    /* For saving state during rotations, etc */
    private static final String SAVED_GATHERING_KEY = "savedGathering";
    private static final String SAVED_NAME_KEY = "savedName";

    /* Various state variables */
    public String mProposedGathering;
    public String mCurrentGatheringName;
    public int mLargestPlayerNumber;

    /* UI Elements */
    public LinearLayout mLinearLayout;
    public Spinner mDisplayModeSpinner;

    /**
     * When the fragment is rotated, save the currently displaying Gathering and pass it to the new onCreate()
     *
     * @param outState Bundle in which to place the saved gathering.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Gathering savedGathering = new Gathering();

        savedGathering.mDisplayMode = mDisplayModeSpinner.getSelectedItemPosition();

        /* Pull all the information about players from the linear layout's children */
        for (int idx = 0; idx < mLinearLayout.getChildCount(); idx++) {
            View player = mLinearLayout.getChildAt(idx);
            assert player != null;

            EditText nameField = player.findViewById(R.id.custom_name);
            assert nameField.getText() != null;
            String name = nameField.getText().toString().trim();

            int startingLife;
            try {
                EditText startingLifeField = player.findViewById(R.id.starting_life);
                assert startingLifeField.getText() != null;
                startingLife = Integer.parseInt(startingLifeField.getText().toString().trim());
            } catch (NumberFormatException e) {
                startingLife = 20;
            }

            savedGathering.mPlayerList.add(new GatheringsPlayerData(name, startingLife));
        }
        outState.remove(SAVED_GATHERING_KEY);
        outState.remove(SAVED_NAME_KEY);
        outState.putSerializable(SAVED_GATHERING_KEY, savedGathering);
        outState.putString(SAVED_NAME_KEY, mCurrentGatheringName);

        super.onSaveInstanceState(outState);
    }

    /**
     * Create the view and grab the LinearLayout and Spinner. Information will be populated later
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The
     *                           fragment should not add the view itself, but this can be used to generate the
     *                           LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given
     *                           here.
     * @return The inflated view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLargestPlayerNumber = 0;

        /* Inflate a view */
        View myFragmentView = inflater.inflate(R.layout.gathering_frag, container, false);
        assert myFragmentView != null;
        mLinearLayout = myFragmentView.findViewById(R.id.gathering_player_list);
        mDisplayModeSpinner = myFragmentView.findViewById(R.id.gathering_display_mode);

        return myFragmentView;
    }

    /**
     * Make sure to add any player rows to the LinearLayout *after* the View has been created.
     * This avoids a weird bug where text is duplicated across EditTexts
     *
     * @param savedInstanceState The Bundle containing information about a previous state
     */
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);


        /* Add some players */
        if (savedInstanceState == null) {
            AddPlayerRow(new GatheringsPlayerData(null, 20));
            AddPlayerRow(new GatheringsPlayerData(null, 20));
        } else {
            mLinearLayout.removeAllViews();
            mLargestPlayerNumber = 0;
            Gathering gathering = (Gathering) savedInstanceState.getSerializable(SAVED_GATHERING_KEY);

            assert gathering != null;
            if (gathering.mDisplayMode >= mDisplayModeSpinner.getAdapter().getCount()) {
                gathering.mDisplayMode = 0;
            }
            mDisplayModeSpinner.setSelection(gathering.mDisplayMode);
            ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
            for (GatheringsPlayerData player : players) {
                AddPlayerRow(player);
            }
            Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
            mCurrentGatheringName = savedInstanceState.getString(SAVED_NAME_KEY);
        }
    }


    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id the ID of the dialog to show
     */
    public void showDialog(final int id) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        /* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

        /* Create and show the dialog. */
        GatheringsDialogFragment newFragment = new GatheringsDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
        newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Handle clicks from the action bar
     *
     * @param item The item that was clicked
     * @return true if the click was acted upon, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_gathering) {
            showDialog(GatheringsDialogFragment.DIALOG_DELETE_GATHERING);
            return true;
        } else if (item.getItemId() == R.id.remove_player) {
            showDialog(GatheringsDialogFragment.DIALOG_REMOVE_PLAYER);
            return true;
        } else if (item.getItemId() == R.id.add_player) {
            /* Use the correct default life for commander mode */
            switch (mDisplayModeSpinner.getSelectedItemPosition()) {
                case 1:
                case 0: {
                    AddPlayerRow(new GatheringsPlayerData(null, LifeCounterFragment.DEFAULT_LIFE));
                    break;
                }
                case 2: {
                    AddPlayerRow(new GatheringsPlayerData(null, LifeCounterFragment.DEFAULT_LIFE_COMMANDER));
                    break;
                }
            }
            return true;
        } else if (item.getItemId() == R.id.load_gathering) {
            showDialog(GatheringsDialogFragment.DIALOG_LOAD_GATHERING);
            return true;
        } else if (item.getItemId() == R.id.save_gathering) {
            showDialog(GatheringsDialogFragment.DIALOG_SAVE_GATHERING);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inflate the menu, gathering_menu
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater The inflater to use to inflate the menu
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.gathering_menu, menu);
    }

    /**
     * Save the current gathering with the given name
     *
     * @param _gatheringName The name of the gathering to save
     */
    public void SaveGathering(String _gatheringName) {
        if (_gatheringName.length() <= 0) {
            SnackbarWrapper.makeAndShowText(getActivity(), R.string.gathering_toast_no_name, SnackbarWrapper.LENGTH_LONG);
            return;
        }

        mCurrentGatheringName = _gatheringName;
        int playersCount = mLinearLayout.getChildCount();
        ArrayList<GatheringsPlayerData> players = new ArrayList<>(playersCount);

        for (int idx = 0; idx < playersCount; idx++) {
            View player = mLinearLayout.getChildAt(idx);
            assert player != null;

            EditText customName = player.findViewById(R.id.custom_name);
            assert customName.getText() != null;
            String name = customName.getText().toString().trim();

            EditText startingLife = player.findViewById(R.id.starting_life);
            assert startingLife.getText() != null;
            int life = Integer.parseInt(startingLife.getText().toString());

            players.add(new GatheringsPlayerData(name, life));
        }

        GatheringsIO.writeGatheringXML(players, _gatheringName, mDisplayModeSpinner.getSelectedItemPosition(),
                Objects.requireNonNull(getActivity()).getFilesDir());
        getActivity().invalidateOptionsMenu();
    }

    /**
     * Checks whether any fields are empty. If they are, the gathering can't be saved
     *
     * @return true if there are empty fields, false otherwise
     */
    public boolean AreAnyFieldsEmpty() {
        int playersCount = mLinearLayout.getChildCount();

        for (int idx = 0; idx < playersCount; idx++) {
            View player = mLinearLayout.getChildAt(idx);

            assert player != null;
            EditText customName = player.findViewById(R.id.custom_name);
            assert customName.getText() != null;
            String name = customName.getText().toString().trim();
            if (name.trim().length() == 0) {
                return true;
            }

            EditText startingLife = player.findViewById(R.id.starting_life);

            assert startingLife.getText() != null;
            if (startingLife.getText().toString().trim().length() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a row in the LinearLayout with the name and starting life in GatheringsPlayerData
     *
     * @param _player The GatheringsPlayerData with a name and starting life
     */
    public void AddPlayerRow(GatheringsPlayerData _player) {

        if (_player.mName == null) {
            mLargestPlayerNumber++;
            _player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
        } else {
            try {
                String[] nameParts = _player.mName.split(" ");
                int number = Integer.parseInt(nameParts[nameParts.length - 1]);
                if (number > mLargestPlayerNumber) {
                    mLargestPlayerNumber = number;
                }
            } catch (NumberFormatException e) {
                /* eat it */
            }
        }
        View newView = getLayoutInflater().inflate(R.layout.gathering_create_player_row, mLinearLayout, false);
        assert newView != null;

        ((TextView) newView.findViewById(R.id.custom_name)).setText(_player.mName);
        ((TextView) newView.findViewById(R.id.starting_life)).setText(String.valueOf(_player.mStartingLife));

        mLinearLayout.addView(newView);
        Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
    }

    /**
     * Show or remove invalid items from the action bar
     *
     * @param menu The menu to show or hide the "announce life totals" button in.
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem removePlayer = menu.findItem(R.id.remove_player);
        MenuItem deleteGathering = menu.findItem(R.id.delete_gathering);
        MenuItem loadGathering = menu.findItem(R.id.load_gathering);
        assert removePlayer != null;
        assert deleteGathering != null;
        assert loadGathering != null;

        try {
            removePlayer.setVisible(mLinearLayout.getChildCount() != 0 && getFamiliarActivity().mIsMenuVisible);
        } catch (NullPointerException e) {
            /* the if () statement throwing a NullPointerException for some users. I don't know which part was null,
            or why, but this catches it well enough */
            removePlayer.setVisible(true);
        }

        if (GatheringsIO.getNumberOfGatherings(Objects.requireNonNull(getActivity()).getFilesDir()) <= 0 ||
                getFamiliarActivity() == null || !getFamiliarActivity().mIsMenuVisible) {
            deleteGathering.setVisible(false);
            loadGathering.setVisible(false);
        } else {
            deleteGathering.setVisible(true);
            loadGathering.setVisible(true);
        }
    }
}
