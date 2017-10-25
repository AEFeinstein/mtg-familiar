package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
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
        return (LifeCounterFragment) getParentFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        mDialogId = getArguments().getInt(ID_KEY);

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
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
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
                                getParentLifeCounterFragment().mGridLayout.invalidate();

                                getParentLifeCounterFragment().setCommanderInfo(position);

                                if (getParentLifeCounterFragment().mDisplayMode == LifeCounterFragment.DISPLAY_COMMANDER) {
                                    getParentLifeCounterFragment().mCommanderPlayerView.removeAllViews();
                                    if (getParentLifeCounterFragment().mPlayers.size() > 0) {
                                        getParentLifeCounterFragment().mCommanderPlayerView.addView(getParentLifeCounterFragment().mPlayers.get(0).mView);
                                    }
                                }
                            }
                        });

                return builder.build();
            }
            case DIALOG_RESET_CONFIRM: {
                builder.content(getString(R.string.life_counter_clear_dialog_text))
                        .cancelable(true)
                        .positiveText(getString(R.string.dialog_both))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
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
                        })
                        .neutralText(getString(R.string.dialog_life))
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                /* Only reset life totals */
                                for (LcPlayer player : getParentLifeCounterFragment().mPlayers) {
                                    player.resetStats();
                                }
                                getParentLifeCounterFragment().mGridLayout.invalidate();
                                dialog.dismiss();
                            }
                        })
                        .negativeText(getString(R.string.dialog_cancel));

                return builder.build();
            }
            case DIALOG_CHANGE_DISPLAY: {

                builder.title(R.string.pref_display_mode_title);
                builder.items((CharSequence[]) getResources().getStringArray(R.array.display_array_entries))
                        .itemsCallbackSingleChoice(getParentLifeCounterFragment().mDisplayMode,
                                new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                        dialog.dismiss();

                                        if (getParentLifeCounterFragment().mDisplayMode != which) {
                                            getParentLifeCounterFragment().mDisplayMode = which;
                                            getParentLifeCounterFragment().changeDisplayMode(true);
                                        }
                                        return true;
                                    }
                                }
                        );

                return builder.build();
            }
            case DIALOG_SET_GATHERING: {
                        /* If there aren't any dialogs, don't show the dialog. Pop a toast instead */
                if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
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
                builder.title(R.string.life_counter_gathering_dialog_title);
                builder.items((CharSequence[]) properNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
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
                            }
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