package com.gelakinetic.mtgfam.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

	/* Game information */
	public ArrayList<HistoryEntry> mLifeHistory = new ArrayList<HistoryEntry>();
	public ArrayList<HistoryEntry> mPoisonHistory = new ArrayList<HistoryEntry>();
	public ArrayList<HistoryEntry> mCommanderHistory = new ArrayList<HistoryEntry>();
	private HistoryArrayAdapter mHistoryLifeAdapter;
	private HistoryArrayAdapter mHistoryPoisonAdapter;

	public int mLife = 20;
	public int mPoison = 0;
	public String mName;
	public int mCommanderCasting = 0;
	public int mDefaultLifeTotal = 20;

	/* UI Elements */
	private TextView mReadoutTextView;
	public View mView;
	private ListView mHistoryList;
	private TextView mNameTextView;
	private LinearLayout mHistoryListView;

	/* Helper */
	private Handler mHandler = new Handler();
	private final FamiliarActivity mActivity;
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

	/**
	 * Constructor
	 *
	 * @param activity The activity, used for inflating views
	 */
	public LcPlayer(FamiliarActivity activity) {
		mActivity = activity;
		mName = mActivity.getString(R.string.life_counter_default_name) + " 1";
	}

	/**
	 * Sets the display mode between life and poison. Switches the readout and history
	 *
	 * @param mode either LIFE or POISON
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
				mReadoutTextView.setTextColor(mActivity.getResources().getColor(android.R.color.holo_red_dark));
				break;
			case POISON:
				if (mHistoryList != null) {
					mHistoryList.setAdapter(mHistoryPoisonAdapter);
					mHistoryList.invalidate();
				}
				mReadoutTextView.setText(mPoison + "");
				mReadoutTextView.setTextColor(mActivity.getResources().getColor(android.R.color.holo_green_dark));
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
				break;
			case LIFE:
				mLife += delta;
				mReadoutTextView.setText(mLife + "");
				break;
		}
		mHandler.removeCallbacks(mLifePoisonCommitter);
		mHandler.postDelayed(mLifePoisonCommitter, 1000);
	}

	/**
	 * Create the View for this player, to be added to the scroll view
	 *
	 * @return The View for this player
	 */
	public View newView(boolean isCompact) {
		assert mView != null;

		if (isCompact) {
			mView = LayoutInflater.from(mActivity).inflate(R.layout.life_counter_player_compact, null);
			mHistoryList = null;
			mHistoryListView = null;
			mHistoryLifeAdapter = null;
			mHistoryPoisonAdapter = null;
		}
		else {
			mView = LayoutInflater.from(mActivity).inflate(R.layout.life_counter_player, null);
			mHistoryList = (ListView) mView.findViewById(R.id.player_history);
			mHistoryListView = (LinearLayout) mView.findViewById(R.id.player_history_layout);
			mHistoryList.setAdapter(mHistoryLifeAdapter);
			mHistoryLifeAdapter = new HistoryArrayAdapter(mActivity, LIFE);
			mHistoryPoisonAdapter = new HistoryArrayAdapter(mActivity, POISON);
		}

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
		setMode(LIFE);

		mView.findViewById(R.id.player_name).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog(NAME_DIALOG);
			}
		});
		return mView;
	}

	/**
	 * Sets the display mode by showing or hiding the history
	 *
	 * @param mode either LifeCounterFragment.DISPLAY_COMPACT or LifeCounterFragment.DISPLAY_NORMAL
	 */
	public void setDisplayMode(int mode) {
		if (mHistoryListView != null) {
			if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				switch (mode) {
					case LifeCounterFragment.DISPLAY_NORMAL:
						mHistoryListView.setVisibility(View.VISIBLE);
						break;
					case LifeCounterFragment.DISPLAY_COMPACT:
						mHistoryListView.setVisibility(View.GONE);
						break;
					case LifeCounterFragment.DISPLAY_COMMANDER:
						break;
				}
			}
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
		for (HistoryEntry entry : mCommanderHistory) {
			if (first) {
				first = false;
				data += ";" + entry.mAbsolute;
			}
			else {
				data += "," + entry.mAbsolute;
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
		mCommanderHistory.clear();
		mLife = mDefaultLifeTotal;
		mPoison = 0;
		mCommanderCasting = 0;

		mHistoryLifeAdapter.notifyDataSetChanged();
		mHistoryPoisonAdapter.notifyDataSetChanged();

		switch (mMode) {
			case LIFE:
				mReadoutTextView.setText(mLife + "");
				break;
			case POISON:
				mReadoutTextView.setText(mPoison + "");
				break;
		}
	}

	/**
	 * Set the size of the player's view
	 *
	 * @param orientation       either Configuration.ORIENTATION_LANDSCAPE Configuration.ORIENTATION_PORTRAIT
	 * @param mGridLayoutWidth  The width of the GridLayout in which to put the player's view
	 * @param mGridLayoutHeight The height of the GridLayout in which to put the player's view
	 * @param mDisplayMode      either LifeCounterFragment.DISPLAY_COMPACT or LifeCounterFragment.DISPLAY_NORMAL
	 */
	public void setSize(int orientation, int mGridLayoutWidth, int mGridLayoutHeight, int mDisplayMode) {
		GridLayout.LayoutParams params = (GridLayout.LayoutParams) mView.getLayoutParams();
		assert params != null;
		switch (orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				params.width = mGridLayoutWidth / 2;
				params.height = mGridLayoutHeight;
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				params.width = mGridLayoutWidth;
				params.height = mGridLayoutHeight / 2;
				break;
		}
		if (mDisplayMode == LifeCounterFragment.DISPLAY_COMPACT) {
			params.width /= 2;
		}
		mView.setLayoutParams(params);
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
			super(context, R.layout.history_adapter_row, (type == LIFE) ? mLifeHistory : mPoisonHistory);
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
				view = LayoutInflater.from(mActivity).inflate(R.layout.history_adapter_row, null);
			}
			assert view != null;
			switch (mType) {
				case LIFE:
					((TextView) view.findViewById(R.id.absolute)).setText(mLifeHistory.get(position).mAbsolute + "");
					if (mLifeHistory.get(position).mDelta > 0) {
						((TextView) view.findViewById(R.id.relative)).setText("+" + mLifeHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mActivity.getResources().getColor(android.R.color.holo_green_dark));
					}
					else {
						((TextView) view.findViewById(R.id.relative)).setText("" + mLifeHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mActivity.getResources().getColor(android.R.color.holo_red_dark));
					}
					break;
				case POISON:
					((TextView) view.findViewById(R.id.absolute)).setText(mPoisonHistory.get(position).mAbsolute + "");
					if (mPoisonHistory.get(position).mDelta > 0) {
						((TextView) view.findViewById(R.id.relative)).setText("+" + mPoisonHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mActivity.getResources().getColor(android.R.color.holo_green_dark));
					}
					else {
						((TextView) view.findViewById(R.id.relative)).setText("" + mPoisonHistory.get(position).mDelta);
						((TextView) view.findViewById(R.id.relative)).setTextColor(mActivity.getResources().getColor(android.R.color.holo_red_dark));
					}
					break;
			}
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

		mActivity.removeDialogFragment(mActivity.getSupportFragmentManager());

		/* Create and show the dialog. */
		final FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case NAME_DIALOG: {
						View textEntryView = mActivity.getLayoutInflater().inflate(R.layout.alert_dialog_text_entry, null);
						assert textEntryView != null;
						final EditText nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
						nameInput.setText(LcPlayer.this.mName);

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
									}
								})
								.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								})
								.create();
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(mActivity.getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}
}