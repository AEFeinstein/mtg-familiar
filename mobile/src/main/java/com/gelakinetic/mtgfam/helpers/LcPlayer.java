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

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.dialogs.LcPlayerDialogFragment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class LcPlayer {
    /* Handler for committing life changes */
    private final Handler mHandler = new Handler();
    /* Reference to parent fragment */
    private final LifeCounterFragment mFragment;
    /* Histories and adapters for life, poison, commander damage */
    public ArrayList<HistoryEntry> mLifeHistory = new ArrayList<>();
    public ArrayList<HistoryEntry> mPoisonHistory = new ArrayList<>();
    public ArrayList<CommanderEntry> mCommanderDamage = new ArrayList<>();
    public CommanderDamageAdapter mCommanderDamageAdapter;

    /* Stats */
    public int mLife = LifeCounterFragment.DEFAULT_LIFE;
    public int mPoison = 0;
    public String mName;
    public int mCommanderCasting = 0;
    public int mCommanderExperienceCounter = 0;
    public int mDefaultLifeTotal = LifeCounterFragment.DEFAULT_LIFE;
    /* The player's View to be drawn in the LifeCounterFragment */
    public View mView;
    public View mCommanderRowView;
    private HistoryArrayAdapter mHistoryLifeAdapter;
    private HistoryArrayAdapter mHistoryPoisonAdapter;
    private boolean mCommitting = false;
    /* This runnable will commit any life / poison changes to history, and notify the adapter to display the changes
       in the ListView */
    private final Runnable mLifePoisonCommitter = new Runnable() {
        @Override
        public void run() {
            mCommitting = false;

            if (mLifeHistory.size() > 0 && mLifeHistory.get(0).mDelta == 0) {
                mLifeHistory.remove(0);
            }
            if (mPoisonHistory.size() > 0 && mPoisonHistory.get(0).mDelta == 0) {
                mPoisonHistory.remove(0);
            }

            if (mHistoryLifeAdapter != null) {
                mHistoryLifeAdapter.notifyDataSetChanged();
            }
            if (mHistoryPoisonAdapter != null) {
                mHistoryPoisonAdapter.notifyDataSetChanged();
            }
        }
    };
    public int mMode = LifeCounterFragment.STAT_LIFE;
    /* UI Elements */
    public TextView mNameTextView;
    public TextView mReadoutTextView;
    public TextView mCommanderNameTextView;
    private TextView mCommanderReadoutTextView;
    private Button mCommanderCastingButton;
    private Button mCommanderExperienceCountersButton;
    private ListView mHistoryList;

    /**
     * Constructor, save a reference to the parent fragment, and set a default name just in case
     *
     * @param fragment The parent fragment
     */
    public LcPlayer(LifeCounterFragment fragment) {
        mFragment = fragment;
        mName = Objects.requireNonNull(mFragment.getActivity()).getString(R.string.life_counter_default_name);
    }

    /**
     * Sets the display mode between life and poison. Switches the readout, commander readout, color, and history
     *
     * @param mode either STAT_LIFE, STAT_POISON, or STAT_COMMANDER
     */
    public void setMode(int mode) {

        if (null == mReadoutTextView) {
            return;
        }

        /* Commit any changes before switching modes */
        if (mCommitting) {
            mCommitting = false;
            mHandler.removeCallbacks(mLifePoisonCommitter);
            mLifePoisonCommitter.run();
        }

        mMode = mode;
        switch (mMode) {
            case LifeCounterFragment.STAT_LIFE:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mHistoryLifeAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(formatInt(mLife, false));
                mReadoutTextView.setTextColor(ContextCompat.getColor(Objects.requireNonNull(mFragment.getContext()),
                        R.color.material_red_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(formatInt(mLife, false));
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_red_500));
                }
                break;
            case LifeCounterFragment.STAT_POISON:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mHistoryPoisonAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(formatInt(mPoison, false));
                mReadoutTextView.setTextColor(ContextCompat.getColor(Objects.requireNonNull(mFragment.getContext()),
                        R.color.material_green_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(formatInt(mPoison, false));
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_green_500));
                }
                break;
            case LifeCounterFragment.STAT_COMMANDER:
                if (mHistoryList != null) {
                    mHistoryList.setAdapter(mCommanderDamageAdapter);
                    mHistoryList.invalidate();
                }
                mReadoutTextView.setText(formatInt(mLife, false));
                mReadoutTextView.setTextColor(ContextCompat.getColor(Objects.requireNonNull(mFragment.getContext()),
                        R.color.material_red_500));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(formatInt(mLife, false));
                    mCommanderReadoutTextView.setTextColor(
                            ContextCompat.getColor(mFragment.getContext(), R.color.material_red_500));
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
    public void changeValue(int delta, boolean immediate) {
        switch (mMode) {
            case LifeCounterFragment.STAT_POISON:
                mPoison += delta;
                mReadoutTextView.setText(formatInt(mPoison, false));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(formatInt(mPoison, false));
                }
                break;
            case LifeCounterFragment.STAT_COMMANDER:
            case LifeCounterFragment.STAT_LIFE:
                mLife += delta;
                mReadoutTextView.setText(formatInt(mLife, false));
                if (mCommanderReadoutTextView != null) {
                    mCommanderReadoutTextView.setText(formatInt(mLife, false));
                }
                break;
        }

        if (delta == 0) {
            return;
        }

        /* If we're not committing yet, make a new history entry */
        if (!mCommitting) {
            addNewLifeHistoryEntry();
            addNewPoisonHistoryEntry();
        } else if (!immediate) {
            /* Modify current historyEntry */
            switch (mMode) {
                case LifeCounterFragment.STAT_POISON: {
                    if (mPoisonHistory.isEmpty()) {
                        addNewPoisonHistoryEntry();
                    }
                    mPoisonHistory.get(0).mDelta += delta;
                    mPoisonHistory.get(0).mAbsolute += delta;
                    if (null != mHistoryPoisonAdapter) {
                        mHistoryPoisonAdapter.notifyDataSetChanged();
                    }
                    break;
                }
                case LifeCounterFragment.STAT_COMMANDER:
                case LifeCounterFragment.STAT_LIFE: {
                    if (mLifeHistory.isEmpty()) {
                        addNewLifeHistoryEntry();
                    }
                    mLifeHistory.get(0).mDelta += delta;
                    mLifeHistory.get(0).mAbsolute += delta;
                    if (null != mHistoryLifeAdapter) {
                        mHistoryLifeAdapter.notifyDataSetChanged();
                    }
                    break;
                }
            }
        }

        if (!immediate) {
            mCommitting = true;
            mHandler.removeCallbacks(mLifePoisonCommitter);
            mHandler.postDelayed(mLifePoisonCommitter,
                    Integer.parseInt(PreferenceAdapter.getLifeTimer(mFragment.getContext())));

        }
    }

    /**
     * Create and add a new HistoryEntry to the mPoisonHistory
     * If there was a change, notify the adapter
     */
    private void addNewPoisonHistoryEntry() {
        HistoryEntry entry = new HistoryEntry();
        if (mPoisonHistory.size() == 0) {
            entry.mDelta = mPoison;
        } else {
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

    /**
     * Create and add a new HistoryEntry to the mDefaultLifeTotal
     * If there was a change, notify the adapter
     */
    private void addNewLifeHistoryEntry() {
        /* Create a new historyEntry */
        HistoryEntry entry = new HistoryEntry();
        /* If there are no entries, assume life is mDefaultLifeTotal */
        if (mLifeHistory.size() == 0) {
            entry.mDelta = mLife - mDefaultLifeTotal;
        } else {
            entry.mDelta = mLife - mLifeHistory.get(0).mAbsolute;
        }
        entry.mAbsolute = mLife;
        if (entry.mDelta != 0) {
            mLifeHistory.add(0, entry);
            if (mHistoryLifeAdapter != null) {
                mHistoryLifeAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Inflate the necessary views for this player, set the onClickListeners, and return the view to be added to the
     * GridView
     *
     * @param displayMode         The display mode, either DISPLAY_COMMANDER, DISPLAY_COMPACT or DISPLAY_NORMAL
     * @param statType            The stat type being displayed, either STAT_POISON, STAT_LIFE, or STAT_COMMANDER
     * @param playersView         The GridLayout to inflate all players into
     * @param commanderPlayerView The LinearLayout to inflate commander players into
     * @return The view to be added to the GridView. Can either be mView or mCommanderRowView
     */
    public View newView(int displayMode, int statType, GridLayout playersView, LinearLayout commanderPlayerView) {
        switch (displayMode) {
            case LifeCounterFragment.DISPLAY_COMMANDER:
            case LifeCounterFragment.DISPLAY_NORMAL: {
                /* Inflate the player view */
                if (LifeCounterFragment.DISPLAY_COMMANDER == displayMode) {
                    mView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player, commanderPlayerView, false);
                } else {
                    mView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.life_counter_player, playersView, false);
                }
                assert mView != null;
                mHistoryList = mView.findViewById(R.id.player_history);
                mCommanderCastingButton = mView.findViewById(R.id.commanderCast);
                mCommanderExperienceCountersButton = mView.findViewById(R.id.commanderExperienceCounter);

                /* Make new adapters */
                mHistoryLifeAdapter = new HistoryArrayAdapter(mFragment.getActivity(), LifeCounterFragment.STAT_LIFE);
                mHistoryPoisonAdapter
                        = new HistoryArrayAdapter(mFragment.getActivity(), LifeCounterFragment.STAT_POISON);
                mCommanderDamageAdapter = new CommanderDamageAdapter(mFragment.getActivity());

                /* If it's commander, also inflate the entry to display in the grid, and set up the casting and experience counter button */
                if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                    setupCommanderCastingButton();
                    setupCommanderExperienceCounterButton();

                    mCommanderRowView = LayoutInflater.from(
                            mFragment.getActivity()).inflate(R.layout.life_counter_player_commander, playersView, false);
                    assert mCommanderRowView != null;
                    mCommanderNameTextView = mCommanderRowView.findViewById(R.id.player_name);
                    if (mName != null) {
                        mCommanderNameTextView.setText(mName);
                    }
                    mCommanderReadoutTextView = mCommanderRowView.findViewById(R.id.player_readout);
                }
                /* otherwise hide the commander casting and experience counter button */
                else {
                    mView.findViewById(R.id.commanderCastText).setVisibility(View.GONE);
                    mCommanderCastingButton.setVisibility(View.GONE);

                    mView.findViewById(R.id.commanderExperienceCounterText).setVisibility(View.GONE);
                    mCommanderExperienceCountersButton.setVisibility(View.GONE);
                }

                break;
            }
            case LifeCounterFragment.DISPLAY_COMPACT: {
                /* inflate the compact view */
                mView = LayoutInflater
                        .from(mFragment.getActivity()).inflate(R.layout.life_counter_player_compact, playersView, false);
                /* don't bother with adapters */
                mHistoryList = null;
                mHistoryLifeAdapter = null;
                mHistoryPoisonAdapter = null;
                mCommanderDamageAdapter = null;
                break;
            }
        }
        assert mView != null;

        /* Set the name, will be in either compact or normal mView */
        mNameTextView = mView.findViewById(R.id.player_name);
        if (mName != null) {
            mNameTextView.setText(mName);
        }
        /* If the user touches the life total, pop a dialog to change it via keyboard */
        mReadoutTextView = mView.findViewById(R.id.player_readout);
        mReadoutTextView.setOnClickListener(view -> {
            /* Commit any changes before showing the dialog */
            if (mCommitting) {
                mCommitting = false;
                mHandler.removeCallbacks(mLifePoisonCommitter);
                mLifePoisonCommitter.run();
            }
            showDialog(LcPlayerDialogFragment.DIALOG_CHANGE_LIFE, -1);
        });

        /* Set the life / poison modifier buttons */
        mView.findViewById(R.id.player_minus1).setOnClickListener(view -> changeValue(-1, false));
        mView.findViewById(R.id.player_minus5).setOnClickListener(view -> changeValue(-5, false));
        mView.findViewById(R.id.player_plus1).setOnClickListener(view -> changeValue(1, false));
        mView.findViewById(R.id.player_plus5).setOnClickListener(view -> changeValue(5, false));

        mView.findViewById(R.id.player_name).setOnClickListener(view -> showDialog(LcPlayerDialogFragment.DIALOG_SET_NAME, -1));

        setMode(statType);

        if (displayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
            return mCommanderRowView;
        } else {
            return mView;
        }
    }

    private void setupCommanderCastingButton() {
        mView.findViewById(R.id.commanderCastText).setVisibility(View.VISIBLE);
        mCommanderCastingButton.setText(formatInt(mCommanderCasting, false));
        mCommanderCastingButton.setVisibility(View.VISIBLE);
        mCommanderCastingButton.setOnClickListener(view -> {
            mCommanderCasting++;
            mCommanderCastingButton.setText(formatInt(mCommanderCasting, false));
        });
        mCommanderCastingButton.setOnLongClickListener(view -> {
            mCommanderCasting--;
            if (mCommanderCasting < 0) {
                mCommanderCasting = 0;
            }
            mCommanderCastingButton.setText(formatInt(mCommanderCasting, false));
            return true;
        });
    }

    private void setupCommanderExperienceCounterButton() {
        mView.findViewById(R.id.commanderExperienceCounterText).setVisibility(View.VISIBLE);
        mCommanderExperienceCountersButton.setText(formatInt(mCommanderExperienceCounter, false));
        mCommanderExperienceCountersButton.setVisibility(View.VISIBLE);
        mCommanderExperienceCountersButton.setOnClickListener(view -> {
            mCommanderExperienceCounter++;
            mCommanderExperienceCountersButton.setText(formatInt(mCommanderExperienceCounter, false));
        });
        mCommanderExperienceCountersButton.setOnLongClickListener(view -> {
            mCommanderExperienceCounter--;
            if (mCommanderExperienceCounter < 0) {
                mCommanderExperienceCounter = 0;
            }
            mCommanderExperienceCountersButton.setText(formatInt(mCommanderExperienceCounter, false));
            return true;
        });
    }

    /**
     * @param i       The int to turn into a string
     * @param addSign true to have a leading "+", false otherwise
     * @return The String representation of i
     */
    private String formatInt(int i, boolean addSign) {
        if (addSign) {
            return String.format(Locale.getDefault(), "%+d", i);
        }
        return String.format(Locale.getDefault(), "%d", i);
    }

    /**
     * Returns a string containing all the player data in the form:
     * name; life; life History; poison; poison History; default Life; commander History; commander casting; commander experience
     * The history entries are comma delimited
     *
     * @return A string of player data
     */
    @NonNull
    public String toString() {
        StringBuilder data = new StringBuilder();
        data.append(mName.replace(";", ""));
        data.append(";");

        boolean first = true;
        data.append(mLife);
        data.append(";");
        for (HistoryEntry entry : mLifeHistory) {
            if (first) {
                first = false;
            } else {
                data.append(",");
            }
            data.append(entry.mAbsolute);
        }

        data.append(";");

        first = true;
        data.append(mPoison);
        data.append(";");
        for (HistoryEntry entry : mPoisonHistory) {
            if (first) {
                first = false;
            } else {
                data.append(",");
            }
            data.append(entry.mAbsolute);
        }

        data.append(";");
        data.append(mDefaultLifeTotal);

        first = true;
        for (CommanderEntry entry : mCommanderDamage) {
            if (first) {
                first = false;
                data.append(";");
            } else {
                data.append(",");
            }
            data.append(entry.mLife);
        }

        data.append(";");
        data.append(mCommanderCasting);
        data.append(";");
        data.append(mCommanderExperienceCounter);
        data.append(";\n");

        return data.toString();
    }

    /**
     * Reset the life, poison, and commander damage to default while preserving the name and default life
     */
    public void resetStats() {
        mLifeHistory.clear();
        mPoisonHistory.clear();
        mLife = mDefaultLifeTotal;
        mPoison = 0;
        mCommanderCasting = 0;
        mCommanderExperienceCounter = 0;

        for (CommanderEntry entry : mCommanderDamage) {
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

        /* Check for -1 life? */
        if (mLife == -1) {
            mLife = LifeCounterFragment.DEFAULT_LIFE;
        }

        /* Redraw life totals */
        changeValue(0, true);
        if (mCommanderCastingButton != null) {
            mCommanderCastingButton.setText(formatInt(mCommanderCasting, false));
        }

        if (mCommanderExperienceCountersButton != null) {
            mCommanderExperienceCountersButton.setText(formatInt(mCommanderExperienceCounter, false));
        }
    }

    /**
     * Set the size of the player's view
     *
     * @param mGridLayoutWidth  The width of the GridLayout in which to put the player's view
     * @param mGridLayoutHeight The height of the GridLayout in which to put the player's view
     * @param numRows           The number of rows for the compact view
     * @param numCols           The number of columns for the compact view
     * @param displayMode       either LifeCounterFragment.DISPLAY_COMPACT or LifeCounterFragment.DISPLAY_NORMAL
     * @param isPortrait        The orientation of the device
     * @param isSingle          true if this is the only player, false otherwise
     */
    public void setSize(int mGridLayoutWidth, int mGridLayoutHeight, int numRows, int numCols, int displayMode, boolean isPortrait, boolean isSingle) {

        if (null == mView) {
            return;
        }

        switch (displayMode) {
            case LifeCounterFragment.DISPLAY_NORMAL: {
                ViewGroup.LayoutParams params = mView.getLayoutParams();
                if (null != params) {
                    if (isSingle) {
                        params.width = mGridLayoutWidth;
                        params.height = mGridLayoutHeight;
                    } else if (isPortrait) {
                        params.width = mGridLayoutWidth;
                        params.height = mGridLayoutHeight / 2;
                    } else {
                        params.width = mGridLayoutWidth / 2;
                        params.height = mGridLayoutHeight;
                    }
                    mView.setLayoutParams(params);
                }
                break;
            }
            case LifeCounterFragment.DISPLAY_COMPACT: {
                ViewGroup.LayoutParams params = mView.getLayoutParams();
                if (null != params) {
                    params.width = mGridLayoutWidth / numCols;
                    params.height = mGridLayoutHeight / numRows;
                    mView.setLayoutParams(params);
                }
                break;
            }
            case LifeCounterFragment.DISPLAY_COMMANDER: {
                /* Set the row height to 48dp and the width to some fraction of the screen */
                if (null != mCommanderRowView) {
                    ViewGroup.LayoutParams rowParams = mCommanderRowView.getLayoutParams();
                    if (null != rowParams) {
                        rowParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                                Objects.requireNonNull(mFragment.getActivity()).getResources().getDisplayMetrics());
                        if (isPortrait) {
                            rowParams.width = mGridLayoutWidth / 2;
                        } else {
                            rowParams.width = mGridLayoutWidth / 4;
                        }
                        mCommanderRowView.setLayoutParams(rowParams);
                    }
                }

                /* Then set the player view to half the screen, if in landscape */
                ViewGroup.LayoutParams viewParams = mView.getLayoutParams();
                if (null != viewParams) {
                    if (!isPortrait) {
                        viewParams.width = mGridLayoutWidth / 2;
                    }
                    mView.setLayoutParams(viewParams);
                }
                break;
            }
        }
    }

    /**
     * Called when the parent fragment pauses. Use this time to commit all pending transactions
     */
    public void onPause() {
        mHandler.removeCallbacks(mLifePoisonCommitter);
        mLifePoisonCommitter.run();
    }

    /**
     * Remove any showing dialogs, and show the requested one
     *
     * @param id       the ID of the dialog to show
     * @param position Which commander the player is taking damage from
     */
    private void showDialog(final int id, final int position) throws IllegalStateException {
        /* DialogFragment.show() will take care of adding the fragment in a transaction. We also want to remove any
        currently showing dialog, so make our own transaction and take care of that here. */

        ((FamiliarActivity) mFragment.getActivity())
                .removeDialogFragment(Objects.requireNonNull(mFragment.getActivity()).getSupportFragmentManager());

        /* Create and show the dialog. */
        LcPlayerDialogFragment newFragment = new LcPlayerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(FamiliarDialogFragment.ID_KEY, id);
        arguments.putInt(LcPlayerDialogFragment.POSITION_KEY, position);
        newFragment.setArguments(arguments);
        newFragment.setLcPlayer(this);
        newFragment.show(mFragment.getActivity().getSupportFragmentManager(), FamiliarActivity.DIALOG_TAG);
    }

    /**
     * Inner class to encapsulate an entry in the history list
     */
    public static class HistoryEntry {
        public int mDelta;
        public int mAbsolute;
    }

    /**
     * Inner class to encapsulate an entry in the commander damage list
     */
    public static class CommanderEntry {
        public int mLife;
        public String mName;
    }

    /**
     * Inner class to display the HistoryEntries in a ListView
     */
    class HistoryArrayAdapter extends ArrayAdapter<HistoryEntry> {

        private final int mType;

        /**
         * Constructor
         *
         * @param context a context to use in the superclass constructor
         * @param type    either STAT_LIFE or STAT_POISON
         */
        HistoryArrayAdapter(Context context, int type) {
            super(context, R.layout.life_counter_history_adapter_row,
                    (type == LifeCounterFragment.STAT_LIFE) ? mLifeHistory : mPoisonHistory);
            mType = type;
        }

        /**
         * Called to get a view for an entry in the listView
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible.
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = LayoutInflater.from(mFragment.getActivity())
                        .inflate(R.layout.life_counter_history_adapter_row, parent, false);
            }
            assert view != null;

            if (mCommitting && position == 0) {
                ((TextView) view.findViewById(R.id.absolute)).setTextColor(ContextCompat.getColor(Objects.requireNonNull(mFragment.getContext()),
                        mFragment.getResourceIdFromAttr(R.attr.colorPrimary_attr)));
            } else {
                ((TextView) view.findViewById(R.id.absolute)).setTextColor(ContextCompat.getColor(Objects.requireNonNull(mFragment.getContext()),
                        mFragment.getResourceIdFromAttr(R.attr.color_text)));
            }

            switch (mType) {
                case LifeCounterFragment.STAT_LIFE:
                    ((TextView) view.findViewById(R.id.absolute)).setText(formatInt(mLifeHistory.get(position).mAbsolute, false));
                    if (mLifeHistory.get(position).mDelta > 0) {
                        ((TextView) view.findViewById(R.id.relative)).setText(formatInt(mLifeHistory.get(position).mDelta, true));
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(),
                                        R.color.material_green_500)
                        );
                    } else {
                        ((TextView) view.findViewById(R.id.relative)).setText(formatInt(mLifeHistory.get(position).mDelta, true));
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(),
                                        R.color.material_red_500)
                        );
                    }
                    break;
                case LifeCounterFragment.STAT_POISON:
                    ((TextView) view.findViewById(R.id.absolute)).setText(formatInt(mPoisonHistory.get(position).mAbsolute, false));
                    if (mPoisonHistory.get(position).mDelta > 0) {
                        ((TextView) view.findViewById(R.id.relative))
                                .setText(formatInt(mPoisonHistory.get(position).mDelta, true));
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(),
                                        R.color.material_green_500)
                        );
                    } else {
                        ((TextView) view.findViewById(R.id.relative)).setText(formatInt(mPoisonHistory.get(position).mDelta, true));
                        ((TextView) view.findViewById(R.id.relative)).setTextColor(
                                ContextCompat.getColor(mFragment.getContext(),
                                        R.color.material_red_500)
                        );
                    }
                    break;
            }
            return view;
        }

    }

    /**
     * Inner class to display the HistoryEntries in a ListView
     */
    public class CommanderDamageAdapter extends ArrayAdapter<CommanderEntry> {

        /**
         * Constructor
         *
         * @param context a context to use in the superclass constructor
         */
        CommanderDamageAdapter(Context context) {
            super(context, R.layout.life_counter_player_commander, mCommanderDamage);
        }

        /**
         * Called to get a view for an entry in the listView
         *
         * @param position    The position of the listView to populate
         * @param convertView The old view to reuse, if possible.
         * @param parent      The parent this view will eventually be attached to
         * @return The view for the data at this position
         */
        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = LayoutInflater.from(mFragment.getActivity())
                        .inflate(R.layout.life_counter_player_commander, parent, false);
            }
            assert view != null;

            ((TextView) view.findViewById(R.id.player_name)).setText(mCommanderDamage.get(position).mName);
            ((TextView) view.findViewById(R.id.player_readout)).setText(formatInt(mCommanderDamage.get(position).mLife, false));

            view.findViewById(R.id.dividerH).setVisibility(View.GONE);
            view.findViewById(R.id.dividerV).setVisibility(View.GONE);

            view.setOnClickListener(view1 -> showDialog(LcPlayerDialogFragment.DIALOG_COMMANDER_DAMAGE, position));
            return view;
        }

    }
}