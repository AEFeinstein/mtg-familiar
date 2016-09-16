package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

import org.jetbrains.annotations.NotNull;

/**
 * Class that creates dialogs for WishlistFragment
 */
public class WishlistDialogFragment extends FamiliarDialogFragment {

    /* Dialog constants */
    public static final int DIALOG_UPDATE_CARD = 1;
    public static final int DIALOG_PRICE_SETTING = 2;
    public static final int DIALOG_CONFIRMATION = 3;
    public static final int DIALOG_SORT = 4;

    public static final String NAME_KEY = "name";

    /**
     * @return The currently viewed DiceFragment
     */
    private WishlistFragment getParentWishlistFragment() {
        return (WishlistFragment) getFamiliarFragment();
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setShowsDialog(true);
        mDialogId = getArguments().getInt(ID_KEY);
        String cardName = getArguments().getString(NAME_KEY);
        switch (mDialogId) {
            case DIALOG_UPDATE_CARD: {
                Dialog dialog = WishlistHelpers.getDialog(cardName, getParentWishlistFragment(), true);
                if (dialog == null) {
                    getParentWishlistFragment().handleFamiliarDbException(false);
                    return DontShowDialog();
                }
                return dialog;
            }
            case DIALOG_PRICE_SETTING: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.trader_pricing_dialog_title)
                        .items(new String[]{getString(R.string.trader_Low),
                                getString(R.string.trader_Average), getString(R.string.trader_High)})
                        .itemsCallbackSingleChoice(getParentWishlistFragment().mPriceSetting, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                if (getParentWishlistFragment().mPriceSetting != which) {
                                    getParentWishlistFragment().mPriceSetting = which;
                                    getFamiliarActivity().mPreferenceAdapter.setTradePrice(
                                            String.valueOf(getParentWishlistFragment().mPriceSetting));
                                    getParentWishlistFragment().mWishlistAdapter.notifyDataSetChanged();
                                    getParentWishlistFragment().sumTotalPrice();
                                }
                                dialog.dismiss();
                                return true;
                            }
                        })
                        .build();
            }
            case DIALOG_CONFIRMATION: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.wishlist_empty_dialog_title)
                        .content(R.string.wishlist_empty_dialog_text)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                WishlistHelpers.ResetCards(getActivity());
                                getParentWishlistFragment().mCompressedWishlist.clear();
                                getParentWishlistFragment().mWishlistAdapter.notifyDataSetChanged();
                                getParentWishlistFragment().sumTotalPrice();
                                /* Clear input too */
                                getParentWishlistFragment().mNameField.setText("");
                                getParentWishlistFragment().mNumberField.setText("1");
                                getParentWishlistFragment().mCheckboxFoil.setChecked(false);
                                dialog.dismiss();
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .cancelable(true)
                        .build();

            }
            case DIALOG_SORT: {
                return new MaterialDialog.Builder(this.getActivity())
                        .title(R.string.wishlist_sort_by)
                        .items(R.array.wishlist_sort_type)
                        .itemsCallbackSingleChoice(getParentWishlistFragment().mWishlistSortType, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                getParentWishlistFragment().mWishlistSortType = which;
                                return true;
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .neutralText(R.string.wishlist_ascending)
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                getParentWishlistFragment().mWishlistSortOrder = WishlistFragment.ASCENDING;
                                getParentWishlistFragment().sortWishlist();
                            }
                        })
                        .positiveText(R.string.wishlist_descending)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                getParentWishlistFragment().mWishlistSortOrder = WishlistFragment.DESCENDING;
                                getParentWishlistFragment().sortWishlist();
                            }
                        })
                        .alwaysCallSingleChoiceCallback()
                        .cancelable(true)
                        .build();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}