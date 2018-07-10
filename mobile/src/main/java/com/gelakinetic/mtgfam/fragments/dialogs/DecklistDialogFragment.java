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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Class that creates dialogs for DecklistFragment.
 */
public class DecklistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_SAVE_DECK_AS = 2;
    public static final int DIALOG_LOAD_DECK = 3;
    public static final int DIALOG_DELETE_DECK = 4;
    public static final int DIALOG_CONFIRMATION = 5;
    public static final int DIALOG_GET_LEGALITY = 6;

    public static final String NAME_KEY = "name";
    public static final String SIDE_KEY = "side";

    /**
     * @return The currently viewed DecklistFragment
     */
    @Nullable
    private DecklistFragment getParentDecklistFragment() {
        try {
            return (DecklistFragment) getParentFamiliarFragment();
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

        setShowsDialog(true);
        mDialogId = getArguments().getInt(ID_KEY);
        final String cardName = getArguments().getString(NAME_KEY);
        final boolean isSideboard = getArguments().getBoolean(SIDE_KEY);

        if (null == getParentDecklistFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                Dialog dialog = CardHelpers.getDialog(
                        cardName,
                        getParentDecklistFragment(),
                        true,
                        isSideboard
                );
                if (dialog == null) {
                    if (null != getParentDecklistFragment()) {
                        getParentDecklistFragment().handleFamiliarDbException(false);
                    }
                    return DontShowDialog();
                }
                return dialog;
            }
            case DIALOG_SAVE_DECK_AS: {
                /* Inflate a view to type in the deck's name and show it in an AlertDialog */
                @SuppressLint("InflateParams") View textEntryView = getActivity().getLayoutInflater()
                        .inflate(R.layout.alert_dialog_text_entry, null, false);
                assert textEntryView != null;
                final EditText nameInput = textEntryView.findViewById(R.id.text_entry);
                nameInput.append(getParentDecklistFragment().mCurrentDeck);
                /* Set the button to clear the text field */
                textEntryView.findViewById(R.id.clear_button).setOnClickListener(
                        v -> nameInput.setText(""));
                Dialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.decklist_save)
                        .customView(textEntryView, false)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog1, which) -> {

                            if (nameInput.getText() == null) {
                                return;
                            }
                            String deckName = nameInput.getText().toString();
                            /* Don't save if there is not a name */
                            if (deckName.length() == 0 || deckName.equals("")) {
                                return;
                            }
                            synchronized (getParentDecklistFragment().mCompressedDecklist) {
                                DecklistHelpers.WriteCompressedDecklist(
                                        getActivity(),
                                        getParentDecklistFragment().mCompressedDecklist,
                                        deckName + DecklistFragment.DECK_EXTENSION
                                );
                            }
                            getParentDecklistFragment().mCurrentDeck = deckName;
                            getParentDecklistFragment().mDeckName.setText(deckName);

                        })
                        .negativeText(R.string.dialog_cancel)
                        .build();
                dialog.getWindow()
                        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dialog;
            }
            case DIALOG_LOAD_DECK: {

                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* If there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.decklist_toast_no_decks,
                            SnackbarWrapper.LENGTH_LONG);
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback((dialog, itemView, position, text) -> {

                            /* First save the current deck */
                            synchronized (getParentDecklistFragment().mCompressedDecklist) {
                                DecklistHelpers.WriteCompressedDecklist(
                                        getActivity(),
                                        getParentDecklistFragment().mCompressedDecklist,
                                        getParentDecklistFragment().mCurrentDeck + DecklistFragment.DECK_EXTENSION
                                );
                            }
                            /* Then read the next one */
                            getParentDecklistFragment()
                                    .readAndCompressDecklist(null, deckNames[position]);
                            getParentDecklistFragment().mCurrentDeck = deckNames[position];
                            /* Alert things to update */
                            getParentDecklistFragment().getCardDataAdapter(0).notifyDataSetChanged();

                        })
                        .build();
            }
            case DIALOG_DELETE_DECK: {

                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* if there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    SnackbarWrapper.makeAndShowText(
                            this.getActivity(),
                            R.string.decklist_toast_no_decks,
                            SnackbarWrapper.LENGTH_LONG
                    );
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_delete_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback((dialog, itemView, position, text) -> {

                            File toDelete = new File(getActivity().getFilesDir(),
                                    deckNames[position] + DecklistFragment.DECK_EXTENSION);
                            if (!toDelete.delete()) {
                                SnackbarWrapper.makeAndShowText(
                                        getActivity(),
                                        toDelete.getName() + " "
                                                + getString(R.string.not_deleted),
                                        SnackbarWrapper.LENGTH_LONG
                                );
                            }

                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_clear)
                        .content(R.string.decklist_clear_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive((dialog, which) -> {

                            /* do some cleaning up */
                            getParentDecklistFragment().mCurrentDeck = "autosave";
                            synchronized (getParentDecklistFragment().mCompressedDecklist) {
                                getParentDecklistFragment().mCompressedDecklist.clear();
                            }
                            getParentDecklistFragment().getCardDataAdapter(0).notifyDataSetChanged();
                            getParentDecklistFragment().mDeckName.setText(
                                    R.string.decklist_unnamed_deck
                            );
                            getParentDecklistFragment().mDeckCards.setText(getResources().getQuantityString(R.plurals.decklist_cards_count, 0, 0));
                            synchronized (getParentDecklistFragment().mCompressedDecklist) {
                                DecklistHelpers.WriteCompressedDecklist(
                                        getActivity(),
                                        getParentDecklistFragment().mCompressedDecklist,
                                        getParentDecklistFragment().getCurrentDeckName()
                                );
                            }
                            getParentDecklistFragment().clearCardNameInput();
                            getParentDecklistFragment().clearCardNumberInput();
                            getParentDecklistFragment().uncheckFoilCheckbox();
                            dialog.dismiss();

                        })
                        .negativeText(R.string.dialog_cancel)
                        .cancelable(true)
                        .build();
            }
            case DIALOG_GET_LEGALITY: {
                if (null == getParentDecklistFragment() || getParentDecklistFragment().legalityMap.isEmpty()) {
                    return DontShowDialog();
                }

                SimpleAdapter adapter = new SimpleAdapter(
                        getActivity(), getParentDecklistFragment().legalityMap, R.layout.card_view_legal_row,
                        DecklistFragment.LEGALITY_DIAOG_FROM, DecklistFragment.LEGALITY_DIALOG_TO);
                ListView lv = new ListView(getActivity());
                lv.setAdapter(adapter);
                return new MaterialDialog.Builder(getActivity())
                        .customView(lv, false)
                        .title(R.string.decklist_legality)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

}
