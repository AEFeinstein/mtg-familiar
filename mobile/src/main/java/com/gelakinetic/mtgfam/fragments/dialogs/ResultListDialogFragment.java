package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.helpers.CardHelpers;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

/**
 * Class that creates dialogs for ResultListFragment
 */
public class ResultListDialogFragment extends FamiliarDialogFragment {

    public static final int DIALOG_SORT = 0;
    public static final int QUICK_ADD = 1;
    public static final String NAME_KEY = "cardname";
    public static final String NAME_SET = "cardset";

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        /* This will be set to false if we are returning a null dialog. It prevents a crash */
        setShowsDialog(true);

        mDialogId = getArguments().getInt(ID_KEY);
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
                                // TODO implement
                            }
                        })
                        .build();
            }
            default: {
                return DontShowDialog();
            }
        }
    }

    private ResultListFragment getResultListFragment() {
        return (ResultListFragment) getFamiliarFragment();
    }
}
