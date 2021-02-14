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

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Class that creates dialogs for LifeCounterFragment
 */
public class LifeCounterDialogFragment extends FamiliarDialogFragment {

    /* Dialog Constants */
    public static final int DIALOG_REMOVE_PLAYER = 0;
    public static final int DIALOG_RESET_CONFIRM = 1;
    public static final int DIALOG_CHANGE_DISPLAY = 2;
    public static final int DIALOG_SET_GATHERING = 3;

    /**
     * @return The currently viewed LifeCounterFragment
     */
    @Nullable
    private LifeCounterFragment getParentLifeCounterFragment() {
        try {
            return (LifeCounterFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(Objects.requireNonNull(getActivity()));
        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);

        if (null == getParentLifeCounterFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_REMOVE_PLAYER: {
                /* Get all the player names */
                String[] names = new String[getParentLifeCounterFragment().mPlayers.size()];
                for (int i = 0; i < getParentLifeCounterFragment().mPlayers.size(); i++) {
                    names[i] = getParentLifeCounterFragment().mPlayers.get(i).mName;
                }

                /* Build the dialog */
                builder.title(getString(R.string.life_counter_remove_player));

                builder.items((CharSequence[]) names)
                        .itemsCallback((dialog, itemView, position, text) -> {
                            /* Remove the view from the GridLayout based on display mode, then remove the player
                               from the ArrayList and redraw. Also notify other players to remove this player from
                               the commander list, and reset the main commander player view in case that player was
                               removed */
                            if (getParentLifeCounterFragment().mDisplayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                                getParentLifeCounterFragment().mGridLayout.removeView(getParentLifeCounterFragment().mPlayers.get(position).mCommanderRowView);
                            } else {
                                getParentLifeCounterFragment().mGridLayout.removeView(getParentLifeCounterFragment().mPlayers.get(position).mView);
                            }
                            getParentLifeCounterFragment().mPlayers.remove(position);
                            getParentLifeCounterFragment().resizeAllPlayers();
                            getParentLifeCounterFragment().mGridLayout.invalidate();

                            getParentLifeCounterFragment().setCommanderInfo(position);

                            if (getParentLifeCounterFragment().mDisplayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                                getParentLifeCounterFragment().mCommanderPlayerView.removeAllViews();
                                if (getParentLifeCounterFragment().mPlayers.size() > 0) {
                                    getParentLifeCounterFragment().mCommanderPlayerView.addView(getParentLifeCounterFragment().mPlayers.get(0).mView);
                                }
                            }
                        });

                return builder.build();
            }
            case DIALOG_RESET_CONFIRM: {
                builder.content(getString(R.string.life_counter_clear_dialog_text))
                        .cancelable(true)
                        .positiveText(getString(R.string.dialog_both))
                        .onPositive((dialog, which) -> {
                            /* Remove all players, then add defaults */
                            getParentLifeCounterFragment().mPlayers.clear();
                            getParentLifeCounterFragment().mLargestPlayerNumber = 0;
                            getParentLifeCounterFragment().addPlayer();
                            getParentLifeCounterFragment().addPlayer();

                            getParentLifeCounterFragment().setCommanderInfo(-1);

                            /* Clear and then add the views */
                            getParentLifeCounterFragment().changeDisplayMode(false);
                            dialog.dismiss();
                        })
                        .neutralText(getString(R.string.dialog_life))
                        .onNeutral((dialog, which) -> {
                            /* Only reset life totals */
                            for (LcPlayer player : getParentLifeCounterFragment().mPlayers) {
                                player.resetStats();
                            }
                            getParentLifeCounterFragment().mGridLayout.invalidate();
                            dialog.dismiss();
                        })
                        .negativeText(getString(R.string.dialog_cancel));

                return builder.build();
            }
            case DIALOG_CHANGE_DISPLAY: {

                builder.title(R.string.pref_display_mode_title);
                builder.items((CharSequence[]) getResources().getStringArray(R.array.display_array_entries))
                        .itemsCallbackSingleChoice(getParentLifeCounterFragment().mDisplayMode,
                                (dialog, itemView, which, text) -> {
                                    dialog.dismiss();

                                    if (getParentLifeCounterFragment().mDisplayMode != which) {
                                        getParentLifeCounterFragment().mDisplayMode = which;
                                        getParentLifeCounterFragment().changeDisplayMode(true);
                                    }
                                    return true;
                                }
                        );

                return builder.build();
            }
            case DIALOG_SET_GATHERING: {
                /* If there aren't any dialogs, don't show the dialog. Pop a toast instead */
                if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                    SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.gathering_toast_no_gatherings,
                            SnackbarWrapper.LENGTH_LONG);
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
                builder.title(R.string.life_counter_gathering_dialog_title);
                builder.items((CharSequence[]) properNames)
                        .itemsCallback((dialog, itemView, position, text) -> {
                            /* Read the gathering from XML, clear and set all the info! changeDisplayMode() adds
                               the player Views */
                            Gathering gathering = GatheringsIO
                                    .ReadGatheringXML(gatherings.get(position), getActivity().getFilesDir());

                            getParentLifeCounterFragment().mDisplayMode = gathering.mDisplayMode;

                            getParentLifeCounterFragment().mPlayers.clear();
                            ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                            for (GatheringsPlayerData player : players) {
                                getParentLifeCounterFragment().addPlayer(player.mName, player.mStartingLife);
                            }

                            getParentLifeCounterFragment().setCommanderInfo(-1);
                            getParentLifeCounterFragment().changeDisplayMode(false);
                        });
                return builder.build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}