/**
 Copyright 2012 Jonathan Bettger

 This file is part of MTG Familiar.

 MTG Familiar is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 MTG Familiar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
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
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import java.util.ArrayList;

/**
 * This fragment handles the creation, loading, and saving of Gatherings (default sets of players, lives, and view modes
 * for the life counter
 */
public class GatheringsFragment extends FamiliarFragment {
	/* Dialog constants */
	private static final int DIALOG_SET_NAME = 1;
	private static final int DIALOG_GATHERING_EXIST = 2;
	private static final int DIALOG_DELETE_GATHERING = 3;
	private static final int DIALOG_REMOVE_PLAYER = 4;
	private static final int DIALOG_LOAD_GATHERING = 5;

	private static final String SAVED_GATHERING_KEY = "savedGathering";
	private static final String SAVED_NAME_KEY = "savedName";

	/* Used to hold the gathering name between dialogs if it already exists */
	private String mProposedGathering;
	private String mCurrentGatheringName;
	private int mLargestPlayerNumber;

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
		outState.putSerializable(SAVED_GATHERING_KEY, savedGathering);
		outState.putString(SAVED_NAME_KEY, mCurrentGatheringName);

		mLinearLayout.removeAllViews();

		super.onSaveInstanceState(outState);
	}

	/**
	 * Create the view, grab the LinearLayout and Spinner, and set the displayed gathering either as default, or what
	 * was saved before the rotation
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

		View myFragmentView = inflater.inflate(R.layout.gathering_create_activity, container, false);
		assert myFragmentView != null;
		mLinearLayout = (LinearLayout) myFragmentView.findViewById(R.id.gathering_player_list);
		mDisplayModeSpinner = (Spinner) myFragmentView.findViewById(R.id.gathering_display_mode);

		if (savedInstanceState == null) {
			AddPlayerRowFromData(new GatheringsPlayerData(null, 20));
			AddPlayerRowFromData(new GatheringsPlayerData(null, 20));
		}
		else {
			Gathering savedGathering = (Gathering) savedInstanceState.getSerializable(SAVED_GATHERING_KEY);
			assert savedGathering != null;
			mDisplayModeSpinner.setSelection(savedGathering.mDisplayMode);
			for (int i = 0; i < savedGathering.mPlayerList.size(); i++) {
				AddPlayerRowFromData(savedGathering.mPlayerList.get(i));
			}
			mCurrentGatheringName = savedInstanceState.getString(SAVED_NAME_KEY);
		}
		return myFragmentView;
	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id the ID of the dialog to show
	 */
	void showDialog(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
		if (!this.isVisible()) {
			return;
		}

		removeDialog(getFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				super.onCreateDialog(savedInstanceState);

				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case DIALOG_SET_NAME: {

						if (AreAnyFieldsEmpty()) {
							Toast.makeText(getActivity(), R.string.gathering_empty_field, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						LayoutInflater factory = LayoutInflater.from(this.getActivity());
						final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
						assert textEntryView != null;
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
						nameInput.setText(mCurrentGatheringName);
						Dialog dialog = new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.gathering_enter_name)
								.setView(textEntryView)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										assert nameInput.getText() != null;
										String gatheringName = nameInput.getText().toString().trim();
										if (gatheringName.length() <= 0) {
											Toast.makeText(getActivity(), R.string.gathering_toast_no_name,
													Toast.LENGTH_LONG).show();
											return;
										}

										ArrayList<String> existingGatheringsFiles =
												GatheringsIO.getGatheringFileList(getActivity().getFilesDir());

										boolean existing = false;
										for (String existingGatheringFile : existingGatheringsFiles) {
											String givenName = GatheringsIO.ReadGatheringNameFromXML(
													existingGatheringFile, getActivity().getFilesDir());

											if (gatheringName.equals(givenName)) {
												// throw existing dialog
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
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.gathering_dialog_overwrite_title)
								.setMessage(R.string.gathering_dialog_overwrite_text)
								.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										GatheringsIO.DeleteGatheringByName(mProposedGathering,
												getActivity().getFilesDir());
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
						if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
							Toast.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
									Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						ArrayList<String> dGatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
						final String[] dfGatherings = dGatherings.toArray(new String[dGatherings.size()]);
						final String[] dProperNames = new String[dGatherings.size()];
						for (int idx = 0; idx < dGatherings.size(); idx++) {
							dProperNames[idx] = GatheringsIO.ReadGatheringNameFromXML(dGatherings.get(idx),
									getActivity().getFilesDir());
						}

						return new AlertDialog.Builder(getActivity())
								.setTitle(R.string.gathering_delete)
								.setItems(dProperNames, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialogInterface, int item) {
										GatheringsIO.DeleteGathering(dfGatherings[item], getActivity().getFilesDir());
									}
								}).create();
					}
					case DIALOG_REMOVE_PLAYER: {
						ArrayList<String> names = new ArrayList<String>();
						for (int idx = 0; idx < mLinearLayout.getChildCount(); idx++) {
							View player = mLinearLayout.getChildAt(idx);
							assert player != null;
							EditText customName = (EditText) player.findViewById(R.id.custom_name);
							assert customName.getText() != null;
							names.add(customName.getText().toString().trim());
						}
						final String[] aNames = names.toArray(new String[names.size()]);

						if(names.size() == 0) {
							setShowsDialog(false);
							return null;
						}

						return new AlertDialog.Builder(getActivity())
								.setTitle(R.string.gathering_remove_player)
								.setItems(aNames, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialogInterface, int item) {
										mLinearLayout.removeViewAt(item);
										getActivity().invalidateOptionsMenu();
									}
								})
								.create();
					}
					case DIALOG_LOAD_GATHERING: {
						if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
							Toast.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
									Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						ArrayList<String> gatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
						final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
						final String[] properNames = new String[gatherings.size()];
						for (int idx = 0; idx < gatherings.size(); idx++) {
							properNames[idx] = GatheringsIO.ReadGatheringNameFromXML(gatherings.get(idx),
									getActivity().getFilesDir());
						}

						return new AlertDialog.Builder(getActivity())
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
											AddPlayerRowFromData(player);
										}
										getActivity().invalidateOptionsMenu();
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
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.gdelete_gathering:
				showDialog(DIALOG_DELETE_GATHERING);
				return true;
			case R.id.gremove_player:
				showDialog(DIALOG_REMOVE_PLAYER);
				return true;
			case R.id.gadd_player:
				AddPlayerRowFromData(new GatheringsPlayerData(null, 20));
				return true;
			case R.id.gload_gathering:
				showDialog(DIALOG_LOAD_GATHERING);
				return true;
			case R.id.gsave_gathering:
				showDialog(DIALOG_SET_NAME);
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
			Toast.makeText(getActivity(), R.string.gathering_toast_no_name, Toast.LENGTH_LONG).show();
			return;
		}

		mCurrentGatheringName = _gatheringName;
		int playersCount = mLinearLayout.getChildCount();
		ArrayList<GatheringsPlayerData> players = new ArrayList<GatheringsPlayerData>(playersCount);

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
	private void AddPlayerRowFromData(GatheringsPlayerData _player) {

		if (_player.mName == null) {
			mLargestPlayerNumber++;
			_player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
		}
		else {
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
		View view = getActivity().getLayoutInflater().inflate(R.layout.gathering_create_player_row, null);
		assert view != null;

		((TextView) view.findViewById(R.id.custom_name)).setText(_player.mName);
		((TextView) view.findViewById(R.id.starting_life)).setText(String.valueOf(_player.mStartingLife));

		mLinearLayout.addView(view);
		getActivity().invalidateOptionsMenu();
	}

	/**
	 * If there are no players, remove the "remove players" button
	 *
	 * @param menu The menu to show or hide the "announce life totals" button in.
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem removePlayer = menu.findItem(R.id.gremove_player);
		assert removePlayer != null;
		if(mLinearLayout.getChildCount() == 0 || !((FamiliarActivity) getActivity()).mIsMenuVisible) {
			removePlayer.setVisible(false);
		}
		else {
			removePlayer.setVisible(true);
		}
	}
}
