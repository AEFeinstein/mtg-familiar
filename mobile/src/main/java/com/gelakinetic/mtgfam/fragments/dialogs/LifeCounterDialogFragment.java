package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.LifeCounterFragment;
import com.gelakinetic.mtgfam.helpers.LcPlayer;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Created by Adam on 4/21/2016.
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
    LifeCounterFragment getParentLifeCounterFragment() {
        return (LifeCounterFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(getActivity());
        mDialogId = getArguments().getInt(ID_KEY);
        switch (mDialogId) {
            case DIALOG_REMOVE_PLAYER: {
                        /* Get all the player names */
                String[] names = new String[getParentLifeCounterFragment().mPlayers.size()];
                for (int i = 0; i < getParentLifeCounterFragment().mPlayers.size(); i++) {
                    names[i] = getParentLifeCounterFragment().mPlayers.get(i).mName;
                }

                        /* Build the dialog */
                builder.setTitle(getString(R.string.life_counter_remove_player));

                builder.setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                                /* Remove the view from the GridLayout based on display mode, then remove the player
                                   from the ArrayList and redraw. Also notify other players to remove this player from
                                   the commander list, and reset the main commander player view in case that player was
                                   removed */
                        if (getParentLifeCounterFragment().mDisplayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                            getParentLifeCounterFragment().mGridLayout.removeView(getParentLifeCounterFragment().mPlayers.get(item).mCommanderRowView);
                        } else {
                            getParentLifeCounterFragment().mGridLayout.removeView(getParentLifeCounterFragment().mPlayers.get(item).mView);
                        }
                        getParentLifeCounterFragment().mPlayers.remove(item);
                        getParentLifeCounterFragment().mGridLayout.invalidate();

                        getParentLifeCounterFragment().setCommanderInfo(item);

                        if (getParentLifeCounterFragment().mDisplayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                            getParentLifeCounterFragment().mCommanderPlayerView.removeAllViews();
                            if (getParentLifeCounterFragment().mPlayers.size() > 0) {
                                getParentLifeCounterFragment().mCommanderPlayerView.addView(getParentLifeCounterFragment().mPlayers.get(0).mView);
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
                                        getParentLifeCounterFragment().mPlayers.clear();
                                        getParentLifeCounterFragment().mLargestPlayerNumber = 0;
                                        getParentLifeCounterFragment().addPlayer();
                                        getParentLifeCounterFragment().addPlayer();

                                        getParentLifeCounterFragment().setCommanderInfo(-1);

                                                /* Clear and then add the views */
                                        getParentLifeCounterFragment().changeDisplayMode(false);
                                        dialog.dismiss();
                                    }
                                }
                        )
                        .setNeutralButton(getString(R.string.dialog_life),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                                /* Only reset life totals */
                                        for (LcPlayer player : getParentLifeCounterFragment().mPlayers) {
                                            player.resetStats();
                                        }
                                        getParentLifeCounterFragment().mGridLayout.invalidate();
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
                        getParentLifeCounterFragment().mDisplayMode,
                        new DialogInterface.OnClickListener() {
                            /* The dialog selection order matches the static integers DISPLAY_NORMAL, etc.
                               Convenient */
                            public void onClick(DialogInterface dialog, int selection) {
                                dialog.dismiss();

                                if (getParentLifeCounterFragment().mDisplayMode != selection) {
                                    getParentLifeCounterFragment().mDisplayMode = selection;
                                    getParentLifeCounterFragment().changeDisplayMode(true);
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

                        getParentLifeCounterFragment().mDisplayMode = gathering.mDisplayMode;

                        getParentLifeCounterFragment().mPlayers.clear();
                        ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                        for (GatheringsPlayerData player : players) {
                            getParentLifeCounterFragment().addPlayer(player.mName, player.mStartingLife);
                        }

                        getParentLifeCounterFragment().setCommanderInfo(-1);
                        getParentLifeCounterFragment().changeDisplayMode(false);
                    }
                });
                return builder.create();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}