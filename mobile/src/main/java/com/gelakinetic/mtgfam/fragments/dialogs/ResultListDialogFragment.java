package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
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
        super.onCreateDialog(savedInstanceState);

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
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                WishlistHelpers.addItemToWishlist(getContext(),
                                        new WishlistHelpers.CompressedWishlistInfo(
                                                CardHelpers.makeMtgCard(getContext(), cardName, cardSet, false, 1), 0));
                            }
                        })
                        .negativeText(R.string.result_list_Add_to_decklist)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                // Show the dialog to pick a deck
                                if (null != getParentResultListFragment()) {
                                    getParentResultListFragment().showDialog(PICK_DECK, cardName, cardSet);
                                }
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
                    ToastWrapper.makeText(this.getActivity(), R.string.decklist_toast_no_decks,
                            ToastWrapper.LENGTH_LONG).show();
                    return DontShowDialog();
                }

                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.decklist_select_dialog_title)
                        .negativeText(R.string.dialog_cancel)
                        .items((CharSequence[]) deckNames)
                        .itemsCallback(new MaterialDialog.ListCallback() {

                            @Override
                            public void onSelection(
                                    MaterialDialog dialog,
                                    View itemView,
                                    int position,
                                    CharSequence text) {

                                // Read the decklist
                                String deckFileName = deckNames[position] + DecklistFragment.DECK_EXTENSION;
                                ArrayList<Pair<MtgCard, Boolean>> decklist =
                                        DecklistHelpers.ReadDecklist(getContext(), deckFileName);

                                // Look through the decklist for any existing matches
                                boolean entryIncremented = false;
                                for (Pair<MtgCard, Boolean> deckEntry : decklist) {
                                    if (!deckEntry.second && // not in the sideboard
                                            deckEntry.first.mName.equals(cardName) &&
                                            deckEntry.first.setCode.equals(cardSet)) {
                                        // Increment the card already in the deck
                                        deckEntry.first.numberOf++;
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
                            }

                        })
                        .build();
            }
            default: {
                return DontShowDialog();
            }
        }
    }

    @Nullable
    private ResultListFragment getParentResultListFragment() {
        return (ResultListFragment) getParentFamiliarFragment();
    }
}
