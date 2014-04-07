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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import org.jetbrains.annotations.Nullable;

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
	private ArrayList<GatheringsPlayerData> mPlayerList = new ArrayList<GatheringsPlayerData>();

	private Spinner mDisplayModeSpinner;
	private GatheringsArrayAdapter mAdapter;

	/**
	 * When the fragment is rotated, save the currently displaying Gathering and pass it to the new onCreate()
	 *
	 * @param outState Bundle in which to place the saved gathering.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Gathering toSave = new Gathering();
		toSave.mDisplayMode = mDisplayModeSpinner.getSelectedItemPosition();
		toSave.mPlayerList.addAll(mPlayerList);
		outState.putSerializable(SAVED_GATHERING_KEY, toSave);
		outState.putString(SAVED_NAME_KEY, mCurrentGatheringName);

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

		/* Inflate a view */
		View myFragmentView = inflater.inflate(R.layout.gathering_frag, container, false);
		assert myFragmentView != null;
		ListView listView = (ListView) myFragmentView.findViewById(R.id.gathering_player_list);

		listView.setItemsCanFocus(true);
		listView.setClickable(false);
		mDisplayModeSpinner = (Spinner) myFragmentView.findViewById(R.id.gathering_display_mode);

		mPlayerList.clear();

		/* Add some players */
		if (savedInstanceState == null) {
			AddPlayerRow(new GatheringsPlayerData(null, 20));
			AddPlayerRow(new GatheringsPlayerData(null, 20));
		}
		else {
			mCurrentGatheringName = savedInstanceState.getString(SAVED_NAME_KEY);
			Gathering savedGathering = (Gathering) savedInstanceState.getSerializable(SAVED_GATHERING_KEY);
			assert savedGathering != null;
			for (GatheringsPlayerData gpd : savedGathering.mPlayerList) {
				AddPlayerRow(gpd);
			}
			mDisplayModeSpinner.setSelection(savedGathering.mDisplayMode);
		}

		mAdapter = new GatheringsArrayAdapter(getActivity(), mPlayerList);
		mAdapter.notifyDataSetChanged();
		listView.setAdapter(mAdapter);
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
					case DIALOG_SAVE_GATHERING: {
						/* If there are no empty fields, try to save the Gathering. If a gathering with the same
 						name already exists, prompt the user to overwrite it or not. */

						if (AreAnyFieldsEmpty()) {
							Toast.makeText(getActivity(), R.string.gathering_empty_field, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						LayoutInflater factory = LayoutInflater.from(this.getActivity());
						final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
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
						/* Show all gatherings, and delete the selected one */
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
										getActivity().invalidateOptionsMenu();
									}
								}).create();
					}
					case DIALOG_REMOVE_PLAYER: {
						/* Remove a player from the Gathering and linear layout */
						ArrayList<String> names = new ArrayList<String>();
						for (GatheringsPlayerData gpd : mPlayerList) {
							names.add(gpd.mName);
						}
						final String[] aNames = names.toArray(new String[names.size()]);

						if (names.size() == 0) {
							setShowsDialog(false);
							return null;
						}

						return new AlertDialog.Builder(getActivity())
								.setTitle(R.string.gathering_remove_player)
								.setItems(aNames, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialogInterface, int item) {
										mPlayerList.remove(item);
										mAdapter.notifyDataSetChanged();
										getActivity().invalidateOptionsMenu();
									}
								})
								.create();
					}
					case DIALOG_LOAD_GATHERING: {
						/* Load a gathering, if there is a gathering to load */
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
										mLargestPlayerNumber = 0;
										Gathering loaded = GatheringsIO.ReadGatheringXML(fGatherings[item],
												getActivity().getFilesDir());

										mCurrentGatheringName = GatheringsIO.ReadGatheringNameFromXML(fGatherings[item],
												getActivity().getFilesDir());
										mDisplayModeSpinner.setSelection(loaded.mDisplayMode);
										mPlayerList.clear();
										for (GatheringsPlayerData gpd : loaded.mPlayerList) {
											AddPlayerRow(gpd);
										}
										mAdapter.notifyDataSetChanged();
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
						AddPlayerRow(new GatheringsPlayerData(null, 20));
						break;
					}
					case 2: {
						AddPlayerRow(new GatheringsPlayerData(null, 40));
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
			Toast.makeText(getActivity(), R.string.gathering_toast_no_name, Toast.LENGTH_LONG).show();
			return;
		}

		mCurrentGatheringName = _gatheringName;

		GatheringsIO.writeGatheringXML(mPlayerList, _gatheringName,
				mDisplayModeSpinner.getSelectedItemPosition(), getActivity().getFilesDir());
		getActivity().invalidateOptionsMenu();
	}

	/**
	 * Checks whether any fields are empty. If they are, the gathering can't be saved
	 *
	 * @return true if there are empty fields, false otherwise
	 */
	private boolean AreAnyFieldsEmpty() {

		for (GatheringsPlayerData gpd : mPlayerList) {

			if (gpd.mName.trim().length() == 0) {
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

		mPlayerList.add(_player);
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
		getActivity().invalidateOptionsMenu();
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

		if (mPlayerList.size() == 0 || !getFamiliarActivity().mIsMenuVisible) {
			removePlayer.setVisible(false);
		}
		else {
			removePlayer.setVisible(true);
		}

		if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0 ||
				!getFamiliarActivity().mIsMenuVisible) {
			deleteGathering.setVisible(false);
			loadGathering.setVisible(false);
		}
		else {
			deleteGathering.setVisible(true);
			loadGathering.setVisible(true);
		}
	}

	/**
	 * TODO
	 */
	private class GatheringsArrayAdapter extends ArrayAdapter<GatheringsPlayerData> {

		/**
		 * Constructor
		 *
		 * @param context A context to pass to super
		 * @param mItems  The list of items to display
		 */
		public GatheringsArrayAdapter(Context context, ArrayList<GatheringsPlayerData> mItems) {
			super(context, R.layout.gathering_create_player_row, mItems);
		}

		/**
		 * @param position
		 * @param convertView
		 * @param parent
		 * @return
		 */
		@Nullable
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			/* TODO Should use holder pattern, but I don't know how to track which TextWatchers to remove before adding the new one */
			convertView = getActivity().getLayoutInflater()
					.inflate(R.layout.gathering_create_player_row, parent, false);
			assert convertView != null;
			((TextView) convertView.findViewById(R.id.custom_name)).setText(mPlayerList.get(position).mName);
			((TextView) convertView.findViewById(R.id.custom_name)).addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
					/* ignore */
				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
					/* Ignore */
				}

				@Override
				public void afterTextChanged(Editable editable) {
					mPlayerList.get(position).mName = editable.toString();
				}
			});
			((TextView) convertView.findViewById(R.id.starting_life))
					.setText(String.valueOf(mPlayerList.get(position).mStartingLife));
			((TextView) convertView.findViewById(R.id.starting_life)).addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
					/* ignore */
				}

				@Override
				public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
					/* ignore */
				}

				@Override
				public void afterTextChanged(Editable editable) {
					try {
						mPlayerList.get(position).mStartingLife = Integer.parseInt(editable.toString());
					} catch (NumberFormatException e) {
						mPlayerList.get(position).mStartingLife = 0;
					}
				}
			});
			return convertView;
		}
	}
}
