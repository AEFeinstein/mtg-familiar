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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.RulesFragment;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.database.DatabaseManager;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import org.jetbrains.annotations.NotNull;

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

    @NotNull
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

        switch (DIALOG_SEARCH) {
            case DIALOG_SEARCH: {
                        /* Inflate a view to type in the player's name, and show it in an AlertDialog */
                @SuppressLint("InflateParams") View textEntryView = getActivity().getLayoutInflater().inflate(R.layout.alert_dialog_text_entry,
                        null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameInput.setText("");
                    }
                });

                String title;
                if (getParentRulesFragment().mCategory == -1) {
                    title = getString(R.string.rules_search_all);
                } else {
                    try {
                        SQLiteDatabase database = DatabaseManager.getInstance(getActivity(), false).openDatabase(false);
                        title = String.format(getString(R.string.rules_search_cat),
                                CardDbAdapter.getCategoryName(getParentRulesFragment().mCategory, getParentRulesFragment().mSubcategory, database));
                    } catch (FamiliarDbException e) {
                        title = String.format(getString(R.string.rules_search_cat),
                                getString(R.string.rules_this_cat));
                    }
                    DatabaseManager.getInstance(getActivity(), false).closeDatabase(false);
                }

                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(title)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (nameInput.getText() == null) {
                                    dialog.dismiss();
                                    return;
                                }
                                String keyword = nameInput.getText().toString();
                                if (keyword.length() < 3) {
                                    ToastWrapper.makeText(getActivity(),
                                            R.string.rules_short_key_toast, ToastWrapper.LENGTH_LONG).show();
                                } else {
                                    searchArgs = new Bundle();
                                    searchArgs.putString(RulesFragment.KEYWORD_KEY, keyword);
                                    searchArgs.putInt(RulesFragment.CATEGORY_KEY, getParentRulesFragment().mCategory);
                                    searchArgs.putInt(RulesFragment.SUBCATEGORY_KEY, getParentRulesFragment().mSubcategory);
                                }
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            default: {
                savedInstanceState.putInt("id", DIALOG_SEARCH);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}
