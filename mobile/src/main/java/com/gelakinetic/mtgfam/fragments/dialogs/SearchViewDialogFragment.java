package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Adam on 4/22/2016.
 */
public class SearchViewDialogFragment extends FamiliarDialogFragment {

    /* Dialog IDs */
    public static final int SET_LIST = 1;
    public static final int FORMAT_LIST = 2;
    public static final int RARITY_LIST = 3;

    /**
     * @return The currently viewed SearchViewFragment
     */
    SearchViewFragment getParentSearchViewFragment() {
        return (SearchViewFragment) getFamiliarFragment();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getParentSearchViewFragment().checkDialogButtonColors();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

                /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        DialogInterface.OnMultiChoiceClickListener multiChoiceClickListener =
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {

                    }
                };
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        };

        mDialogId = getArguments().getInt(ID_KEY);
        try {
                    /* Build the dialogs to display format, rarity, and set choices. The arrays were already filled in
                       onCreate() */
            switch (mDialogId) {
                case SET_LIST: {
                    getParentSearchViewFragment().mSetDialog = new AlertDialogWrapper.Builder(this.getActivity()).setTitle(R.string.search_sets)
                            .setMultiChoiceItems(getParentSearchViewFragment().mSetNames, getParentSearchViewFragment().mSetChecked, multiChoiceClickListener)
                            .setPositiveButton(R.string.dialog_ok, clickListener).create();
                    return getParentSearchViewFragment().mSetDialog;
                }
                case FORMAT_LIST: {
                    getParentSearchViewFragment().mFormatDialog = new AlertDialogWrapper.Builder(this.getActivity()).
                            setTitle(R.string.search_formats).setSingleChoiceItems(getParentSearchViewFragment().mFormatNames,
                            getParentSearchViewFragment().mSelectedFormat, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    getParentSearchViewFragment().mSelectedFormat = which;
                                }
                            }
                    ).setPositiveButton(R.string.dialog_ok, clickListener).create();
                    return getParentSearchViewFragment().mFormatDialog;
                }
                case RARITY_LIST: {
                    getParentSearchViewFragment().mRarityDialog = new AlertDialogWrapper.Builder(this.getActivity())
                            .setTitle(R.string.search_rarities).setMultiChoiceItems(getParentSearchViewFragment().mRarityNames,
                                    getParentSearchViewFragment().mRarityChecked, multiChoiceClickListener)
                            .setPositiveButton(R.string.dialog_ok, clickListener).create();
                    return getParentSearchViewFragment().mRarityDialog;
                }
                default: {
                    return DontShowDialog();
                }
            }
        } catch (NullPointerException e) {
                    /* if the db failed to open, these arrays will be null. */
            getParentSearchViewFragment().handleFamiliarDbException(false);
            return DontShowDialog();
        }
    }
}