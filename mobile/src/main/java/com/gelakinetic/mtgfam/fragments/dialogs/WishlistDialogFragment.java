package com.gelakinetic.mtgfam.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ListView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Adam on 4/24/2016.
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
    WishlistFragment getParentWishlistFragment() {
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
                return new AlertDialogWrapper.Builder(this.getActivity())
                        .setTitle(R.string.trader_pricing_dialog_title)
                        .setSingleChoiceItems(new String[]{getString(R.string.trader_Low),
                                        getString(R.string.trader_Average), getString(R.string.trader_High)},
                                getParentWishlistFragment().mPriceSetting,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (getParentWishlistFragment().mPriceSetting != which) {
                                            getParentWishlistFragment().mPriceSetting = which;
                                            getFamiliarActivity().mPreferenceAdapter.setTradePrice(
                                                    String.valueOf(getParentWishlistFragment().mPriceSetting));
                                            getParentWishlistFragment().mWishlistAdapter.notifyDataSetChanged();
                                            getParentWishlistFragment().sumTotalPrice();
                                        }
                                        dialog.dismiss();
                                    }
                                }
                        )
                        .create();
            }
            case DIALOG_CONFIRMATION: {
                return new AlertDialogWrapper.Builder(this.getActivity())
                        .setTitle(R.string.wishlist_empty_dialog_title)
                        .setMessage(R.string.wishlist_empty_dialog_text)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                WishlistHelpers.ResetCards(getActivity());
                                getParentWishlistFragment().mCompressedWishlist.clear();
                                getParentWishlistFragment().mWishlistAdapter.notifyDataSetChanged();
                                getParentWishlistFragment().sumTotalPrice();
                                dialog.dismiss();
                                        /* Clear input too */
                                getParentWishlistFragment().mNameField.setText("");
                                getParentWishlistFragment().mNumberField.setText("1");
                                getParentWishlistFragment().mCheckboxFoil.setChecked(false);
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true).create();

            }
            case DIALOG_SORT: {
                return new AlertDialogWrapper.Builder(this.getActivity())
                        .setTitle(R.string.wishlist_sort_by)
                        .setSingleChoiceItems(R.array.wishlist_sort_type, getParentWishlistFragment().mWishlistSortType, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                        /* If this listener is null, the dialog crashes */
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.wishlist_ascending, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                getParentWishlistFragment().mWishlistSortOrder = WishlistFragment.ASCENDING;
                                ListView lw = ((MaterialDialog) dialog).getListView();
                                getParentWishlistFragment().mWishlistSortType = lw.getCheckedItemPosition();
                                getParentWishlistFragment().sortWishlist();
                            }
                        })
                        .setPositiveButton(R.string.wishlist_descending, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                getParentWishlistFragment().mWishlistSortOrder = WishlistFragment.DESCENDING;
                                ListView lw = ((MaterialDialog) dialog).getListView();
                                getParentWishlistFragment().mWishlistSortType = lw.getCheckedItemPosition();
                                getParentWishlistFragment().sortWishlist();
                            }
                        })
                        .setCancelable(true).create();
            }
            default: {
                savedInstanceState.putInt("id", mDialogId);
                return super.onCreateDialog(savedInstanceState);
            }
        }
    }
}