package com.gelakinetic.mtgfam.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.LcPlayer.CommanderEntry;
import com.gelakinetic.mtgfam.helpers.LcPlayer.HistoryEntry;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class LifeCounterFragment extends FamiliarFragment implements TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnUtteranceCompletedListener {

	/* Dialog Constants */
	private static final int REMOVE_PLAYER_DIALOG = 0;
	private static final int RESET_CONFIRM_DIALOG = 1;
	private static final int DIALOG_CHANGE_DISPLAY = 2;
	private static final int SET_GATHERING_DIALOG = 3;

	/* Constant for persisting data */
	private static final String DISPLAY_MODE = "display_mode";

	/* Constants for TTS */
	private static final String LIFE_ANNOUNCE = "life_announce";
	private static final int IMPROBABLE_NUMBER = 531865548;
	private static final String OVER_9000_KEY = "@over_9000";

	/* constants for display mode */
	public static final int DISPLAY_NORMAL = 0;
	public static final int DISPLAY_COMPACT = 1;
	public static final int DISPLAY_COMMANDER = 2;

	/* UI Elements, measurement */
	private GridLayout mGridLayout;
	private LinearLayout mCommanderPlayerView;
	private ImageView mPoisonButton;
	private ImageView mLifeButton;
	private ImageView mCommanderButton;
	private View mScrollView;
	private int mListSizeWidth = -1;
	private int mListSizeHeight = -1;

	private int mDisplayMode = DISPLAY_NORMAL;

	/* Keeping track of players */
	private ArrayList<LcPlayer> mPlayers = new ArrayList<LcPlayer>();
	private int mStatDisplaying = LcPlayer.LIFE;
	private int mLargestPlayerNumber = 0;

	/* Stuff for TTS */
	private TextToSpeech mTts;
	private boolean mTtsInit;
	private AudioManager mAudioManager;
	private MediaPlayer m9000Player;
	LinkedList<String> mVocalizations = new LinkedList<String>();

	/* For proper dimming during wake lock */
	private float mDefaultBrightness;

	/**
	 * When the fragment is created, set up the TTS engine, AudioManager, and MediaPlayer for life total vocalization
	 *
	 * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTtsInit = false;
		mTts = new TextToSpeech(getActivity(), this);
		mTts.setOnUtteranceCompletedListener(this);

		mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

		m9000Player = MediaPlayer.create(getActivity(), R.raw.over_9000);
		m9000Player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				onUtteranceCompleted(LIFE_ANNOUNCE);
			}
		});
	}

	/**
	 * When the fragment is destroyed, clean up the TTS engine and MediaPlayer
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		if (m9000Player != null) {
			m9000Player.reset();
			m9000Player.release();
		}
	}

	/**
	 * Set up the base UI, clear the measurement data and attach a ViewTreeObserver to measure the UI when it is drawn.
	 * Get the life/poison mode from the savedInstanceState if the fragment is persisting.
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
		mListSizeWidth = -1;
		mListSizeHeight = -1;

		View myFragmentView = inflater.inflate(R.layout.life_counter_frag, container, false);
		assert myFragmentView != null;
		mGridLayout = (GridLayout) myFragmentView.findViewById(R.id.playerList);

		mDisplayMode = Integer.valueOf(((FamiliarActivity) getActivity()).mPreferenceAdapter.getDisplayMode());

		mCommanderPlayerView = (LinearLayout) myFragmentView.findViewById(R.id.commander_player);

		mScrollView = myFragmentView.findViewById(R.id.playerScrollView);
		ViewTreeObserver viewTreeObserver = mScrollView.getViewTreeObserver();
		assert viewTreeObserver != null;
		viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			public void onGlobalLayout() {
				if (isVisible()) {
					boolean changed = false;
					if (mListSizeHeight < mScrollView.getHeight()) {
						mListSizeHeight = mScrollView.getHeight();
						changed = true;
					}
					if (mListSizeWidth < mScrollView.getWidth()) {
						mListSizeWidth = mScrollView.getWidth();
						changed = true;
					}
					if (changed) {
						if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
							if (mDisplayMode == DISPLAY_COMMANDER) {
								/* Conveniently takes care of re-adding the sized views in the right number of rows */
								changeDisplayMode(mDisplayMode);
							}
						}
						for (LcPlayer player : mPlayers) {
							player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode, getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
						}
					}
				}
			}
		});

		mPoisonButton = (ImageView) myFragmentView.findViewById(R.id.poison_button);
		mPoisonButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setStatDisplaying(LcPlayer.POISON);
			}
		});

		mLifeButton = (ImageView) myFragmentView.findViewById(R.id.life_button);
		mLifeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setStatDisplaying(LcPlayer.LIFE);
			}
		});

		mCommanderButton = (ImageView) myFragmentView.findViewById(R.id.commander_button);
		mCommanderButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setStatDisplaying(LcPlayer.COMMANDER);
			}
		});

		myFragmentView.findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showDialog(RESET_CONFIRM_DIALOG);
			}
		});

		if (savedInstanceState != null) {
			mStatDisplaying = savedInstanceState.getInt(DISPLAY_MODE, LcPlayer.LIFE);
		}

		WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
		mDefaultBrightness = layoutParams.screenBrightness;

		return myFragmentView;
	}

	/**
	 * When the orientation is changed, save mStatDisplaying so that the fragment can display the right thing
	 * when it is recreated
	 *
	 * @param outState Bundle in which to place your saved state.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(DISPLAY_MODE, mStatDisplaying);
		super.onSaveInstanceState(outState);
	}

	/**
	 * When the fragment is paused, save all the player data to shared preferences, in string form, then remove all
	 * the player views from the ListView and clear the players ArrayList.
	 */
	@Override
	public void onPause() {
		super.onPause();
		String playerData = "";
		for (LcPlayer player : mPlayers) {
			playerData += player.toString();
		}
		((FamiliarActivity) getActivity()).mPreferenceAdapter.setPlayerData(playerData);
		mGridLayout.removeAllViews();
		mPlayers.clear();

		getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/**
	 * When the fragment is resumed, attempt to populate the life counter with player information in shared preferences.
	 * If that doesn't exist, add the two default players. Set whether to display life or poison based on persisted
	 * data.
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

		setCommanderInfo(-1);

		changeDisplayMode(mDisplayMode);

		setStatDisplaying(mStatDisplaying);

		if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getWakelock()) {
			getActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	/**
	 * Called from the activity if the user is inactive. May be overridden to do things like dim the screen
	 */
	@Override
	public void onUserInactive() {
		if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getWakelock()) {
			WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
			layoutParams.screenBrightness = 0.0f;
			getActivity().getWindow().setAttributes(layoutParams);
		}
	}

	@Override
	public void onUserActive() {
		if (((FamiliarActivity) getActivity()).mPreferenceAdapter.getWakelock()) {
			WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
			layoutParams.screenBrightness = mDefaultBrightness;
			getActivity().getWindow().setAttributes(layoutParams);
		}
	}

	/**
	 * Inflate the options menu.
	 *
	 * @param menu     The options menu in which you place your items.
	 * @param inflater The inflater to use to inflate the menu
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.life_counter_menu, menu);
	}

	/**
	 * If TTS is not initialized, remove it from the menu. If it is initialized, show it.
	 *
	 * @param menu The menu to show or hide the "announce life totals" button in.
	 */
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem menuItem = menu.findItem(R.id.announce_life);
		assert menuItem != null;
		if (!mTtsInit || !((FamiliarActivity) getActivity()).mIsMenuVisible) {
			menuItem.setVisible(false);
		}
		else {
			menuItem.setVisible(true);
		}
	}

	/**
	 * Handle menu items being selected
	 *
	 * @param item The menu item selected
	 * @return true if the selection was acted upon, false otherwise
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.add_player:
				/* Add the player to the ArrayList, and draw the new view */
				addPlayer();
				setCommanderInfo(-1);
				addPlayerView(mPlayers.get(mPlayers.size() - 1), true);
				return true;
			case R.id.remove_player:
				showDialog(REMOVE_PLAYER_DIALOG);
				return true;
			case R.id.announce_life:
				announceLifeTotals();
				return true;
			case R.id.change_gathering:
				GatheringsFragment rlFrag = new GatheringsFragment();
				startNewFragment(rlFrag, null);
				return true;
			case R.id.set_gathering:
				showDialog(SET_GATHERING_DIALOG);
				return true;
			case R.id.display_mode:
				showDialog(DIALOG_CHANGE_DISPLAY);
				return true;
			default:
				return super.onOptionsItemSelected(item);
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

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				switch (id) {
					case REMOVE_PLAYER_DIALOG: {
						/* Get all the player names */
						String[] names = new String[mPlayers.size()];
						for (int i = 0; i < mPlayers.size(); i++) {
							names[i] = mPlayers.get(i).mName;
						}

						/* Build the dialog */
						builder.setTitle(getString(R.string.life_counter_remove_player));

						builder.setItems(names, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								/* Remove the view from the LinearLayout, remove the player from the ArrayList,
								   redraw */
								if (mDisplayMode == DISPLAY_COMMANDER) {
									mGridLayout.removeView(mPlayers.get(item).mCommanderRowView);
								}
								else {
									mGridLayout.removeView(mPlayers.get(item).mView);
								}
								mPlayers.remove(item);
								mGridLayout.invalidate();

								setCommanderInfo(item);

								/* Reset the displayed player, in case the displayed player was removed */
								if (mDisplayMode == DISPLAY_COMMANDER) {
									mCommanderPlayerView.removeAllViews();
									mCommanderPlayerView.addView(mPlayers.get(0).mView);
								}
							}
						});

						return builder.create();
					}
					case RESET_CONFIRM_DIALOG: {
						builder.setMessage(getString(R.string.life_counter_clear_dialog_text))
								.setCancelable(true)
								.setPositiveButton(getString(R.string.dialog_both), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										/* Remove all players, then add defaults */
										mPlayers.clear();
										mLargestPlayerNumber = 0;
										addPlayer();
										addPlayer();

										setCommanderInfo(-1);

										/* Clear and then add the views */
										changeDisplayMode(mDisplayMode);
										dialog.dismiss();
									}
								})
								.setNeutralButton(getString(R.string.dialog_life), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										/* Only reset life totals */
										for (LcPlayer player : mPlayers) {
											player.resetStats();
										}
										mGridLayout.invalidate();
										dialog.dismiss();
									}
								})
								.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();
									}
								});

						return builder.create();
					}
					case DIALOG_CHANGE_DISPLAY: {

						builder.setTitle(R.string.pref_display_mode_title);
						builder.setSingleChoiceItems(getResources().getStringArray(R.array.display_array_entries), mDisplayMode,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();

										if (mDisplayMode != which) {
											mDisplayMode = which;
											changeDisplayMode(mDisplayMode);
										}
									}
								});

						return builder.create();
					}
					case SET_GATHERING_DIALOG: {
						if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
							Toast.makeText(this.getActivity(), R.string.life_counter_no_gatherings_exist, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}

						final ArrayList<String> gatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
						final String[] properNames = new String[gatherings.size()];
						for (int idx = 0; idx < gatherings.size(); idx++) {
							properNames[idx] = GatheringsIO.ReadGatheringNameFromXML(gatherings.get(idx), getActivity().getFilesDir());
						}

						builder.setTitle(R.string.life_counter_gathering_dialog_title);
						builder.setItems(properNames, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialogInterface, final int item) {
								/* If the user rotates while the dialog is open, the dialog stays open but crashes
								   because getActivity() will return null for some odd reason. */
								Gathering gathering = GatheringsIO.ReadGatheringXML(gatherings.get(item), getActivity().getFilesDir());

								mDisplayMode = gathering.mDisplayMode;

								mPlayers.clear();
								ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
								for (GatheringsPlayerData player : players) {
									addPlayer(player.mName, player.mStartingLife);
								}

								setCommanderInfo(-1);
								changeDisplayMode(mDisplayMode);
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

	public void setCommanderInfo(int toBeRemoved) {
		for (LcPlayer player1 : mPlayers) {
			if (toBeRemoved != -1) {
				player1.mCommanderDamage.remove(toBeRemoved);
			}
			else {
				for (int i = 0; i < mPlayers.size(); i++) {
					/* An entry for this player exists, just set the name */
					if (player1.mCommanderDamage.size() > i) {
						player1.mCommanderDamage.get(i).mName = mPlayers.get(i).mName;
					}
					/* An entry for this player doesn't exist, create one and add it */
					else {
						CommanderEntry ce = new CommanderEntry();
						ce.mName = mPlayers.get(i).mName;
						ce.mLife = 0;
						player1.mCommanderDamage.add(ce);
					}
				}
			}
			/* Redraw the information */
			if (player1.mCommanderDamageAdapter != null) {
				player1.mCommanderDamageAdapter.notifyDataSetChanged();
			}
		}
	}

	private void changeDisplayMode(int displayMode) {
		// And also update the preference
		((FamiliarActivity) getActivity()).mPreferenceAdapter.setDisplayMode(String.valueOf(displayMode));

		mGridLayout.removeAllViews();

		if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			switch (displayMode) {
				case DISPLAY_NORMAL:
					mGridLayout.setOrientation(GridLayout.HORIZONTAL);
					mGridLayout.setColumnCount(1);
					mGridLayout.setRowCount(GridLayout.UNDEFINED);
					break;
				case DISPLAY_COMPACT:
					mGridLayout.setOrientation(GridLayout.HORIZONTAL);
					mGridLayout.setColumnCount(2);
					mGridLayout.setRowCount(GridLayout.UNDEFINED);
					break;
				case DISPLAY_COMMANDER:
					mGridLayout.setOrientation(GridLayout.HORIZONTAL);
					mGridLayout.setColumnCount(2);
					mGridLayout.setRowCount(GridLayout.UNDEFINED);
					break;
			}
		}
		else {
			switch (displayMode) {
				case DISPLAY_NORMAL:
					mGridLayout.setOrientation(GridLayout.HORIZONTAL);
					mGridLayout.setColumnCount(GridLayout.UNDEFINED);
					mGridLayout.setRowCount(1);
					break;
				case DISPLAY_COMPACT:
					mGridLayout.setOrientation(GridLayout.HORIZONTAL);
					mGridLayout.setColumnCount(GridLayout.UNDEFINED);
					mGridLayout.setRowCount(1);
					break;
				case DISPLAY_COMMANDER:
					mGridLayout.setOrientation(GridLayout.VERTICAL);
					mGridLayout.setColumnCount(GridLayout.UNDEFINED);
					if (mListSizeHeight != -1) {
						float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getActivity().getResources().getDisplayMetrics());
						mGridLayout.setRowCount((int) (mListSizeHeight / height));
					}
					else {
						mGridLayout.setRowCount(GridLayout.UNDEFINED);
					}
					break;
			}
		}

		for (LcPlayer player : mPlayers) {
			/* Only reset a player's default life / life if that player is unaltered and doesn't have a noticeably
			 * custom default life */
			if (player.mLifeHistory.size() == 0
					&& player.mPoisonHistory.size() == 0
					&& player.mLife == player.mDefaultLifeTotal
					&& (player.mDefaultLifeTotal == 20 || player.mDefaultLifeTotal == 40)) {
				player.mDefaultLifeTotal = getDefaultLife();
				player.mLife = player.mDefaultLifeTotal;
			}
			addPlayerView(player, true);
		}

		if (displayMode == DISPLAY_COMMANDER) {
			mCommanderButton.setVisibility(View.VISIBLE);
			mCommanderPlayerView.setVisibility(View.VISIBLE);
			mCommanderPlayerView.removeAllViews();
			mCommanderPlayerView.addView(mPlayers.get(0).mView);
			mPlayers.get(0).setSize(mListSizeWidth, mListSizeHeight, displayMode, getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		}
		else {
			mCommanderPlayerView.setVisibility(View.GONE);
			mCommanderButton.setVisibility(View.GONE);
			if (mStatDisplaying == LcPlayer.COMMANDER) {
				setStatDisplaying(LcPlayer.LIFE);
			}
		}
	}

	private void addPlayerView(final LcPlayer player, boolean isNew) {
		if (isNew) {
			mGridLayout.addView(player.newView(mDisplayMode, mStatDisplaying));
			if (mDisplayMode == DISPLAY_COMMANDER) {
				player.mCommanderRowView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						mCommanderPlayerView.removeAllViews();
						mCommanderPlayerView.addView(player.mView);
						player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode, getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
					}
				});
			}
		}
		else {
			if (mDisplayMode == DISPLAY_COMMANDER) {
				mGridLayout.addView(player.mCommanderRowView);
			}
			else {
				mGridLayout.addView(player.mView);
			}
		}

		if (mListSizeHeight != -1) {
			player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode, getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		}
	}

	/**
	 * Change whether life or poison is displayed. This will redraw buttons and notify the LcPlayer objects to show
	 * different things.
	 *
	 * @param statMode either LcPlayer.LIFE or LcPlayer.POISON
	 */
	private void setStatDisplaying(int statMode) {
		mStatDisplaying = statMode;

		switch (statMode) {
			case LcPlayer.LIFE:
				mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
				mLifeButton.setImageResource(R.drawable.lc_life_enabled);
				mCommanderButton.setImageResource(R.drawable.lc_commander_disabled);
				break;
			case LcPlayer.POISON:
				mPoisonButton.setImageResource(R.drawable.lc_poison_enabled);
				mLifeButton.setImageResource(R.drawable.lc_life_disabled);
				mCommanderButton.setImageResource(R.drawable.lc_commander_disabled);
				break;
			case LcPlayer.COMMANDER:
				mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
				mLifeButton.setImageResource(R.drawable.lc_life_disabled);
				mCommanderButton.setImageResource(R.drawable.lc_commander_enabled);
				break;
		}

		for (LcPlayer player : mPlayers) {
			player.setMode(statMode);
		}
	}

	/**
	 * Add a default player. It is added to the ArrayList and the view is added to the LinearLayout. It is given an
	 * incremented number name i.e. Player N
	 */
	private void addPlayer() {
		final LcPlayer player = new LcPlayer(this);

		mLargestPlayerNumber++;
		player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
		player.mDefaultLifeTotal = getDefaultLife();
		player.mLife = player.mDefaultLifeTotal;

		mPlayers.add(player);
	}

	/**
	 * Add a player from the gatherings
	 *
	 * @param name         The player's name
	 * @param startingLife The player's starting life total
	 */
	private void addPlayer(String name, int startingLife) {
		LcPlayer player = new LcPlayer(this);
		player.mName = name;
		player.mDefaultLifeTotal = startingLife;
		player.mLife = startingLife;

		try {
			String nameParts[] = player.mName.split(" ");
			int number = Integer.parseInt(nameParts[nameParts.length - 1]);
			if (number > mLargestPlayerNumber) {
				mLargestPlayerNumber = number;
			}
		} catch (NumberFormatException e) {
				/* eat it */
		}

		mPlayers.add(player);
	}

	/**
	 * Add a player from a semicolon delimited string of data. The delimited fields are:
	 * name; life; life History; poison; poison History; default Life; commander History; commander casting
	 * The history entries are comma delimited
	 *
	 * @param line The string of player data to build an object from
	 */
	private void addPlayer(String line) {
		try {
			LcPlayer player = new LcPlayer(this);

			String[] data = line.split(";");

			player.mName = data[0];

			/* Keep track of the largest numbered player, for adding new players */
			try {
				String nameParts[] = player.mName.split(" ");
				int number = Integer.parseInt(nameParts[nameParts.length - 1]);
				if (number > mLargestPlayerNumber) {
					mLargestPlayerNumber = number;
				}
			} catch (NumberFormatException e) {
				/* eat it */
			}

			player.mLife = Integer.parseInt(data[1]);

			try {
				player.mDefaultLifeTotal = Integer.parseInt(data[5]);
			} catch (Exception e) {
				player.mDefaultLifeTotal = getDefaultLife();
			}

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
						entry.mDelta = entry.mAbsolute - player.mDefaultLifeTotal;
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
						entry.mDelta = entry.mAbsolute;
					}
					player.mPoisonHistory.add(0, entry);
				}

			} catch (NumberFormatException e) {
				/* Eat it */
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Eat it */
			}

			try {
				String[] commanderLifeString = data[6].split(",");
				player.mCommanderDamage = new ArrayList<CommanderEntry>(commanderLifeString.length);
				CommanderEntry entry;
				for (String aCommanderLifeString : commanderLifeString) {
					entry = new CommanderEntry();
					entry.mLife = Integer.parseInt(aCommanderLifeString);
					player.mCommanderDamage.add(entry);
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
		} catch (ArrayIndexOutOfBoundsException e) {
			/* Eat it */
		}
	}

	private int getDefaultLife() {
		if (mDisplayMode == DISPLAY_COMMANDER) {
			return 40;
		}
		return 20;
	}

	/**
	 * When mTts is initialized, set the boolean flag and display the option in the ActionBar
	 *
	 * @param status SUCCESS or ERROR.
	 */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			mTtsInit = true;
			getActivity().invalidateOptionsMenu();
		}
	}

	/**
	 * Build a LinkedList of all the things to say, which can include TTS calls and MediaPlayer calls. Then call
	 * onUtteranceCompleted to start running through the LinkedList, even though no utterance was spoken.
	 */
	private void announceLifeTotals() {
		if (mTtsInit) {
			mVocalizations.clear();
			for (LcPlayer p : mPlayers) {
				switch (mStatDisplaying) {
					case LcPlayer.LIFE:
						if (p.mLife > 9000) {
							/* If the life is over 9000, split the string on an IMPROBABLE_NUMBER, and insert a call to
							   the m9000Player */
							String tmp = String
									.format(getString(R.string.life_counter_spoken_life), p.mName, IMPROBABLE_NUMBER);
							String parts[] = tmp.split(Integer.toString(IMPROBABLE_NUMBER));
							mVocalizations.add(parts[0]);
							mVocalizations.add(OVER_9000_KEY);
							mVocalizations.add(parts[1]);
						}
						else {
							mVocalizations.add(
									String.format(getString(R.string.life_counter_spoken_life), p.mName, p.mLife));
						}
						break;
					case LcPlayer.POISON:
						mVocalizations.add(
								String.format(getString(R.string.life_counter_spoken_poison), p.mName, p.mPoison));
						break;
				}

			}

			if (mVocalizations.size() > 0) {
				int res = mAudioManager.requestAudioFocus(
						this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
				if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					onUtteranceCompleted(LIFE_ANNOUNCE);
				}
			}
		}
	}

	/**
	 * Necessary to implement OnAudioFocusChangeListener, ignored
	 *
	 * @param i some irrelevant integer
	 */
	@Override
	public void onAudioFocusChange(int i) {

	}

	/**
	 * This is called every time an utterance is completed, as well as when the m9000Player finishes shouting.
	 * It polls an item out of the LinkedList and speaks it, or returns audio focus to the system.
	 *
	 * @param key A key to determine what was just uttered. This is ignored
	 */
	@Override
	public void onUtteranceCompleted(String key) {
		if (mVocalizations.size() > 0) {
			String toSpeak = mVocalizations.poll();
			if (toSpeak.equals(OVER_9000_KEY)) {
				try {
					m9000Player.stop();
					m9000Player.prepare();
					m9000Player.start();
				} catch (IOException e) {
					/* If the media was not played, fall back to TTSing "over 9000" */
					HashMap<String, String> ttsParams = new HashMap<String, String>();
					ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
					ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);
					mTts.speak(getString(R.string.life_counter_over_9000), TextToSpeech.QUEUE_FLUSH, ttsParams);
				}
			}
			else {
				HashMap<String, String> ttsParams = new HashMap<String, String>();
				ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
				ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);
				mTts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams);
			}
		}
		else {
			mAudioManager.abandonAudioFocus(this);
		}
	}
}
