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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.RulesFragment;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbHandle;

import java.util.Objects;

/**
 * Class that creates dialogs for RulesFragment
 */
public class RulesDialogFragment extends FamiliarDialogFragment {

    /* Dialog constant */
    private static final int DIALOG_SEARCH = 1;

    private Bundle searchArgs = null;

    /**
     * @return The currently viewed RulesFragment
     */
    @Nullable
    private RulesFragment getParentRulesFragment() {
        try {
            return (RulesFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchArgs != null && getParentFamiliarFragment() != null) {
            RulesFragment frag = new RulesFragment();
            getParentFamiliarFragment().startNewFragment(frag, searchArgs);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            setShowsDialog(false);
            return DontShowDialog();
        }

        searchArgs = null;

        if (null == getParentRulesFragment()) {
            return DontShowDialog();
        }

        /* Inflate a view to type in the player's name, and show it in an AlertDialog */
        @SuppressLint("InflateParams") View textEntryView = Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.alert_dialog_text_entry,
                null, false);
        assert textEntryView != null;
        final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
        textEntryView.findViewById(R.id.clear_button).setOnClickListener(view -> nameInput.setText(""));

        String title;
        if (getParentRulesFragment().mCategory == -1) {
            title = getString(R.string.rules_search_all);
        } else {
            FamiliarDbHandle handle = new FamiliarDbHandle();
            try {
                SQLiteDatabase database = DatabaseManager.openDatabase(getActivity(), false, handle);
                title = String.format(getString(R.string.rules_search_cat),
                        CardDbAdapter.getCategoryName(getParentRulesFragment().mCategory, getParentRulesFragment().mSubcategory, database));
            } catch (SQLiteException | FamiliarDbException e) {
                title = String.format(getString(R.string.rules_search_cat),
                        getString(R.string.rules_this_cat));
            } finally {
                DatabaseManager.closeDatabase(getActivity(), handle);
            }
        }

        Dialog dialog = new MaterialDialog.Builder(getActivity())
                .title(title)
                .customView(textEntryView, false)
                .positiveText(R.string.dialog_ok)
                .onPositive((dialog1, which) -> {
                    if (nameInput.getText() == null) {
                        dialog1.dismiss();
                        return;
                    }
                    String keyword = nameInput.getText().toString();
                    if (keyword.length() < 3) {
                        SnackbarWrapper.makeAndShowText(getActivity(),
                                R.string.rules_short_key_toast, SnackbarWrapper.LENGTH_LONG);
                    } else {
                        searchArgs = new Bundle();
                        searchArgs.putString(RulesFragment.KEYWORD_KEY, keyword);
                        searchArgs.putInt(RulesFragment.CATEGORY_KEY, getParentRulesFragment().mCategory);
                        searchArgs.putInt(RulesFragment.SUBCATEGORY_KEY, getParentRulesFragment().mSubcategory);
                    }
                })
                .negativeText(R.string.dialog_cancel)
                .build();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
