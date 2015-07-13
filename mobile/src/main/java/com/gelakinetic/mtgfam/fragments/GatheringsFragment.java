/**
 * Copyright 2012 Jonathan Bettger
 * <p/>
 * This file is part of MTG Familiar.
 * <p/>
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * This fragment handles the creation, loading, and saving of Gatherings (default sets of players, lives, and view modes
 * for the life counter
 */
public class GatheringsFragment extends FamiliarFragment {
    /* Dialog constants */
    private static final int DIALOG_SAVE_GATHERING = 1;
    private static final int DIALOG_GATHERING_EXIST = 2;
    private static final int DIALOG_DELETE_GATHERING = 3;
    private static final int DIALOG_REMOVE_PLAYER = 4;
    private static final int DIALOG_LOAD_GATHERING = 5;

    /* For saving state during rotations, etc */
    private static final String SAVED_GATHERING_KEY = "savedGathering";
    private static final String SAVED_NAME_KEY = "savedName";

    /* Various state variables */
    private String mProposedGathering;
    private String mCurrentGatheringName;
    private int mLargestPlayerNumber;

    /* UI Elements */
    private LinearLayout mLinearLayout;
    private Spinner mDisplayModeSpinner;

    /**
     * When the fragment is rotated, save the currently displaying Gathering and pass it to the new onCreate()
     *
     * @param outState Bundle in which to place the saved gathering.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Gathering savedGathering = new Gathering();

        savedGathering.mDisplayMode = mDisplayModeSpinner.getSelectedItemPosition();

		/* Pull all the information about players from the linear layout's children */
        for (int idx = 0; idx < mLinearLayout.getChildCount(); idx++) {
            View player = mLinearLayout.getChildAt(idx);
            assert player != null;

            EditText nameField = ((EditText) player.findViewById(R.id.custom_name));
            assert nameField.getText() != null;
            String name = nameField.getText().toString().trim();

            int startingLife;
            try {
                EditText startingLifeField = ((EditText) player.findViewById(R.id.starting_life));
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLargestPlayerNumber = 0;

		/* Inflate a view */
        View myFragmentView = inflater.inflate(R.layout.gathering_frag, container, false);
        assert myFragmentView != null;
        mLinearLayout = (LinearLayout) myFragmentView.findViewById(R.id.gathering_player_list);
        mDisplayModeSpinner = (Spinner) myFragmentView.findViewById(R.id.gathering_display_mode);

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
            mDisplayModeSpinner.setSelection(gathering.mDisplayMode);
            ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
            for (GatheringsPlayerData player : players) {
                AddPlayerRow(player);
            }
            getActivity().supportInvalidateOptionsMenu();
            mCurrentGatheringName = savedInstanceState.getString(SAVED_NAME_KEY);
        }
    }


    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id the ID of the dialog to show
     */
    private void showDialog(final int id) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
        if (!this.isVisible()) {
            return;
        }

        removeDialog(getFragmentManager());

		/* Create and show the dialog. */
        final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

            @NotNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);

                switch (id) {
                    case DIALOG_SAVE_GATHERING: {
                        /* If there are no empty fields, try to save the Gathering. If a gathering with the same
                            name already exists, prompt the user to overwrite it or not. */

                        if (AreAnyFieldsEmpty()) {
                            ToastWrapper.makeText(getActivity(), R.string.gathering_empty_field, ToastWrapper.LENGTH_LONG).show();
                            return DontShowDialog();
                        }

                        LayoutInflater factory = LayoutInflater.from(this.getActivity());
                        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry,
                                null, false);
                        assert textEntryView != null;
                        final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
                        if (mCurrentGatheringName != null) {
                            nameInput.append(mCurrentGatheringName);
                        }

                        textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                nameInput.setText("");
                            }
                        });

                        Dialog dialog = new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.gathering_enter_name)
                                .setView(textEntryView)
                                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        assert nameInput.getText() != null;
                                        String gatheringName = nameInput.getText().toString().trim();
                                        if (gatheringName.length() <= 0) {
                                            ToastWrapper.makeText(getActivity(), R.string.gathering_toast_no_name,
                                                    ToastWrapper.LENGTH_LONG).show();
                                            return;
                                        }

                                        ArrayList<String> existingGatheringsFiles =
                                                GatheringsIO.getGatheringFileList(getActivity().getFilesDir());

                                        boolean existing = false;
                                        for (String existingGatheringFile : existingGatheringsFiles) {
                                            String givenName = GatheringsIO.ReadGatheringNameFromXML(
                                                    existingGatheringFile, getActivity().getFilesDir());

                                            if (gatheringName.equals(givenName)) {
												/* Show a dialog if the gathering already exists */
                                                existing = true;
                                                mProposedGathering = gatheringName;
                                                showDialog(DIALOG_GATHERING_EXIST);
                                                break;
                                            }
                                        }

                                        if (existingGatheringsFiles.size() <= 0 || !existing) {
                                            SaveGathering(gatheringName);
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .create();
                        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        return dialog;
                    }
                    case DIALOG_GATHERING_EXIST: {
						/* The user tried to save, and the gathering already exists. Prompt to overwrite */
                        return new AlertDialogPro.Builder(this.getActivity())
                                .setTitle(R.string.gathering_dialog_overwrite_title)
                                .setMessage(R.string.gathering_dialog_overwrite_text)
                                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        GatheringsIO.DeleteGatheringByName(mProposedGathering,
                                                getActivity().getFilesDir(), getActivity());
                                        SaveGathering(mProposedGathering);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                                .create();
                    }
                    case DIALOG_DELETE_GATHERING: {
						/* Show all gatherings, and delete the selected one */
                        if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                            ToastWrapper.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
                                    ToastWrapper.LENGTH_LONG).show();
                            return DontShowDialog();
                        }

                        ArrayList<String> dGatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
                        final String[] dfGatherings = dGatherings.toArray(new String[dGatherings.size()]);
                        final String[] dProperNames = new String[dGatherings.size()];
                        for (int idx = 0; idx < dGatherings.size(); idx++) {
                            dProperNames[idx] = GatheringsIO.ReadGatheringNameFromXML(dGatherings.get(idx),
                                    getActivity().getFilesDir());
                        }

                        return new AlertDialogPro.Builder(getActivity())
                                .setTitle(R.string.gathering_delete)
                                .setItems(dProperNames, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface, int item) {
                                        GatheringsIO.DeleteGathering(dfGatherings[item], getActivity().getFilesDir(),
                                                getActivity());
                                        getActivity().supportInvalidateOptionsMenu();
                                    }
                                }).create();
                    }
                    case DIALOG_REMOVE_PLAYER: {
						/* Remove a player from the Gathering and linear layout */
                        ArrayList<String> names = new ArrayList<>();
                        for (int idx = 0; idx < mLinearLayout.getChildCount(); idx++) {
                            View player = mLinearLayout.getChildAt(idx);
                            assert player != null;
                            EditText customName = (EditText) player.findViewById(R.id.custom_name);
                            assert customName.getText() != null;
                            names.add(customName.getText().toString().trim());
                        }
                        final String[] aNames = names.toArray(new String[names.size()]);

                        if (names.size() == 0) {
                            return DontShowDialog();
                        }

                        return new AlertDialogPro.Builder(getActivity())
                                .setTitle(R.string.gathering_remove_player)
                                .setItems(aNames, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface, int item) {
                                        mLinearLayout.removeViewAt(item);
                                        getActivity().supportInvalidateOptionsMenu();
                                    }
                                })
                                .create();
                    }
                    case DIALOG_LOAD_GATHERING: {
						/* Load a gathering, if there is a gathering to load */
                        if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                            ToastWrapper.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
                                    ToastWrapper.LENGTH_LONG).show();
                            return DontShowDialog();
                        }

                        ArrayList<String> gatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
                        final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
                        final String[] properNames = new String[gatherings.size()];
                        for (int idx = 0; idx < gatherings.size(); idx++) {
                            properNames[idx] = GatheringsIO.ReadGatheringNameFromXML(gatherings.get(idx),
                                    getActivity().getFilesDir());
                        }

                        return new AlertDialogPro.Builder(getActivity())
                                .setTitle(R.string.gathering_load)
                                .setItems(properNames, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface, int item) {
                                        mLinearLayout.removeAllViews();
                                        mLargestPlayerNumber = 0;
                                        Gathering gathering = GatheringsIO.ReadGatheringXML(fGatherings[item],
                                                getActivity().getFilesDir());

                                        mCurrentGatheringName = GatheringsIO.ReadGatheringNameFromXML(fGatherings[item],
                                                getActivity().getFilesDir());
                                        mDisplayModeSpinner.setSelection(gathering.mDisplayMode);
                                        ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                                        for (GatheringsPlayerData player : players) {
                                            AddPlayerRow(player);
                                        }
                                        getActivity().supportInvalidateOptionsMenu();
                                    }
                                }).create();
                    }
                    default: {
                        savedInstanceState.putInt("id", id);
                        return super.onCreateDialog(savedInstanceState);
                    }
                }
            }
        };
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
        switch (item.getItemId()) {
            case R.id.delete_gathering:
                showDialog(DIALOG_DELETE_GATHERING);
                return true;
            case R.id.remove_player:
                showDialog(DIALOG_REMOVE_PLAYER);
                return true;
            case R.id.add_player:
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
            case R.id.load_gathering:
                showDialog(DIALOG_LOAD_GATHERING);
                return true;
            case R.id.save_gathering:
                showDialog(DIALOG_SAVE_GATHERING);
                return true;
            default:
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.gathering_menu, menu);
    }

    /**
     * Save the current gathering with the given name
     *
     * @param _gatheringName The name of the gathering to save
     */
    private void SaveGathering(String _gatheringName) {
        if (_gatheringName.length() <= 0) {
            ToastWrapper.makeText(getActivity(), R.string.gathering_toast_no_name, ToastWrapper.LENGTH_LONG).show();
            return;
        }

        mCurrentGatheringName = _gatheringName;
        int playersCount = mLinearLayout.getChildCount();
        ArrayList<GatheringsPlayerData> players = new ArrayList<>(playersCount);

        for (int idx = 0; idx < playersCount; idx++) {
            View player = mLinearLayout.getChildAt(idx);
            assert player != null;

            EditText customName = (EditText) player.findViewById(R.id.custom_name);
            assert customName.getText() != null;
            String name = customName.getText().toString().trim();

            EditText startingLife = (EditText) player.findViewById(R.id.starting_life);
            assert startingLife.getText() != null;
            int life = Integer.parseInt(startingLife.getText().toString());

            players.add(new GatheringsPlayerData(name, life));
        }

        GatheringsIO.writeGatheringXML(players, _gatheringName, mDisplayModeSpinner.getSelectedItemPosition(),
                getActivity().getFilesDir());
        getActivity().supportInvalidateOptionsMenu();
    }

    /**
     * Checks whether any fields are empty. If they are, the gathering can't be saved
     *
     * @return true if there are empty fields, false otherwise
     */
    private boolean AreAnyFieldsEmpty() {
        int playersCount = mLinearLayout.getChildCount();

        for (int idx = 0; idx < playersCount; idx++) {
            View player = mLinearLayout.getChildAt(idx);

            assert player != null;
            EditText customName = (EditText) player.findViewById(R.id.custom_name);
            assert customName.getText() != null;
            String name = customName.getText().toString().trim();
            if (name.trim().length() == 0) {
                return true;
            }

            EditText startingLife = (EditText) player.findViewById(R.id.starting_life);

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
    private void AddPlayerRow(GatheringsPlayerData _player) {

        if (_player.mName == null) {
            mLargestPlayerNumber++;
            _player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
        } else {
            try {
                String nameParts[] = _player.mName.split(" ");
                int number = Integer.parseInt(nameParts[nameParts.length - 1]);
                if (number > mLargestPlayerNumber) {
                    mLargestPlayerNumber = number;
                }
            } catch (NumberFormatException e) {
				/* eat it */
            }
        }
        View newView = getLayoutInflater(null).inflate(R.layout.gathering_create_player_row, null, false);
        assert newView != null;

        ((TextView) newView.findViewById(R.id.custom_name)).setText(_player.mName);
        ((TextView) newView.findViewById(R.id.starting_life)).setText(String.valueOf(_player.mStartingLife));

        mLinearLayout.addView(newView);
        getActivity().supportInvalidateOptionsMenu();
    }

    /**
     * Show or remove invalid items from the action bar
     *
     * @param menu The menu to show or hide the "announce life totals" button in.
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem removePlayer = menu.findItem(R.id.remove_player);
        MenuItem deleteGathering = menu.findItem(R.id.delete_gathering);
        MenuItem loadGathering = menu.findItem(R.id.load_gathering);
        assert removePlayer != null;
        assert deleteGathering != null;
        assert loadGathering != null;

        try {
            if (mLinearLayout.getChildCount() == 0 || !getFamiliarActivity().mIsMenuVisible) {
                removePlayer.setVisible(false);
            } else {
                removePlayer.setVisible(true);
            }
        } catch (NullPointerException e) {
			/* the if () statement throwing a NullPointerException for some users. I don't know which part was null,
			or why, but this catches it well enough */
            removePlayer.setVisible(true);
        }

        if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0 ||
                getFamiliarActivity() == null || !getFamiliarActivity().mIsMenuVisible) {
            deleteGathering.setVisible(false);
            loadGathering.setVisible(false);
        } else {
            deleteGathering.setVisible(true);
            loadGathering.setVisible(true);
        }
    }
}
