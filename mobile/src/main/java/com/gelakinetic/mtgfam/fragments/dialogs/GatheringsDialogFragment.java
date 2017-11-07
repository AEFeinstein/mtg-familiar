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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.GatheringsFragment;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.gatherings.Gathering;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.gatherings.GatheringsPlayerData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Class that creates dialogs for GatheringsFragment
 */
public class GatheringsDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_SAVE_GATHERING = 1;
    private static final int DIALOG_GATHERING_EXIST = 2;
    public static final int DIALOG_DELETE_GATHERING = 3;
    public static final int DIALOG_REMOVE_PLAYER = 4;
    public static final int DIALOG_LOAD_GATHERING = 5;

    /**
     * @return The currently viewed DiceFragment
     */
    @Nullable
    private GatheringsFragment getParentGatheringsFragment() {
        try {
            return (GatheringsFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);

        if (null == getParentGatheringsFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_SAVE_GATHERING: {
                        /* If there are no empty fields, try to save the Gathering. If a gathering with the same
                            name already exists, prompt the user to overwrite it or not. */

                if (getParentGatheringsFragment().AreAnyFieldsEmpty()) {
                    ToastWrapper.makeText(getActivity(), R.string.gathering_empty_field, ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                LayoutInflater factory = LayoutInflater.from(this.getActivity());
                @SuppressLint("InflateParams") final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry,
                        null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                if (getParentGatheringsFragment().mCurrentGatheringName != null) {
                    nameInput.append(getParentGatheringsFragment().mCurrentGatheringName);
                }

                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameInput.setText("");
                    }
                });

                Dialog dialog = new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.gathering_enter_name)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                assert nameInput.getText() != null;
                                String gatheringName = nameInput.getText().toString().trim();
                                if (gatheringName.length() <= 0) {
                                    ToastWrapper.makeText(getActivity(), R.string.gathering_toast_no_name,
                                            ToastWrapper.LENGTH_LONG).show();
                                    return;
                                }

                                ArrayList<String> existingGatheringsFiles =
                                        GatheringsIO.getGatheringFileList(getActivity().getFilesDir());

                                boolean existing = false;
                                for (String existingGatheringFile : existingGatheringsFiles) {
                                    String givenName = GatheringsIO.ReadGatheringNameFromXML(
                                            existingGatheringFile, getActivity().getFilesDir());

                                    if (gatheringName.equals(givenName)) {
                                                /* Show a dialog if the gathering already exists */
                                        existing = true;
                                        getParentGatheringsFragment().mProposedGathering = gatheringName;
                                        getParentGatheringsFragment().showDialog(DIALOG_GATHERING_EXIST);
                                        break;
                                    }
                                }

                                if (existingGatheringsFiles.size() <= 0 || !existing) {
                                    getParentGatheringsFragment().SaveGathering(gatheringName);
                                }
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_GATHERING_EXIST: {
                        /* The user tried to save, and the gathering already exists. Prompt to overwrite */
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.gathering_dialog_overwrite_title)
                        .content(R.string.gathering_dialog_overwrite_text)
                        .positiveText(R.string.dialog_yes)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                GatheringsIO.DeleteGatheringByName(getParentGatheringsFragment().mProposedGathering,
                                        getActivity().getFilesDir(), getActivity());
                                getParentGatheringsFragment().SaveGathering(getParentGatheringsFragment().mProposedGathering);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
            }
            case DIALOG_DELETE_GATHERING: {
                        /* Show all gatherings, and delete the selected one */
                if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                ArrayList<String> dGatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
                final String[] dfGatherings = dGatherings.toArray(new String[dGatherings.size()]);
                final String[] dProperNames = new String[dGatherings.size()];
                for (int idx = 0; idx < dGatherings.size(); idx++) {
                    dProperNames[idx] = GatheringsIO.ReadGatheringNameFromXML(dGatherings.get(idx),
                            getActivity().getFilesDir());
                }

                return new MaterialDialog.Builder(getActivity())
                        .title(R.string.gathering_delete)
                        .items((CharSequence[]) dProperNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                GatheringsIO.DeleteGathering(dfGatherings[position], getActivity().getFilesDir(),
                                        getActivity());
                                getActivity().invalidateOptionsMenu();
                            }
                        })
                        .build();
            }
            case DIALOG_REMOVE_PLAYER: {
                        /* Remove a player from the Gathering and linear layout */
                ArrayList<String> names = new ArrayList<>();
                for (int idx = 0; idx < getParentGatheringsFragment().mLinearLayout.getChildCount(); idx++) {
                    View player = getParentGatheringsFragment().mLinearLayout.getChildAt(idx);
                    assert player != null;
                    EditText customName = player.findViewById(R.id.custom_name);
                    assert customName.getText() != null;
                    names.add(customName.getText().toString().trim());
                }
                final String[] aNames = names.toArray(new String[names.size()]);

                if (names.size() == 0) {
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(getActivity())
                        .title(R.string.life_counter_remove_player)
                        .items((CharSequence[]) aNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                getParentGatheringsFragment().mLinearLayout.removeViewAt(position);
                                getActivity().invalidateOptionsMenu();
                            }
                        })
                        .build();
            }
            case DIALOG_LOAD_GATHERING: {
                        /* Load a gathering, if there is a gathering to load */
                if (GatheringsIO.getNumberOfGatherings(getActivity().getFilesDir()) <= 0) {
                    ToastWrapper.makeText(this.getActivity(), R.string.gathering_toast_no_gatherings,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                ArrayList<String> gatherings = GatheringsIO.getGatheringFileList(getActivity().getFilesDir());
                final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
                final String[] properNames = new String[gatherings.size()];
                for (int idx = 0; idx < gatherings.size(); idx++) {
                    properNames[idx] = GatheringsIO.ReadGatheringNameFromXML(gatherings.get(idx),
                            getActivity().getFilesDir());
                }

                return new MaterialDialog.Builder(getActivity())
                        .title(R.string.gathering_load)
                        .items((CharSequence[]) properNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                getParentGatheringsFragment().mLinearLayout.removeAllViews();
                                getParentGatheringsFragment().mLargestPlayerNumber = 0;
                                Gathering gathering = GatheringsIO.ReadGatheringXML(fGatherings[position],
                                        getActivity().getFilesDir());

                                getParentGatheringsFragment().mCurrentGatheringName = GatheringsIO.ReadGatheringNameFromXML(fGatherings[position],
                                        getActivity().getFilesDir());
                                getParentGatheringsFragment().mDisplayModeSpinner.setSelection(gathering.mDisplayMode);
                                ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                                for (GatheringsPlayerData player : players) {
                                    getParentGatheringsFragment().AddPlayerRow(player);
                                }
                                getActivity().invalidateOptionsMenu();
                            }
                        })
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}