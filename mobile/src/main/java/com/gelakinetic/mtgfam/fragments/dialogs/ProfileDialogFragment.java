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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.ProfileFragment;
import com.gelakinetic.mtgfam.helpers.PreferenceAdapter;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for ProfileFragment
 */
public class ProfileDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    private static final int DIALOG_DCI_NUMBER = 1;

    /**
     * @return The currently viewed ProfileFragment
     */
    @Nullable
    private ProfileFragment getParentProfileFragment() {
        try {
            return (ProfileFragment) getParentFamiliarFragment();
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

        /* We're setting this to false if we return null, so we should reset it every time to be safe */
        setShowsDialog(true);

        if (null == getParentProfileFragment()) {
            return DontShowDialog();
        }

        switch (DIALOG_DCI_NUMBER) {
            case DIALOG_DCI_NUMBER: {
                @SuppressLint("InflateParams") View view = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.alert_dialog_text_entry, null, false);

                final EditText dciEditText = view.findViewById(R.id.text_entry);
                dciEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                view.findViewById(R.id.clear_button).setOnClickListener(view1 -> dciEditText.setText(""));

                final String strDCI = PreferenceAdapter.getDCINumber(getContext());

                dciEditText.setText(strDCI);

                return new MaterialDialog.Builder(getActivity())
                        .title(R.string.profile_update_dci_dialog_title)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog, which) -> {
                            String strNumber = dciEditText.getText().toString();

                            if (strNumber.isEmpty()) {
                                SnackbarWrapper.makeAndShowText(getActivity(),
                                        R.string.profile_invalid_dci,
                                        SnackbarWrapper.LENGTH_SHORT);

                                return;
                            }

                            PreferenceAdapter.setDCINumber(getContext(), strNumber);
                            if (null != getParentProfileFragment()) {
                                getParentProfileFragment().mDCINumber = strNumber;
                                getParentProfileFragment().checkDCINumber();
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .onNegative((dialog, which) -> {
                            if (null != getParentProfileFragment()) {
                                getParentProfileFragment().checkDCINumber();
                            }
                        })
                        .customView(view, false)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}