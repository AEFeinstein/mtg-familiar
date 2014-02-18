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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;

import java.util.ArrayList;

public class LcPlayer {
	/* Constants */
	public final static int LIFE = 0;
	public final static int POISON = 1;

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
	private View mView;
	private ListView mHistoryList;
	private TextView mNameTextView;

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
				mHistoryLifeAdapter.notifyDataSetChanged();
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
				mHistoryPoisonAdapter.notifyDataSetChanged();
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
				mHistoryList.setAdapter(mHistoryLifeAdapter);
				mReadoutTextView.setText(mLife + "");
				mReadoutTextView.setTextColor(mActivity.getResources().getColor(android.R.color.holo_red_dark));
				break;
			case POISON:
				mHistoryList.setAdapter(mHistoryPoisonAdapter);
				mReadoutTextView.setText(mPoison + "");
				mReadoutTextView.setTextColor(mActivity.getResources().getColor(android.R.color.holo_green_dark));
				break;
		}
		mHistoryList.invalidate();
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
	public View newView() {
		mView = LayoutInflater.from(mActivity).inflate(R.layout.life_counter_player, null);
		assert mView != null;
		mNameTextView = (TextView) mView.findViewById(R.id.player_name);
		if (mName != null) {
			mNameTextView.setText(mName);
		}
		mReadoutTextView = (TextView) mView.findViewById(R.id.player_readout);
		mHistoryList = (ListView) mView.findViewById(R.id.player_history);
		mHistoryLifeAdapter = new HistoryArrayAdapter(mActivity, LIFE);
		mHistoryPoisonAdapter = new HistoryArrayAdapter(mActivity, POISON);
		mHistoryList.setAdapter(mHistoryLifeAdapter);

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

	public View getView() {
		return mView;
	}

	public String toString() {
		String data = mName + ";";

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

	public void setSize(int orientation, int mListSizeWidth, int mListSizeHeight) {
		switch (orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				mView.setLayoutParams(new LinearLayout.LayoutParams(mListSizeWidth/2, mListSizeHeight));
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				mView.setLayoutParams(new LinearLayout.LayoutParams(mListSizeWidth, mListSizeHeight/2));
				break;
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

		/* If the fragment isn't visible (maybe being loaded by the pager), don't show dialogs */
//		if (!this.isVisible()) {
//			return;
//		}

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