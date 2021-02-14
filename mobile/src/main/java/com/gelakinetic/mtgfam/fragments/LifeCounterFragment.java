/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.LifeCounterDialogFragment;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.LcPlayer.CommanderEntry;
import com.gelakinetic.mtgfam.helpers.LcPlayer.HistoryEntry;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.view.ViewUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class LifeCounterFragment extends FamiliarFragment implements TextToSpeech.OnInitListener,
        AudioManager.OnAudioFocusChangeListener, TextToSpeech.OnUtteranceCompletedListener {

    /* constants for display mode */
    public static final int DISPLAY_NORMAL = 0;
    public static final int DISPLAY_COMPACT = 1;
    public static final int DISPLAY_COMMANDER = 2;
    /* constants for stat displaying */
    public final static int STAT_LIFE = 0;
    public final static int STAT_POISON = 1;
    public final static int STAT_COMMANDER = 2;
    /* Life total constants */
    public static final int DEFAULT_LIFE_COMMANDER = 40;
    public static final int DEFAULT_LIFE = 20;
    /* Constant for persisting data */
    private static final String DISPLAY_MODE = "display_mode";
    /* Constants for TTS */
    private static final String LIFE_ANNOUNCE = "life_announce";
    private static final int IMPROBABLE_NUMBER = 531865548;
    private static final String OVER_9000_KEY = "@over_9000";
    /* Keeping track of players, display state */
    public final ArrayList<LcPlayer> mPlayers = new ArrayList<>();
    private final LinkedList<String> mVocalizations = new LinkedList<>();
    public int mDisplayMode = DISPLAY_NORMAL;
    private int mStatDisplaying = STAT_LIFE;
    /* UI Elements, measurement */
    public GridLayout mGridLayout;
    public LinearLayout mCommanderPlayerView;
    private ImageView mPoisonButton;
    private ImageView mLifeButton;
    private ImageView mCommanderButton;
    private View mScrollView;
    private int mListSizeWidth = -1;
    private int mListSizeHeight = -1;
    private int mNumCols = 2;
    private int mNumRows = 1;
    public int mLargestPlayerNumber = 0;
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

        mAudioManager = (AudioManager) Objects.requireNonNull(getActivity()).getSystemService(Context.AUDIO_SERVICE);

        m9000Player = MediaPlayer.create(getActivity(), R.raw.over_9000);
        if (m9000Player != null) {
            m9000Player.setOnCompletionListener(mp -> onUtteranceCompleted(LIFE_ANNOUNCE));
        }
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListSizeWidth = -1;
        mListSizeHeight = -1;

        View myFragmentView = inflater.inflate(R.layout.life_counter_frag, container, false);
        assert myFragmentView != null;
        mGridLayout = myFragmentView.findViewById(R.id.playerList);

        mDisplayMode = Integer.parseInt(PreferenceAdapter.getDisplayMode(getContext()));

        mCommanderPlayerView = myFragmentView.findViewById(R.id.commander_player);

        if (null != myFragmentView.findViewById(R.id.playerScrollView_horz)) {
            mScrollView = myFragmentView.findViewById(R.id.playerScrollView_horz);
        } else {
            mScrollView = myFragmentView.findViewById(R.id.playerScrollView_vert);
        }
        ViewTreeObserver viewTreeObserver = mScrollView.getViewTreeObserver();
        assert viewTreeObserver != null;
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            if (isVisible()) {
                boolean changed = false;
                if (mListSizeHeight < mScrollView.getHeight()) {
                    mListSizeHeight = mScrollView.getHeight();
                    float height = ViewUtil.convertPixelsToDp(mListSizeHeight, getContext());
                    mNumRows = Math.max((int) Math.floor(height / 220f), mNumRows);
                    changed = true;
                }
                if (mListSizeWidth < mScrollView.getWidth()) {
                    mListSizeWidth = mScrollView.getWidth();
                    float width = ViewUtil.convertPixelsToDp(mListSizeWidth, getContext());
                    mNumCols = Math.max((int) Math.floor(width / 150f), mNumCols);
                    changed = true;
                }
                if (changed) {
                    changeDisplayMode(false);
                    resizeAllPlayers();
                }
            }
        });

        mPoisonButton = myFragmentView.findViewById(R.id.poison_button);
        mPoisonButton.setOnClickListener(view -> setStatDisplaying(STAT_POISON));

        mLifeButton = myFragmentView.findViewById(R.id.life_button);
        mLifeButton.setOnClickListener(view -> setStatDisplaying(STAT_LIFE));

        mCommanderButton = myFragmentView.findViewById(R.id.commander_button);
        mCommanderButton.setOnClickListener(view -> setStatDisplaying(STAT_COMMANDER));

        myFragmentView.findViewById(R.id.reset_button).setOnClickListener(view -> showDialog(LifeCounterDialogFragment.DIALOG_RESET_CONFIRM));

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        StringBuilder playerData = new StringBuilder();
        for (LcPlayer player : mPlayers) {
            player.onPause();
            playerData.append(player.toString());
        }
        PreferenceAdapter.setPlayerData(getContext(), playerData.toString());
        mGridLayout.removeAllViews();
        mPlayers.clear();

        /* Remove the screen on lock, restore the brightness */
        Objects.requireNonNull(getActivity()).getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        String playerData = PreferenceAdapter.getPlayerData(getContext());
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

        if (PreferenceAdapter.getKeepScreenOn(getContext())) {
            Objects.requireNonNull(getActivity()).getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Called from the activity if the user is inactive. Dims the screen.
     */
    @Override
    public void onUserInactive() {
        try {
            if (PreferenceAdapter.getKeepScreenOn(getContext()) &&
                    PreferenceAdapter.getDimScreen(getContext())) {
                float dimLevel = (float) PreferenceAdapter.getDimLevel(getContext()) / (float) 100;
                WindowManager.LayoutParams layoutParams = Objects.requireNonNull(getActivity()).getWindow().getAttributes();
                layoutParams.screenBrightness = dimLevel;
                getActivity().getWindow().setAttributes(layoutParams);
            }
        } catch (NullPointerException e) {
            /* Can't dim the screen, oh well */
        }
    }

    /**
     * Called from the activity if the user is active again. Restore the saved brightness.
     */
    @Override
    public void onUserActive() {
        if (PreferenceAdapter.getKeepScreenOn(getContext())) {
            WindowManager.LayoutParams layoutParams = Objects.requireNonNull(getActivity()).getWindow().getAttributes();
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.life_counter_menu, menu);
    }

    /**
     * If TTS is not initialized, remove it from the menu. If it is initialized, show it.
     *
     * @param menu The menu to show or hide the "announce life totals" button in.
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.announce_life);
        assert menuItem != null;
        menuItem.setVisible(mTtsInit && getFamiliarActivity() != null && getFamiliarActivity().mIsMenuVisible);
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
        if (item.getItemId() == R.id.add_player) {                /* Add the player to the ArrayList, set the commander info, and draw the new view */
            addPlayer();
            setCommanderInfo(-1);
            addPlayerView(mPlayers.get(mPlayers.size() - 1));
            return true;
        } else if (item.getItemId() == R.id.remove_player) {                /* Show a dialog of players to remove */
            showDialog(LifeCounterDialogFragment.DIALOG_REMOVE_PLAYER);
            return true;
        } else if (item.getItemId() == R.id.announce_life) {                /* Vocalize the current life totals */
            announceLifeTotals();
            return true;
        } else if (item.getItemId() == R.id.edit_gatherings) {                /* Start a GatheringsFragment to edit gatherings */
            GatheringsFragment rlFrag = new GatheringsFragment();
            startNewFragment(rlFrag, null);
            return true;
        } else if (item.getItemId() == R.id.set_gathering) {                /* Show a dialog of gatherings a user can set */
            showDialog(LifeCounterDialogFragment.DIALOG_SET_GATHERING);
            return true;
        } else if (item.getItemId() == R.id.display_mode) {                /* Show a dialog to change the display mode (normal, compact, commander) */
            showDialog(LifeCounterDialogFragment.DIALOG_CHANGE_DISPLAY);
            return true;
        } else {
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
        LifeCounterDialogFragment newFragment = new LifeCounterDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        newFragment.setArguments(arguments);
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
    public void changeDisplayMode(boolean shouldDefaultLives) {
        /* update the preference */
        PreferenceAdapter.setDisplayMode(getContext(), String.valueOf(mDisplayMode));

        mGridLayout.removeAllViews();

        if (Objects.requireNonNull(getActivity()).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            switch (mDisplayMode) {
                case DISPLAY_NORMAL:
                    mGridLayout.setOrientation(GridLayout.HORIZONTAL);
                    mGridLayout.setColumnCount(1);
                    mGridLayout.setRowCount(GridLayout.UNDEFINED);
                    break;
                case DISPLAY_COMPACT:
                    mGridLayout.setOrientation(GridLayout.HORIZONTAL);
                    mGridLayout.setColumnCount(mNumCols);
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
                    mGridLayout.setOrientation(GridLayout.VERTICAL);
                    mGridLayout.setColumnCount(GridLayout.UNDEFINED);
                    mGridLayout.setRowCount(1);
                    break;
                case DISPLAY_COMPACT:
                    mGridLayout.setOrientation(GridLayout.VERTICAL);
                    mGridLayout.setColumnCount(GridLayout.UNDEFINED);
                    mGridLayout.setRowCount(mNumRows);
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
            if (mPlayers.size() > 0 && null != mPlayers.get(0).mView) {
                mCommanderPlayerView.addView(mPlayers.get(0).mView);
                mPlayers.get(0).setSize(mListSizeWidth, mListSizeHeight, mNumRows, mNumCols, mDisplayMode, getActivity().getResources()
                                .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT,
                        mPlayers.size() == 1);
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
        try {
            mGridLayout.addView(player.newView(mDisplayMode, mStatDisplaying, mGridLayout, mCommanderPlayerView));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (mDisplayMode == DISPLAY_COMMANDER) {
            player.mCommanderRowView.setOnClickListener(view -> {
                /* Show this player's info in mCommanderPlayerView */
                mCommanderPlayerView.removeAllViews();
                mCommanderPlayerView.addView(player.mView);
                player.setSize(mListSizeWidth, mListSizeHeight, mNumRows, mNumCols, mDisplayMode,
                        Objects.requireNonNull(getActivity()).getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_PORTRAIT, mPlayers.size() == 1
                );
            });

            if (mPlayers.size() == 1) {
                mCommanderPlayerView.addView(mPlayers.get(0).mView);
            }
        }

        /* If the size has already been measured, set the player's size */
        resizeAllPlayers();
    }

    /**
     * Change whether life, poison, or commander damage is displayed. This will redraw buttons and notify the LcPlayer
     * objects to show different things.
     *
     * @param statMode either STAT_LIFE, STAT_POISON, or STAT_COMMANDER
     */
    private void setStatDisplaying(int statMode) {
        mStatDisplaying = statMode;

        int disabledColor = ContextCompat.getColor(Objects.requireNonNull(getContext()),
                getFamiliarActivity().getResourceIdFromAttr(R.attr.lc_disabled));
        int enabledColor = ContextCompat.getColor(getContext(),
                getFamiliarActivity().getResourceIdFromAttr(R.attr.lc_enabled));

        // Disable all buttons
        mLifeButton.getDrawable().setColorFilter(
                disabledColor,
                PorterDuff.Mode.SRC_IN);
        mPoisonButton.getDrawable().setColorFilter(
                disabledColor,
                PorterDuff.Mode.SRC_IN);
        mCommanderButton.getDrawable().setColorFilter(
                disabledColor,
                PorterDuff.Mode.SRC_IN);

        // Enable the selected one
        switch (statMode) {
            case STAT_LIFE:
                mLifeButton.getDrawable().setColorFilter(
                        enabledColor,
                        PorterDuff.Mode.SRC_IN);
                break;
            case STAT_POISON:
                mPoisonButton.getDrawable().setColorFilter(
                        enabledColor,
                        PorterDuff.Mode.SRC_IN);
                break;
            case STAT_COMMANDER:
                mCommanderButton.getDrawable().setColorFilter(
                        enabledColor,
                        PorterDuff.Mode.SRC_IN);
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
    public void addPlayer() {
        final LcPlayer player = new LcPlayer(this);

        /* Increment the largest player number */
        mLargestPlayerNumber++;
        player.mName = getString(R.string.life_counter_default_name) + " " + mLargestPlayerNumber;
        player.mDefaultLifeTotal = getDefaultLife();
        player.mLife = player.mDefaultLifeTotal;

        resizeAllPlayers();

        mPlayers.add(player);
    }

    /**
     * Add a player from a name and a starting life. Typically this is from a Gathering
     *
     * @param name         The player's name
     * @param startingLife The player's starting life total
     */
    public void addPlayer(String name, int startingLife) {
        LcPlayer player = new LcPlayer(this);
        player.mName = name;
        player.mDefaultLifeTotal = startingLife;
        player.mLife = startingLife;

        /* If the player's name ends in a number, and the number is larger than mPlayerNumber, set mPlayerNumber as
           it */
        try {
            String[] nameParts = player.mName.split(" ");
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
                String[] nameParts = player.mName.split(" ");
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

            try {
                player.mCommanderExperienceCounter = Integer.parseInt(data[8]);
            } catch (Exception e) {
                player.mCommanderExperienceCounter = 0;
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
                int result;
                try {
                    result = mTts.setLanguage(getResources().getConfiguration().locale);
                } catch (IllegalArgumentException e) {
                    /* This is a new exception on Samsung devices, setting the language isn't necessary */
                    result = TextToSpeech.LANG_AVAILABLE;
                }
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    FamiliarActivity activity = getFamiliarActivity();
                    if (activity != null) {
                        activity.showTtsDialog();
                    }
                } else {
                    mTtsInit = true;
                    if (mIsSearchViewOpen) {
                        /* Search view is open, pend menu refresh */
                        mAfterSearchClosedRunnable = () -> Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                    } else {
                        /* Redraw menu */
                        Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                    }
                }
            } else if (status == TextToSpeech.ERROR) {
                FamiliarActivity activity = getFamiliarActivity();
                if (activity != null) {
                    activity.showTtsDialog();
                }
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
                            String tmp = getResources().getQuantityString(R.plurals.life_counter_spoken_life, IMPROBABLE_NUMBER, p.mName, IMPROBABLE_NUMBER);
                            String[] parts = tmp.split(Integer.toString(IMPROBABLE_NUMBER));
                            mVocalizations.add(parts[0]);
                            mVocalizations.add(OVER_9000_KEY);
                            mVocalizations.add(parts[1]);
                        } else {
                            mVocalizations.add(getResources().getQuantityString(R.plurals.life_counter_spoken_life, p.mLife, p.mName, p.mLife));
                        }
                        break;
                    case STAT_POISON:
                        mVocalizations.add(getResources().getQuantityString(R.plurals.life_counter_spoken_poison, p.mPoison, p.mName, p.mPoison));
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
            if (Objects.requireNonNull(toSpeak).equals(OVER_9000_KEY)) {
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
                        FamiliarActivity activity = getFamiliarActivity();
                        if (activity != null) {
                            activity.showTtsDialog();
                        }
                    }
                }
            } else {
                HashMap<String, String> ttsParams = new HashMap<>();
                ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, LIFE_ANNOUNCE);

                if (mTts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams) == TextToSpeech.ERROR) {
                    FamiliarActivity activity = getFamiliarActivity();
                    if (activity != null) {
                        activity.showTtsDialog();
                    }
                }
            }
        } else {
            mAudioManager.abandonAudioFocus(this);
        }
    }

    /**
     * Resize all players according to the measured screen size
     */
    public void resizeAllPlayers() {
        if (mListSizeHeight != -1) {
            for (LcPlayer player : mPlayers) {
                player.setSize(mListSizeWidth, mListSizeHeight, mNumRows, mNumCols, mDisplayMode,
                        Objects.requireNonNull(getActivity()).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT,
                        mPlayers.size() == 1);
            }
        }
    }
}
