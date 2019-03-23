package com.gelakinetic.mtgfam.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.HtmlDocFragment;
import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;

import org.jetbrains.annotations.NotNull;

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
            return (HtmlDocFragment) ((JudgesCornerFragment) Objects.requireNonNull(getParentFamiliarFragment())).getCurrentFragment();
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

        if (null == getParentHtmlDocFragment()) {
            return DontShowDialog();
        }

        switch (DIALOG_SEARCH) {
            case DIALOG_SEARCH: {
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
            default: {
                savedInstanceState.putInt("id", DIALOG_SEARCH);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}
