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
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.LcPlayer.HistoryEntry;

import java.util.ArrayList;

/**
 * TODO save life / poison state on rotation, etc
 */
public class LifeCounterFragment extends FamiliarFragment {

	private static final int REMOVE_PLAYER_DIALOG = 0;
	private static final int RESET_CONFIRM_DIALOG = 1;
	private static final String DISPLAY_MODE = "display_mode";

	private LinearLayout mLinearLayout;
	private ArrayList<LcPlayer> mPlayers = new ArrayList<LcPlayer>();
	private ImageView mPoisonButton;
	private ImageView mLifeButton;
	private int mDisplayMode;

	/**
	 * Force the child fragments to override onCreateView
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
		View myFragmentView = inflater.inflate(R.layout.life_counter_frag, container, false);
		assert myFragmentView != null;

		mLinearLayout = (LinearLayout) myFragmentView.findViewById(R.id.playerList);

		mPoisonButton = (ImageView) myFragmentView.findViewById(R.id.poison_button);
		mPoisonButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setMode(LcPlayer.POISON);
			}
		});
		mLifeButton = (ImageView) myFragmentView.findViewById(R.id.life_button);
		mLifeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setMode(LcPlayer.LIFE);
			}
		});
		myFragmentView.findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showDialog(RESET_CONFIRM_DIALOG);
			}
		});

		if (savedInstanceState != null) {
			mDisplayMode = savedInstanceState.getInt(DISPLAY_MODE, LcPlayer.LIFE);
		}

		return myFragmentView;
	}

	/**
	 * TODO
	 */
	@Override
	public void onPause() {
		super.onPause();
		String playerData = "";
		for (LcPlayer player : mPlayers) {
			playerData += player.toString();
		}
		((FamiliarActivity) getActivity()).mPreferenceAdapter.setPlayerData(playerData);
		for (LcPlayer player : mPlayers) {
			mLinearLayout.removeView(player.getView());
		}
		mPlayers.clear();
	}

	/**
	 * TODO
	 */
	@Override
	public void onResume() {
		super.onResume();
		String playerData = ((FamiliarActivity) getActivity()).mPreferenceAdapter.getPlayerData();
		if (playerData == null || playerData.length() == 0) {
			addPlayer();
			addPlayer();
		}
		else {
			String[] playerLines = playerData.split("\n");
			for (String line : playerLines) {
				addPlayer(line);
			}
		}
		setMode(mDisplayMode);
	}

	/**
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.life_counter_menu, menu);
	}

	/**
	 * @param item
	 * @return
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.add_player:
				addPlayer();
				return true;
			case R.id.remove_player:
				showDialog(REMOVE_PLAYER_DIALOG);
				return true;
			case R.id.announce_life:
				return true;
			case R.id.change_gathering:
				return true;
			case R.id.set_gathering:
				return true;
			case R.id.display_mode:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * TODO
	 *
	 * @param outState Bundle in which to place your saved state.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(DISPLAY_MODE, mDisplayMode);
		super.onSaveInstanceState(outState);
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
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
				setShowsDialog(true);

				switch (id) {
					case REMOVE_PLAYER_DIALOG: {
						String[] names = new String[mPlayers.size()];
						for (int i = 0; i < mPlayers.size(); i++) {
							names[i] = mPlayers.get(i).mName;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(getString(R.string.life_counter_remove_player));

						builder.setItems(names, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								mLinearLayout.removeView(mPlayers.get(item).getView());
								mPlayers.remove(item);
								mLinearLayout.invalidate();
							}
						});

						return builder.create();
					}
					case RESET_CONFIRM_DIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder
								.setMessage(getString(R.string.life_counter_clear_dialog_text))
								.setCancelable(true)
								.setPositiveButton(getString(R.string.dialog_both), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										/* Remove all players */
										for (LcPlayer player : mPlayers) {
											mLinearLayout.removeView(player.getView());
										}
										mPlayers.clear();

										/* Add default players */
										addPlayer();
										addPlayer();

										dialog.dismiss();
									}
								}).setNeutralButton(getString(R.string.dialog_life), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								for (LcPlayer player : mPlayers) {
									player.resetStats();
								}
								mLinearLayout.invalidate();
								dialog.dismiss();
							}
						}).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
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
		newFragment.show(getFragmentManager(), FamiliarActivity.DIALOG_TAG);
	}

	/**
	 * TODO
	 *
	 * @param displayMode
	 */
	private void setMode(int displayMode) {
		mDisplayMode = displayMode;

		switch (displayMode) {
			case LcPlayer.LIFE:
				mPoisonButton.setImageResource(R.drawable.lc_poison_enabled);
				mLifeButton.setImageResource(R.drawable.lc_life_disabled);
				break;
			case LcPlayer.POISON:
				mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
				mLifeButton.setImageResource(R.drawable.lc_life_enabled);
				break;
		}

		for (LcPlayer player : mPlayers) {
			player.setMode(displayMode);
		}
	}

	/**
	 * TODO
	 */
	private void addPlayer() {
		LcPlayer player = new LcPlayer((FamiliarActivity) getActivity());
		mPlayers.add(player);
		mLinearLayout.addView(player.newView());
	}

	/**
	 * TODO
	 *
	 * @param line
	 */
	private void addPlayer(String line) {
		try {
			LcPlayer player = new LcPlayer((FamiliarActivity) getActivity());

			String[] data = line.split(";");

			player.mName = data[0];
			player.mLife = Integer.parseInt(data[1]);

			try {
				String[] lifeHistory = data[2].split(","); // ArrayIndexOutOfBoundsException??
				player.mLifeHistory = new ArrayList<HistoryEntry>(lifeHistory.length);
				HistoryEntry entry;
				for (int i = lifeHistory.length - 1; i >= 0; i--) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(lifeHistory[i]);
					if (i != lifeHistory.length - 1) {
						entry.mDelta = entry.mAbsolute - player.mLifeHistory.get(0).mAbsolute;
					}
					else {
						entry.mDelta = 0;
					}
					player.mLifeHistory.add(0, entry);
				}

			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			player.mPoison = Integer.parseInt(data[3]);

			try {
				String[] poisonHistory = data[4].split(","); // ArrayIndexOutOfBoundsException??
				player.mPoisonHistory = new ArrayList<HistoryEntry>(poisonHistory.length);
				HistoryEntry entry;
				for (int i = poisonHistory.length - 1; i >= 0; i--) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(poisonHistory[i]);
					if (i != poisonHistory.length - 1) {
						entry.mDelta = entry.mAbsolute - player.mPoisonHistory.get(0).mAbsolute;
					}
					else {
						entry.mDelta = 0;
					}
					player.mPoisonHistory.add(0, entry);
				}

			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			try {
				player.mDefaultLifeTotal = Integer.parseInt(data[5]);
			} catch (Exception e) {
				player.mDefaultLifeTotal = 20; // TODO static const?
			}

			try {
				String[] commanderLifeString = data[6].split(",");
				player.mCommanderHistory = new ArrayList<HistoryEntry>(commanderLifeString.length);
				HistoryEntry entry;
				for (String aCommanderLifeString : commanderLifeString) {
					entry = new HistoryEntry();
					entry.mAbsolute = Integer.parseInt(aCommanderLifeString);
					player.mCommanderHistory.add(entry);
				}
			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			try {
				player.mCommanderCasting = Integer.parseInt(data[7]);
			} catch (Exception e) {
				player.mCommanderCasting = 0;
			}

			mPlayers.add(player);
			mLinearLayout.addView(player.newView());
		} catch (ArrayIndexOutOfBoundsException e) {
			/* Eat it */
		}
	}
}
