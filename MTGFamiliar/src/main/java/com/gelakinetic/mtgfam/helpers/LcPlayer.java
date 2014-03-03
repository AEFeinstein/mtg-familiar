package com.gelakinetic.mtgfam.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;

import java.util.ArrayList;

public class LcPlayer {
	/* Constants */
	public final static int LIFE = 0;
	public final static int POISON = 1;
	public final static int COMMANDER = 2;

	public final static int NAME_DIALOG = 0;
	public final static int COMMANDER_DIALOG = 1;

	/* Game information */
	public ArrayList<HistoryEntry> mLifeHistory = new ArrayList<HistoryEntry>();
	public ArrayList<HistoryEntry> mPoisonHistory = new ArrayList<HistoryEntry>();
	public ArrayList<CommanderEntry> mCommanderDamage = new ArrayList<CommanderEntry>();
	private HistoryArrayAdapter mHistoryLifeAdapter;
	private HistoryArrayAdapter mHistoryPoisonAdapter;
	public CommanderDamageAdapter mCommanderDamageAdapter;

	public int mLife = 20;
	public int mPoison = 0;
	public String mName;
	public int mCommanderCasting = 0;
	public int mDefaultLifeTotal = 20;

	/* UI Elements */
	private TextView mNameTextView;
	private TextView mReadoutTextView;
	private TextView mCommanderNameTextView;
	private TextView mCommanderReadoutTextView;
	private Button mCommanderCastingButton;

	public View mView;
	public View mCommanderRowView;

	private ListView mHistoryList;

	/* Helper */
	private Handler mHandler = new Handler();
	private final LifeCounterFragment mFragment;
	private int mMode = LIFE;

	Runnable mLifePoisonCommitter = new Runnable() {
		@Override
		public void run() {
			HistoryEntry entry = new HistoryEntry();
			if (mLifeHistory.size() == 0) {
				entry.mDelta = mLife - mDefaultLifeTotal;
			}
			else {
				entry.mDelta = mLife - mLifeHistory.get(0).mAbsolute;
			}
			entry.mAbsolute = mLife;
			if (entry.mDelta != 0) {
				mLifeHistory.add(0, entry);
				if (mHistoryLifeAdapter != null) {
					mHistoryLifeAdapter.notifyDataSetChanged();
				}
			}

			entry = new HistoryEntry();
			if (mPoisonHistory.size() == 0) {
				entry.mDelta = mPoison;
			}
			else {
				entry.mDelta = mPoison - mPoisonHistory.get(0).mAbsolute;
			}
			entry.mAbsolute = mPoison;
			if (entry.mDelta != 0) {
				mPoisonHistory.add(0, entry);
				if (mHistoryPoisonAdapter != null) {
					mHistoryPoisonAdapter.notifyDataSetChanged();
				}
			}
		}
	};
	private int mTakingDamageFromCommander;

	public LcPlayer(LifeCounterFragment fragment) {
		mFragment = fragment;
		mName = mFragment.getActivity().getString(R.string.life_counter_default_name) + " 1";
	}

	/**
	 * Sets the display mode between life and poison. Switches the readout and history
	 *
	 * @param mode either LIFE, POISON, or COMMANDER
	 */
	public void setMode(int mode) {
		mMode = mode;
		switch (mMode) {
			case LIFE:
				if (mHistoryList != null) {
					mHistoryList.setAdapter(mHistoryLifeAdapter);
					mHistoryList.invalidate();
				}
				mReadoutTextView.setText(mLife + "");
				mReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
				if (mCommanderReadoutTextView != null) {
					mCommanderReadoutTextView.setText(mLife + "");
					mCommanderReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
				}
				break;
			case POISON:
				if (mHistoryList != null) {
					mHistoryList.setAdapter(mHistoryPoisonAdapter);
					mHistoryList.invalidate();
				}
				mReadoutTextView.setText(mPoison + "");
				mReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_green_dark));
				if (mCommanderReadoutTextView != null) {
					mCommanderReadoutTextView.setText(mPoison + "");
					mCommanderReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_green_dark));
				}
				break;
			case COMMANDER:
				if (mHistoryList != null) {
					mHistoryList.setAdapter(mCommanderDamageAdapter);
					mHistoryList.invalidate();
				}
				mReadoutTextView.setText(mLife + "");
				mReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
				if (mCommanderReadoutTextView != null) {
					mCommanderReadoutTextView.setText(mLife + "");
					mCommanderReadoutTextView.setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
				}
				break;
		}
	}

	/**
	 * Convenience method to change the currently displayed value, either poison or life, and start the handler to
	 * commit the value to history
	 *
	 * @param delta How much the current value should be changed
	 */
	private void changeValue(int delta) {
		switch (mMode) {
			case POISON:
				mPoison += delta;
				mReadoutTextView.setText(mPoison + "");
				if (mCommanderReadoutTextView != null) {
					mCommanderReadoutTextView.setText(mPoison + "");
				}
				break;
			case COMMANDER:
			case LIFE:
				mLife += delta;
				mReadoutTextView.setText(mLife + "");
				if (mCommanderReadoutTextView != null) {
					mCommanderReadoutTextView.setText(mLife + "");
				}
				break;
		}
		mHandler.removeCallbacks(mLifePoisonCommitter);
		mHandler.postDelayed(mLifePoisonCommitter, 1000);
	}

	public View newView(int displayMode, int statType) {
		switch (displayMode) {
			case LifeCounterFragment.DISPLAY_COMMANDER:
			case LifeCounterFragment.DISPLAY_NORMAL: {
				mView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player, null);
				assert mView != null;
				mHistoryList = (ListView) mView.findViewById(R.id.player_history);
				mHistoryLifeAdapter = new HistoryArrayAdapter(mFragment.getActivity(), LIFE);
				mHistoryPoisonAdapter = new HistoryArrayAdapter(mFragment.getActivity(), POISON);
				mCommanderDamageAdapter = new CommanderDamageAdapter(mFragment.getActivity());

				mCommanderCastingButton = (Button) mView.findViewById(R.id.commanderCast);
				mCommanderCastingButton.setText("" + mCommanderCasting);

				/* If it's commander, also inflate the entry to display in the grid */
				if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
					mView.findViewById(R.id.commanderCastText).setVisibility(View.VISIBLE);
					mCommanderCastingButton.setVisibility(View.VISIBLE);
					mCommanderCastingButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							mCommanderCasting++;
							mCommanderCastingButton.setText("" + mCommanderCasting);
						}
					});

					mCommanderRowView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player_commander, null);
					assert mCommanderRowView != null;
					mCommanderNameTextView = (TextView) mCommanderRowView.findViewById(R.id.player_name);
					if (mName != null) {
						mCommanderNameTextView.setText(mName);
					}
					mCommanderReadoutTextView = (TextView) mCommanderRowView.findViewById(R.id.player_readout);
				}
				else {
					mView.findViewById(R.id.commanderCastText).setVisibility(View.GONE);
					mCommanderCastingButton.setVisibility(View.GONE);
				}

				break;
			}
			case LifeCounterFragment.DISPLAY_COMPACT: {
				mView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player_compact, null);
				mHistoryList = null;
				mHistoryLifeAdapter = null;
				mHistoryPoisonAdapter = null;
				mCommanderDamageAdapter = null;
				break;
			}
		}
		assert mView != null;

		mNameTextView = (TextView) mView.findViewById(R.id.player_name);
		if (mName != null) {
			mNameTextView.setText(mName);
		}
		mReadoutTextView = (TextView) mView.findViewById(R.id.player_readout);

		mView.findViewById(R.id.player_minus1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeValue(-1);
			}
		});
		mView.findViewById(R.id.player_minus5).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeValue(-5);
			}
		});
		mView.findViewById(R.id.player_plus1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeValue(1);
			}
		});
		mView.findViewById(R.id.player_plus5).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				changeValue(5);
			}
		});

		mView.findViewById(R.id.player_name).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(NAME_DIALOG);
			}
		});

		setMode(statType);

		if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
			return mCommanderRowView;
		}
		else {
			return mView;
		}
	}

	/**
	 * Returns a string containing all the player data in the form:
	 * name; life; life History; poison; poison History; default Life; commander History; commander casting
	 * The history entries are comma delimited
	 *
	 * @return A string of player data
	 */
	public String toString() {
		String data = mName.replace(";", "") + ";";

		boolean first = true;
		data += mLife + ";";
		for (HistoryEntry entry : mLifeHistory) {
			if (first) {
				first = false;
				data += entry.mAbsolute;
			}
			else {
				data += "," + entry.mAbsolute;
			}
		}

		data += ";";

		first = true;
		data += mPoison + ";";
		for (HistoryEntry entry : mPoisonHistory) {
			if (first) {
				first = false;
				data += entry.mAbsolute;
			}
			else {
				data += "," + entry.mAbsolute;
			}
		}

		data += ";" + mDefaultLifeTotal;

		first = true;
		for (CommanderEntry entry : mCommanderDamage) {
			if (first) {
				first = false;
				data += ";" + entry.mLife;
			}
			else {
				data += "," + entry.mLife;
			}
		}

		data += ";" + mCommanderCasting;

		return data + ";\n";
	}

	/**
	 * Reset the life, poison, and commander damage to default while preserving the name
	 */
	public void resetStats() {
		mLifeHistory.clear();
		mPoisonHistory.clear();
		mLife = mDefaultLifeTotal;
		mPoison = 0;
		mCommanderCasting = 0;

		for(CommanderEntry entry : mCommanderDamage) {
			entry.mLife = 0;
		}

		if (mHistoryLifeAdapter != null) {
			mHistoryLifeAdapter.notifyDataSetChanged();
		}
		if (mHistoryPoisonAdapter != null) {
			mHistoryPoisonAdapter.notifyDataSetChanged();
		}
		if (mCommanderDamageAdapter != null) {
			mCommanderDamageAdapter.notifyDataSetChanged();
		}

		/* Redraw life totals */
		changeValue(0);
		if(mCommanderCastingButton != null) {
			mCommanderCastingButton.setText(""+mCommanderCasting);
		}
	}

	/**
	 * Set the size of the player's view
	 *
	 * @param mGridLayoutWidth  The width of the GridLayout in which to put the player's view
	 * @param mGridLayoutHeight The height of the GridLayout in which to put the player's view
	 * @param mDisplayMode      either LifeCounterFragment.DISPLAY_COMPACT or LifeCounterFragment.DISPLAY_NORMAL
	 */
	public void setSize(int mGridLayoutWidth, int mGridLayoutHeight, int mDisplayMode, boolean isPortrait) {

		switch (mDisplayMode) {
			case LifeCounterFragment.DISPLAY_NORMAL: {
				GridLayout.LayoutParams params = (GridLayout.LayoutParams) mView.getLayoutParams();
				assert params != null;
				if (isPortrait) {
					/* Portrait */
					params.width = mGridLayoutWidth;
					params.height = mGridLayoutHeight / 2;
				}
				else {
					/* Landscape */
					params.width = mGridLayoutWidth / 2;
					params.height = mGridLayoutHeight;
				}
				mView.setLayoutParams(params);
				break;
			}
			case LifeCounterFragment.DISPLAY_COMPACT: {
				GridLayout.LayoutParams params = (GridLayout.LayoutParams) mView.getLayoutParams();
				assert params != null;
				if (isPortrait) {
					/* Portrait */
					params.width = mGridLayoutWidth / 2;
					params.height = mGridLayoutHeight / 2;
				}
				else {
					/* Landscape */
					params.width = mGridLayoutWidth / 4;
					params.height = mGridLayoutHeight;
				}
				mView.setLayoutParams(params);
				break;
			}
			case LifeCounterFragment.DISPLAY_COMMANDER: {
				GridLayout.LayoutParams rowParams = (GridLayout.LayoutParams) mCommanderRowView.getLayoutParams();
				assert rowParams != null;
				rowParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mFragment.getActivity().getResources().getDisplayMetrics());
				if (isPortrait) {
					/* Portrait */
					rowParams.width = mGridLayoutWidth / 2;
				}
				else {
					/* Landscape */
					rowParams.width = mGridLayoutWidth / 4;
				}
				mCommanderRowView.setLayoutParams(rowParams);

				LinearLayout.LayoutParams viewParams = (LinearLayout.LayoutParams) mView.getLayoutParams();
				if (viewParams != null) {
					if (!isPortrait) {
					/* Landscape */
						viewParams.width = mGridLayoutWidth / 2;
					}
					mView.setLayoutParams(viewParams);
				}
				break;
			}
		}
	}

	/**
	 * Inner class to encapsulate an entry in the history list
	 */
	public static class HistoryEntry {
		public int mDelta;
		public int mAbsolute;
	}

	/**
	 * Inner class to display the HistoryEntries in a ListView
	 */
	public class HistoryArrayAdapter extends ArrayAdapter<HistoryEntry> {

		private final int mType;

		public HistoryArrayAdapter(Context context, int type) {
			super(context, R.layout.life_counter_history_adapter_row, (type == LIFE) ? mLifeHistory : mPoisonHistory);
			mType = type;
		}

		/**
		 * Called to get a view for an entry in the listView
		 *
		 * @param position    The position of the listView to populate
		 * @param convertView The old view to reuse, if possible. Since the layouts for entries and headers are
		 *                    different, this will be ignored
		 * @param parent      The parent this view will eventually be attached to
		 * @return The view for the data at this position
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView != null) {
				view = convertView;
			}
			else {
				view = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_history_adapter_row, null);
			}
			assert view != null;
			switch (mType) {
				case LIFE:
					((TextView) view.findViewById(R.id.absolute)).setText(mLifeHistory.get(position).mAbsolute + "");
					if (mLifeHistory.get(position).mDelta > 0) {
						((TextView) view.findViewById(R.id.relative)).setText("+" + mLifeHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_green_dark));
					}
					else {
						((TextView) view.findViewById(R.id.relative)).setText("" + mLifeHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
					}
					break;
				case POISON:
					((TextView) view.findViewById(R.id.absolute)).setText(mPoisonHistory.get(position).mAbsolute + "");
					if (mPoisonHistory.get(position).mDelta > 0) {
						((TextView) view.findViewById(R.id.relative)).setText("+" + mPoisonHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_green_dark));
					}
					else {
						((TextView) view.findViewById(R.id.relative)).setText("" + mPoisonHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mFragment.getActivity().getResources().getColor(R.color.holo_red_dark));
					}
					break;
			}
			return view;
		}

	}

	/**
	 * Inner class to encapsulate an entry in the history list
	 */
	public static class CommanderEntry implements Comparable<CommanderEntry> {
		public int mLife;
		public String mName = "Test";

		@Override
		public int compareTo(CommanderEntry commanderEntry) {
			if (commanderEntry.mName.equals(mName)) {
				return 0;
			}
			return 1;
		}
	}

	/**
	 * Inner class to display the HistoryEntries in a ListView
	 */
	public class CommanderDamageAdapter extends ArrayAdapter<CommanderEntry> {

		public CommanderDamageAdapter(Context context) {
			super(context, R.layout.life_counter_player_commander, mCommanderDamage);
		}

		/**
		 * Called to get a view for an entry in the listView
		 *
		 * @param position    The position of the listView to populate
		 * @param convertView The old view to reuse, if possible. Since the layouts for entries and headers are
		 *                    different, this will be ignored
		 * @param parent      The parent this view will eventually be attached to
		 * @return The view for the data at this position
		 */
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView != null) {
				view = convertView;
			}
			else {
				view = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player_commander, null);
			}
			assert view != null;

			((TextView) view.findViewById(R.id.player_name)).setText(mCommanderDamage.get(position).mName + "");
			((TextView) view.findViewById(R.id.player_readout)).setText(mCommanderDamage.get(position).mLife + "");

			view.findViewById(R.id.dividerH).setVisibility(View.GONE);
			view.findViewById(R.id.dividerV).setVisibility(View.GONE);

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mTakingDamageFromCommander = position;
					showDialog(COMMANDER_DIALOG);
				}
			});
			return view;
		}

	}

	/**
	 * Remove any showing dialogs, and show the requested one
	 *
	 * @param id the ID of the dialog to show
	 */
	void showDialog(final int id) {
		/* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
		currently showing dialog, so make our own transaction and take care of that here. */

		((FamiliarActivity) mFragment.getActivity()).removeDialogFragment(mFragment.getActivity().getSupportFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case NAME_DIALOG: {
						View textEntryView = mFragment.getActivity().getLayoutInflater().inflate(R.layout.alert_dialog_text_entry, null);
						assert textEntryView != null;
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
						nameInput.setText(LcPlayer.this.mName);
						textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								nameInput.setText("");
							}
						});

						return new AlertDialog.Builder(getActivity())
								.setTitle(R.string.life_counter_edit_name_dialog_title)
								.setView(textEntryView)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										if (nameInput == null) {
											return;
										}
										assert nameInput.getText() != null;
										String newName = nameInput.getText().toString();
										if (newName.equals("")) {
											return;
										}
										LcPlayer.this.mName = newName;
										LcPlayer.this.mNameTextView.setText(newName);
										if (LcPlayer.this.mCommanderNameTextView != null) {
											LcPlayer.this.mCommanderNameTextView.setText(newName);
										}
										mFragment.setCommanderInfo(-1);
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								})
								.create();
					}
					case COMMANDER_DIALOG: {
						View view = LayoutInflater.from(getActivity()).inflate(R.layout.life_counter_edh_dialog, null);
						assert view != null;
						final TextView deltaText = (TextView) view.findViewById(R.id.delta);
						final TextView absoluteText = (TextView) view.findViewById(R.id.absolute);

						final int[] delta = {0};
						final int[] absolute = {mCommanderDamage.get(mTakingDamageFromCommander).mLife};

						deltaText.setText(((delta[0] >= 0) ? "+" : "-") + delta[0]);
						absoluteText.setText("" + absolute[0]);

						view.findViewById(R.id.commander_plus1).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								delta[0]++;
								absolute[0]++;
								deltaText.setText(((delta[0] >= 0) ? "+" : "-") + delta[0]);
								absoluteText.setText("" + absolute[0]);
							}
						});

						view.findViewById(R.id.commander_minus1).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								delta[0]--;
								absolute[0]--;
								deltaText.setText(((delta[0] >= 0) ? "+" : "-") + delta[0]);
								absoluteText.setText("" + absolute[0]);
							}
						});

						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(String.format(getResources().getString(R.string.life_counter_edh_dialog_title), mCommanderDamage.get(mTakingDamageFromCommander).mName))
								.setView(view)
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										dialogInterface.dismiss();
									}
								})
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										mCommanderDamage.get(mTakingDamageFromCommander).mLife = absolute[0];
										mCommanderDamageAdapter.notifyDataSetChanged();
										changeValue(-delta[0]);
									}
								});

						return builder.create();
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(mFragment.getActivity().getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}
}