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
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;

import java.util.Objects;

/**
 * Class that creates dialogs for SearchViewFragment
 */
public class SearchViewDialogFragment extends FamiliarDialogFragment {

    public static final int FORMAT_LIST = 2;
    public static final int RARITY_LIST = 3;

    /**
     * @return The currently viewed SearchViewFragment
     */
    @Nullable
    private SearchViewFragment getParentSearchViewFragment() {
        try {
            return (SearchViewFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            if (null != getParentSearchViewFragment()) {
                getParentSearchViewFragment().checkDialogButtonColors();
            }
        } catch (NullPointerException e) {
            /* Ignore it if there's no activity */
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

        mDialogId = Objects.requireNonNull(getArguments()).getInt(ID_KEY);

        if (null == getParentSearchViewFragment()) {
            return DontShowDialog();
        }

        try {
            /* Build the dialogs to display format and rarity choices. The arrays were
                already filled in onCreate() */
            switch (mDialogId) {
                case FORMAT_LIST: {
                    getParentSearchViewFragment().mFormatDialog = new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()))
                            .title(R.string.search_formats)
                            .items((CharSequence[]) getParentSearchViewFragment().mFormatNames)
                            .itemsCallbackSingleChoice(getParentSearchViewFragment().mSelectedFormat, (dialog, itemView, which, text) -> {
                                getParentSearchViewFragment().mSelectedFormat = which;
                                return true;
                            })
                            .positiveText(R.string.dialog_ok)
                            .build();
                    if (null != getParentSearchViewFragment()) {
                        return getParentSearchViewFragment().mFormatDialog;
                    }
                    return DontShowDialog();
                }
                case RARITY_LIST: {
                    getParentSearchViewFragment().mRarityDialog = new MaterialDialog.Builder(Objects.requireNonNull(this.getActivity()))
                            .title(R.string.search_rarities)
                            .positiveText(R.string.dialog_ok)
                            .items((CharSequence[]) getParentSearchViewFragment().mRarityNames)
                            .alwaysCallMultiChoiceCallback()
                            .itemsCallbackMultiChoice(toIntegerArray(getParentSearchViewFragment().mRarityCheckedIndices), (dialog, which, text) -> {
                                getParentSearchViewFragment().mRarityCheckedIndices = toIntArray(which);
                                return true;
                            })
                            .build();
                    if (null != getParentSearchViewFragment()) {
                        return getParentSearchViewFragment().mRarityDialog;
                    }
                    return DontShowDialog();
                }
                default: {
                    savedInstanceState.putInt("id", mDialogId);
                    return super.onCreateDialog(savedInstanceState);
                }
            }
        } catch (NullPointerException e) {
            /* if the db failed to open, these arrays will be null. */
            if (null != getParentSearchViewFragment()) {
                getParentSearchViewFragment().handleFamiliarDbException(false);
            }
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
        int[] tmp = new int[which.length];
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
        Integer[] tmp = new Integer[which.length];
        for (int i = 0; i < which.length; i++) {
            tmp[i] = which[i];
        }
        return tmp;
    }
}