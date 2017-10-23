package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for SearchViewFragment
 */
public class SearchViewDialogFragment extends FamiliarDialogFragment {

    /* Dialog IDs */
    public static final int SET_LIST = 1;
    public static final int FORMAT_LIST = 2;
    public static final int RARITY_LIST = 3;

    /**
     * @return The currently viewed SearchViewFragment
     */
    @Nullable
    private SearchViewFragment getParentSearchViewFragment() {
        return (SearchViewFragment) getParentFamiliarFragment();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            if (null != getParentSearchViewFragment()) {
                getParentSearchViewFragment().checkDialogButtonColors();
            }
        } catch (NullPointerException e) {
            /* Ignore it if there's no activity */
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);

        if (null == getParentSearchViewFragment()) {
            return DontShowDialog();
        }

        try {
            /* Build the dialogs to display format and rarity choices. The arrays were
                already filled in onCreate() */
            switch (mDialogId) {
                case FORMAT_LIST: {
                    getParentSearchViewFragment().mFormatDialog = new MaterialDialog.Builder(this.getActivity())
                            .title(R.string.search_formats)
                            .items((CharSequence[]) getParentSearchViewFragment().mFormatNames)
                            .itemsCallbackSingleChoice(getParentSearchViewFragment().mSelectedFormat, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    getParentSearchViewFragment().mSelectedFormat = which;
                                    return true;
                                }
                            })
                            .positiveText(R.string.dialog_ok)
                            .build();
                    if (null != getParentSearchViewFragment()) {
                        return getParentSearchViewFragment().mFormatDialog;
                    }
                    return DontShowDialog();
                }
                case RARITY_LIST: {
                    getParentSearchViewFragment().mRarityDialog = new MaterialDialog.Builder(this.getActivity())
                            .title(R.string.search_rarities)
                            .positiveText(R.string.dialog_ok)
                            .items((CharSequence[]) getParentSearchViewFragment().mRarityNames)
                            .alwaysCallMultiChoiceCallback()
                            .itemsCallbackMultiChoice(toIntegerArray(getParentSearchViewFragment().mRarityCheckedIndices), new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                    getParentSearchViewFragment().mRarityCheckedIndices = toIntArray(which);
                                    return true;
                                }
                            })
                            .build();
                    if (null != getParentSearchViewFragment()) {
                        return getParentSearchViewFragment().mRarityDialog;
                    }
                    return DontShowDialog();
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

    /**
     * Make an int[] from an Integer[]
     *
     * @param which An Integer[]
     * @return An int[] with the same values as "which"
     */
    private int[] toIntArray(Integer[] which) {
        int tmp[] = new int[which.length];
        for (int i = 0; i < which.length; i++) {
            tmp[i] = which[i];
        }
        return tmp;
    }

    /**
     * Make an int[] from an Integer[]
     *
     * @param which An Integer[]
     * @return An int[] with the same values as "which"
     */
    private Integer[] toIntegerArray(int[] which) {
        Integer tmp[] = new Integer[which.length];
        for (int i = 0; i < which.length; i++) {
            tmp[i] = which[i];
        }
        return tmp;
    }
}