package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.AlertDialogWrapper;
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
    private GatheringsFragment getParentGatheringsFragment() {
        return (GatheringsFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);

        switch (mDialogId) {
            case DIALOG_SAVE_GATHERING: {
                        /* If there are no empty fields, try to save the Gathering. If a gathering with the same
                            name already exists, prompt the user to overwrite it or not. */

                if (getParentGatheringsFragment().AreAnyFieldsEmpty()) {
                    ToastWrapper.makeText(getActivity(), R.string.gathering_empty_field, ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                LayoutInflater factory = LayoutInflater.from(this.getActivity());
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry,
                        null, false);
                assert textEntryView != null;
                final EditText nameInput = (EditText) textEntryView.findViewById(R.id.text_entry);
                if (getParentGatheringsFragment().mCurrentGatheringName != null) {
                    nameInput.append(getParentGatheringsFragment().mCurrentGatheringName);
                }

                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameInput.setText("");
                    }
                });

                Dialog dialog = new AlertDialogWrapper.Builder(this.getActivity())
                        .setTitle(R.string.gathering_enter_name)
                        .setView(textEntryView)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
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
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_GATHERING_EXIST: {
                        /* The user tried to save, and the gathering already exists. Prompt to overwrite */
                return new AlertDialogWrapper.Builder(this.getActivity())
                        .setTitle(R.string.gathering_dialog_overwrite_title)
                        .setMessage(R.string.gathering_dialog_overwrite_text)
                        .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                GatheringsIO.DeleteGatheringByName(getParentGatheringsFragment().mProposedGathering,
                                        getActivity().getFilesDir(), getActivity());
                                getParentGatheringsFragment().SaveGathering(getParentGatheringsFragment().mProposedGathering);
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .create();
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

                return new AlertDialogWrapper.Builder(getActivity())
                        .setTitle(R.string.gathering_delete)
                        .setItems(dProperNames, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item) {
                                GatheringsIO.DeleteGathering(dfGatherings[item], getActivity().getFilesDir(),
                                        getActivity());
                                getActivity().supportInvalidateOptionsMenu();
                            }
                        }).create();
            }
            case DIALOG_REMOVE_PLAYER: {
                        /* Remove a player from the Gathering and linear layout */
                ArrayList<String> names = new ArrayList<>();
                for (int idx = 0; idx < getParentGatheringsFragment().mLinearLayout.getChildCount(); idx++) {
                    View player = getParentGatheringsFragment().mLinearLayout.getChildAt(idx);
                    assert player != null;
                    EditText customName = (EditText) player.findViewById(R.id.custom_name);
                    assert customName.getText() != null;
                    names.add(customName.getText().toString().trim());
                }
                final String[] aNames = names.toArray(new String[names.size()]);

                if (names.size() == 0) {
                    return DontShowDialog();
                }

                return new AlertDialogWrapper.Builder(getActivity())
                        .setTitle(R.string.gathering_remove_player)
                        .setItems(aNames, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item) {
                                getParentGatheringsFragment().mLinearLayout.removeViewAt(item);
                                getActivity().supportInvalidateOptionsMenu();
                            }
                        })
                        .create();
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

                return new AlertDialogWrapper.Builder(getActivity())
                        .setTitle(R.string.gathering_load)
                        .setItems(properNames, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int item) {
                                getParentGatheringsFragment().mLinearLayout.removeAllViews();
                                getParentGatheringsFragment().mLargestPlayerNumber = 0;
                                Gathering gathering = GatheringsIO.ReadGatheringXML(fGatherings[item],
                                        getActivity().getFilesDir());

                                getParentGatheringsFragment().mCurrentGatheringName = GatheringsIO.ReadGatheringNameFromXML(fGatherings[item],
                                        getActivity().getFilesDir());
                                getParentGatheringsFragment().mDisplayModeSpinner.setSelection(gathering.mDisplayMode);
                                ArrayList<GatheringsPlayerData> players = gathering.mPlayerList;
                                for (GatheringsPlayerData player : players) {
                                    getParentGatheringsFragment().AddPlayerRow(player);
                                }
                                getActivity().supportInvalidateOptionsMenu();
                            }
                        }).create();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}