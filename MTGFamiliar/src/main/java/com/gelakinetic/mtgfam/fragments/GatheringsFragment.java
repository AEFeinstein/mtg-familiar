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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

public class GatheringsFragment extends FamiliarFragment {
	private static final int DIALOG_SET_NAME = 1;
	private static final int DIALOG_GATHERING_EXIST = 2;

	private String proposedGathering;

	private Context mCtx;
	private GatheringsIO gIO;
	private LinearLayout mainLayout;
	private Spinner displayModeSpinner;

	public GatheringsFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View myFragmentView = inflater.inflate(R.layout.gathering_create_activity, container, false);

		mainLayout = (LinearLayout) myFragmentView.findViewById(R.id.gathering_player_list);
		displayModeSpinner = (Spinner) myFragmentView.findViewById(R.id.gathering_display_mode);

		mCtx = this.getActivity();
		gIO = new GatheringsIO(mCtx);

		AddPlayerRowFromData(new GatheringsPlayerData(getString(R.string.gathering_player) + " 1", 20));
		AddPlayerRowFromData(new GatheringsPlayerData(getString(R.string.gathering_player) + " 2", 20));
		return myFragmentView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(FamiliarActivity.DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
					case DIALOG_SET_NAME: {
						LayoutInflater factory = LayoutInflater.from(this.getActivity());
						final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
						nameInput.setText(proposedGathering);
						Dialog dialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.gathering_enter_name)
								.setView(textEntryView).setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										String gatheringName = nameInput.getText().toString().trim();
										if (gatheringName.length() <= 0) {
											Toast.makeText(mCtx, R.string.gathering_toast_no_name, Toast.LENGTH_LONG).show();
											return;
										}

										ArrayList<String> existingGatheringsFiles = gIO.getGatheringFileList();

										boolean existing = false;
										for (String existingGatheringFile : existingGatheringsFiles) {
											String givenName = gIO.ReadGatheringNameFromXML(existingGatheringFile);

											if (gatheringName.equals(givenName)) {
												// throw existing dialog
												existing = true;
												proposedGathering = gatheringName;
												showDialog(DIALOG_GATHERING_EXIST);
												break;
											}
										}

										if (existingGatheringsFiles.size() <= 0 || !existing) {
											SaveGathering(gatheringName);
										}
									}
								}).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
									}
								}).create();
						dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
						return dialog;
					}
					case DIALOG_GATHERING_EXIST: {
						return new AlertDialog.Builder(this.getActivity())
								.setTitle(R.string.gathering_dialog_overwrite_title)
								.setMessage(R.string.gathering_dialog_overwrite_text)
								.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										gIO.DeleteGatheringByName(proposedGathering);
										SaveGathering(proposedGathering);
									}
								}).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
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
		newFragment.show(ft, FamiliarActivity.DIALOG_TAG);
	}

	private void SaveGathering(String _gatheringName) {
		if (_gatheringName.length() <= 0) {
			Toast.makeText(mCtx, R.string.gathering_toast_no_name, Toast.LENGTH_LONG).show();
			return;
		}

		int playersCount = mainLayout.getChildCount();
		ArrayList<GatheringsPlayerData> players = new ArrayList<GatheringsPlayerData>(playersCount);

		for (int idx = 0; idx < playersCount; idx++) {
			View player = mainLayout.getChildAt(idx);

			EditText customName = (EditText) player.findViewById(R.id.custom_name);
			String name = customName.getText().toString().trim();

			EditText startingLife = (EditText) player.findViewById(R.id.starting_life);
			int life = Integer.parseInt(startingLife.getText().toString());

			players.add(new GatheringsPlayerData(name, life));
		}

		gIO.writeGatheringXML(players, _gatheringName, displayModeSpinner.getSelectedItemPosition());
	}

	private boolean AreAnyFieldsEmpty() {
		int playersCount = mainLayout.getChildCount();

		for (int idx = 0; idx < playersCount; idx++) {
			View player = mainLayout.getChildAt(idx);

			EditText customName = (EditText) player.findViewById(R.id.custom_name);
			String name = customName.getText().toString().trim();
			if (name.trim().length() == 0) {
				return true;
			}


			EditText startingLife = (EditText) player.findViewById(R.id.starting_life);

			if (startingLife.getText().toString().trim().length() == 0) {
				return true;
			}
		}
		return false;
	}

	private void AddPlayerRowFromData(GatheringsPlayerData _player) {
		LayoutInflater inf = this.getActivity().getLayoutInflater();
		View v = inf.inflate(R.layout.gathering_create_player_row, null);

		TextView name = (TextView) v.findViewById(R.id.custom_name);
		name.setText(_player.mName);

		TextView life = (TextView) v.findViewById(R.id.starting_life);
		life.setText(String.valueOf(_player.mStartingLife));

		mainLayout.addView(v);
	}

	private void RemoveAllPlayerRows() {
		mainLayout.removeAllViews();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.gdelete_gathering:
				if (gIO.getNumberOfGatherings() <= 0) {
					Toast.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings, Toast.LENGTH_LONG).show();
					return true;
				}

				ArrayList<String> dGatherings = gIO.getGatheringFileList();
				final String[] dfGatherings = dGatherings.toArray(new String[dGatherings.size()]);
				final String[] dProperNames = new String[dGatherings.size()];
				for (int idx = 0; idx < dGatherings.size(); idx++) {
					dProperNames[idx] = gIO.ReadGatheringNameFromXML(dGatherings.get(idx));
				}

				AlertDialog.Builder dbuilder = new AlertDialog.Builder(mCtx);
				dbuilder.setTitle(R.string.gathering_delete);
				dbuilder.setItems(dProperNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						gIO.DeleteGathering(dfGatherings[item]);
					}
				});
				dbuilder.create().show();
				return true;
			case R.id.gremove_player:
				ArrayList<String> names = new ArrayList<String>();
				for (int idx = 0; idx < mainLayout.getChildCount(); idx++) {
					View player = mainLayout.getChildAt(idx);

					EditText customName = (EditText) player.findViewById(R.id.custom_name);
					names.add(customName.getText().toString().trim());
				}
				final String[] aNames = names.toArray(new String[names.size()]);

				AlertDialog.Builder builderRemove = new AlertDialog.Builder(mCtx);
				builderRemove.setTitle(R.string.gathering_load);
				builderRemove.setItems(aNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						mainLayout.removeViewAt(item);
						return;
					}
				});
				builderRemove.create().show();
				return true;
			case R.id.gadd_player:
				int playersCount = mainLayout.getChildCount();
				AddPlayerRowFromData(new GatheringsPlayerData(getString(R.string.gathering_player) + " "
						+ String.valueOf(playersCount + 1), 20));
				return true;
			case R.id.gload_gathering:
				if (gIO.getNumberOfGatherings() <= 0) {
					Toast.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings, Toast.LENGTH_LONG).show();
					return true;
				}

				ArrayList<String> gatherings = gIO.getGatheringFileList();
				final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
				final String[] properNames = new String[gatherings.size()];
				for (int idx = 0; idx < gatherings.size(); idx++) {
					properNames[idx] = gIO.ReadGatheringNameFromXML(gatherings.get(idx));
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
				builder.setTitle(R.string.gathering_load);
				builder.setItems(properNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						RemoveAllPlayerRows();
						Gathering gathering = gIO.ReadGatheringXML(fGatherings[item]);

						displayModeSpinner.setSelection(gathering.mDisplayMode);
						ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
						for (GatheringsPlayerData player : players) {
							AddPlayerRowFromData(player);
						}
					}
				});
				builder.create().show();
				return true;
			case R.id.gsave_gathering:

				if (AreAnyFieldsEmpty()) {
					Toast.makeText(mCtx, R.string.gathering_empty_field, Toast.LENGTH_LONG).show();
					return true;
				}

				showDialog(DIALOG_SET_NAME);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.gathering_menu, menu);
	}
}
