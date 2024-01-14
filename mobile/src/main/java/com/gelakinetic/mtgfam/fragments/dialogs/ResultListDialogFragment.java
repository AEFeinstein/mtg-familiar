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
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.SnackbarWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.gelakinetic.mtgfam.helpers.database.FamiliarDbException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class ResultListDialogFragment extends FamiliarDialogFragment {

    public static final int DIALOG_SORT = 0;
    public static final int QUICK_ADD = 1;
    private static final int PICK_DECK = 2;
    public static final String NAME_KEY = "cardname";
    public static final String NAME_SET = "cardset";

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!canCreateDialog()) {
            return DontShowDialog();
        }

        mDialogId = requireArguments().getInt(ID_KEY);

        if (null == getParentResultListFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case QUICK_ADD: {
                final String cardName = getArguments().getString(NAME_KEY);
                final String cardSet = getArguments().getString(NAME_SET);
                return new AlertDialog.Builder(this.requireActivity())
                        .setTitle(Objects.requireNonNull(cardName))
                        .setPositiveButton(R.string.result_list_Add_to_wishlist, (dialog, which) -> WishlistHelpers.addItemToWishlist(getActivity(),
                                new WishlistHelpers.CompressedWishlistInfo(
                                        new MtgCard(cardName, cardSet, "", false, 1, false), 0)))
                        .setNegativeButton(R.string.result_list_Add_to_decklist, (dialog, which) -> {
                            // Show the dialog to pick a deck
                            if (null != getParentResultListFragment()) {
                                getParentResultListFragment().showDialog(PICK_DECK, cardName, cardSet);
                            }
                        })
                        .create();
            }
            case PICK_DECK: {
                final String cardName = getArguments().getString(NAME_KEY);
                final String cardSet = getArguments().getString(NAME_SET);
                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* If there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    SnackbarWrapper.makeAndShowText(this.getActivity(), R.string.decklist_toast_no_decks,
                            SnackbarWrapper.LENGTH_LONG);
                    return DontShowDialog();
                }

                /* Sort alphabetically for convenience */
                Arrays.sort(deckNames, String.CASE_INSENSITIVE_ORDER);

                return new AlertDialog.Builder(this.requireActivity())
                        .setTitle(R.string.decklist_select_dialog_title)
                        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
                        .setItems(deckNames, (dialog, which) -> {

                            try {
                                // Read the decklist
                                String deckFileName = deckNames[which] + DecklistFragment.DECK_EXTENSION;
                                ArrayList<MtgCard> decklist =
                                        DecklistHelpers.ReadDecklist(getActivity(), deckFileName, false);

                                // Look through the decklist for any existing matches
                                boolean entryIncremented = false;
                                for (MtgCard deckEntry : decklist) {
                                    if (!deckEntry.isSideboard() && // not in the sideboard
                                            deckEntry.getName().equals(cardName) &&
                                            deckEntry.getExpansion().equals(cardSet)) {
                                        // Increment the card already in the deck
                                        deckEntry.mNumberOf++;
                                        entryIncremented = true;
                                        break;
                                    }
                                }
                                if (!entryIncremented) {
                                    // Add a new card to the deck
                                    decklist.add(new MtgCard(cardName, cardSet, "", false, 1, false));
                                }

                                // Write the decklist back
                                DecklistHelpers.WriteDecklist(getActivity(), decklist, deckFileName);
                            } catch (FamiliarDbException e) {
                                getParentResultListFragment().handleFamiliarDbException(false);
                            }
                        })
                        .create();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }

    @Nullable
    private ResultListFragment getParentResultListFragment() {
        try {
            return (ResultListFragment) getParentFamiliarFragment();
        } catch (ClassCastException e) {
            return null;
        }
    }
}
