package com.gelakinetic.mtgfam.fragments;

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

import com.alertdialogpro.AlertDialogPro;
import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.LcPlayer.CommanderEntry;
import com.gelakinetic.mtgfam.helpers.LcPlayer.HistoryEntry;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class LifeCounterFragment extends FamiliarFragment implements TextToSpeech.OnInitListener,
        AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnUtteranceCompletedListener {

    /* constants for display mode */
    public static final int DISPLAY_NORMAL = 0;
    private int mDisplayMode = DISPLAY_NORMAL;
    public static final int DISPLAY_COMPACT = 1;
    public static final int DISPLAY_COMMANDER = 2;
    /* constants for stat displaying */
    public final static int STAT_LIFE = 0;
    private int mStatDisplaying = STAT_LIFE;
    public final static int STAT_POISON = 1;
    public final static int STAT_COMMANDER = 2;
    /* Life total constants */
    public static final int DEFAULT_LIFE_COMMANDER = 40;
    public static final int DEFAULT_LIFE = 20;
    /* Dialog Constants */
    private static final int DIALOG_REMOVE_PLAYER = 0;
    private static final int DIALOG_RESET_CONFIRM = 1;
    private static final int DIALOG_CHANGE_DISPLAY = 2;
    private static final int DIALOG_SET_GATHERING = 3;
    /* Constant for persisting data */
    private static final String DISPLAY_MODE = "display_mode";
    /* Constants for TTS */
    private static final String LIFE_ANNOUNCE = "life_announce";
    private static final int IMPROBABLE_NUMBER = 531865548;
    private static final String OVER_9000_KEY = "@over_9000";
    /* Keeping track of players, display state */
    private final ArrayList<LcPlayer> mPlayers = new ArrayList<>();
    private final LinkedList<String> mVocalizations = new LinkedList<>();
    /* UI Elements, measurement */
    private GridLayout mGridLayout;
    private LinearLayout mCommanderPlayerView;
    private ImageView mPoisonButton;
    private ImageView mLifeButton;
    private ImageView mCommanderButton;
    private View mScrollView;
    private int mListSizeWidth = -1;
    private int mListSizeHeight = -1;
    private int mLargestPlayerNumber = 0;
    /* TTS variables */
    private TextToSpeech mTts;
    private boolean mTtsInit;
    private AudioManager mAudioManager;
    private MediaPlayer m9000Player;

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
     * Get UI element references, set onClickListeners for the toolbar, clear the measurement data and attach a
     * ViewTreeObserver to measure the UI when it is drawn. Get the life/poison mode from the savedInstanceState if the
     * fragment is persisting. Save the current brightness. Players are not added here.
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

        mDisplayMode = Integer.valueOf(getFamiliarActivity().mPreferenceAdapter.getDisplayMode());

        mCommanderPlayerView = (LinearLayout) myFragmentView.findViewById(R.id.commander_player);

        if (null != myFragmentView.findViewById(R.id.playerScrollView_horz)) {
            mScrollView = myFragmentView.findViewById(R.id.playerScrollView_horz);
        } else {
            mScrollView = myFragmentView.findViewById(R.id.playerScrollView_vert);
        }
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
                        if (getActivity().getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE) {
                            if (mDisplayMode == DISPLAY_COMMANDER) {
                                /* Conveniently takes care of re-adding the sized views in the right number of rows */
                                changeDisplayMode(false);
                            }
                        }
                        for (LcPlayer player : mPlayers) {
                            player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode,
                                    getActivity().getResources().getConfiguration().orientation
                                            == Configuration.ORIENTATION_PORTRAIT
                            );
                        }
                    }
                }
            }
        });

        mPoisonButton = (ImageView) myFragmentView.findViewById(R.id.poison_button);
        mPoisonButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setStatDisplaying(STAT_POISON);
            }
        });

        mLifeButton = (ImageView) myFragmentView.findViewById(R.id.life_button);
        mLifeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setStatDisplaying(STAT_LIFE);
            }
        });

        mCommanderButton = (ImageView) myFragmentView.findViewById(R.id.commander_button);
        mCommanderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setStatDisplaying(STAT_COMMANDER);
            }
        });

        myFragmentView.findViewById(R.id.reset_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(DIALOG_RESET_CONFIRM);
            }
        });

        if (savedInstanceState != null) {
            mStatDisplaying = savedInstanceState.getInt(DISPLAY_MODE, STAT_LIFE);
        }

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
            player.onPause();
            playerData += player.toString();
        }
        getFamiliarActivity().mPreferenceAdapter.setPlayerData(playerData);
        mGridLayout.removeAllViews();
        mPlayers.clear();

		/* Remove the screen on lock, restore the brightness */
        getActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onUserActive();
    }

    /**
     * When the fragment is resumed, attempt to populate the life counter with player information in shared preferences.
     * If that doesn't exist, add the two default players. Set whether to display life or poison based on persisted
     * data. Add commander data to each player, and set the current display mode and stat displaying, set in
     * onCreateView. addPlayer() adds players to the ArrayList, and setStatDisplaying() takes care of drawing the player
     * Views
     */
    @Override
    public void onResume() {
        super.onResume();

        String playerData = getFamiliarActivity().mPreferenceAdapter.getPlayerData();
        if (playerData == null || playerData.length() == 0) {
            addPlayer();
            addPlayer();
        } else {
            String[] playerLines = playerData.split("\n");
            for (String line : playerLines) {
                addPlayer(line);
            }
        }

        setCommanderInfo(-1);

        changeDisplayMode(false);

        setStatDisplaying(mStatDisplaying);

        if (getFamiliarActivity().mPreferenceAdapter.getKeepScreenOn()) {
            getActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Called from the activity if the user is inactive. Dims the screen.
     */
    @Override
    public void onUserInactive() {
        if (getFamiliarActivity().mPreferenceAdapter.getKeepScreenOn() &&
                getFamiliarActivity().mPreferenceAdapter.getDimScreen()) {
            float dimLevel = (float) getFamiliarActivity().mPreferenceAdapter.getDimLevel() / (float) 100;
            WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
            layoutParams.screenBrightness = dimLevel;
            getActivity().getWindow().setAttributes(layoutParams);
        }
    }

    /**
     * Called from the activity if the user is active again. Restore the saved brightness.
     */
    @Override
    public void onUserActive() {
        if (getFamiliarActivity().mPreferenceAdapter.getKeepScreenOn()) {
            WindowManager.LayoutParams layoutParams = getActivity().getWindow().getAttributes();
            layoutParams.screenBrightness = -1;
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
        if (!mTtsInit || getFamiliarActivity() == null || !getFamiliarActivity().mIsMenuVisible) {
            menuItem.setVisible(false);
        } else {
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
        /* Handle item selection */
        switch (item.getItemId()) {
            case R.id.add_player:
                /* Add the player to the ArrayList, set the commander info, and draw the new view */
                addPlayer();
                setCommanderInfo(-1);
                addPlayerView(mPlayers.get(mPlayers.size() - 1));
                return true;
            case R.id.remove_player:
                /* Show a dialog of players to remove */
                showDialog(DIALOG_REMOVE_PLAYER);
                return true;
            case R.id.announce_life:
				/* Vocalize the current life totals */
                announceLifeTotals();
                return true;
            case R.id.edit_gatherings:
				/* Start a GatheringsFragment to edit gatherings */
                GatheringsFragment rlFrag = new GatheringsFragment();
                startNewFragment(rlFrag, null);
                return true;
            case R.id.set_gathering:
				/* Show a dialog of gatherings a user can set */
                showDialog(DIALOG_SET_GATHERING);
                return true;
            case R.id.display_mode:
				/* Show a dialog to change the display mode (normal, compact, commander) */
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
				/* This will be set to false if we are returning a null dialog. It prevents a crash */
                setShowsDialog(true);

                AlertDialogPro.Builder builder = new AlertDialogPro.Builder(getActivity());
                switch (id) {
                    case DIALOG_REMOVE_PLAYER: {
						/* Get all the player names */
                        String[] names = new String[mPlayers.size()];
                        for (int i = 0; i < mPlayers.size(); i++) {
                            names[i] = mPlayers.get(i).mName;
                        }

						/* Build the dialog */
                        builder.setTitle(getString(R.string.life_counter_remove_player));

                        builder.setItems(names, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
								/* Remove the view from the GridLayout based on display mode, then remove the player
								   from the ArrayList and redraw. Also notify other players to remove this player from
								   the commander list, and reset the main commander player view in case that player was
								   removed */
                                if (mDisplayMode == DISPLAY_COMMANDER) {
                                    mGridLayout.removeView(mPlayers.get(item).mCommanderRowView);
                                } else {
                                    mGridLayout.removeView(mPlayers.get(item).mView);
                                }
                                mPlayers.remove(item);
                                mGridLayout.invalidate();

                                setCommanderInfo(item);

                                if (mDisplayMode == DISPLAY_COMMANDER) {
                                    mCommanderPlayerView.removeAllViews();
                                    if (mPlayers.size() > 0) {
                                        mCommanderPlayerView.addView(mPlayers.get(0).mView);
                                    }
                                }
                            }
                        });

                        return builder.create();
                    }
                    case DIALOG_RESET_CONFIRM: {
                        builder.setMessage(getString(R.string.life_counter_clear_dialog_text))
                                .setCancelable(true)
                                .setPositiveButton(getString(R.string.dialog_both),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
												/* Remove all players, then add defaults */
                                                mPlayers.clear();
                                                mLargestPlayerNumber = 0;
                                                addPlayer();
                                                addPlayer();

                                                setCommanderInfo(-1);

												/* Clear and then add the views */
                                                changeDisplayMode(false);
                                                dialog.dismiss();
                                            }
                                        }
                                )
                                .setNeutralButton(getString(R.string.dialog_life),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
												/* Only reset life totals */
                                                for (LcPlayer player : mPlayers) {
                                                    player.resetStats();
                                                }
                                                mGridLayout.invalidate();
                                                dialog.dismiss();
                                            }
                                        }
                                )
                                .setNegativeButton(getString(R.string.dialog_cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        }
                                );

                        return builder.create();
                    }
                    case DIALOG_CHANGE_DISPLAY: {

                        builder.setTitle(R.string.pref_display_mode_title);
                        builder.setSingleChoiceItems(getResources().getStringArray(R.array.display_array_entries),
                                mDisplayMode,
                                new DialogInterface.OnClickListener() {
                                    /* The dialog selection order matches the static integers DISPLAY_NORMAL, etc.
                                       Convenient */
                                    public void onClick(DialogInterface dialog, int selection) {
                                        dialog.dismiss();

                                        if (mDisplayMode != selection) {
                                            mDisplayMode = selection;
                                            changeDisplayMode(true);
                                        }
                                    }
                                }
                        );

                        return builder.create();
                    }
                    case DIALOG_SET_GATHERING: {
						/* If there aren't any dialogs, don't show the dialog. Pop a toast instead */
                        if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                            ToastWrapper.makeText(this.getActivity(), R.string.life_counter_no_gatherings_exist,
                                    ToastWrapper.LENGTH_LONG).show();
                            return DontShowDialog();
                        }

						/* Get a list of Gatherings, and their names extracted from XML */
                        final ArrayList<String> gatherings = GatheringsIO
                                .getGatheringFileList(getActivity().getFilesDir());
                        final String[] properNames = new String[gatherings.size()];
                        for (int idx = 0; idx < gatherings.size(); idx++) {
                            properNames[idx] = GatheringsIO
                                    .ReadGatheringNameFromXML(gatherings.get(idx), getActivity().getFilesDir());
                        }

						/* Set the AlertDialog title, items */
                        builder.setTitle(R.string.life_counter_gathering_dialog_title);
                        builder.setItems(properNames, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, final int item) {
								/* Read the gathering from XML, clear and set all the info! changeDisplayMode() adds
								   the player Views */
                                Gathering gathering = GatheringsIO
                                        .ReadGatheringXML(gatherings.get(item), getActivity().getFilesDir());

                                mDisplayMode = gathering.mDisplayMode;

                                mPlayers.clear();
                                ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                                for (GatheringsPlayerData player : players) {
                                    addPlayer(player.mName, player.mStartingLife);
                                }

                                setCommanderInfo(-1);
                                changeDisplayMode(false);
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
     * Update the list of who a player took commander damage from. This info doesn't exist in saved player data, hence
     * this method. If an entry in the player's list of opponents exists, update the name. Otherwise, add it to the
     * list. If toBeRemoved is positive, remove that opponent from each player's list instead of adding/updating.
     *
     * @param toBeRemoved The index of the opponent to be removed, or negative if an opponent is being added/updated
     */
    public void setCommanderInfo(int toBeRemoved) {
        for (LcPlayer player1 : mPlayers) {
            if (toBeRemoved != -1) {
                player1.mCommanderDamage.remove(toBeRemoved);
            } else {
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

    /**
     * Updates the display mode based on the current value of mDisplayMode. This updates the GridLayout's parameters
     * and draws the player's views in the fragment. It also shows and hides buttons and views relating to
     * commander mode.
     */
    private void changeDisplayMode(boolean shouldDefaultLives) {
		/* update the preference */
        getFamiliarActivity().mPreferenceAdapter.setDisplayMode(String.valueOf(mDisplayMode));

        mGridLayout.removeAllViews();

        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            switch (mDisplayMode) {
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
        } else {
            switch (mDisplayMode) {
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
                        float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                                getActivity().getResources().getDisplayMetrics());
                        mGridLayout.setRowCount((int) (mListSizeHeight / height));
                    } else {
                        mGridLayout.setRowCount(GridLayout.UNDEFINED);
                    }
                    break;
            }
        }

        boolean areLivesDefault = true;
        for (LcPlayer player : mPlayers) {
			/* Only reset a player's default life / life if that player is unaltered and doesn't have a noticeably
			 * custom default life */
            if (!(player.mLifeHistory.size() == 0
                    && player.mPoisonHistory.size() == 0
                    && player.mLife == player.mDefaultLifeTotal
                    && (player.mDefaultLifeTotal == DEFAULT_LIFE || player.mDefaultLifeTotal == DEFAULT_LIFE_COMMANDER))) {
                areLivesDefault = false;
            }
        }

        if (areLivesDefault && shouldDefaultLives) {
            for (LcPlayer player : mPlayers) {
                player.mDefaultLifeTotal = getDefaultLife();
                player.mLife = player.mDefaultLifeTotal;
            }
        }

        for (LcPlayer player : mPlayers) {
			/* Draw the player's view */
            addPlayerView(player);
        }

        if (mDisplayMode == DISPLAY_COMMANDER) {
            mCommanderButton.setVisibility(View.VISIBLE);
            mCommanderPlayerView.setVisibility(View.VISIBLE);
            mCommanderPlayerView.removeAllViews();
            if (mPlayers.size() > 0) {
                mCommanderPlayerView.addView(mPlayers.get(0).mView);
                mPlayers.get(0).setSize(mListSizeWidth, mListSizeHeight, mDisplayMode, getActivity().getResources()
                        .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            }
        } else {
            mCommanderPlayerView.setVisibility(View.GONE);
            mCommanderButton.setVisibility(View.GONE);
            if (mStatDisplaying == STAT_COMMANDER) {
                setStatDisplaying(STAT_LIFE);
            }
        }
    }

    /**
     * This function adds the player's view to the GridLayout. player.newView() will inflate the view. If the counter
     * is in commander mode, an onClickListener will be set to display that player's information in
     * mCommanderPlayerView.
     *
     * @param player The player to be added
     */
    private void addPlayerView(final LcPlayer player) {
        mGridLayout.addView(player.newView(mDisplayMode, mStatDisplaying));
        if (mDisplayMode == DISPLAY_COMMANDER) {
            player.mCommanderRowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
						/* Show this player's info in mCommanderPlayerView */
                    mCommanderPlayerView.removeAllViews();
                    mCommanderPlayerView.addView(player.mView);
                    player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode,
                            getActivity().getResources().getConfiguration().orientation
                                    == Configuration.ORIENTATION_PORTRAIT
                    );
                }
            });

            if (mPlayers.size() == 1) {
                mCommanderPlayerView.addView(mPlayers.get(0).mView);
            }
        }

		/* If the size has already been measured, set the player's size */
        if (mListSizeHeight != -1) {
            player.setSize(mListSizeWidth, mListSizeHeight, mDisplayMode,
                    getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        }
    }

    /**
     * Change whether life, poison, or commander damage is displayed. This will redraw buttons and notify the LcPlayer
     * objects to show different things.
     *
     * @param statMode either STAT_LIFE, STAT_POISON, or STAT_COMMANDER
     */
    private void setStatDisplaying(int statMode) {
        mStatDisplaying = statMode;

        switch (statMode) {
            case STAT_LIFE:
                mLifeButton.setImageResource(R.drawable.lc_life_enabled);
                mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
                mCommanderButton.setImageResource(R.drawable.lc_commander_disabled);
                break;
            case STAT_POISON:
                mLifeButton.setImageResource(R.drawable.lc_life_disabled);
                mPoisonButton.setImageResource(R.drawable.lc_poison_enabled);
                mCommanderButton.setImageResource(R.drawable.lc_commander_disabled);
                break;
            case STAT_COMMANDER:
                mLifeButton.setImageResource(R.drawable.lc_life_disabled);
                mPoisonButton.setImageResource(R.drawable.lc_poison_disabled);
                mCommanderButton.setImageResource(R.drawable.lc_commander_enabled);
                break;
        }

        for (LcPlayer player : mPlayers) {
            player.setMode(statMode);
        }
    }

    /**
     * Add a default player to the ArrayList mPlayers. It is given an incremented number name i.e. Player N. The
     * starting life will be either 20 or 40, depending on display mode.
     */
    private void addPlayer() {
        final LcPlayer player = new LcPlayer(this);

		/* Increment the largest player number */
        mLargestPlayerNumber++;
        player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
        player.mDefaultLifeTotal = getDefaultLife();
        player.mLife = player.mDefaultLifeTotal;

        mPlayers.add(player);
    }

    /**
     * Add a player from a name and a starting life. Typically this is from a Gathering
     *
     * @param name         The player's name
     * @param startingLife The player's starting life total
     */
    private void addPlayer(String name, int startingLife) {
        LcPlayer player = new LcPlayer(this);
        player.mName = name;
        player.mDefaultLifeTotal = startingLife;
        player.mLife = startingLife;

		/* If the player's name ends in a number, and the number is larger than mPlayerNumber, set mPlayerNumber as
		   it */
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

            try {
                player.mName = data[0];
            } catch (Exception e) {
                player.mName = getResources().getString(R.string.life_counter_default_name);
            }

			/* If the player's name ends in a number, and the number is larger than mPlayerNumber, set mPlayerNumber as
			   it */
            try {
                String nameParts[] = player.mName.split(" ");
                int number = Integer.parseInt(nameParts[nameParts.length - 1]);
                if (number > mLargestPlayerNumber) {
                    mLargestPlayerNumber = number;
                }
            } catch (NumberFormatException e) {
				/* eat it */
            }

            try {
                player.mLife = Integer.parseInt(data[1]);
            } catch (Exception e) {
                player.mLife = getDefaultLife();
            }

            try {
                player.mDefaultLifeTotal = Integer.parseInt(data[5]);
            } catch (Exception e) {
                player.mDefaultLifeTotal = getDefaultLife();
            }

            try {
                String[] lifeHistory = data[2].split(",");
                player.mLifeHistory = new ArrayList<>(lifeHistory.length);
                HistoryEntry entry;
                for (int i = lifeHistory.length - 1; i >= 0; i--) {
                    entry = new HistoryEntry();
                    entry.mAbsolute = Integer.parseInt(lifeHistory[i]);
                    if (i != lifeHistory.length - 1) {
                        entry.mDelta = entry.mAbsolute - player.mLifeHistory.get(0).mAbsolute;
                    } else {
                        entry.mDelta = entry.mAbsolute - player.mDefaultLifeTotal;
                    }
                    player.mLifeHistory.add(0, entry);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                player.mLifeHistory = new ArrayList<>();
            }

            try {
                player.mPoison = Integer.parseInt(data[3]);
            } catch (Exception e) {
                player.mPoison = getDefaultLife();
            }

            try {
                String[] poisonHistory = data[4].split(",");
                player.mPoisonHistory = new ArrayList<>(poisonHistory.length);
                HistoryEntry entry;
                for (int i = poisonHistory.length - 1; i >= 0; i--) {
                    entry = new HistoryEntry();
                    entry.mAbsolute = Integer.parseInt(poisonHistory[i]);
                    if (i != poisonHistory.length - 1) {
                        entry.mDelta = entry.mAbsolute - player.mPoisonHistory.get(0).mAbsolute;
                    } else {
                        entry.mDelta = entry.mAbsolute;
                    }
                    player.mPoisonHistory.add(0, entry);
                }

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                player.mPoisonHistory = new ArrayList<>();
            }

            try {
                String[] commanderLifeString = data[6].split(",");
                player.mCommanderDamage = new ArrayList<>(commanderLifeString.length);
                CommanderEntry entry;
                for (String aCommanderLifeString : commanderLifeString) {
                    entry = new CommanderEntry();
                    entry.mLife = Integer.parseInt(aCommanderLifeString);
                    player.mCommanderDamage.add(entry);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                player.mCommanderDamage = new ArrayList<>();
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

    /**
     * Return 40 life if in commander mode, 20 life otherwise
     *
     * @return 40 life in commander mode, 20 life otherwise
     */
    private int getDefaultLife() {
        if (mDisplayMode == DISPLAY_COMMANDER) {
            return DEFAULT_LIFE_COMMANDER;
        }
        return DEFAULT_LIFE;
    }

    /**
     * When mTts is initialized, set the boolean flag and display the option in the ActionBar
     *
     * @param status SUCCESS or ERROR.
     */
    @Override
    public void onInit(final int status) {
        if (isAdded()) {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTts.setLanguage(getResources().getConfiguration().locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    getFamiliarActivity().showTtsDialog();
                } else {
                    mTtsInit = true;
                    if (mIsSearchViewOpen) {
						/* Search view is open, pend menu refresh */
                        mAfterSearchClosedRunnable = new Runnable() {
                            @Override
                            public void run() {
                                getActivity().supportInvalidateOptionsMenu();
                            }
                        };
                    } else {
						/* Redraw menu */
                        getActivity().supportInvalidateOptionsMenu();
                    }
                }
            } else if (status == TextToSpeech.ERROR) {
                getFamiliarActivity().showTtsDialog();
            }
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
                    case STAT_LIFE:
                        if (p.mLife > 9000) {
							/* If the life is over 9000, split the string on an IMPROBABLE_NUMBER, and insert a call to
							   the m9000Player */
                            String tmp = String
                                    .format(getString(R.string.life_counter_spoken_life), p.mName, IMPROBABLE_NUMBER);
                            String parts[] = tmp.split(Integer.toString(IMPROBABLE_NUMBER));
                            mVocalizations.add(parts[0]);
                            mVocalizations.add(OVER_9000_KEY);
                            mVocalizations.add(parts[1]);
                        } else {
                            if (p.mLife == 1) {
                                mVocalizations.add(String.format(getString(R.string.life_counter_spoken_life_singular),
                                        p.mName, p.mLife));
                            } else {
                                mVocalizations.add(String.format(getString(R.string.life_counter_spoken_life),
                                        p.mName, p.mLife));
                            }
                        }
                        break;
                    case STAT_POISON:
                        if (p.mPoison == 1) {
                            mVocalizations.add(String.format(getString(R.string.life_counter_spoken_poison_singular),
                                    p.mName, p.mPoison));
                        } else {
                            mVocalizations.add(String.format(getString(R.string.life_counter_spoken_poison),
                                    p.mName, p.mPoison));
                        }
                        break;
                }

            }

            if (mVocalizations.size() > 0) {
				/* Get the audio focus, and tell everyone else to be quiet for a moment */
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
     * @param i some irrelevant integer. poor integer
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
                    HashMap<String, String> ttsParams = new HashMap<>();
                    ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                    ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);
                    if (mTts.speak(getString(R.string.life_counter_over_9000), TextToSpeech.QUEUE_FLUSH, ttsParams) == TextToSpeech.ERROR) {
                        getFamiliarActivity().showTtsDialog();
                    }
                }
            } else {
                HashMap<String, String> ttsParams = new HashMap<>();
                ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);

                if (mTts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams) == TextToSpeech.ERROR) {
                    getFamiliarActivity().showTtsDialog();
                }
            }
        } else {
            mAudioManager.abandonAudioFocus(this);
        }
    }
}
