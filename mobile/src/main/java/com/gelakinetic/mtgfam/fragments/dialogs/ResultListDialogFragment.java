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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.DecklistFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.DecklistHelpers;
import com.gelakinetic.mtgfam.helpers.MtgCard;
import com.gelakinetic.mtgfam.helpers.ToastWrapper;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

import java.util.ArrayList;

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
            setShowsDialog(false);
            return DontShowDialog();
        }

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);

        if (null == getParentResultListFragment()) {
            return DontShowDialog();
        }

        switch (mDialogId) {
            case QUICK_ADD: {
                final String cardName = getArguments().getString(NAME_KEY);
                final String cardSet = getArguments().getString(NAME_SET);
                return new MaterialDialog.Builder(this.getActivity())
                        .stackingBehavior(StackingBehavior.ALWAYS)
                        .title(cardName)
                        .positiveText(R.string.result_list_Add_to_wishlist)
                        .onPositive((dialog, which) -> WishlistHelpers.addItemToWishlist(getContext(),
                                new WishlistHelpers.CompressedWishlistInfo(
                                        CardHelpers.makeMtgCard(getContext(), cardName, cardSet, false, 1), 0)))
                        .negativeText(R.string.result_list_Add_to_decklist)
                        .onNegative((dialog, which) -> {
                            // Show the dialog to pick a deck
                            if (null != getParentResultListFragment()) {
                                getParentResultListFragment().showDialog(PICK_DECK, cardName, cardSet);
                            }
                        })
                        .build();
            }
            case PICK_DECK: {
                final String cardName = getArguments().getString(NAME_KEY);
                final String cardSet = getArguments().getString(NAME_SET);
                final String[] deckNames = getFiles(DecklistFragment.DECK_EXTENSION);

                /* If there are no files, don't show the dialog */
                if (deckNames.length == 0) {
                    ToastWrapper.makeAndShowText(this.getActivity(), R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG);
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback((dialog, itemView, position, text) -> {

                            // Read the decklist
                            String deckFileName = deckNames[position] + DecklistFragment.DECK_EXTENSION;
                            ArrayList<Pair<MtgCard, Boolean>> decklist =
                                    DecklistHelpers.ReadDecklist(getContext(), deckFileName);

                            // Look through the decklist for any existing matches
                            boolean entryIncremented = false;
                            for (Pair<MtgCard, Boolean> deckEntry : decklist) {
                                if (!deckEntry.second && // not in the sideboard
                                        deckEntry.first.mName.equals(cardName) &&
                                        deckEntry.first.mExpansion.equals(cardSet)) {
                                    // Increment the card already in the deck
                                    deckEntry.first.mNumberOf++;
                                    entryIncremented = true;
                                    break;
                                }
                            }
                            if (!entryIncremented) {
                                // Add a new card to the deck
                                decklist.add(new Pair<>(CardHelpers.makeMtgCard(getContext(), cardName, cardSet, false, 1), false));
                            }

                            // Write the decklist back
                            DecklistHelpers.WriteDecklist(getContext(), decklist, deckFileName);
                        })
                        .build();
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
