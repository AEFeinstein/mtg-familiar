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
import androidx.appcompat.app.AlertDialog;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;

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
            return DontShowDialog();
        }

        mDialogId = requireArguments().getInt(ID_KEY);

        if (null == getParentSearchViewFragment()) {
            return DontShowDialog();
        }

        try {
            /* Build the dialogs to display format and rarity choices. The arrays were
                already filled in onCreate() */
            switch (mDialogId) {
                case FORMAT_LIST: {
                    getParentSearchViewFragment().mFormatDialog = new AlertDialog.Builder(this.requireActivity())
                            .setTitle(R.string.search_formats)
                            .setSingleChoiceItems(getParentSearchViewFragment().mFormatNames, getParentSearchViewFragment().mSelectedFormat,
                                    (dialog, which) -> getParentSearchViewFragment().mSelectedFormat = which)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                            .setNegativeButton(R.string.mana_pool_clear_all, (dialog, which) -> {
                                getParentSearchViewFragment().mSelectedFormat = -1;
                                getParentSearchViewFragment().removeDialog(getParentFragmentManager());
                                getParentSearchViewFragment().checkDialogButtonColors();
                            })
                            .create();
                    if (null != getParentSearchViewFragment()) {
                        return getParentSearchViewFragment().mFormatDialog;
                    }
                    return DontShowDialog();
                }
                case RARITY_LIST: {
                    getParentSearchViewFragment().mRarityDialog = new AlertDialog.Builder(this.requireActivity())
                            .setTitle(R.string.search_rarities)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                            .setMultiChoiceItems(getParentSearchViewFragment().mRarityNames, getParentSearchViewFragment().mRarityNamesChecked,
                                    (dialog, which, isChecked) -> getParentSearchViewFragment().mRarityNamesChecked[which] = isChecked)
                            .create();
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

}