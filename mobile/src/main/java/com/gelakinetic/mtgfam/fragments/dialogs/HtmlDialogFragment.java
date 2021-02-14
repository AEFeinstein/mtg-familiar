/*
 * Copyright 2019 Adam Feinstein
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
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.HtmlDocFragment;
import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;

import java.util.Objects;

public class HtmlDialogFragment extends FamiliarDialogFragment {

    /* Dialog constant */
    private static final int DIALOG_SEARCH = 1;

    /**
     * @return The currently viewed HtmlDocFragment
     */
    @Nullable
    private HtmlDocFragment getParentHtmlDocFragment() {
        try {
            JudgesCornerFragment pagerFrag = ((JudgesCornerFragment) getParentFamiliarFragment());
            if (null != pagerFrag) {
                return (HtmlDocFragment) pagerFrag.getCurrentFragment();
            }
        } catch (ClassCastException e) {
            return null;
        }
        return null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        if (null == getParentHtmlDocFragment()) {
            return DontShowDialog();
        }

        /* Inflate a view to type in the player's name, and show it in an AlertDialog */
        @SuppressLint("InflateParams") View textEntryView = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.alert_dialog_text_entry,
                null, false);
        assert textEntryView != null;
        final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
        textEntryView.findViewById(R.id.clear_button).setOnClickListener(view -> nameInput.setText(""));
        nameInput.setText(getParentHtmlDocFragment().getLastSearchTerm());

        String title = String.format(getString(R.string.rules_search_cat),
                getParentHtmlDocFragment().getName());

        Dialog dialog = new MaterialDialog.Builder(getActivity())
                .title(title)
                .customView(textEntryView, false)
                .positiveText(R.string.dialog_ok)
                .onPositive((dialog1, which) -> getParentHtmlDocFragment().doSearch(nameInput.getText().toString()))
                .negativeText(R.string.dialog_cancel)
                .onNegative((dialog1, which) -> getParentHtmlDocFragment().cancelSearch())
                .build();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
